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
/// circle; the selected day gets a ring. Chevrons move between months.
struct MonthCalendarView: View {
    /// The day the user has tapped.
    @Binding var selectedDate: Date
    /// Start-of-day dates to paint green (days with flights).
    let highlightedDays: Set<Date>

    /// First day of the month currently on screen.
    @State private var displayedMonth = MonthGrid.startOfMonth(for: Date())
    /// True while the month/year dropdowns replace the weekday header.
    @State private var showMonthYearPicker = false

    private let calendar = Calendar.current
    private let columns = Array(repeating: GridItem(.flexible()), count: 7)

    var body: some View {
        VStack(spacing: 12) {
            header
            if showMonthYearPicker {
                monthYearPickers
            }
            weekdayLabels
            dayGrid
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
            Button {
                displayedMonth = MonthGrid.month(displayedMonth, offsetBy: -1)
            } label: {
                Image(systemName: "chevron.left")
            }
            Spacer()
            // Tapping the title reveals dropdowns to jump straight to any
            // month and year.
            Button {
                withAnimation { showMonthYearPicker.toggle() }
            } label: {
                HStack(spacing: 4) {
                    Text(displayedMonth, format: .dateTime.month(.wide).year())
                        .font(.headline)
                    Image(systemName: "chevron.down")
                        .font(.caption.bold())
                        .rotationEffect(showMonthYearPicker ? .degrees(180) : .zero)
                }
            }
            .foregroundStyle(.primary)
            Spacer()
            Button {
                displayedMonth = MonthGrid.month(displayedMonth, offsetBy: 1)
            } label: {
                Image(systemName: "chevron.right")
            }
            .disabled(MonthGrid.month(displayedMonth, offsetBy: 1) > today)
        }
        .buttonStyle(.borderless)
    }

    /// Dropdown menus for jumping to a specific month and year.
    private var monthYearPickers: some View {
        HStack {
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
        .pickerStyle(.menu)
    }

    /// The current year back through 60 years of logbook history.
    private var selectableYears: [Int] {
        let currentYear = calendar.component(.year, from: Date())
        return Array((currentYear - 60...currentYear).reversed())
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
    /// selected. Future days are greyed out and can't be tapped.
    private func dayCell(for day: Date) -> some View {
        let hasFlight = highlightedDays.contains(calendar.startOfDay(for: day))
        let isSelected = calendar.isDate(day, inSameDayAs: selectedDate)
        let isFuture = calendar.startOfDay(for: day) > today

        return Button {
            selectedDate = day
        } label: {
            Text(day, format: .dateTime.day())
                .font(.callout)
                .monospacedDigit()
                .fontWeight(hasFlight ? .semibold : .regular)
                .foregroundStyle(
                    isFuture ? AnyShapeStyle(.tertiary)
                        : hasFlight ? AnyShapeStyle(.white)
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
