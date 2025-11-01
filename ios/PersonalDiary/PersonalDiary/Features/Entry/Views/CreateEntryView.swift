//
//  CreateEntryView.swift
//  PersonalDiary
//
//  Created by Claude Code on 2025-10-31.
//  Copyright © 2025 Personal Diary Platform. All rights reserved.
//

import SwiftUI
import PhotosUI

struct CreateEntryView: View {
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var authViewModel: AuthenticationViewModel
    @StateObject private var viewModel: EntryViewModel

    @State private var showingPhotoPicker = false
    @State private var showingCamera = false
    @State private var showingTagInput = false
    @State private var newTagName = ""
    @FocusState private var contentFocused: Bool

    init(entry: Entry? = nil) {
        _viewModel = StateObject(wrappedValue: EntryViewModel(entry: entry))
    }

    var body: some View {
        NavigationView {
            ZStack {
                Color(.systemGroupedBackground)
                    .ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 16) {
                        // Title input
                        TextField("Title (optional)", text: $viewModel.title)
                            .font(.title2)
                            .fontWeight(.semibold)
                            .padding()
                            .background(Color(.systemBackground))
                            .cornerRadius(10)

                        // Content input
                        TextEditor(text: $viewModel.content)
                            .frame(minHeight: 200)
                            .padding(8)
                            .background(Color(.systemBackground))
                            .cornerRadius(10)
                            .focused($contentFocused)
                            .overlay(
                                Group {
                                    if viewModel.content.isEmpty {
                                        Text("What's on your mind?")
                                            .foregroundColor(.secondary)
                                            .padding(.top, 16)
                                            .padding(.leading, 12)
                                            .allowsHitTesting(false)
                                    }
                                },
                                alignment: .topLeading
                            )

                        // Word/character count
                        HStack {
                            Text("\(viewModel.wordCount) words")
                                .font(.caption)
                                .foregroundColor(.secondary)

                            Text("•")
                                .foregroundColor(.secondary)

                            Text("\(viewModel.characterCount) characters")
                                .font(.caption)
                                .foregroundColor(.secondary)

                            Spacer()
                        }
                        .padding(.horizontal)

                        // Tags section
                        VStack(alignment: .leading, spacing: 12) {
                            HStack {
                                Label("Tags", systemImage: "tag.fill")
                                    .font(.headline)

                                Spacer()

                                Button(action: { viewModel.generateAutoTags() }) {
                                    Text("Auto-generate")
                                        .font(.caption)
                                        .foregroundColor(.blue)
                                }

                                Button(action: { showingTagInput = true }) {
                                    Image(systemName: "plus.circle.fill")
                                        .foregroundColor(.blue)
                                }
                            }

                            if !viewModel.tags.isEmpty {
                                FlowLayout(spacing: 8) {
                                    ForEach(viewModel.tags) { tag in
                                        TagChipEditable(tag: tag) {
                                            viewModel.removeTag(tag)
                                        }
                                    }
                                }
                            } else {
                                Text("No tags added")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                        .padding()
                        .background(Color(.systemBackground))
                        .cornerRadius(10)

                        // Media section
                        VStack(alignment: .leading, spacing: 12) {
                            HStack {
                                Label("Media", systemImage: "photo.on.rectangle")
                                    .font(.headline)

                                Spacer()

                                Button(action: { showingCamera = true }) {
                                    Image(systemName: "camera.fill")
                                        .foregroundColor(.blue)
                                }

                                Button(action: { showingPhotoPicker = true }) {
                                    Image(systemName: "photo.fill")
                                        .foregroundColor(.blue)
                                }
                            }

                            if !viewModel.media.isEmpty {
                                ScrollView(.horizontal, showsIndicators: false) {
                                    HStack(spacing: 12) {
                                        ForEach(viewModel.media) { media in
                                            MediaThumbnail(media: media) {
                                                Task {
                                                    try? await viewModel.removeMedia(media)
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text("No media added")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                        .padding()
                        .background(Color(.systemBackground))
                        .cornerRadius(10)

                        // Error message
                        if let error = viewModel.errorMessage {
                            Text(error)
                                .font(.caption)
                                .foregroundColor(.red)
                                .padding()
                        }
                    }
                    .padding()
                }
            }
            .navigationTitle(viewModel.title.isEmpty ? "New Entry" : "Edit Entry")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        dismiss()
                    }
                }

                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: saveEntry) {
                        if viewModel.isSaving {
                            ProgressView()
                        } else {
                            Text("Save")
                                .fontWeight(.semibold)
                        }
                    }
                    .disabled(!viewModel.isValid || viewModel.isSaving)
                }
            }
            .onAppear {
                contentFocused = true
            }
            .sheet(isPresented: $showingPhotoPicker) {
                PhotoPickerView { image in
                    Task {
                        let tier = authViewModel.currentUser?.encryptionTier ?? .e2e
                        try? await viewModel.addPhoto(image, encryptionTier: tier)
                    }
                }
            }
            .sheet(isPresented: $showingCamera) {
                CameraView { image in
                    Task {
                        let tier = authViewModel.currentUser?.encryptionTier ?? .e2e
                        try? await viewModel.addPhoto(image, encryptionTier: tier)
                    }
                }
            }
            .alert("Add Tag", isPresented: $showingTagInput) {
                TextField("Tag name", text: $newTagName)
                Button("Cancel", role: .cancel) {
                    newTagName = ""
                }
                Button("Add") {
                    Task {
                        try? await viewModel.addTag(newTagName)
                        newTagName = ""
                    }
                }
            }
        }
    }

    // MARK: - Actions

    private func saveEntry() {
        Task {
            do {
                let tier = authViewModel.currentUser?.encryptionTier ?? .e2e
                try await viewModel.save(encryptionTier: tier)
                dismiss()
            } catch {
                // Error handled by viewModel
            }
        }
    }
}

// MARK: - Supporting Views

struct TagChipEditable: View {
    let tag: Tag
    let onDelete: () -> Void

    var body: some View {
        HStack(spacing: 4) {
            Text(tag.name)
                .font(.caption)

            Button(action: onDelete) {
                Image(systemName: "xmark.circle.fill")
                    .font(.caption)
            }
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 6)
        .background(Color.blue.opacity(0.1))
        .foregroundColor(.blue)
        .cornerRadius(8)
    }
}

struct MediaThumbnail: View {
    let media: Media
    let onDelete: () -> Void

    var body: some View {
        ZStack(alignment: .topTrailing) {
            // Thumbnail image
            Rectangle()
                .fill(Color.gray.opacity(0.2))
                .frame(width: 100, height: 100)
                .cornerRadius(8)
                .overlay(
                    Image(systemName: media.type == .photo ? "photo" : "video")
                        .foregroundColor(.gray)
                )

            // Delete button
            Button(action: onDelete) {
                Image(systemName: "xmark.circle.fill")
                    .foregroundColor(.white)
                    .background(Circle().fill(Color.black.opacity(0.5)))
                    .font(.title3)
            }
            .padding(4)
        }
    }
}

// MARK: - Flow Layout (for tags)

struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let result = FlowResult(
            in: proposal.replacingUnspecifiedDimensions().width,
            subviews: subviews,
            spacing: spacing
        )
        return result.size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = FlowResult(
            in: bounds.width,
            subviews: subviews,
            spacing: spacing
        )

        for (index, subview) in subviews.enumerated() {
            subview.place(at: result.positions[index], proposal: .unspecified)
        }
    }

    struct FlowResult {
        var size: CGSize = .zero
        var positions: [CGPoint] = []

        init(in maxWidth: CGFloat, subviews: Subviews, spacing: CGFloat) {
            var x: CGFloat = 0
            var y: CGFloat = 0
            var lineHeight: CGFloat = 0

            for subview in subviews {
                let size = subview.sizeThatFits(.unspecified)

                if x + size.width > maxWidth, x > 0 {
                    x = 0
                    y += lineHeight + spacing
                    lineHeight = 0
                }

                positions.append(CGPoint(x: x, y: y))
                lineHeight = max(lineHeight, size.height)
                x += size.width + spacing
            }

            self.size = CGSize(width: maxWidth, height: y + lineHeight)
        }
    }
}

// MARK: - Preview

#Preview {
    CreateEntryView()
        .environmentObject(AuthenticationViewModel())
}
