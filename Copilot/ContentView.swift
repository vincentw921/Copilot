//
//  ContentView.swift
//  Copilot
//
//  Created by Vincent Wang on 9/14/25.
//

import SwiftUI
import CoreData

struct ContentView: View {
    var body: some View {
        TabView {
            NavigationStack {
                HomeView()
            }
            .tabItem {
                Image(systemName: "house")
            }
            
            NavigationStack {
                LogbookView()
            }
            .tabItem {
                Image(systemName: "book")
            }
            
            NavigationStack {
                CalendarView()
            }
            .tabItem {
                Image(systemName: "calendar")
            }
            
            NavigationStack {
                ReportView()
            }
            .tabItem {
                Image(systemName: "list.clipboard")
            }
            
            NavigationStack {
                ProfileView()
            }
            .tabItem {
                Image(systemName: "person")
            }
        }
    }
}

#Preview {
    ContentView()
}
