//
//  CopilotApp.swift
//  Copilot
//
//  Created by Vincent Wang on 9/14/25.
//

import SwiftUI

@main
struct CopilotApp: App {
    let persistenceController = PersistenceController.shared

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(\.managedObjectContext, persistenceController.container.viewContext)
        }
    }
}
