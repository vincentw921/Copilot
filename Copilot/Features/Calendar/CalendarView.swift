//
//  CalendarView.swift
//  Copilot
//
//  Browse the logbook by date. Days with logged flights are highlighted
//  green; tap a day to list (and edit) that day's flights.
//

import SwiftUI
import CoreData

/// Date-picker view of the logbook with flight days highlighted.
struct CalendarView: View {
    @FetchRequest(
        sortDescriptors: [NSSortDescriptor(keyPath: \Item.date, ascending: false)],
        animation: .default
    ) private var items: FetchedResults<Item>

    /// The day whose flights are listed below the calendar.
    @State private var selectedDate = Date()
    /// Entry currently open in the read-only preview sheet.
    @State private var viewingItem: Item?

    /// Start-of-day for every date that has at least one flight.
    private var flightDays: Set<Date> {
        Set(items.compactMap { item in
            item.date.map { Calendar.current.startOfDay(for: $0) }
        })
    }

    /// Flights logged on the selected calendar day.
    private var flightsOnSelectedDay: [Item] {
        items.filter { item in
            guard let date = item.date else { return false }
            return Calendar.current.isDate(date, inSameDayAs: selectedDate)
        }
    }

    var body: some View {
        List {
            Section {
                MonthCalendarView(selectedDate: $selectedDate, highlightedDays: flightDays)
            }

            Section(selectedDate.formatted(date: .abbreviated, time: .omitted)) {
                if flightsOnSelectedDay.isEmpty {
                    Text("No flights on this day.")
                        .foregroundStyle(.secondary)
                } else {
                    ForEach(flightsOnSelectedDay) { item in
                        Button {
                            viewingItem = item
                        } label: {
                            FlightRow(item: item)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
        .navigationTitle("Calendar")
        .sheet(item: $viewingItem) { item in
            FlightDetailView(item: item)
        }
    }
}

/// A single-month calendar grid. Days in `highlightedDays` get a green
/// circle; the selected day gets a ring; today's number is tinted.
/// Chevrons move between months.
struct MonthCalendarView: View {
    /// The day the user has tapped.
    @Binding var selectedDate: Date
    /// Start-of-day dates to paint green (days with flights).
    let highlightedDays: Set<Date>
    /// Called after every day tap — including a tap on the day that is
    /// already selected — so hosts (like the flight form) can collapse
    /// the calendar.
    var onDaySelected: (() -> Void)?

    /// First day of the month currently on screen.
    @State private var displayedMonth: Date
    /// True while the month/year wheels replace the day grid, matching
    /// the system date picker's behavior in the flight form.
    @State private var showMonthYearPicker = false

    init(
        selectedDate: Binding<Date>,
        highlightedDays: Set<Date>,
        onDaySelected: (() -> Void)? = nil
    ) {
        _selectedDate = selectedDate
        self.highlightedDays = highlightedDays
        self.onDaySelected = onDaySelected
        // Open on the month of the current selection (today for the
        // Calendar tab, the flight's date when editing an old entry).
        _displayedMonth = State(initialValue: MonthGrid.startOfMonth(for: selectedDate.wrappedValue))
    }

    private let calendar = Calendar.current
    private let columns = Array(repeating: GridItem(.flexible()), count: 7)

    var body: some View {
        VStack(spacing: 12) {
            header
            if showMonthYearPicker {
                monthYearWheels
            } else {
                weekdayLabels
                dayGrid
            }
        }
        .padding(.vertical, 4)
    }

    /// Start of today; days after this are blocked out.
    private var today: Date { calendar.startOfDay(for: Date()) }

    /// Month title with previous/next month buttons. The next-month
    /// button stops at the current month since future days can't be
    /// selected anyway.
    private var header: some View {
        HStack {
            // Tapping the title swaps the day grid for month/year wheels,
            // just like the system date picker in the flight form.
            Button {
                withAnimation { showMonthYearPicker.toggle() }
            } label: {
                HStack(spacing: 4) {
                    Text(displayedMonth, format: .dateTime.month(.wide).year())
                        .font(.headline)
                    Image(systemName: "chevron.right")
                        .font(.caption.bold())
                        .rotationEffect(showMonthYearPicker ? .degrees(90) : .zero)
                }
            }
            .foregroundStyle(showMonthYearPicker ? AnyShapeStyle(.tint) : AnyShapeStyle(.primary))

            Spacer()

            // The paging chevrons hide while the wheels are up, matching
            // the system picker.
            if !showMonthYearPicker {
                Button {
                    displayedMonth = MonthGrid.month(displayedMonth, offsetBy: -1)
                } label: {
                    Image(systemName: "chevron.left")
                }
                Button {
                    displayedMonth = MonthGrid.month(displayedMonth, offsetBy: 1)
                } label: {
                    Image(systemName: "chevron.right")
                }
                .disabled(MonthGrid.month(displayedMonth, offsetBy: 1) > today)
                .padding(.leading, 16)
            }
        }
        .buttonStyle(.borderless)
    }

    /// Side-by-side month and year wheels shown in place of the day grid,
    /// the same interaction the system date picker uses.
    private var monthYearWheels: some View {
        HStack(spacing: 0) {
            Picker("Month", selection: monthBinding) {
                ForEach(1...12, id: \.self) { month in
                    Text(DateFormatter().monthSymbols[month - 1]).tag(month)
                }
            }
            Picker("Year", selection: yearBinding) {
                ForEach(selectableYears, id: \.self) { year in
                    Text(verbatim: String(year)).tag(year)
                }
            }
        }
        .pickerStyle(.wheel)
        .frame(height: 214)
        .clipped()
    }

    /// The current year back through 60 years of logbook history,
    /// ascending so scrolling down moves toward the present.
    private var selectableYears: [Int] {
        let currentYear = calendar.component(.year, from: Date())
        return Array(currentYear - 60...currentYear)
    }

    /// Month component of the displayed month; setting it jumps the grid.
    private var monthBinding: Binding<Int> {
        Binding(
            get: { calendar.component(.month, from: displayedMonth) },
            set: { setDisplayedMonth(month: $0, year: calendar.component(.year, from: displayedMonth)) }
        )
    }

    /// Year component of the displayed month; setting it jumps the grid.
    private var yearBinding: Binding<Int> {
        Binding(
            get: { calendar.component(.year, from: displayedMonth) },
            set: { setDisplayedMonth(month: calendar.component(.month, from: displayedMonth), year: $0) }
        )
    }

    /// Jumps to the given month/year, clamped to the current month so the
    /// dropdowns can't wander into the blocked-out future.
    private func setDisplayedMonth(month: Int, year: Int) {
        guard let target = calendar.date(from: DateComponents(year: year, month: month)) else { return }
        let currentMonth = MonthGrid.startOfMonth(for: Date(), calendar: calendar)
        displayedMonth = min(target, currentMonth)
    }

    /// Localized single-letter weekday header row.
    private var weekdayLabels: some View {
        LazyVGrid(columns: columns) {
            ForEach(MonthGrid.orderedWeekdaySymbols(for: calendar), id: \.self) { symbol in
                Text(symbol)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
        }
    }

    /// The 7-column day grid, with nil cells padding the first week.
    private var dayGrid: some View {
        LazyVGrid(columns: columns, spacing: 6) {
            ForEach(Array(MonthGrid.gridDays(for: displayedMonth, calendar: calendar).enumerated()),
                    id: \.offset) { _, day in
                if let day {
                    dayCell(for: day)
                } else {
                    Color.clear.frame(height: 36)
                }
            }
        }
    }

    /// One tappable day: green fill when a flight exists, ring when
    /// selected, tinted number for today (matching the system date
    /// picker). Future days are greyed out and can't be tapped.
    private func dayCell(for day: Date) -> some View {
        let hasFlight = highlightedDays.contains(calendar.startOfDay(for: day))
        let isSelected = calendar.isDate(day, inSameDayAs: selectedDate)
        let isFuture = calendar.startOfDay(for: day) > today
        let isToday = calendar.isDateInToday(day)

        return Button {
            selectedDate = day
            onDaySelected?()
        } label: {
            Text(day, format: .dateTime.day())
                .font(.callout)
                .monospacedDigit()
                .fontWeight(hasFlight || isToday ? .semibold : .regular)
                .foregroundStyle(
                    isFuture ? AnyShapeStyle(.tertiary)
                        : hasFlight ? AnyShapeStyle(.white)
                        : isToday ? AnyShapeStyle(.tint)
                        : AnyShapeStyle(.primary)
                )
                .frame(width: 36, height: 36)
                .background {
                    if hasFlight {
                        Circle().fill(.green)
                    }
                }
                .overlay {
                    if isSelected {
                        Circle().strokeBorder(.tint, lineWidth: 2)
                    }
                }
        }
        .buttonStyle(.plain)
        .disabled(isFuture)
    }
}

/// Pure date math behind `MonthCalendarView`, kept separate for unit testing.
enum MonthGrid {
    /// Midnight on the first day of `date`'s month.
    static func startOfMonth(for date: Date, calendar: Calendar = .current) -> Date {
        calendar.date(from: calendar.dateComponents([.year, .month], from: date)) ?? date
    }

    /// The first of the month `offset` months away from `month`.
    static func month(_ month: Date, offsetBy offset: Int, calendar: Calendar = .current) -> Date {
        calendar.date(byAdding: .month, value: offset, to: startOfMonth(for: month, calendar: calendar))
            ?? month
    }

    /// Weekday symbols reordered to start at the calendar's first weekday.
    static func orderedWeekdaySymbols(for calendar: Calendar) -> [String] {
        let symbols = calendar.veryShortWeekdaySymbols
        let first = calendar.firstWeekday - 1
        return Array(symbols[first...] + symbols[..<first])
    }

    /// Every cell of the month grid in order: nil for the blank cells
    /// before day 1, then one date per day of the month.
    static func gridDays(for month: Date, calendar: Calendar = .current) -> [Date?] {
        let start = startOfMonth(for: month, calendar: calendar)
        guard let dayCount = calendar.range(of: .day, in: .month, for: start)?.count else {
            return []
        }

        let firstWeekday = calendar.component(.weekday, from: start)
        let leadingBlanks = (firstWeekday - calendar.firstWeekday + 7) % 7

        var cells: [Date?] = Array(repeating: nil, count: leadingBlanks)
        for day in 0..<dayCount {
            cells.append(calendar.date(byAdding: .day, value: day, to: start))
        }
        return cells
    }
}

#Preview {
    NavigationStack {
        CalendarView()
    }
    .environment(\.managedObjectContext, PersistenceController.preview.container.viewContext)
}
