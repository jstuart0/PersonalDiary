//
//  SyncService.swift
//  PersonalDiary
//
//  Created by Claude Code on 2025-10-31.
//  Copyright © 2025 Personal Diary Platform. All rights reserved.
//

import Foundation
import Combine

/// Service for synchronizing local data with the server
final class SyncService: ObservableObject {
    // MARK: - Singleton

    static let shared = SyncService()

    // MARK: - Published Properties

    @Published private(set) var isSyncing = false
    @Published private(set) var lastSyncDate: Date?
    @Published private(set) var syncProgress: Double = 0
    @Published private(set) var pendingUploads = 0
    @Published private(set) var pendingDownloads = 0
    @Published private(set) var syncError: Error?

    // MARK: - Properties

    private let databaseService = DatabaseService.shared
    private let apiClient = APIClient.shared
    private let mediaService = MediaService.shared
    private let keychainService = KeychainService()

    private var syncTask: Task<Void, Never>?
    private var isFullSyncRequired = false

    // MARK: - Initialization

    private init() {
        loadLastSyncDate()
        observeNetworkChanges()
    }

    // MARK: - Sync Operations

    /// Performs a full sync (upload pending + download changes)
    func syncFull() async {
        guard !isSyncing else { return }

        await MainActor.run {
            isSyncing = true
            syncError = nil
            syncProgress = 0
        }

        do {
            // Step 1: Upload pending entries (25%)
            try await uploadPendingEntries()
            await updateProgress(0.25)

            // Step 2: Upload pending media (50%)
            try await uploadPendingMedia()
            await updateProgress(0.5)

            // Step 3: Download server changes (75%)
            try await downloadServerChanges()
            await updateProgress(0.75)

            // Step 4: Resolve conflicts (90%)
            try await resolveConflicts()
            await updateProgress(0.9)

            // Step 5: Update sync metadata (100%)
            await updateLastSyncDate()
            await updateProgress(1.0)

            await MainActor.run {
                isSyncing = false
                isFullSyncRequired = false
            }
        } catch {
            await handleSyncError(error)
        }
    }

    /// Performs an incremental sync (only new/changed items)
    func syncIncremental() async {
        guard !isSyncing else { return }

        if isFullSyncRequired {
            await syncFull()
            return
        }

        await MainActor.run {
            isSyncing = true
            syncError = nil
        }

        do {
            // Quick sync: upload new, download new
            try await uploadPendingEntries()
            try await uploadPendingMedia()
            try await downloadServerChanges(since: lastSyncDate)

            await updateLastSyncDate()

            await MainActor.run {
                isSyncing = false
            }
        } catch {
            await handleSyncError(error)
        }
    }

    /// Uploads a single entry immediately
    func uploadEntry(_ entry: Entry) async throws {
        let request = entry.toCreateRequest()

        if entry.serverID == nil {
            // Create new entry
            let _: EntryResponse = try await apiClient.post(.createEntry, body: request)
        } else {
            // Update existing entry
            let updateRequest = entry.toUpdateRequest()
            let _: EntryResponse = try await apiClient.put(.updateEntry(entry.serverID!), body: updateRequest)
        }

        // Mark as synced
        var updatedEntry = entry
        updatedEntry.syncStatus = .synced
        updatedEntry.lastSyncedAt = Date()
        try databaseService.saveEntry(updatedEntry)
    }

    // MARK: - Upload

    private func uploadPendingEntries() async throws {
        let pendingEntries = try databaseService.fetchPendingSyncEntries()

        for (index, entry) in pendingEntries.enumerated() {
            try await uploadEntry(entry)

            await MainActor.run {
                pendingUploads = pendingEntries.count - index - 1
            }
        }
    }

    private func uploadPendingMedia() async throws {
        let pendingMedia = try databaseService.fetchPendingUploadMedia()

        for (index, media) in pendingMedia.enumerated() {
            try await mediaService.uploadMedia(media)

            await MainActor.run {
                pendingUploads = pendingMedia.count - index - 1
            }
        }
    }

    // MARK: - Download

    private func downloadServerChanges(since date: Date? = nil) async throws {
        let request = SyncEntriesRequest(
            lastSyncTimestamp: date,
            entries: nil
        )

        let response: SyncEntriesResponse = try await apiClient.post(.syncEntries, body: request)

        await MainActor.run {
            pendingDownloads = response.entries.count
        }

        // Process downloaded entries
        for (index, entryResponse) in response.entries.enumerated() {
            try await processDownloadedEntry(entryResponse)

            await MainActor.run {
                pendingDownloads = response.entries.count - index - 1
            }
        }

        // Process deletions
        for deletedID in response.deletedEntryIDs {
            try databaseService.permanentlyDeleteEntry(id: deletedID)
        }

        // If there are more entries, fetch next batch
        if response.hasMore {
            try await downloadServerChanges(since: response.lastSyncTimestamp)
        }
    }

    private func processDownloadedEntry(_ entryResponse: EntryResponse) async throws {
        // Convert response to domain model
        var entry = try entryResponse.toDomainModel()

        // Check if entry exists locally
        if let existingEntry = try databaseService.fetchEntry(id: entry.id) {
            // Entry exists - check for conflicts
            if existingEntry.updatedAt > entry.updatedAt {
                // Local is newer - potential conflict
                try await resolveConflict(local: existingEntry, remote: entry)
                return
            }
        }

        // Decrypt content if encrypted
        if entry.isEncrypted, let encryptedContentBase64 = entryResponse.encryptedContent {
            entry = try await decryptEntry(entry, encryptedContent: encryptedContentBase64)
        }

        // Save to local database
        entry.syncStatus = .synced
        entry.lastSyncedAt = Date()
        try databaseService.saveEntry(entry)
    }

    private func decryptEntry(_ entry: Entry, encryptedContent: String) async throws -> Entry {
        guard let encryptedData = Data(base64Encoded: encryptedContent) else {
            throw AppError.encryption(.decryptionFailed)
        }

        let tier = try keychainService.retrieveUserTier() ?? .e2e
        let service = tier == .e2e ? E2EEncryptionService() : UCEEncryptionService()

        let decryptedContent = try service.decryptText(encryptedData)

        var updatedEntry = entry
        updatedEntry.content = decryptedContent
        return updatedEntry
    }

    // MARK: - Conflict Resolution

    private func resolveConflicts() async throws {
        // Find entries with pending conflicts
        let allEntries = try databaseService.fetchEntries()

        for entry in allEntries {
            if entry.syncStatus == .conflict {
                // Use last-write-wins strategy for MVP
                // TODO: Implement more sophisticated conflict resolution
                try await uploadEntry(entry)
            }
        }
    }

    private func resolveConflict(local: Entry, remote: Entry) async throws {
        // Simple last-write-wins strategy
        if local.updatedAt > remote.updatedAt {
            // Local wins - upload it
            try await uploadEntry(local)
        } else {
            // Remote wins - save it
            var updatedEntry = remote
            updatedEntry.syncStatus = .synced
            updatedEntry.lastSyncedAt = Date()
            try databaseService.saveEntry(updatedEntry)
        }
    }

    // MARK: - Background Sync

    /// Schedules background sync
    func scheduleBackgroundSync() {
        // Implementation would use BackgroundTasks framework
        // For MVP, we'll rely on app foreground sync
    }

    /// Cancels all pending sync operations
    func cancelSync() {
        syncTask?.cancel()

        Task { @MainActor in
            isSyncing = false
        }
    }

    // MARK: - Sync Status

    /// Forces a full sync on next sync operation
    func requireFullSync() {
        isFullSyncRequired = true
    }

    /// Checks sync status from server
    func checkSyncStatus() async throws {
        let response: SyncStatusResponse = try await apiClient.get(.syncStatus)

        await MainActor.run {
            lastSyncDate = response.lastSyncedAt
            pendingUploads = response.pendingUploads
            pendingDownloads = response.pendingDownloads
        }
    }

    // MARK: - Private Helpers

    private func loadLastSyncDate() {
        if let data = try? keychainService.retrieve(for: "last_sync_date"),
           let timestamp = String(data: data, encoding: .utf8),
           let date = ISO8601DateFormatter().date(from: timestamp) {
            lastSyncDate = date
        }
    }

    private func updateLastSyncDate() async {
        let now = Date()

        await MainActor.run {
            lastSyncDate = now
        }

        // Save to keychain
        let formatter = ISO8601DateFormatter()
        if let timestamp = formatter.string(from: now).data(using: .utf8) {
            try? keychainService.save(timestamp, for: "last_sync_date")
        }
    }

    private func updateProgress(_ progress: Double) async {
        await MainActor.run {
            syncProgress = progress
        }
    }

    private func handleSyncError(_ error: Error) async {
        await MainActor.run {
            isSyncing = false
            syncError = error
            isFullSyncRequired = true // Require full sync after error
        }
    }

    private func observeNetworkChanges() {
        // Monitor network reachability
        // When network becomes available, trigger incremental sync
        // Implementation would use Network framework
    }
}

// MARK: - Keychain Extension

extension KeychainService {
    func retrieveUserTier() throws -> EncryptionTier? {
        guard let tierData = try retrieve(for: Constants.Keychain.userTierKey),
              let tierString = String(data: tierData, encoding: .utf8) else {
            return nil
        }

        return EncryptionTier(rawValue: tierString)
    }
}
