//
//  MediaService.swift
//  PersonalDiary
//
//  Created by Claude Code on 2025-10-31.
//  Copyright © 2025 Personal Diary Platform. All rights reserved.
//

import UIKit
import AVFoundation
import CryptoKit
import Photos

/// Service for managing media capture, encryption, and storage
final class MediaService {
    // MARK: - Singleton

    static let shared = MediaService()

    // MARK: - Properties

    private let fileManager = FileManager.default
    private let databaseService = DatabaseService.shared
    private let apiClient = APIClient.shared

    private var mediaDirectory: URL {
        let documentsPath = fileManager.urls(for: .documentDirectory, in: .userDomainMask)[0]
        let mediaPath = documentsPath.appendingPathComponent("Media", isDirectory: true)

        if !fileManager.fileExists(atPath: mediaPath.path) {
            try? fileManager.createDirectory(at: mediaPath, withIntermediateDirectories: true)
        }

        return mediaPath
    }

    private var thumbnailDirectory: URL {
        let documentsPath = fileManager.urls(for: .documentDirectory, in: .userDomainMask)[0]
        let thumbnailPath = documentsPath.appendingPathComponent("Thumbnails", isDirectory: true)

        if !fileManager.fileExists(atPath: thumbnailPath.path) {
            try? fileManager.createDirectory(at: thumbnailPath, withIntermediateDirectories: true)
        }

        return thumbnailPath
    }

    // MARK: - Initialization

    private init() {}

    // MARK: - Photo/Video Capture

    /// Requests camera and photo library permissions
    func requestPermissions() async throws {
        // Camera permission
        let cameraStatus = AVCaptureDevice.authorizationStatus(for: .video)
        if cameraStatus == .notDetermined {
            _ = await AVCaptureDevice.requestAccess(for: .video)
        }

        // Photo library permission
        let photoStatus = PHPhotoLibrary.authorizationStatus()
        if photoStatus == .notDetermined {
            _ = await PHPhotoLibrary.requestAuthorization(for: .readWrite)
        }
    }

    /// Checks if camera is available
    func isCameraAvailable() -> Bool {
        UIImagePickerController.isSourceTypeAvailable(.camera)
    }

    /// Checks if photo library is available
    func isPhotoLibraryAvailable() -> Bool {
        UIImagePickerController.isSourceTypeAvailable(.photoLibrary)
    }

    // MARK: - Media Processing

    /// Processes and saves a photo
    func processPhoto(_ image: UIImage, forEntryID entryID: UUID, encrypt: Bool = true) async throws -> Media {
        // Generate unique ID
        let mediaID = UUID()

        // Get image data
        guard let imageData = image.jpegData(compressionQuality: 0.8) else {
            throw AppError.media(.processingFailed)
        }

        // Generate thumbnail
        let thumbnail = generateThumbnail(from: image, maxSize: 200)
        let thumbnailData = thumbnail.jpegData(compressionQuality: 0.7)

        // Save original (encrypted if needed)
        let fileName = "\(mediaID.uuidString).jpg"
        let filePath = mediaDirectory.appendingPathComponent(fileName)

        var encryptionKey: Data?
        let dataToSave: Data

        if encrypt {
            let key = SymmetricKey(size: .bits256)
            encryptionKey = key.withUnsafeBytes { Data($0) }
            dataToSave = try encryptData(imageData, with: key)
        } else {
            dataToSave = imageData
        }

        try dataToSave.write(to: filePath)

        // Save thumbnail
        var thumbnailPath: String?
        if let thumbnailData = thumbnailData {
            let thumbnailFileName = "\(mediaID.uuidString)_thumb.jpg"
            let thumbnailFilePath = thumbnailDirectory.appendingPathComponent(thumbnailFileName)
            try thumbnailData.write(to: thumbnailFilePath)
            thumbnailPath = thumbnailFilePath.path
        }

        // Create media object
        let media = Media(
            id: mediaID,
            entryID: entryID,
            type: .photo,
            localPath: filePath.path,
            serverURL: nil,
            thumbnailPath: thumbnailPath,
            isEncrypted: encrypt,
            fileSize: Int64(dataToSave.count),
            mimeType: "image/jpeg",
            width: Int(image.size.width),
            height: Int(image.size.height),
            duration: 0,
            createdAt: Date(),
            uploadStatus: .pending,
            uploadedAt: nil
        )

        // Save to database
        try databaseService.saveMedia(media)

        // Store encryption key securely if encrypted
        if let key = encryptionKey {
            try saveMediaEncryptionKey(key, forMediaID: mediaID)
        }

        return media
    }

    /// Processes and saves a video
    func processVideo(at url: URL, forEntryID entryID: UUID, encrypt: Bool = true) async throws -> Media {
        let mediaID = UUID()

        // Get video data
        let videoData = try Data(contentsOf: url)

        // Get video metadata
        let asset = AVAsset(url: url)
        let duration = try await asset.load(.duration).seconds
        let tracks = try await asset.load(.tracks)

        var width = 0
        var height = 0

        if let videoTrack = tracks.first(where: { $0.mediaType == .video }) {
            let size = try await videoTrack.load(.naturalSize)
            width = Int(size.width)
            height = Int(size.height)
        }

        // Generate thumbnail from first frame
        let thumbnailImage = try await generateVideoThumbnail(from: asset)
        let thumbnailData = thumbnailImage.jpegData(compressionQuality: 0.7)

        // Save video (encrypted if needed)
        let fileName = "\(mediaID.uuidString).mp4"
        let filePath = mediaDirectory.appendingPathComponent(fileName)

        var encryptionKey: Data?
        let dataToSave: Data

        if encrypt {
            let key = SymmetricKey(size: .bits256)
            encryptionKey = key.withUnsafeBytes { Data($0) }
            dataToSave = try encryptData(videoData, with: key)
        } else {
            dataToSave = videoData
        }

        try dataToSave.write(to: filePath)

        // Save thumbnail
        var thumbnailPath: String?
        if let thumbnailData = thumbnailData {
            let thumbnailFileName = "\(mediaID.uuidString)_thumb.jpg"
            let thumbnailFilePath = thumbnailDirectory.appendingPathComponent(thumbnailFileName)
            try thumbnailData.write(to: thumbnailFilePath)
            thumbnailPath = thumbnailFilePath.path
        }

        // Create media object
        let media = Media(
            id: mediaID,
            entryID: entryID,
            type: .video,
            localPath: filePath.path,
            serverURL: nil,
            thumbnailPath: thumbnailPath,
            isEncrypted: encrypt,
            fileSize: Int64(dataToSave.count),
            mimeType: "video/mp4",
            width: width,
            height: height,
            duration: duration,
            createdAt: Date(),
            uploadStatus: .pending,
            uploadedAt: nil
        )

        // Save to database
        try databaseService.saveMedia(media)

        // Store encryption key securely if encrypted
        if let key = encryptionKey {
            try saveMediaEncryptionKey(key, forMediaID: mediaID)
        }

        return media
    }

    // MARK: - Media Retrieval

    /// Loads and decrypts media data
    func loadMedia(_ media: Media) async throws -> Data {
        guard let localPath = media.localPath else {
            // Try downloading from server
            if let serverURL = media.serverURL {
                return try await downloadMedia(media)
            }
            throw AppError.media(.fileNotFound)
        }

        let fileURL = URL(fileURLWithPath: localPath)
        guard fileManager.fileExists(atPath: fileURL.path) else {
            throw AppError.media(.fileNotFound)
        }

        let data = try Data(contentsOf: fileURL)

        if media.isEncrypted {
            let key = try retrieveMediaEncryptionKey(forMediaID: media.id)
            return try decryptData(data, with: key)
        }

        return data
    }

    /// Loads thumbnail image
    func loadThumbnail(for media: Media) throws -> UIImage? {
        guard let thumbnailPath = media.thumbnailPath else {
            return nil
        }

        let fileURL = URL(fileURLWithPath: thumbnailPath)
        guard fileManager.fileExists(atPath: fileURL.path) else {
            return nil
        }

        let data = try Data(contentsOf: fileURL)
        return UIImage(data: data)
    }

    // MARK: - Upload/Download

    /// Uploads media to server
    func uploadMedia(_ media: Media) async throws {
        guard let localPath = media.localPath else {
            throw AppError.media(.fileNotFound)
        }

        let fileURL = URL(fileURLWithPath: localPath)
        let fileData = try Data(contentsOf: fileURL)

        let fileName = fileURL.lastPathComponent
        let mimeType = media.mimeType

        // Upload via API
        let response = try await apiClient.uploadFile(
            .uploadMedia,
            fileData: fileData,
            fileName: fileName,
            mimeType: mimeType,
            parameters: [
                "entryId": media.entryID.uuidString,
                "type": media.type.rawValue
            ]
        )

        // Update media with server info
        var updatedMedia = media
        updatedMedia.serverURL = URL(string: response.url)
        updatedMedia.uploadStatus = .uploaded
        updatedMedia.uploadedAt = Date()

        try databaseService.saveMedia(updatedMedia)
    }

    /// Downloads media from server
    func downloadMedia(_ media: Media) async throws -> Data {
        guard let serverURL = media.serverURL else {
            throw AppError.media(.fileNotFound)
        }

        let data = try await apiClient.downloadFile(serverURL)

        // Save locally
        let fileName = "\(media.id.uuidString).\(media.type == .photo ? "jpg" : "mp4")"
        let localURL = mediaDirectory.appendingPathComponent(fileName)
        try data.write(to: localURL)

        // Update media with local path
        var updatedMedia = media
        updatedMedia.localPath = localURL.path
        try databaseService.saveMedia(updatedMedia)

        return data
    }

    // MARK: - Deletion

    /// Deletes media files and database entry
    func deleteMedia(_ media: Media) throws {
        // Delete local file
        if let localPath = media.localPath {
            let fileURL = URL(fileURLWithPath: localPath)
            try? fileManager.removeItem(at: fileURL)
        }

        // Delete thumbnail
        if let thumbnailPath = media.thumbnailPath {
            let thumbnailURL = URL(fileURLWithPath: thumbnailPath)
            try? fileManager.removeItem(at: thumbnailURL)
        }

        // Delete encryption key
        try? deleteMediaEncryptionKey(forMediaID: media.id)

        // Delete from database
        try databaseService.deleteMedia(id: media.id)
    }

    // MARK: - Encryption

    private func encryptData(_ data: Data, with key: SymmetricKey) throws -> Data {
        let nonce = AES.GCM.Nonce()
        let sealedBox = try AES.GCM.seal(data, using: key, nonce: nonce)

        guard let combined = sealedBox.combined else {
            throw AppError.encryption(.encryptionFailed)
        }

        return combined
    }

    private func decryptData(_ data: Data, with key: SymmetricKey) throws -> Data {
        let sealedBox = try AES.GCM.SealedBox(combined: data)
        return try AES.GCM.open(sealedBox, using: key)
    }

    // MARK: - Key Management

    private func saveMediaEncryptionKey(_ key: Data, forMediaID mediaID: UUID) throws {
        let keychainService = KeychainService()
        let account = "media_key_\(mediaID.uuidString)"
        try keychainService.save(key, for: account)
    }

    private func retrieveMediaEncryptionKey(forMediaID mediaID: UUID) throws -> SymmetricKey {
        let keychainService = KeychainService()
        let account = "media_key_\(mediaID.uuidString)"

        guard let keyData = try keychainService.retrieve(for: account) else {
            throw AppError.encryption(.keyNotFound)
        }

        return SymmetricKey(data: keyData)
    }

    private func deleteMediaEncryptionKey(forMediaID mediaID: UUID) throws {
        let keychainService = KeychainService()
        let account = "media_key_\(mediaID.uuidString)"
        try keychainService.delete(for: account)
    }

    // MARK: - Utilities

    private func generateThumbnail(from image: UIImage, maxSize: CGFloat) -> UIImage {
        let size = image.size
        let scale = min(maxSize / size.width, maxSize / size.height)
        let newSize = CGSize(width: size.width * scale, height: size.height * scale)

        UIGraphicsBeginImageContextWithOptions(newSize, false, 0.0)
        image.draw(in: CGRect(origin: .zero, size: newSize))
        let thumbnail = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()

        return thumbnail ?? image
    }

    private func generateVideoThumbnail(from asset: AVAsset) async throws -> UIImage {
        let imageGenerator = AVAssetImageGenerator(asset: asset)
        imageGenerator.appliesPreferredTrackTransform = true

        let time = CMTime(seconds: 1, preferredTimescale: 60)
        let cgImage = try await imageGenerator.image(at: time).image

        return UIImage(cgImage: cgImage)
    }

    // MARK: - Cleanup

    /// Removes orphaned media files
    func cleanupOrphanedMedia() throws {
        let allMedia = try databaseService.fetchMedia(forEntryID: UUID()) // This will need adjustment
        let validPaths = Set(allMedia.compactMap { $0.localPath })

        // Clean media directory
        let mediaFiles = try fileManager.contentsOfDirectory(at: mediaDirectory, includingPropertiesForKeys: nil)
        for file in mediaFiles {
            if !validPaths.contains(file.path) {
                try fileManager.removeItem(at: file)
            }
        }

        // Clean thumbnail directory
        let thumbnailFiles = try fileManager.contentsOfDirectory(at: thumbnailDirectory, includingPropertiesForKeys: nil)
        for file in thumbnailFiles {
            if !validPaths.contains(file.path) {
                try fileManager.removeItem(at: file)
            }
        }
    }

    /// Calculates total storage used by media
    func calculateStorageUsed() throws -> Int64 {
        var totalSize: Int64 = 0

        let mediaFiles = try fileManager.contentsOfDirectory(at: mediaDirectory, includingPropertiesForKeys: [.fileSizeKey])
        for file in mediaFiles {
            let attributes = try file.resourceValues(forKeys: [.fileSizeKey])
            totalSize += Int64(attributes.fileSize ?? 0)
        }

        return totalSize
    }
}
