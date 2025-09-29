//
//  HomeView.swift
//  Copilot
//
//  Created by Benjamin Tam on 9/14/25.
//

import SwiftUI

struct HomeView: View {
    @StateObject var model = HomeModel()
    
    @FetchRequest(
        sortDescriptors: [NSSortDescriptor(keyPath: \Item.date, ascending: false)]
    ) private var items: FetchedResults<Item>

    var body: some View {
        VStack(spacing: 12) {
            Text("Your CloudKit User ID")
                .font(.title3).fontWeight(.semibold)

            content

            Button("Refresh") { model.fetchCloudID() }
                .buttonStyle(.bordered)
                .padding(.top, 8)
        }
        .multilineTextAlignment(.center)
        .padding()
        .onAppear {
            if case .idle = model.status { model.fetchCloudID() }
        }
        if case .success = model.status {
            List(items) { item in
                Text(item.aircraftRegistration ?? "—")
            }
        }
        
        if case .success = model.status {
            HStack {
                Button("Insert Sample Items") { Task { await model.insertTestData() } }
                Button("Clear All") { Task { try? await model.deleteAllItems() } }
            }
            List(items) { item in
                Text(item.aircraftRegistration ?? "—")
            }
        }
    }

    @ViewBuilder
    private var content: some View {
        switch model.status {
        case .idle, .loading:
            ProgressView("Loading…")
        case .success(let id):
            VStack(spacing: 6) {
                Text(id).font(.system(.body, design: .monospaced))
                Text("Signed in to iCloud").foregroundStyle(.secondary)
            }
        case .notAvailable:
            VStack(spacing: 6) {
                Text("Unavailable")
                Text("Please sign into iCloud in Settings.")
                    .foregroundStyle(.secondary)
            }
        case .failure(let message):
            VStack(spacing: 6) {
                Text("Error")
                Text(message).foregroundStyle(.secondary)
            }
        }
    }
}

#Preview {
    HomeView()
}
