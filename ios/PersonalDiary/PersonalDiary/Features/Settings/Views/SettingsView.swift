//
//  SettingsView.swift
//  PersonalDiary
//
//  Created by Claude Code on 2025-10-31.
//  Copyright © 2025 Personal Diary Platform. All rights reserved.
//

import SwiftUI

struct SettingsView: View {
    @EnvironmentObject var authViewModel: AuthenticationViewModel
    @EnvironmentObject var syncService: SyncService

    @State private var showingLogoutAlert = false
    @State private var showingDeleteAccountAlert = false

    var body: some View {
        NavigationView {
            List {
                // Account section
                Section("Account") {
                    if let user = authViewModel.currentUser {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(user.name ?? "No name")
                                    .font(.headline)

                                Text(user.email)
                                    .font(.subheadline)
                                    .foregroundColor(.secondary)
                            }

                            Spacer()

                            Image(systemName: user.encryptionTier == .e2e ? "lock.shield.fill" : "key.fill")
                                .foregroundColor(user.encryptionTier == .e2e ? .green : .blue)
                        }
                    }

                    NavigationLink(destination: AccountSettingsView()) {
                        Label("Account Settings", systemImage: "person.circle")
                    }
                }

                // Sync section
                Section("Sync") {
                    HStack {
                        Label("Last Synced", systemImage: "arrow.triangle.2.circlepath")

                        Spacer()

                        if let lastSync = syncService.lastSyncDate {
                            Text(lastSync.formatted(date: .numeric, time: .shortened))
                                .font(.caption)
                                .foregroundColor(.secondary)
                        } else {
                            Text("Never")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }

                    Button(action: { Task { await syncService.syncFull() } }) {
                        HStack {
                            Label("Sync Now", systemImage: "arrow.clockwise")

                            Spacer()

                            if syncService.isSyncing {
                                ProgressView()
                                    .scaleEffect(0.8)
                            }
                        }
                    }
                    .disabled(syncService.isSyncing)

                    NavigationLink(destination: SyncSettingsView()) {
                        Label("Sync Settings", systemImage: "gearshape")
                    }
                }

                // Security section
                Section("Security") {
                    NavigationLink(destination: SecuritySettingsView()) {
                        Label("Security & Privacy", systemImage: "lock.shield")
                    }

                    if authViewModel.currentUser?.encryptionTier == .e2e {
                        NavigationLink(destination: RecoveryCodesManagementView()) {
                            Label("Recovery Codes", systemImage: "key.horizontal")
                        }
                    }
                }

                // Storage section
                Section("Storage") {
                    NavigationLink(destination: StorageSettingsView()) {
                        Label("Storage Management", systemImage: "externaldrive")
                    }
                }

                // About section
                Section("About") {
                    HStack {
                        Text("Version")
                        Spacer()
                        Text("1.0.0")
                            .foregroundColor(.secondary)
                    }

                    NavigationLink(destination: Text("Privacy Policy")) {
                        Label("Privacy Policy", systemImage: "hand.raised")
                    }

                    NavigationLink(destination: Text("Terms of Service")) {
                        Label("Terms of Service", systemImage: "doc.text")
                    }
                }

                // Danger zone
                Section {
                    Button(role: .destructive, action: { showingLogoutAlert = true }) {
                        Label("Sign Out", systemImage: "arrow.right.square")
                    }

                    Button(role: .destructive, action: { showingDeleteAccountAlert = true }) {
                        Label("Delete Account", systemImage: "trash")
                    }
                }
            }
            .navigationTitle("Settings")
            .alert("Sign Out", isPresented: $showingLogoutAlert) {
                Button("Cancel", role: .cancel) {}
                Button("Sign Out", role: .destructive) {
                    Task {
                        await authViewModel.logout()
                    }
                }
            } message: {
                Text("Are you sure you want to sign out? Unsync data will be lost.")
            }
            .alert("Delete Account", isPresented: $showingDeleteAccountAlert) {
                Button("Cancel", role: .cancel) {}
                Button("Delete", role: .destructive) {
                    // Handle account deletion
                }
            } message: {
                Text("This action cannot be undone. All your data will be permanently deleted.")
            }
        }
    }
}

// MARK: - Account Settings

struct AccountSettingsView: View {
    @EnvironmentObject var authViewModel: AuthenticationViewModel
    @State private var name = ""
    @State private var email = ""

    var body: some View {
        Form {
            Section("Profile") {
                TextField("Name", text: $name)
                TextField("Email", text: $email)
                    .textContentType(.emailAddress)
                    .keyboardType(.emailAddress)
                    .autocapitalization(.none)
                    .disabled(true)
            }

            Section("Encryption") {
                HStack {
                    Text("Tier")
                    Spacer()
                    Text(authViewModel.currentUser?.encryptionTier == .e2e ? "End-to-End" : "User-Controlled")
                        .foregroundColor(.secondary)
                }

                Text("Encryption tier cannot be changed after account creation")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .navigationTitle("Account")
        .onAppear {
            if let user = authViewModel.currentUser {
                name = user.name ?? ""
                email = user.email
            }
        }
    }
}

// MARK: - Security Settings

struct SecuritySettingsView: View {
    @EnvironmentObject var authViewModel: AuthenticationViewModel
    @State private var biometricEnabled = false

    var body: some View {
        Form {
            Section("Biometric Authentication") {
                Toggle("Enable Face ID / Touch ID", isOn: $biometricEnabled)
                    .onChange(of: biometricEnabled) { _, enabled in
                        if enabled {
                            Task {
                                try? await authViewModel.enableBiometricAuthentication()
                            }
                        } else {
                            try? authViewModel.disableBiometricAuthentication()
                        }
                    }

                Text("Use biometric authentication to unlock the app")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }

            Section("Data Protection") {
                Toggle("Auto-lock", isOn: .constant(true))
                    .disabled(true)

                Text("Automatically lock the app when backgrounded")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .navigationTitle("Security")
    }
}

// MARK: - Sync Settings

struct SyncSettingsView: View {
    @EnvironmentObject var syncService: SyncService

    var body: some View {
        Form {
            Section("Sync Status") {
                HStack {
                    Text("Pending Uploads")
                    Spacer()
                    Text("\(syncService.pendingUploads)")
                        .foregroundColor(.secondary)
                }

                HStack {
                    Text("Pending Downloads")
                    Spacer()
                    Text("\(syncService.pendingDownloads)")
                        .foregroundColor(.secondary)
                }

                if let error = syncService.syncError {
                    Text(error.localizedDescription)
                        .font(.caption)
                        .foregroundColor(.red)
                }
            }

            Section("Options") {
                Toggle("Auto-sync", isOn: .constant(true))
                    .disabled(true)

                Toggle("Sync on WiFi only", isOn: .constant(false))
                    .disabled(true)
            }
        }
        .navigationTitle("Sync")
    }
}

// MARK: - Storage Settings

struct StorageSettingsView: View {
    @State private var storageUsed: Int64 = 0
    @State private var totalStorage: Int64 = 1_000_000_000 // 1GB limit

    var body: some View {
        Form {
            Section("Storage Usage") {
                HStack {
                    VStack(alignment: .leading) {
                        Text("Used")
                            .font(.caption)
                            .foregroundColor(.secondary)

                        Text(ByteCountFormatter.string(fromByteCount: storageUsed, countStyle: .file))
                            .font(.headline)
                    }

                    Spacer()

                    VStack(alignment: .trailing) {
                        Text("Available")
                            .font(.caption)
                            .foregroundColor(.secondary)

                        Text(ByteCountFormatter.string(fromByteCount: totalStorage - storageUsed, countStyle: .file))
                            .font(.headline)
                    }
                }

                ProgressView(value: Double(storageUsed), total: Double(totalStorage))
            }

            Section("Management") {
                Button("Clear Cache") {
                    // Clear cache logic
                }

                Button("Optimize Storage") {
                    // Optimize storage logic
                }

                Button(role: .destructive, action: {}) {
                    Text("Clear All Local Data")
                }
            }
        }
        .navigationTitle("Storage")
        .task {
            storageUsed = (try? MediaService.shared.calculateStorageUsed()) ?? 0
        }
    }
}

// MARK: - Recovery Codes Management

struct RecoveryCodesManagementView: View {
    var body: some View {
        Form {
            Section {
                Text("Recovery codes are used to regain access to your account if you lose your device.")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }

            Section {
                Button("View Recovery Codes") {
                    // Show recovery codes (requires authentication)
                }

                Button("Regenerate Recovery Codes") {
                    // Regenerate codes
                }
                .foregroundColor(.orange)
            }
        }
        .navigationTitle("Recovery Codes")
    }
}

// MARK: - Preview

#Preview {
    SettingsView()
        .environmentObject(AuthenticationViewModel())
        .environmentObject(SyncService.shared)
}
