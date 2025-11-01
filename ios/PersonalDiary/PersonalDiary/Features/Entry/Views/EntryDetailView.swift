//
//  EntryDetailView.swift
//  PersonalDiary
//
//  Created by Claude Code on 2025-10-31.
//  Copyright © 2025 Personal Diary Platform. All rights reserved.
//

import SwiftUI

struct EntryDetailView: View {
    @Environment(\.dismiss) var dismiss
    let entry: Entry

    @State private var showingEditSheet = false
    @State private var showingDeleteAlert = false
    @State private var showingShareSheet = false

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    // Header
                    VStack(alignment: .leading, spacing: 8) {
                        if let title = entry.title, !title.isEmpty {
                            Text(title)
                                .font(.title)
                                .fontWeight(.bold)
                        }

                        HStack {
                            Text(entry.createdAt.formatted(date: .long, time: .shortened))
                                .font(.subheadline)
                                .foregroundColor(.secondary)

                            Spacer()

                            HStack(spacing: 4) {
                                Image(systemName: "lock.fill")
                                    .foregroundColor(entry.encryptionTier == .e2e ? .green : .blue)

                                Text(entry.encryptionTier == .e2e ? "E2E" : "UCE")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }

                        if entry.createdAt != entry.updatedAt {
                            Text("Edited \(entry.updatedAt.relativeDateString())")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                    .padding()
                    .background(Color(.systemBackground))
                    .cornerRadius(12)

                    // Content
                    Text(entry.content)
                        .font(.body)
                        .padding()
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(Color(.systemBackground))
                        .cornerRadius(12)

                    // Tags
                    if !entry.tags.isEmpty {
                        VStack(alignment: .leading, spacing: 12) {
                            Label("Tags", systemImage: "tag.fill")
                                .font(.headline)

                            FlowLayout(spacing: 8) {
                                ForEach(entry.tags) { tag in
                                    TagChip(tag: tag)
                                }
                            }
                        }
                        .padding()
                        .background(Color(.systemBackground))
                        .cornerRadius(12)
                    }

                    // Media
                    if !entry.media.isEmpty {
                        VStack(alignment: .leading, spacing: 12) {
                            Label("Media (\(entry.media.count))", systemImage: "photo.on.rectangle")
                                .font(.headline)

                            LazyVGrid(columns: [
                                GridItem(.flexible()),
                                GridItem(.flexible()),
                                GridItem(.flexible())
                            ], spacing: 12) {
                                ForEach(entry.media) { media in
                                    MediaPreview(media: media)
                                }
                            }
                        }
                        .padding()
                        .background(Color(.systemBackground))
                        .cornerRadius(12)
                    }

                    // Source info
                    if entry.source != .manual {
                        HStack {
                            Image(systemName: entry.source.icon)
                                .foregroundColor(.secondary)

                            Text("Imported from \(entry.source.displayName)")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        .padding()
                    }

                    // Stats
                    HStack(spacing: 20) {
                        StatRow(icon: "text.word.spacing", label: "Words", value: "\(entry.wordCount)")
                        StatRow(icon: "character", label: "Characters", value: "\(entry.content.count)")
                    }
                    .padding()
                    .background(Color(.systemBackground))
                    .cornerRadius(12)
                }
                .padding()
            }
            .background(Color(.systemGroupedBackground))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Done") {
                        dismiss()
                    }
                }

                ToolbarItem(placement: .navigationBarTrailing) {
                    Menu {
                        Button(action: { showingEditSheet = true }) {
                            Label("Edit", systemImage: "pencil")
                        }

                        Button(action: { showingShareSheet = true }) {
                            Label("Share", systemImage: "square.and.arrow.up")
                        }

                        Divider()

                        Button(role: .destructive, action: { showingDeleteAlert = true }) {
                            Label("Delete", systemImage: "trash")
                        }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                }
            }
            .sheet(isPresented: $showingEditSheet) {
                CreateEntryView(entry: entry)
            }
            .alert("Delete Entry", isPresented: $showingDeleteAlert) {
                Button("Cancel", role: .cancel) {}
                Button("Delete", role: .destructive) {
                    // Delete logic handled by parent
                    dismiss()
                }
            } message: {
                Text("Are you sure you want to delete this entry? This action cannot be undone.")
            }
        }
    }
}

// MARK: - Supporting Views

struct MediaPreview: View {
    let media: Media

    var body: some View {
        Rectangle()
            .fill(Color.gray.opacity(0.2))
            .aspectRatio(1, contentMode: .fill)
            .cornerRadius(8)
            .overlay(
                ZStack {
                    Image(systemName: media.type == .photo ? "photo" : "video")
                        .foregroundColor(.gray)

                    if media.type == .video {
                        Image(systemName: "play.circle.fill")
                            .font(.title)
                            .foregroundColor(.white)
                            .shadow(radius: 2)
                    }
                }
            )
    }
}

struct StatRow: View {
    let icon: String
    let label: String
    let value: String

    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: icon)
                .foregroundColor(.secondary)

            VStack(alignment: .leading) {
                Text(value)
                    .font(.headline)

                Text(label)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
    }
}

// MARK: - Extensions

extension Entry {
    var wordCount: Int {
        content.components(separatedBy: .whitespacesAndNewlines)
            .filter { !$0.isEmpty }
            .count
    }
}

extension EntrySource {
    var icon: String {
        switch self {
        case .manual: return "pencil"
        case .facebook: return "f.square.fill"
        }
    }

    var displayName: String {
        switch self {
        case .manual: return "Manual"
        case .facebook: return "Facebook"
        }
    }
}

extension Date {
    func relativeDateString() -> String {
        let formatter = RelativeDateTimeFormatter()
        formatter.unitsStyle = .full
        return formatter.localizedString(for: self, relativeTo: Date())
    }
}

// MARK: - Preview

#Preview {
    EntryDetailView(entry: Entry(
        id: UUID(),
        title: "Sample Entry",
        content: "This is a sample entry with some content to display.",
        createdAt: Date(),
        updatedAt: Date(),
        isEncrypted: true,
        encryptionTier: .e2e,
        tags: [
            Tag(id: UUID(), name: "sample", isAutoGenerated: false),
            Tag(id: UUID(), name: "preview", isAutoGenerated: true)
        ],
        media: [],
        source: .manual,
        sourceID: nil,
        syncStatus: .synced,
        lastSyncedAt: Date(),
        serverID: nil,
        serverUpdatedAt: nil
    ))
}
