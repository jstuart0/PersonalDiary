//
//  SearchViewModel.swift
//  PersonalDiary
//
//  Created by Claude Code on 2025-10-31.
//  Copyright © 2025 Personal Diary Platform. All rights reserved.
//

import Foundation
import Combine

@MainActor
final class SearchViewModel: ObservableObject {
    // MARK: - Published Properties

    @Published var searchQuery = ""
    @Published var results: [Entry] = []
    @Published var isSearching = false
    @Published var errorMessage: String?
    @Published var selectedDateRange: DateRange?
    @Published var selectedTags: [String] = []

    // MARK: - Properties

    private let databaseService = DatabaseService.shared
    private let apiClient = APIClient.shared
    private var searchTask: Task<Void, Never>?
    private var encryptionTier: EncryptionTier = .e2e

    // MARK: - Initialization

    init(encryptionTier: EncryptionTier = .e2e) {
        self.encryptionTier = encryptionTier
    }

    // MARK: - Search

    func search() {
        // Cancel previous search
        searchTask?.cancel()

        guard !searchQuery.isEmpty else {
            results = []
            return
        }

        isSearching = true
        errorMessage = nil

        searchTask = Task {
            do {
                if encryptionTier == .e2e {
                    // Client-side search for E2E
                    results = try await performClientSideSearch()
                } else {
                    // Server-side search for UCE
                    results = try await performServerSearch()
                }

                isSearching = false
            } catch {
                if !(error is CancellationError) {
                    errorMessage = error.localizedDescription
                    isSearching = false
                }
            }
        }
    }

    // MARK: - Client-Side Search (E2E)

    private func performClientSideSearch() async throws -> [Entry] {
        // Fetch all local entries and search in-memory
        var allEntries = try databaseService.fetchEntries()

        // Filter by query
        allEntries = allEntries.filter { entry in
            let titleMatch = entry.title?.localizedCaseInsensitiveContains(searchQuery) ?? false
            let contentMatch = entry.content.localizedCaseInsensitiveContains(searchQuery)
            return titleMatch || contentMatch
        }

        // Filter by date range
        if let dateRange = selectedDateRange {
            allEntries = allEntries.filter { entry in
                entry.createdAt >= dateRange.from && entry.createdAt <= dateRange.to
            }
        }

        // Filter by tags
        if !selectedTags.isEmpty {
            allEntries = allEntries.filter { entry in
                let entryTagNames = Set(entry.tags.map { $0.name })
                return !entryTagNames.isDisjoint(with: Set(selectedTags))
            }
        }

        return allEntries
    }

    // MARK: - Server Search (UCE)

    private func performServerSearch() async throws -> [Entry] {
        let request = SearchRequest(
            query: searchQuery,
            dateFrom: selectedDateRange?.from,
            dateTo: selectedDateRange?.to,
            tags: selectedTags.isEmpty ? nil : selectedTags,
            limit: 50,
            offset: 0
        )

        let response: SearchResponse = try await apiClient.post(.search, body: request)

        // Convert responses to entries
        var entries: [Entry] = []
        for entryResponse in response.entries {
            if let entry = try? entryResponse.toDomainModel() {
                entries.append(entry)
            }
        }

        return entries
    }

    // MARK: - Filters

    func setDateRange(from: Date, to: Date) {
        selectedDateRange = DateRange(from: from, to: to)
        search()
    }

    func clearDateRange() {
        selectedDateRange = nil
        search()
    }

    func toggleTag(_ tag: String) {
        if selectedTags.contains(tag) {
            selectedTags.removeAll { $0 == tag }
        } else {
            selectedTags.append(tag)
        }
        search()
    }

    func clearFilters() {
        selectedDateRange = nil
        selectedTags = []
        search()
    }

    // MARK: - Suggestions

    func getPopularTags() async throws -> [String] {
        let allTags = try databaseService.fetchTags()
        return allTags.prefix(10).map { $0.name }
    }

    func getRecentSearches() -> [String] {
        // Load from UserDefaults
        UserDefaults.standard.stringArray(forKey: "recent_searches") ?? []
    }

    func saveRecentSearch(_ query: String) {
        var recent = getRecentSearches()

        // Remove if already exists
        recent.removeAll { $0 == query }

        // Add to front
        recent.insert(query, at: 0)

        // Keep only last 10
        recent = Array(recent.prefix(10))

        UserDefaults.standard.set(recent, forKey: "recent_searches")
    }

    func clearRecentSearches() {
        UserDefaults.standard.removeObject(forKey: "recent_searches")
    }
}

// MARK: - Supporting Types

struct DateRange {
    let from: Date
    let to: Date
}
