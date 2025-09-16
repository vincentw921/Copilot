//
//  AuthModel.swift
//  Copilot
//
//  Created by Vincent Wang on 9/14/25.
//

import Foundation
import CloudKit

@MainActor
final class AuthModel: ObservableObject {
    enum State {
        case idle
        case checking
        case signedIn(cloudID: String)
        case signedOut                  // iCloud not available / signed out / restricted
        case error(String)
    }

    @Published private(set) var state: State = .idle

    private let container: CKContainer
    private let keychain = Keychain()

    init(container: CKContainer = .default()) {
        self.container = container

        // Observe iCloud account changes (user switches account, signs out/in)
        NotificationCenter.default.addObserver(
            forName: .CKAccountChanged, object: nil, queue: .main
        ) { [weak self] _ in
            Task { await self?.refresh() }
        }
    }

    func start() async {
        // Fast-path from cache
        if let cached = keychain.cloudID {
            state = .signedIn(cloudID: cached)
        } else {
            await refresh()
        }
    }

    func refresh() async {
        state = .checking

        do {
            let status = try await accountStatus()
            guard status == .available else {
                keychain.cloudID = nil
                state = .signedOut
                return
            }

            let id = try await userRecordID()
            keychain.cloudID = id.recordName
            state = .signedIn(cloudID: id.recordName)
        } catch {
            state = .error(error.localizedDescription)
        }
    }

    func signOutLocally() {
        // You cannot sign the user out of iCloud programmatically.
        // Instead, clear your local session & cache.
        keychain.cloudID = nil
        state = .signedOut
    }

    // MARK: - Async wrappers
    private func accountStatus() async throws -> CKAccountStatus {
        try await withCheckedThrowingContinuation { cont in
            container.accountStatus { status, error in
                if let error { cont.resume(throwing: error) }
                else { cont.resume(returning: status) }
            }
        }
    }

    private func userRecordID() async throws -> CKRecord.ID {
        try await withCheckedThrowingContinuation { cont in
            container.fetchUserRecordID { id, error in
                if let error { cont.resume(throwing: error) }
                else if let id { cont.resume(returning: id) }
                else { cont.resume(throwing: CKError(.unknownItem)) }
            }
        }
    }
}
