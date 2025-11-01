//
//  TimelineViewModel.swift
//  PersonalDiary
//
//  Created by Claude Code on 2025-10-31.
//  Copyright © 2025 Personal Diary Platform. All rights reserved.
//

import Foundation
import Combine

@MainActor
final class TimelineViewModel: ObservableObject {
    // MARK: - Published Properties

    @Published var entries: [Entry] = []
    @Published var isLoading = false
    @Published var isRefreshing = false
    @Published var errorMessage: String?
    @Published var hasMore = true

    // MARK: - Properties

    private let databaseService = DatabaseService.shared
    private let syncService = SyncService.shared
    private var currentPage = 0
    private let pageSize = 20

    // MARK: - Initialization

    init() {
        loadEntries()
    }

    // MARK: - Data Loading

    func loadEntries() {
        guard !isLoading else { return }

        isLoading = true
        errorMessage = nil

        Task {
            do {
                entries = try databaseService.fetchEntries(limit: pageSize)
                currentPage = 1
                hasMore = entries.count >= pageSize
                isLoading = false
            } catch {
                errorMessage = error.localizedDescription
                isLoading = false
            }
        }
    }

    func loadMore() {
        guard !isLoading, hasMore else { return }

        isLoading = true

        Task {
            do {
                let offset = currentPage * pageSize
                let moreEntries = try databaseService.fetchEntries(
                    sortDescriptors: [NSSortDescriptor(key: "createdAt", ascending: false)],
                    limit: pageSize
                )

                // Skip entries we already have
                let newEntries = Array(moreEntries.dropFirst(offset))

                if !newEntries.isEmpty {
                    entries.append(contentsOf: newEntries)
                    currentPage += 1
                    hasMore = newEntries.count >= pageSize
                } else {
                    hasMore = false
                }

                isLoading = false
            } catch {
                errorMessage = error.localizedDescription
                isLoading = false
            }
        }
    }

    func refresh() async {
        isRefreshing = true
        errorMessage = nil

        do {
            // Sync with server
            await syncService.syncIncremental()

            // Reload entries
            entries = try databaseService.fetchEntries(limit: pageSize)
            currentPage = 1
            hasMore = entries.count >= pageSize

            isRefreshing = false
        } catch {
            errorMessage = error.localizedDescription
            isRefreshing = false
        }
    }

    // MARK: - Entry Management

    func deleteEntry(_ entry: Entry) async throws {
        do {
            try databaseService.deleteEntry(id: entry.id)

            // Remove from local array
            entries.removeAll { $0.id == entry.id }

            // Sync deletion
            await syncService.syncIncremental()
        } catch {
            throw error
        }
    }

    func toggleFavorite(_ entry: Entry) async throws {
        // This would require adding a favorite field to Entry model
        // For MVP, we can skip this feature
        throw AppError.validation(.notImplemented)
    }

    // MARK: - Filtering

    func filterByDate(from: Date, to: Date) {
        isLoading = true

        Task {
            do {
                entries = try databaseService.fetchEntries(from: from, to: to)
                hasMore = false // No pagination for filtered results
                isLoading = false
            } catch {
                errorMessage = error.localizedDescription
                isLoading = false
            }
        }
    }

    func filterByTag(_ tag: String) {
        isLoading = true

        Task {
            do {
                entries = try databaseService.fetchEntries(withTag: tag)
                hasMore = false // No pagination for filtered results
                isLoading = false
            } catch {
                errorMessage = error.localizedDescription
                isLoading = false
            }
        }
    }

    func clearFilters() {
        loadEntries()
    }

    // MARK: - Stats

    func getTotalEntriesCount() -> Int {
        (try? databaseService.countEntries()) ?? 0
    }

    func getEntriesThisMonth() -> Int {
        let calendar = Calendar.current
        let now = Date()
        let startOfMonth = calendar.date(from: calendar.dateComponents([.year, .month], from: now))!
        let endOfMonth = calendar.date(byAdding: DateComponents(month: 1, day: -1), to: startOfMonth)!

        let predicate = NSPredicate(
            format: "createdAt >= %@ AND createdAt <= %@",
            startOfMonth as CVarArg,
            endOfMonth as CVarArg
        )

        return (try? databaseService.countEntries(predicate: predicate)) ?? 0
    }

    func getCurrentStreak() -> Int {
        // Calculate days in a row with at least one entry
        // This is a simplified implementation
        let calendar = Calendar.current
        var streak = 0
        var currentDate = calendar.startOfDay(for: Date())

        while true {
            let nextDate = calendar.date(byAdding: .day, value: -1, to: currentDate)!
            let predicate = NSPredicate(
                format: "createdAt >= %@ AND createdAt < %@",
                currentDate as CVarArg,
                calendar.date(byAdding: .day, value: 1, to: currentDate)! as CVarArg
            )

            let count = (try? databaseService.countEntries(predicate: predicate)) ?? 0

            if count > 0 {
                streak += 1
                currentDate = nextDate
            } else {
                break
            }
        }

        return streak
    }
}
