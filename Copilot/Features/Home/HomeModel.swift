//
//  HomeModel.swift
//  Copilot
//
//  Created by Vincent Wang on 9/14/25.
//

import Foundation
import CloudKit

@MainActor
final class HomeModel: ObservableObject {
    enum Status {
        case idle, loading, success(String), notAvailable, failure(String)
    }

    @Published var status: Status = .idle
    private let container: CKContainer = .default()

    func fetchCloudID() {
        status = .loading
        container.accountStatus { [weak self] accountStatus, err in
            Task { @MainActor in
                if let err { self?.status = .failure(err.localizedDescription); return }
                guard accountStatus == .available else { self?.status = .notAvailable; return }
                self?.container.fetchUserRecordID { id, error in
                    Task { @MainActor in
                        if let error { self?.status = .failure(error.localizedDescription); return }
                        guard let id else { self?.status = .failure("Unknown item."); return }
                        self?.status = .success(id.recordName)
                    }
                }
            }
        }
    }
}
