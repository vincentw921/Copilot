//
//  AuthView.swift
//  Copilot
//
//  Created by Vincent Wang on 9/14/25.
//

import SwiftUI

struct AuthView: View {
    @StateObject var model = AuthModel()

    var body: some View {
        VStack(spacing: 16) {
            Text("CloudKit Login").font(.title3).bold()

            content

            HStack {
                Button("Refresh") { Task { await model.refresh() } }
                if case .signedIn = model.state {
                    Button("Sign Out (Local)") { model.signOutLocally() }
                }
            }
            .buttonStyle(.bordered)
        }
        .padding()
        .task {
            if case .idle = model.state {
                await model.start()     // auto-login on launch
            }
        }
    }

    @ViewBuilder
    private var content: some View {
        switch model.state {
        case .idle, .checking:
            ProgressView("Checking iCloud…")
        case .signedIn(let cloudID):
            VStack(spacing: 8) {
                Text("Signed in to iCloud")
                    .foregroundStyle(.secondary)
                Text(cloudID)
                    .font(.system(.body, design: .monospaced))
                    .textSelection(.enabled)
            }
        case .signedOut:
            VStack(spacing: 8) {
                Text("Not signed into iCloud.")
                Text("Sign in via Settings → Apple ID, then tap Refresh.")
                    .foregroundStyle(.secondary)
                Button("Open Settings") {
                    // You can't deep-link directly to iCloud settings.
                    // This opens your app’s settings as a courtesy.
                    if let url = URL(string: UIApplication.openSettingsURLString) {
                        UIApplication.shared.open(url)
                    }
                }
            }
        case .error(let message):
            VStack(spacing: 8) {
                Text("Error").bold()
                Text(message).foregroundStyle(.secondary)
            }
        }
    }
}
