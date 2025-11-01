//
//  EntryViewModel.swift
//  PersonalDiary
//
//  Created by Claude Code on 2025-10-31.
//  Copyright © 2025 Personal Diary Platform. All rights reserved.
//

import Foundation
import UIKit
import Combine

@MainActor
final class EntryViewModel: ObservableObject {
    // MARK: - Published Properties

    @Published var title = ""
    @Published var content = ""
    @Published var tags: [Tag] = []
    @Published var media: [Media] = []
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var isSaving = false

    // MARK: - Properties

    private let databaseService = DatabaseService.shared
    private let mediaService = MediaService.shared
    private let syncService = SyncService.shared
    private var entry: Entry?

    // MARK: - Initialization

    init(entry: Entry? = nil) {
        self.entry = entry

        if let entry = entry {
            title = entry.title ?? ""
            content = entry.content
            tags = entry.tags
            loadMedia(for: entry)
        }
    }

    // MARK: - Save

    func save(encryptionTier: EncryptionTier) async throws {
        guard !content.isEmpty else {
            throw AppError.validation(.invalidInput(field: "Content"))
        }

        isSaving = true
        errorMessage = nil

        do {
            let entryToSave: Entry

            if let existing = entry {
                // Update existing
                entryToSave = Entry(
                    id: existing.id,
                    title: title.isEmpty ? nil : title,
                    content: content,
                    createdAt: existing.createdAt,
                    updatedAt: Date(),
                    isEncrypted: true,
                    encryptionTier: encryptionTier,
                    tags: tags,
                    media: media,
                    source: existing.source,
                    sourceID: existing.sourceID,
                    syncStatus: .pending,
                    lastSyncedAt: existing.lastSyncedAt,
                    serverID: existing.serverID,
                    serverUpdatedAt: existing.serverUpdatedAt
                )
            } else {
                // Create new
                entryToSave = Entry(
                    id: UUID(),
                    title: title.isEmpty ? nil : title,
                    content: content,
                    createdAt: Date(),
                    updatedAt: Date(),
                    isEncrypted: true,
                    encryptionTier: encryptionTier,
                    tags: tags,
                    media: media,
                    source: .manual,
                    sourceID: nil,
                    syncStatus: .pending,
                    lastSyncedAt: nil,
                    serverID: nil,
                    serverUpdatedAt: nil
                )
            }

            // Save to database
            try databaseService.saveEntry(entryToSave)

            // Trigger sync
            Task {
                await syncService.syncIncremental()
            }

            isSaving = false
        } catch {
            isSaving = false
            errorMessage = error.localizedDescription
            throw error
        }
    }

    // MARK: - Media

    func addPhoto(_ image: UIImage, encryptionTier: EncryptionTier) async throws {
        isLoading = true

        do {
            // Get entry ID (create temp entry if needed)
            let entryID = entry?.id ?? UUID()

            let media = try await mediaService.processPhoto(
                image,
                forEntryID: entryID,
                encrypt: true
            )

            self.media.append(media)
            isLoading = false
        } catch {
            isLoading = false
            errorMessage = error.localizedDescription
            throw error
        }
    }

    func addVideo(at url: URL, encryptionTier: EncryptionTier) async throws {
        isLoading = true

        do {
            let entryID = entry?.id ?? UUID()

            let media = try await mediaService.processVideo(
                at: url,
                forEntryID: entryID,
                encrypt: true
            )

            self.media.append(media)
            isLoading = false
        } catch {
            isLoading = false
            errorMessage = error.localizedDescription
            throw error
        }
    }

    func removeMedia(_ media: Media) async throws {
        do {
            try mediaService.deleteMedia(media)
            self.media.removeAll { $0.id == media.id }
        } catch {
            errorMessage = error.localizedDescription
            throw error
        }
    }

    private func loadMedia(for entry: Entry) {
        Task {
            do {
                media = try databaseService.fetchMedia(forEntryID: entry.id)
            } catch {
                errorMessage = error.localizedDescription
            }
        }
    }

    // MARK: - Tags

    func addTag(_ name: String) async throws {
        guard !name.isEmpty else { return }

        do {
            let tag = try databaseService.fetchOrCreateTag(name: name)

            if !tags.contains(where: { $0.id == tag.id }) {
                tags.append(tag)
            }
        } catch {
            errorMessage = error.localizedDescription
            throw error
        }
    }

    func removeTag(_ tag: Tag) {
        tags.removeAll { $0.id == tag.id }
    }

    func generateAutoTags() {
        // Simple auto-tag generation based on content
        let words = content.components(separatedBy: .whitespacesAndNewlines)
        let commonWords = Set(["the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by", "from", "up", "about", "into", "through", "during", "before", "after", "above", "below", "between", "under", "since", "without", "is", "am", "are", "was", "were", "be", "been", "being", "have", "has", "had", "do", "does", "did", "will", "would", "should", "could", "may", "might", "must", "can", "this", "that", "these", "those", "i", "you", "he", "she", "it", "we", "they"])

        var wordCounts: [String: Int] = [:]

        for word in words {
            let cleaned = word.lowercased().trimmingCharacters(in: .punctuationCharacters)
            guard cleaned.count > 3, !commonWords.contains(cleaned) else { continue }

            wordCounts[cleaned, default: 0] += 1
        }

        let topWords = wordCounts
            .sorted { $0.value > $1.value }
            .prefix(3)
            .map { $0.key }

        Task {
            for word in topWords {
                try? await addTag(word)
            }
        }
    }

    // MARK: - Validation

    var isValid: Bool {
        !content.isEmpty
    }

    var wordCount: Int {
        content.components(separatedBy: .whitespacesAndNewlines)
            .filter { !$0.isEmpty }
            .count
    }

    var characterCount: Int {
        content.count
    }
}
