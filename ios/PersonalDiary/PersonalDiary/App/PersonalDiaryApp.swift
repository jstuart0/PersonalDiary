//
//  PersonalDiaryApp.swift
//  PersonalDiary
//
//  Created by Claude Code on 2025-10-31.
//  Copyright © 2025 Personal Diary Platform. All rights reserved.
//

import SwiftUI

@main
struct PersonalDiaryApp: App {
    // MARK: - Properties

    @StateObject private var authenticationViewModel = AuthenticationViewModel()
    @StateObject private var syncService = SyncService.shared

    // MARK: - Scene

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(authenticationViewModel)
                .environmentObject(syncService)
                .onAppear {
                    // Check authentication status on app launch
                    Task {
                        await authenticationViewModel.checkAuthenticationStatus()
                    }
                }
                .onReceive(NotificationCenter.default.publisher(for: UIApplication.willEnterForegroundNotification)) { _ in
                    // Trigger sync when app returns from background
                    Task {
                        await syncService.syncIncremental()
                    }
                }
        }
    }
}
