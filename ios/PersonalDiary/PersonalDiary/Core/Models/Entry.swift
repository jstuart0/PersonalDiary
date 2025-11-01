//
//  Entry.swift
//  PersonalDiary
//
//  Created by Claude Code on 2025-10-31.
//  Copyright © 2025 Personal Diary Platform. All rights reserved.
//

import Foundation

/// Entry source enum
enum EntrySource: String, Codable {
    case diary = "diary"
    case facebook = "facebook"
    case instagram = "instagram"

    var displayName: String {
        switch self {
        case .diary:
            return "Diary"
        case .facebook:
            return "Facebook"
        case .instagram:
            return "Instagram"
        }
    }

    var icon: String {
        switch self {
        case .diary:
            return "book.closed.fill"
        case .facebook:
            return "f.circle.fill"
        case .instagram:
            return "camera.circle.fill"
        }
    }

    var color: String {
        switch self {
        case .diary:
            return "blue"
        case .facebook:
            return "indigo"
        case .instagram:
            return "purple"
        }
    }
}

/// Sync status enum
enum SyncStatus: String, Codable {
    case synced = "synced"
    case pending = "pending"
    case failed = "failed"

    var displayName: String {
        switch self {
        case .synced:
            return "Synced"
        case .pending:
            return "Pending"
        case .failed:
            return "Failed"
        }
    }

    var icon: String {
        switch self {
        case .synced:
            return "checkmark.circle.fill"
        case .pending:
            return "clock.fill"
        case .failed:
            return "exclamationmark.triangle.fill"
        }
    }

    var color: String {
        switch self {
        case .synced:
            return "green"
        case .pending:
            return "orange"
        case .failed:
            return "red"
        }
    }
}

/// Entry model
struct Entry: Identifiable, Codable {
    let id: String
    let userId: String
    var encryptedContent: String
    var contentHash: String
    var source: EntrySource
    var createdAt: Date
    var updatedAt: Date
    var syncStatus: SyncStatus
    var tags: [Tag]
    var mediaIds: [String]
    var isDeleted: Bool

    // Transient property for decrypted content (not stored)
    var decryptedContent: String?

    enum CodingKeys: String, CodingKey {
        case id
        case userId = "user_id"
        case encryptedContent = "encrypted_content"
        case contentHash = "content_hash"
        case source
        case createdAt = "created_at"
        case updatedAt = "updated_at"
        case syncStatus = "sync_status"
        case tags
        case mediaIds = "media_ids"
        case isDeleted = "is_deleted"
    }

    /// Computed property to get content preview
    var preview: String {
        guard let content = decryptedContent else {
            return "Encrypted content"
        }
        let maxLength = 150
        if content.count > maxLength {
            let index = content.index(content.startIndex, offsetBy: maxLength)
            return String(content[..<index]) + "..."
        }
        return content
    }

    /// Check if entry has media attachments
    var hasMedia: Bool {
        !mediaIds.isEmpty
    }

    /// Check if entry has tags
    var hasTags: Bool {
        !tags.isEmpty
    }
}

/// Create entry request
struct CreateEntryRequest: Codable {
    let encryptedContent: String
    let contentHash: String
    let source: EntrySource
    let tags: [Tag]
    let mediaIds: [String]

    enum CodingKeys: String, CodingKey {
        case encryptedContent = "encrypted_content"
        case contentHash = "content_hash"
        case source
        case tags
        case mediaIds = "media_ids"
    }
}

/// Update entry request
struct UpdateEntryRequest: Codable {
    let encryptedContent: String?
    let contentHash: String?
    let tags: [Tag]?
    let mediaIds: [String]?

    enum CodingKeys: String, CodingKey {
        case encryptedContent = "encrypted_content"
        case contentHash = "content_hash"
        case tags
        case mediaIds = "media_ids"
    }
}

/// Entry response from API
struct EntryResponse: Codable {
    let entry: Entry
    let media: [Media]?
}

/// External post mapping
struct ExternalPost: Codable, Identifiable {
    let id: String
    let entryId: String
    let platform: String
    let externalPostId: String
    let externalUrl: String
    let postedAt: Date
    let syncStatus: SyncStatus

    enum CodingKeys: String, CodingKey {
        case id
        case entryId = "entry_id"
        case platform
        case externalPostId = "external_post_id"
        case externalUrl = "external_url"
        case postedAt = "posted_at"
        case syncStatus = "sync_status"
    }
}

/// Entry event type
enum EntryEventType: String, Codable {
    case created = "created"
    case edited = "edited"
    case pushed = "pushed"
    case pulled = "pulled"
    case tagged = "tagged"
    case deleted = "deleted"
}

/// Entry event for history/audit trail
struct EntryEvent: Codable, Identifiable {
    let id: String
    let entryId: String
    let eventType: EntryEventType
    let timestamp: Date
    let metadata: [String: String]?

    enum CodingKeys: String, CodingKey {
        case id
        case entryId = "entry_id"
        case eventType = "event_type"
        case timestamp
        case metadata
    }
}
