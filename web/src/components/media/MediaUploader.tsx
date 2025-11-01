/**
 * Media Uploader Component
 *
 * Drag-and-drop media uploader with progress indicators.
 */

import React, { useState, useRef, useCallback } from 'react';
import {
  PhotoIcon,
  XMarkIcon,
  CheckCircleIcon,
  ExclamationCircleIcon
} from '@heroicons/react/24/outline';
import { Button } from '@/components/common/Button';
import { mediaService, UploadProgress } from '@/services/media';

// ============================================================================
// Types
// ============================================================================

export interface MediaUploaderProps {
  entryId: string;
  onUploadComplete: (mediaIds: string[]) => void;
  onCancel: () => void;
  maxFiles?: number;
}

interface FileWithProgress {
  file: File;
  preview: string;
  mediaId?: string;
  progress: number;
  status: 'pending' | 'uploading' | 'completed' | 'failed';
  error?: string;
}

// ============================================================================
// Component
// ============================================================================

export function MediaUploader({
  entryId,
  onUploadComplete,
  onCancel,
  maxFiles = 10
}: MediaUploaderProps) {
  const [files, setFiles] = useState<FileWithProgress[]>([]);
  const [isDragging, setIsDragging] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  /**
   * Handle file selection
   */
  const handleFilesAdded = useCallback((selectedFiles: FileList | null) => {
    if (!selectedFiles) return;

    const fileArray = Array.from(selectedFiles);

    // Validate files
    const validation = mediaService.validateFiles(fileArray);
    if (!validation.valid) {
      alert(validation.errors.join('\n'));
      return;
    }

    // Check max files
    if (files.length + fileArray.length > maxFiles) {
      alert(`Maximum ${maxFiles} files allowed`);
      return;
    }

    // Create preview URLs and add to state
    const newFiles: FileWithProgress[] = fileArray.map((file) => ({
      file,
      preview: URL.createObjectURL(file),
      progress: 0,
      status: 'pending' as const
    }));

    setFiles((prev) => [...prev, ...newFiles]);
  }, [files.length, maxFiles]);

  /**
   * Handle drag and drop
   */
  const handleDragEnter = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);

    const droppedFiles = e.dataTransfer.files;
    handleFilesAdded(droppedFiles);
  };

  /**
   * Handle file input change
   */
  const handleFileInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    handleFilesAdded(e.target.files);
  };

  /**
   * Remove file
   */
  const handleRemoveFile = (index: number) => {
    setFiles((prev) => {
      const newFiles = [...prev];
      URL.revokeObjectURL(newFiles[index].preview);
      newFiles.splice(index, 1);
      return newFiles;
    });
  };

  /**
   * Handle upload progress
   */
  const handleProgress = (index: number) => (mediaId: string, progress: UploadProgress) => {
    setFiles((prev) => {
      const newFiles = [...prev];
      newFiles[index] = {
        ...newFiles[index],
        mediaId,
        progress: progress.progress,
        status: progress.status === 'completed' ? 'completed' :
                progress.status === 'failed' ? 'failed' : 'uploading',
        error: progress.error
      };
      return newFiles;
    });
  };

  /**
   * Upload all files
   */
  const handleUpload = async () => {
    if (files.length === 0) return;

    setIsUploading(true);

    const uploadPromises = files.map(async (fileItem, index) => {
      if (fileItem.status === 'completed') {
        return fileItem.mediaId!;
      }

      const mediaIds = await mediaService.uploadMedia(
        entryId,
        [fileItem.file],
        handleProgress(index)
      );

      return mediaIds[0];
    });

    try {
      const mediaIds = await Promise.all(uploadPromises);
      onUploadComplete(mediaIds.filter(Boolean));

      // Cleanup preview URLs
      files.forEach((f) => URL.revokeObjectURL(f.preview));
    } catch (error) {
      console.error('Upload failed:', error);
      alert('Some files failed to upload. Please try again.');
    } finally {
      setIsUploading(false);
    }
  };

  /**
   * Handle cancel
   */
  const handleCancel = () => {
    // Cleanup preview URLs
    files.forEach((f) => URL.revokeObjectURL(f.preview));
    onCancel();
  };

  const allCompleted = files.length > 0 && files.every((f) => f.status === 'completed');
  const hasFiles = files.length > 0;

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="flex items-center justify-between p-4 border-b">
        <h2 className="text-lg font-semibold">Upload Photos</h2>
        <Button
          variant="ghost"
          size="sm"
          onClick={handleCancel}
          disabled={isUploading}
          aria-label="Close uploader"
        >
          <XMarkIcon className="h-5 w-5" />
        </Button>
      </div>

      {/* Drop Zone */}
      {!hasFiles && (
        <div
          className={`flex-1 m-4 border-2 border-dashed rounded-lg transition-colors ${
            isDragging
              ? 'border-primary bg-primary/5'
              : 'border-muted-foreground/25 hover:border-primary/50'
          }`}
          onDragEnter={handleDragEnter}
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
        >
          <div className="flex flex-col items-center justify-center h-full p-8 text-center">
            <PhotoIcon className="h-16 w-16 text-muted-foreground mb-4" />
            <h3 className="text-lg font-medium mb-2">
              Drag and drop photos here
            </h3>
            <p className="text-sm text-muted-foreground mb-4">
              or click to browse (max {maxFiles} files)
            </p>
            <Button
              onClick={() => fileInputRef.current?.click()}
              variant="outline"
            >
              Browse Files
            </Button>
            <input
              ref={fileInputRef}
              type="file"
              multiple
              accept="image/jpeg,image/png,image/gif,image/webp"
              onChange={handleFileInputChange}
              className="hidden"
            />
          </div>
        </div>
      )}

      {/* File Preview Grid */}
      {hasFiles && (
        <div className="flex-1 overflow-y-auto p-4">
          <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
            {files.map((fileItem, index) => (
              <div
                key={index}
                className="relative aspect-square rounded-lg overflow-hidden border bg-muted"
              >
                {/* Image Preview */}
                <img
                  src={fileItem.preview}
                  alt={fileItem.file.name}
                  className="w-full h-full object-cover"
                />

                {/* Status Overlay */}
                {fileItem.status !== 'pending' && (
                  <div className="absolute inset-0 bg-black/50 flex items-center justify-center">
                    {fileItem.status === 'uploading' && (
                      <div className="text-white text-center">
                        <div className="mb-2">
                          <div className="animate-spin h-8 w-8 border-4 border-white border-t-transparent rounded-full mx-auto" />
                        </div>
                        <div className="text-sm">{fileItem.progress}%</div>
                      </div>
                    )}
                    {fileItem.status === 'completed' && (
                      <CheckCircleIcon className="h-12 w-12 text-green-500" />
                    )}
                    {fileItem.status === 'failed' && (
                      <div className="text-white text-center">
                        <ExclamationCircleIcon className="h-12 w-12 text-red-500 mx-auto mb-2" />
                        <div className="text-xs">{fileItem.error || 'Failed'}</div>
                      </div>
                    )}
                  </div>
                )}

                {/* Remove Button */}
                {fileItem.status === 'pending' && !isUploading && (
                  <button
                    onClick={() => handleRemoveFile(index)}
                    className="absolute top-2 right-2 p-1 rounded-full bg-black/50 text-white hover:bg-black/70"
                    aria-label="Remove file"
                  >
                    <XMarkIcon className="h-4 w-4" />
                  </button>
                )}
              </div>
            ))}

            {/* Add More Button */}
            {files.length < maxFiles && !isUploading && (
              <button
                onClick={() => fileInputRef.current?.click()}
                className="aspect-square rounded-lg border-2 border-dashed border-muted-foreground/25 hover:border-primary/50 flex items-center justify-center bg-muted/50 hover:bg-muted transition-colors"
              >
                <div className="text-center">
                  <PhotoIcon className="h-8 w-8 text-muted-foreground mx-auto mb-2" />
                  <div className="text-sm text-muted-foreground">Add More</div>
                </div>
              </button>
            )}
          </div>

          <input
            ref={fileInputRef}
            type="file"
            multiple
            accept="image/jpeg,image/png,image/gif,image/webp"
            onChange={handleFileInputChange}
            className="hidden"
          />
        </div>
      )}

      {/* Actions */}
      {hasFiles && (
        <div className="flex items-center justify-between p-4 border-t">
          <div className="text-sm text-muted-foreground">
            {files.length} {files.length === 1 ? 'photo' : 'photos'} selected
          </div>
          <div className="flex gap-2">
            <Button
              variant="ghost"
              onClick={handleCancel}
              disabled={isUploading}
            >
              Cancel
            </Button>
            <Button
              onClick={handleUpload}
              disabled={isUploading || allCompleted}
              loading={isUploading}
            >
              {allCompleted ? 'Done' : 'Upload'}
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
