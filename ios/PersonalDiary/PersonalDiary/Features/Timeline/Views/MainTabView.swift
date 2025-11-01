//
//  MainTabView.swift
//  PersonalDiary
//
//  Created by Claude Code on 2025-10-31.
//  Copyright © 2025 Personal Diary Platform. All rights reserved.
//

import SwiftUI

struct MainTabView: View {
    @State private var selectedTab = 0

    var body: some View {
        TabView(selection: $selectedTab) {
            // Timeline
            TimelineView()
                .tabItem {
                    Label("Timeline", systemImage: "book.fill")
                }
                .tag(0)

            // Search
            SearchView()
                .tabItem {
                    Label("Search", systemImage: "magnifyingglass")
                }
                .tag(1)

            // Settings
            SettingsView()
                .tabItem {
                    Label("Settings", systemImage: "gearshape.fill")
                }
                .tag(2)
        }
    }
}

#Preview {
    MainTabView()
        .environmentObject(AuthenticationViewModel())
}
