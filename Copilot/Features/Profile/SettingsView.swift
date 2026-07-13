//
//  SettingsView.swift
//  Copilot
//
//  App preferences, reached from the gear button on the Profile tab.
//

import SwiftUI

/// User-facing app settings.
struct SettingsView: View {
    /// When enabled, NVG time can be logged in the flight form and the
    /// NVG total appears on the Report tab. Off by default since most
    /// civilian pilots never log night-vision-goggle time.
    @AppStorage("nvgLoggingEnabled") private var nvgLoggingEnabled = false

    var body: some View {
        Form {
            Section {
                Toggle("Enable NVG Logging", isOn: $nvgLoggingEnabled)
            } header: {
                Text("Logbook")
            } footer: {
                Text("Shows night-vision-goggle time in the flight form and its total on the Report tab. Already-logged NVG time is kept either way.")
            }
        }
        .navigationTitle("Settings")
    }
}

#Preview {
    NavigationStack {
        SettingsView()
    }
}
