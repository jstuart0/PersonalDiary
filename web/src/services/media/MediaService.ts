/**
 * Media Service
 *
 * Handles media upload, compression, encryption, and management.
 */

import imageCompression from 'browser-image-compression';
import { Media, UploadStatus } from '@/types';
import { db } from '@/services/storage';
import { EncryptionService } from '@/services/encryption';

// ============================================================================
// Types
// ============================================================================

export interface CompressionOptions {
  maxSizeMB: number;
  maxWidthOrHeight: number;
  useWebWorker: boolean;
  fileType?: string;
}

export interface UploadProgress {
  mediaId: string;
  progress: number; // 0-100
  status: 'compressing' | 'encrypting' | 'uploading' | 'completed' | 'failed';
  error?: string;
}

// ============================================================================
// Media Service Class
// ============================================================================

export class MediaService {
  private static instance: MediaService;
  private encryptionService: EncryptionService;
  private uploadProgressCallbacks: Map<string, (progress: UploadProgress) => void> = new Map();

  // Default compression settings
  private readonly DEFAULT_COMPRESSION_OPTIONS: CompressionOptions = {
    maxSizeMB: 1,
    maxWidthOrHeight: 1920,
    useWebWorker: true,
    fileType: 'image/jpeg'
  };

  private constructor() {
    this.encryptionService = EncryptionService.getInstance();
  }

  static getInstance(): MediaService {
    if (!MediaService.instance) {
      MediaService.instance = new MediaService();
    }
    return MediaService.instance;
  }

  /**
   * Upload and process media files
   */
  async uploadMedia(
    entryId: string,
    files: File[],
    onProgress?: (mediaId: string, progress: UploadProgress) => void
  ): Promise<string[]> {
    const mediaIds: string[] = [];

    for (const file of files) {
      try {
        const mediaId = await this.processAndSaveMedia(entryId, file, onProgress);
        mediaIds.push(mediaId);
      } catch (error) {
        console.error('Failed to process media:', file.name, error);
        // Continue with other files
      }
    }

    return mediaIds;
  }

  /**
   * Process single media file
   */
  private async processAndSaveMedia(
    entryId: string,
    file: File,
    onProgress?: (mediaId: string, progress: UploadProgress) => void
  ): Promise<string> {
    const mediaId = this.generateId();

    try {
      // Report compression start
      this.reportProgress(mediaId, {
        mediaId,
        progress: 0,
        status: 'compressing'
      }, onProgress);

      // Compress image
      let processedFile = file;
      if (file.type.startsWith('image/')) {
        processedFile = await this.compressImage(file);
      }

      // Report encryption start
      this.reportProgress(mediaId, {
        mediaId,
        progress: 33,
        status: 'encrypting'
      }, onProgress);

      // Read file as ArrayBuffer
      const arrayBuffer = await this.readFileAsArrayBuffer(processedFile);

      // Encrypt data
      const encryptedData = await this.encryptionService.encryptMedia(arrayBuffer);

      // Generate file hash
      const fileHash = await this.generateFileHash(arrayBuffer);

      // Report upload start
      this.reportProgress(mediaId, {
        mediaId,
        progress: 66,
        status: 'uploading'
      }, onProgress);

      // Get image dimensions if applicable
      let width: number | undefined;
      let height: number | undefined;
      if (file.type.startsWith('image/')) {
        const dimensions = await this.getImageDimensions(processedFile);
        width = dimensions.width;
        height = dimensions.height;
      }

      // Create media record
      const media: Media & { encryptedBlob?: ArrayBuffer } = {
        id: mediaId,
        entryId,
        encryptedBlob: encryptedData,
        mimeType: processedFile.type,
        fileSize: encryptedData.byteLength,
        fileHash,
        width,
        height,
        createdAt: new Date().toISOString(),
        uploadStatus: UploadStatus.UPLOADED
      };

      // Save to IndexedDB
      await db.saveMedia(media);

      // Report completion
      this.reportProgress(mediaId, {
        mediaId,
        progress: 100,
        status: 'completed'
      }, onProgress);

      return mediaId;
    } catch (error) {
      // Report failure
      this.reportProgress(mediaId, {
        mediaId,
        progress: 0,
        status: 'failed',
        error: error instanceof Error ? error.message : 'Unknown error'
      }, onProgress);

      throw error;
    }
  }

  /**
   * Compress image file
   */
  private async compressImage(file: File): Promise<File> {
    try {
      const compressedFile = await imageCompression(file, this.DEFAULT_COMPRESSION_OPTIONS);
      return compressedFile;
    } catch (error) {
      console.warn('Image compression failed, using original:', error);
      return file;
    }
  }

  /**
   * Read file as ArrayBuffer
   */
  private async readFileAsArrayBuffer(file: File): Promise<ArrayBuffer> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result as ArrayBuffer);
      reader.onerror = reject;
      reader.readAsArrayBuffer(file);
    });
  }

  /**
   * Get image dimensions
   */
  private async getImageDimensions(file: File): Promise<{ width: number; height: number }> {
    return new Promise((resolve, reject) => {
      const img = new Image();
      const url = URL.createObjectURL(file);

      img.onload = () => {
        URL.revokeObjectURL(url);
        resolve({
          width: img.naturalWidth,
          height: img.naturalHeight
        });
      };

      img.onerror = () => {
        URL.revokeObjectURL(url);
        reject(new Error('Failed to load image'));
      };

      img.src = url;
    });
  }

  /**
   * Generate file hash
   */
  private async generateFileHash(data: ArrayBuffer): Promise<string> {
    const hashBuffer = await crypto.subtle.digest('SHA-256', data);
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    return hashArray.map((b) => b.toString(16).padStart(2, '0')).join('');
  }

  /**
   * Get media by ID and decrypt
   */
  async getMedia(mediaId: string): Promise<Blob | null> {
    const media = await db.getMedia(mediaId);
    if (!media?.encryptedBlob) {
      return null;
    }

    const decryptedData = await this.encryptionService.decryptMedia(media.encryptedBlob);

    return new Blob([decryptedData], { type: media.mimeType });
  }

  /**
   * Get all media for entry
   */
  async getMediaByEntry(entryId: string): Promise<Media[]> {
    return db.getMediaByEntry(entryId);
  }

  /**
   * Delete media
   */
  async deleteMedia(mediaId: string): Promise<void> {
    await db.deleteMedia(mediaId);
  }

  /**
   * Get total media storage size
   */
  async getStorageSize(): Promise<number> {
    return db.getMediaStorageSize();
  }

  /**
   * Validate file type and size
   */
  validateFile(file: File): { valid: boolean; error?: string } {
    const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];

    if (!ALLOWED_TYPES.includes(file.type)) {
      return {
        valid: false,
        error: 'Only JPEG, PNG, GIF, and WebP images are supported'
      };
    }

    if (file.size > MAX_FILE_SIZE) {
      return {
        valid: false,
        error: 'File size must be less than 10MB'
      };
    }

    return { valid: true };
  }

  /**
   * Validate multiple files
   */
  validateFiles(files: File[]): { valid: boolean; errors: string[] } {
    const MAX_FILES = 10;
    const errors: string[] = [];

    if (files.length > MAX_FILES) {
      errors.push(`Maximum ${MAX_FILES} files allowed`);
    }

    for (const file of files) {
      const validation = this.validateFile(file);
      if (!validation.valid && validation.error) {
        errors.push(`${file.name}: ${validation.error}`);
      }
    }

    return {
      valid: errors.length === 0,
      errors
    };
  }

  // ============================================================================
  // Helper Methods
  // ============================================================================

  /**
   * Generate unique ID
   */
  private generateId(): string {
    return `media_${Date.now()}_${Math.random().toString(36).substring(2, 11)}`;
  }

  /**
   * Report progress to callback
   */
  private reportProgress(
    mediaId: string,
    progress: UploadProgress,
    onProgress?: (mediaId: string, progress: UploadProgress) => void
  ): void {
    if (onProgress) {
      onProgress(mediaId, progress);
    }
  }
}

// Export singleton instance
export const mediaService = MediaService.getInstance();
