//
//  ContentView.swift
//  PersonalDiary
//
//  Created by Claude Code on 2025-10-31.
//  Copyright © 2025 Personal Diary Platform. All rights reserved.
//

import SwiftUI

struct ContentView: View {
    @EnvironmentObject var authenticationViewModel: AuthenticationViewModel

    var body: some View {
        Group {
            if authenticationViewModel.isLoading {
                SplashView()
            } else if authenticationViewModel.isAuthenticated {
                MainTabView()
            } else {
                LoginView()
            }
        }
    }
}

struct SplashView: View {
    var body: some View {
        ZStack {
            Color.blue.opacity(0.1)
                .ignoresSafeArea()

            VStack(spacing: 20) {
                Image(systemName: "book.closed.fill")
                    .font(.system(size: 80))
                    .foregroundColor(.blue)

                Text("Personal Diary")
                    .font(.largeTitle)
                    .fontWeight(.bold)

                ProgressView()
                    .scaleEffect(1.5)
                    .padding(.top, 20)
            }
        }
    }
}

#Preview {
    ContentView()
        .environmentObject(AuthenticationViewModel())
}
