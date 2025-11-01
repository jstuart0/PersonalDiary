//
//  TimelineView.swift
//  PersonalDiary
//
//  Created by Claude Code on 2025-10-31.
//  Copyright © 2025 Personal Diary Platform. All rights reserved.
//

import SwiftUI

struct TimelineView: View {
    @StateObject private var viewModel = TimelineViewModel()
    @State private var showingNewEntry = false
    @State private var selectedEntry: Entry?

    var body: some View {
        NavigationView {
            ZStack {
                Color(.systemGroupedBackground)
                    .ignoresSafeArea()

                VStack(spacing: 0) {
                    // Stats header
                    StatsHeaderView(viewModel: viewModel)
                        .padding()

                    // Entry list
                    if viewModel.entries.isEmpty && !viewModel.isLoading {
                        emptyStateView
                    } else {
                        entryList
                    }
                }
            }
            .navigationTitle("My Diary")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { showingNewEntry = true }) {
                        Image(systemName: "square.and.pencil")
                            .font(.title3)
                    }
                }
            }
            .sheet(isPresented: $showingNewEntry) {
                CreateEntryView()
            }
            .sheet(item: $selectedEntry) { entry in
                EntryDetailView(entry: entry)
            }
            .refreshable {
                await viewModel.refresh()
            }
        }
    }

    // MARK: - Entry List

    private var entryList: some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                ForEach(viewModel.entries) { entry in
                    EntryCard(entry: entry)
                        .onTapGesture {
                            selectedEntry = entry
                        }
                        .contextMenu {
                            Button(action: { selectedEntry = entry }) {
                                Label("View", systemImage: "eye")
                            }

                            Button(role: .destructive, action: { deleteEntry(entry) }) {
                                Label("Delete", systemImage: "trash")
                            }
                        }
                        .onAppear {
                            if entry == viewModel.entries.last {
                                viewModel.loadMore()
                            }
                        }
                }

                if viewModel.isLoading {
                    ProgressView()
                        .padding()
                }
            }
            .padding()
        }
    }

    // MARK: - Empty State

    private var emptyStateView: some View {
        VStack(spacing: 20) {
            Image(systemName: "book.closed")
                .font(.system(size: 60))
                .foregroundColor(.gray)

            Text("No Entries Yet")
                .font(.title2)
                .fontWeight(.semibold)

            Text("Start writing your first diary entry")
                .font(.subheadline)
                .foregroundColor(.secondary)

            Button(action: { showingNewEntry = true }) {
                Label("New Entry", systemImage: "square.and.pencil")
                    .fontWeight(.semibold)
                    .padding()
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(10)
            }
        }
        .frame(maxHeight: .infinity)
    }

    // MARK: - Actions

    private func deleteEntry(_ entry: Entry) {
        Task {
            try? await viewModel.deleteEntry(entry)
        }
    }
}

// MARK: - Stats Header

struct StatsHeaderView: View {
    @ObservedObject var viewModel: TimelineViewModel

    var body: some View {
        HStack(spacing: 16) {
            StatItem(
                title: "Total",
                value: "\(viewModel.getTotalEntriesCount())",
                icon: "book.fill",
                color: .blue
            )

            StatItem(
                title: "This Month",
                value: "\(viewModel.getEntriesThisMonth())",
                icon: "calendar",
                color: .green
            )

            StatItem(
                title: "Streak",
                value: "\(viewModel.getCurrentStreak())",
                icon: "flame.fill",
                color: .orange
            )
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: Color.black.opacity(0.05), radius: 5, x: 0, y: 2)
    }
}

struct StatItem: View {
    let title: String
    let value: String
    let icon: String
    let color: Color

    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundColor(color)

            Text(value)
                .font(.title3)
                .fontWeight(.bold)

            Text(title)
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - Entry Card

struct EntryCard: View {
    let entry: Entry

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Header
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    if let title = entry.title, !title.isEmpty {
                        Text(title)
                            .font(.headline)
                            .lineLimit(1)
                    }

                    Text(entry.createdAt.formatted(date: .long, time: .shortened))
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                Spacer()

                if entry.syncStatus == .pending {
                    Image(systemName: "arrow.triangle.2.circlepath")
                        .foregroundColor(.orange)
                        .font(.caption)
                }

                Image(systemName: "lock.fill")
                    .foregroundColor(entry.encryptionTier == .e2e ? .green : .blue)
                    .font(.caption)
            }

            // Content preview
            Text(entry.content)
                .font(.subheadline)
                .foregroundColor(.primary)
                .lineLimit(3)

            // Tags and media
            HStack {
                if !entry.tags.isEmpty {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 6) {
                            ForEach(entry.tags.prefix(3)) { tag in
                                TagChip(tag: tag)
                            }

                            if entry.tags.count > 3 {
                                Text("+\(entry.tags.count - 3)")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                    }
                }

                Spacer()

                if !entry.media.isEmpty {
                    Label("\(entry.media.count)", systemImage: "photo")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: Color.black.opacity(0.05), radius: 3, x: 0, y: 1)
    }
}

struct TagChip: View {
    let tag: Tag

    var body: some View {
        Text(tag.name)
            .font(.caption)
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(Color.blue.opacity(0.1))
            .foregroundColor(.blue)
            .cornerRadius(6)
    }
}

// MARK: - Preview

#Preview {
    TimelineView()
        .environmentObject(AuthenticationViewModel())
}
