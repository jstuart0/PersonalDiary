//
//  PersistenceController.swift
//  PersonalDiary
//
//  Created by Claude Code on 2025-10-31.
//  Copyright © 2025 Personal Diary Platform. All rights reserved.
//

import CoreData
import Foundation

/// Manages Core Data stack and provides centralized database access
final class PersistenceController {
    // MARK: - Singleton

    static let shared = PersistenceController()

    // MARK: - Preview

    /// In-memory instance for SwiftUI previews and testing
    static var preview: PersistenceController = {
        let controller = PersistenceController(inMemory: true)
        let viewContext = controller.container.viewContext

        // Create sample data for previews
        for i in 0..<10 {
            let entry = EntryEntity(context: viewContext)
            entry.id = UUID()
            entry.title = "Sample Entry \(i + 1)"
            entry.content = "This is sample content for entry \(i + 1)"
            entry.createdAt = Date().addingTimeInterval(TimeInterval(-i * 86400))
            entry.updatedAt = entry.createdAt
            entry.isEncrypted = true
            entry.encryptionTier = EncryptionTier.e2e.rawValue
            entry.syncStatus = SyncStatus.synced.rawValue
        }

        do {
            try viewContext.save()
        } catch {
            print("Preview data creation failed: \(error.localizedDescription)")
        }

        return controller
    }()

    // MARK: - Properties

    let container: NSPersistentContainer

    var viewContext: NSManagedObjectContext {
        container.viewContext
    }

    // MARK: - Initialization

    private init(inMemory: Bool = false) {
        container = NSPersistentContainer(name: "PersonalDiary")

        if inMemory {
            container.persistentStoreDescriptions.first?.url = URL(fileURLWithPath: "/dev/null")
        } else {
            // Configure persistent store for production
            if let description = container.persistentStoreDescriptions.first {
                // Enable automatic migration
                description.setOption(true as NSNumber, forKey: NSMigratePersistentStoresAutomaticallyOption)
                description.setOption(true as NSNumber, forKey: NSInferMappingModelAutomaticallyOption)

                // Enable persistent history tracking for sync
                description.setOption(true as NSNumber, forKey: NSPersistentHistoryTrackingKey)

                // Enable remote change notifications
                description.setOption(true as NSNumber, forKey: NSPersistentStoreRemoteChangeNotificationPostOptionKey)
            }
        }

        container.loadPersistentStores { description, error in
            if let error = error {
                fatalError("Core Data store failed to load: \(error.localizedDescription)")
            }
        }

        // Configure view context
        container.viewContext.automaticallyMergesChangesFromParent = true
        container.viewContext.mergePolicy = NSMergeByPropertyObjectTrumpMergePolicy

        // Set up notification observers for remote changes
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleRemoteChange),
            name: .NSPersistentStoreRemoteChange,
            object: container.persistentStoreCoordinator
        )
    }

    // MARK: - Remote Change Handling

    @objc private func handleRemoteChange(_ notification: Notification) {
        // Refresh view context when remote changes occur
        viewContext.perform {
            self.viewContext.refreshAllObjects()
        }
    }

    // MARK: - Context Management

    /// Creates a background context for performing operations off the main thread
    func newBackgroundContext() -> NSManagedObjectContext {
        let context = container.newBackgroundContext()
        context.mergePolicy = NSMergeByPropertyObjectTrumpMergePolicy
        return context
    }

    /// Performs a block on a background context
    func performBackgroundTask(_ block: @escaping (NSManagedObjectContext) -> Void) {
        container.performBackgroundTask(block)
    }

    // MARK: - Save

    /// Saves the view context if it has changes
    func save() throws {
        guard viewContext.hasChanges else { return }
        try viewContext.save()
    }

    /// Saves a background context if it has changes
    func save(context: NSManagedObjectContext) throws {
        guard context.hasChanges else { return }
        try context.save()
    }

    // MARK: - Batch Operations

    /// Deletes all data from the database
    func deleteAll() throws {
        let entities = container.managedObjectModel.entities

        for entity in entities {
            guard let entityName = entity.name else { continue }

            let fetchRequest = NSFetchRequest<NSFetchRequestResult>(entityName: entityName)
            let deleteRequest = NSBatchDeleteRequest(fetchRequest: fetchRequest)

            try viewContext.execute(deleteRequest)
        }

        try save()
    }

    /// Deletes entries that match a predicate
    func batchDelete(entityName: String, predicate: NSPredicate?) throws {
        let fetchRequest = NSFetchRequest<NSFetchRequestResult>(entityName: entityName)
        fetchRequest.predicate = predicate

        let deleteRequest = NSBatchDeleteRequest(fetchRequest: fetchRequest)
        deleteRequest.resultType = .resultTypeObjectIDs

        let result = try viewContext.execute(deleteRequest) as? NSBatchDeleteResult

        if let objectIDs = result?.result as? [NSManagedObjectID] {
            let changes = [NSDeletedObjectsKey: objectIDs]
            NSManagedObjectContext.mergeChanges(fromRemoteContextSave: changes, into: [viewContext])
        }
    }

    // MARK: - Fetch

    /// Generic fetch request execution
    func fetch<T: NSManagedObject>(_ request: NSFetchRequest<T>) throws -> [T] {
        try viewContext.fetch(request)
    }

    /// Fetches a single object by ID
    func fetchObject<T: NSManagedObject>(with id: NSManagedObjectID) throws -> T? {
        try viewContext.existingObject(with: id) as? T
    }

    // MARK: - Count

    /// Counts objects matching a fetch request
    func count<T: NSManagedObject>(_ request: NSFetchRequest<T>) throws -> Int {
        try viewContext.count(for: request)
    }
}
