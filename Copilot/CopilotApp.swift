//
//  CopilotApp.swift
//  Copilot
//
//  Created by Vincent Wang on 9/14/25.
//

import SwiftUI

struct RootView: View {
    @EnvironmentObject var auth: AuthModel

    var body: some View {
        Group {
            switch auth.state {
            case .idle, .checking:
                ProgressView("Checking iCloud…")

            case .signedOut, .error:
                AuthView()          // <-- dedicated authentication view

            case .signedIn:
                HomeView()          // <-- your real app UI
            }
        }
        .task {
            if case .idle = auth.state {
                await auth.start()  // auto-login on launch
            }
        }
    }
}


@main
struct CopilotApp: App {
    let persistenceController = PersistenceController.shared
    
    @StateObject private var auth = AuthModel()
    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(auth)
                .environment(\.managedObjectContext, persistenceController.container.viewContext)
        }
    }
}
