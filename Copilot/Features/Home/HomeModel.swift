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
                if let err {
                    self?.status = .failure(err.localizedDescription)
                    return
                }
                guard accountStatus == .available else {
                    self?.status = .notAvailable
                    return
                }

                // Ask for permission to discover user's name
                self?.container.requestApplicationPermission(.userDiscoverability) { permission, permError in
                    Task { @MainActor in
                        if let permError {
                            self?.status = .failure(permError.localizedDescription)
                            return
                        }
                        guard permission == .granted else {
                            self?.status = .failure("User discoverability permission denied.")
                            return
                        }

                        // Get the user's record ID
                        self?.container.fetchUserRecordID { recordID, error in
                            Task { @MainActor in
                                if let error {
                                    self?.status = .failure(error.localizedDescription)
                                    return
                                }
                                guard let recordID else {
                                    self?.status = .failure("Unknown item.")
                                    return
                                }

                                // Now discover the user identity (name) using record ID
                                self?.container.discoverUserIdentity(withUserRecordID: recordID) { identity, discoverError in
                                    Task { @MainActor in
                                        if let discoverError {
                                            self?.status = .failure(discoverError.localizedDescription)
                                            return
                                        }

                                        let nameComponents = identity?.nameComponents
                                        let fullName = [nameComponents?.givenName, nameComponents?.familyName]
                                            .compactMap { $0 }
                                            .joined(separator: " ")

                                        // fallback to recordName if no name
                                        let display = fullName.isEmpty ? recordID.recordName : fullName

                                        self?.status = .success(display)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}
