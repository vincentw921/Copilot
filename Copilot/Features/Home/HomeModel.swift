//
//  HomeModel.swift
//  Copilot
//
//  Created by Vincent Wang on 9/14/25.
//

import Foundation
import CoreData
import CloudKit

@MainActor
final class HomeModel: ObservableObject {
    enum Status {
        case idle, loading, success(String), notAvailable, failure(String)
    }

    @Published var status: Status = .idle
    @Published var items: [Item] = []
    
    private let viewContext: NSManagedObjectContext
    private let container: CKContainer = .default()
    
    init(viewContext: NSManagedObjectContext = PersistenceController.shared.container.viewContext) {
        self.viewContext = viewContext
        
        // Refresh when CloudKit sync brings in new data
        NotificationCenter.default.addObserver(
            forName: .NSPersistentStoreRemoteChange,
            object: viewContext.persistentStoreCoordinator,
            queue: .main
        ) { [weak self] _ in
            Task { await self?.fetchData() }
        }
    }

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
    
    func fetchData() async {
        status = .loading
        do {
            let req: NSFetchRequest<Item> = Item.fetchRequest()
            // Optional: sort newest first
            req.sortDescriptors = [NSSortDescriptor(keyPath: \Item.date, ascending: false)]
            // If your dataset is large, set a fetchBatchSize
            req.fetchBatchSize = 100

            let results: [Item] = try await viewContext.perform {
                try self.viewContext.fetch(req)
            }
            self.items = results
            self.status = .success("Loaded \(results.count) records")
        } catch {
            self.status = .failure(error.localizedDescription)
        }
    }

    func insertTestData(count: Int = 5) async {
        do {
            try await self.viewContext.perform {
                for i in 0..<count {
                    let it = Item(context: self.viewContext)
                    it.id = UUID()
                    it.date = Calendar.current.date(byAdding: .day, value: -i, to: .now)
                    it.aircraftType = "C172"
                    it.aircraftRegistration = "N\(Int.random(in: 1000...9999))\(["AB","CD","EF"].randomElement()!)"
                    it.departureAirport = "KSNA"
                    it.arrivalAirport = ["KLAX","KSQL","KPAO","KSBP"].randomElement()
                    it.totalTime = Double.random(in: 0.6...2.2)
                    it.picTime = it.totalTime
                    it.dayTakeoffs = Int16(Int.random(in: 1...3))
                    it.dayFullStopLandings = Int16(Int.random(in: 1...3))
                }
                try self.viewContext.save()
            }
            await fetchData() // if you have this, refresh your list
        } catch {
            status = .failure("Seed failed: \(error.localizedDescription)")
        }
    }
    
    func deleteAllItems() async throws {
        try await self.viewContext.perform {
            let fr: NSFetchRequest<Item> = Item.fetchRequest()
            let all = try self.viewContext.fetch(fr)
            all.forEach(self.viewContext.delete)
            try self.viewContext.save()
        }
    }
    
}
