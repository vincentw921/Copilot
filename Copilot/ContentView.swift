//
//  ContentView.swift
//  Copilot
//
//  The app's main tab bar, shown once the user is signed into iCloud.
//

import SwiftUI

/// Top-level tab navigation: Home, Logbook, Calendar, Report, Profile.
struct ContentView: View {
    var body: some View {
        TabView {
            NavigationStack {
                HomeView()
            }
            .tabItem {
                Label("Home", systemImage: "house")
            }

            NavigationStack {
                LogbookView()
            }
            .tabItem {
                Label("Logbook", systemImage: "book")
            }

            NavigationStack {
                CalendarView()
            }
            .tabItem {
                Label("Calendar", systemImage: "calendar")
            }

            NavigationStack {
                ReportView()
            }
            .tabItem {
                Label("Report", systemImage: "list.clipboard")
            }

            NavigationStack {
                ProfileView()
            }
            .tabItem {
                Label("Profile", systemImage: "person")
            }
        }
    }
}

#Preview {
    ContentView()
        .environmentObject(AuthModel())
        .environment(\.managedObjectContext, PersistenceController.preview.container.viewContext)
}
