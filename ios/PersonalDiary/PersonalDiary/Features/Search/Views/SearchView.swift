//
//  SearchView.swift
//  PersonalDiary
//
//  Created by Claude Code on 2025-10-31.
//  Copyright © 2025 Personal Diary Platform. All rights reserved.
//

import SwiftUI

struct SearchView: View {
    @EnvironmentObject var authViewModel: AuthenticationViewModel
    @StateObject private var viewModel: SearchViewModel
    @State private var showingFilters = false
    @State private var selectedEntry: Entry?

    init() {
        _viewModel = StateObject(wrappedValue: SearchViewModel())
    }

    var body: some View {
        NavigationView {
            ZStack {
                Color(.systemGroupedBackground)
                    .ignoresSafeArea()

                VStack(spacing: 0) {
                    // Search bar
                    searchBar

                    // Active filters
                    if viewModel.selectedDateRange != nil || !viewModel.selectedTags.isEmpty {
                        activeFiltersView
                    }

                    // Content
                    if viewModel.searchQuery.isEmpty {
                        suggestionsView
                    } else if viewModel.isSearching {
                        loadingView
                    } else if viewModel.results.isEmpty {
                        emptyResultsView
                    } else {
                        resultsView
                    }
                }
            }
            .navigationTitle("Search")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { showingFilters = true }) {
                        Image(systemName: "line.3.horizontal.decrease.circle")
                    }
                }
            }
            .sheet(isPresented: $showingFilters) {
                SearchFiltersView(viewModel: viewModel)
            }
            .sheet(item: $selectedEntry) { entry in
                EntryDetailView(entry: entry)
            }
        }
        .onAppear {
            if let tier = authViewModel.currentUser?.encryptionTier {
                viewModel.encryptionTier = tier
            }
        }
    }

    // MARK: - Search Bar

    private var searchBar: some View {
        HStack {
            HStack {
                Image(systemName: "magnifyingglass")
                    .foregroundColor(.secondary)

                TextField("Search entries...", text: $viewModel.searchQuery)
                    .textFieldStyle(.plain)
                    .autocorrectionDisabled()
                    .onSubmit {
                        viewModel.search()
                        viewModel.saveRecentSearch(viewModel.searchQuery)
                    }

                if !viewModel.searchQuery.isEmpty {
                    Button(action: {
                        viewModel.searchQuery = ""
                        viewModel.results = []
                    }) {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(.secondary)
                    }
                }
            }
            .padding(10)
            .background(Color(.systemBackground))
            .cornerRadius(10)

            Button(action: {
                viewModel.search()
                viewModel.saveRecentSearch(viewModel.searchQuery)
            }) {
                Text("Search")
                    .fontWeight(.semibold)
            }
            .disabled(viewModel.searchQuery.isEmpty)
        }
        .padding()
    }

    // MARK: - Active Filters

    private var activeFiltersView: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                if let dateRange = viewModel.selectedDateRange {
                    FilterChip(
                        text: "Date: \(dateRange.from.formatted(date: .abbreviated, time: .omitted)) - \(dateRange.to.formatted(date: .abbreviated, time: .omitted))",
                        onRemove: { viewModel.clearDateRange() }
                    )
                }

                ForEach(viewModel.selectedTags, id: \.self) { tag in
                    FilterChip(
                        text: "Tag: \(tag)",
                        onRemove: { viewModel.toggleTag(tag) }
                    )
                }

                Button(action: { viewModel.clearFilters() }) {
                    Text("Clear All")
                        .font(.caption)
                        .foregroundColor(.red)
                }
            }
            .padding(.horizontal)
            .padding(.bottom, 8)
        }
    }

    // MARK: - Suggestions

    private var suggestionsView: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                // Recent searches
                if !viewModel.getRecentSearches().isEmpty {
                    VStack(alignment: .leading, spacing: 12) {
                        HStack {
                            Text("Recent Searches")
                                .font(.headline)

                            Spacer()

                            Button(action: { viewModel.clearRecentSearches() }) {
                                Text("Clear")
                                    .font(.caption)
                                    .foregroundColor(.blue)
                            }
                        }

                        ForEach(viewModel.getRecentSearches(), id: \.self) { query in
                            Button(action: {
                                viewModel.searchQuery = query
                                viewModel.search()
                            }) {
                                HStack {
                                    Image(systemName: "clock.arrow.circlepath")
                                        .foregroundColor(.secondary)

                                    Text(query)
                                        .foregroundColor(.primary)

                                    Spacer()

                                    Image(systemName: "arrow.up.left")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                                .padding()
                                .background(Color(.systemBackground))
                                .cornerRadius(10)
                            }
                        }
                    }
                }

                // Popular tags
                VStack(alignment: .leading, spacing: 12) {
                    Text("Popular Tags")
                        .font(.headline)

                    FlowLayout(spacing: 8) {
                        ForEach(Array(viewModel.selectedTags.prefix(10)), id: \.self) { tag in
                            Button(action: {
                                viewModel.searchQuery = tag
                                viewModel.search()
                            }) {
                                Text("#\(tag)")
                                    .font(.subheadline)
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 6)
                                    .background(Color.blue.opacity(0.1))
                                    .foregroundColor(.blue)
                                    .cornerRadius(8)
                            }
                        }
                    }
                }
            }
            .padding()
        }
    }

    // MARK: - Loading

    private var loadingView: some View {
        VStack {
            Spacer()

            ProgressView()
                .scaleEffect(1.5)

            Text("Searching...")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .padding(.top)

            Spacer()
        }
    }

    // MARK: - Empty Results

    private var emptyResultsView: some View {
        VStack(spacing: 20) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 60))
                .foregroundColor(.gray)

            Text("No Results Found")
                .font(.title2)
                .fontWeight(.semibold)

            Text("Try different keywords or adjust your filters")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxHeight: .infinity)
        .padding()
    }

    // MARK: - Results

    private var resultsView: some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                ForEach(viewModel.results) { entry in
                    EntryCard(entry: entry)
                        .onTapGesture {
                            selectedEntry = entry
                        }
                }
            }
            .padding()
        }
    }
}

// MARK: - Filter Chip

struct FilterChip: View {
    let text: String
    let onRemove: () -> Void

    var body: some View {
        HStack(spacing: 4) {
            Text(text)
                .font(.caption)

            Button(action: onRemove) {
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

// MARK: - Search Filters Sheet

struct SearchFiltersView: View {
    @Environment(\.dismiss) var dismiss
    @ObservedObject var viewModel: SearchViewModel

    @State private var startDate = Date().addingTimeInterval(-30 * 86400)
    @State private var endDate = Date()
    @State private var availableTags: [String] = []

    var body: some View {
        NavigationView {
            Form {
                Section("Date Range") {
                    Toggle("Filter by date", isOn: Binding(
                        get: { viewModel.selectedDateRange != nil },
                        set: { enabled in
                            if enabled {
                                viewModel.setDateRange(from: startDate, to: endDate)
                            } else {
                                viewModel.clearDateRange()
                            }
                        }
                    ))

                    if viewModel.selectedDateRange != nil {
                        DatePicker("From", selection: $startDate, displayedComponents: .date)
                            .onChange(of: startDate) { _, newValue in
                                viewModel.setDateRange(from: newValue, to: endDate)
                            }

                        DatePicker("To", selection: $endDate, displayedComponents: .date)
                            .onChange(of: endDate) { _, newValue in
                                viewModel.setDateRange(from: startDate, to: newValue)
                            }
                    }
                }

                Section("Tags") {
                    ForEach(availableTags, id: \.self) { tag in
                        Button(action: { viewModel.toggleTag(tag) }) {
                            HStack {
                                Text(tag)
                                    .foregroundColor(.primary)

                                Spacer()

                                if viewModel.selectedTags.contains(tag) {
                                    Image(systemName: "checkmark")
                                        .foregroundColor(.blue)
                                }
                            }
                        }
                    }
                }

                Section {
                    Button("Clear All Filters") {
                        viewModel.clearFilters()
                        dismiss()
                    }
                    .foregroundColor(.red)
                }
            }
            .navigationTitle("Filters")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") {
                        dismiss()
                    }
                }
            }
            .task {
                availableTags = (try? await viewModel.getPopularTags()) ?? []
            }
        }
    }
}

// MARK: - Preview

#Preview {
    SearchView()
        .environmentObject(AuthenticationViewModel())
}
