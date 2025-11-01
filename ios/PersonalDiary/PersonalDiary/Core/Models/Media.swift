//
//  Media.swift
//  PersonalDiary
//
//  Created by Claude Code on 2025-10-31.
//  Copyright © 2025 Personal Diary Platform. All rights reserved.
//

import Foundation
import UIKit

/// Media type enum
enum MediaType: String, Codable {
    case image = "image"
    case video = "video"

    var displayName: String {
        switch self {
        case .image:
            return "Image"
        case .video:
            return "Video"
        }
    }

    var icon: String {
        switch self {
        case .image:
            return "photo.fill"
        case .video:
            return "video.fill"
        }
    }

    /// Get media type from MIME type
    static func from(mimeType: String) -> MediaType? {
        if mimeType.hasPrefix("image/") {
            return .image
        } else if mimeType.hasPrefix("video/") {
            return .video
        }
        return nil
    }
}

/// Media model
struct Media: Identifiable, Codable {
    let id: String
    let entryId: String
    var encryptedFileUrl: String?  // Server URL
    var localFileUrl: String?      // Local encrypted file path
    var fileHash: String
    var mimeType: String
    var fileSize: Int64
    var width: Int?
    var height: Int?
    var duration: Double?  // For videos
    var syncStatus: SyncStatus
    var createdAt: Date
    var updatedAt: Date

    enum CodingKeys: String, CodingKey {
        case id
        case entryId = "entry_id"
        case encryptedFileUrl = "encrypted_file_url"
        case localFileUrl = "local_file_url"
        case fileHash = "file_hash"
        case mimeType = "mime_type"
        case fileSize = "file_size"
        case width
        case height
        case duration
        case syncStatus = "sync_status"
        case createdAt = "created_at"
        case updatedAt = "updated_at"
    }

    /// Computed media type
    var mediaType: MediaType? {
        MediaType.from(mimeType: mimeType)
    }

    /// File size in human-readable format
    var fileSizeFormatted: String {
        ByteCountFormatter.string(fromByteCount: fileSize, countStyle: .file)
    }

    /// Dimensions as string
    var dimensionsFormatted: String? {
        guard let width = width, let height = height else {
            return nil
        }
        return "\(width) × \(height)"
    }

    /// Duration formatted for videos
    var durationFormatted: String? {
        guard let duration = duration else {
            return nil
        }
        let minutes = Int(duration) / 60
        let seconds = Int(duration) % 60
        return String(format: "%d:%02d", minutes, seconds)
    }

    /// Check if media is available locally
    var isAvailableLocally: Bool {
        guard let localPath = localFileUrl else {
            return false
        }
        return FileManager.default.fileExists(atPath: localPath)
    }

    /// Check if media is synced to server
    var isSynced: Bool {
        syncStatus == .synced && encryptedFileUrl != nil
    }
}

/// Media upload request
struct MediaUploadRequest: Codable {
    let entryId: String
    let fileHash: String
    let mimeType: String
    let fileSize: Int64
    let width: Int?
    let height: Int?
    let duration: Double?

    enum CodingKeys: String, CodingKey {
        case entryId = "entry_id"
        case fileHash = "file_hash"
        case mimeType = "mime_type"
        case fileSize = "file_size"
        case width
        case height
        case duration
    }
}

/// Media upload response
struct MediaUploadResponse: Codable {
    let media: Media
    let uploadUrl: String  // Pre-signed S3 URL for upload

    enum CodingKeys: String, CodingKey {
        case media
        case uploadUrl = "upload_url"
    }
}

/// Media download response
struct MediaDownloadResponse: Codable {
    let media: Media
    let downloadUrl: String  // Pre-signed S3 URL for download

    enum CodingKeys: String, CodingKey {
        case media
        case downloadUrl = "download_url"
    }
}

/// Decrypted media wrapper
struct DecryptedMedia {
    let mediaId: String
    let data: Data
    let mediaType: MediaType

    /// Get UIImage if media is image
    var image: UIImage? {
        guard mediaType == .image else {
            return nil
        }
        return UIImage(data: data)
    }

    /// Get local URL for video playback
    func saveTemporaryFile() -> URL? {
        let tempDir = FileManager.default.temporaryDirectory
        let fileUrl = tempDir.appendingPathComponent("\(mediaId).\(fileExtension)")

        do {
            try data.write(to: fileUrl)
            return fileUrl
        } catch {
            print("Error saving temporary media file: \(error)")
            return nil
        }
    }

    private var fileExtension: String {
        switch mediaType {
        case .image:
            return "jpg"
        case .video:
            return "mp4"
        }
    }
}
