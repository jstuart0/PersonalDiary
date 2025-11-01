//
//  PasswordResetView.swift
//  PersonalDiary
//
//  Created by Claude Code on 2025-10-31.
//  Copyright © 2025 Personal Diary Platform. All rights reserved.
//

import SwiftUI

struct PasswordResetView: View {
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var viewModel: AuthenticationViewModel

    @State private var email = ""
    @State private var requestSent = false

    var body: some View {
        NavigationView {
            ZStack {
                Color(.systemGroupedBackground)
                    .ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 24) {
                        if requestSent {
                            successView
                        } else {
                            resetForm
                        }
                    }
                    .padding()
                }
            }
            .navigationTitle("Reset Password")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
            }
        }
    }

    // MARK: - Reset Form

    private var resetForm: some View {
        VStack(alignment: .leading, spacing: 20) {
            Image(systemName: "lock.rotation")
                .font(.system(size: 50))
                .foregroundColor(.blue)
                .frame(maxWidth: .infinity)

            Text("Forgot your password?")
                .font(.title2)
                .fontWeight(.bold)

            Text("Enter your email address and we'll send you instructions to reset your password.")
                .font(.subheadline)
                .foregroundColor(.secondary)

            // Note for E2E users
            VStack(alignment: .leading, spacing: 8) {
                Label("Note", systemImage: "info.circle.fill")
                    .foregroundColor(.orange)
                    .font(.headline)

                Text("If you use End-to-End Encryption, password reset is not available. You'll need to use your recovery codes to regain access.")
                    .font(.footnote)
                    .foregroundColor(.secondary)
            }
            .padding()
            .background(Color.orange.opacity(0.1))
            .cornerRadius(10)

            TextField("Email", text: $email)
                .textContentType(.emailAddress)
                .keyboardType(.emailAddress)
                .autocapitalization(.none)
                .padding()
                .background(Color(.systemBackground))
                .cornerRadius(10)

            if let error = viewModel.errorMessage {
                Text(error)
                    .font(.footnote)
                    .foregroundColor(.red)
            }

            Button(action: sendResetRequest) {
                HStack {
                    if viewModel.isLoading {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                    } else {
                        Text("Send Reset Instructions")
                            .fontWeight(.semibold)
                    }
                }
                .frame(maxWidth: .infinity)
                .padding()
                .background(isFormValid ? Color.blue : Color.gray)
                .foregroundColor(.white)
                .cornerRadius(10)
            }
            .disabled(!isFormValid || viewModel.isLoading)
        }
    }

    // MARK: - Success View

    private var successView: some View {
        VStack(spacing: 20) {
            Image(systemName: "envelope.badge.fill")
                .font(.system(size: 60))
                .foregroundColor(.green)

            Text("Check Your Email")
                .font(.title2)
                .fontWeight(.bold)

            Text("We've sent password reset instructions to \(email)")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)

            Button(action: { dismiss() }) {
                Text("Done")
                    .fontWeight(.semibold)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(10)
            }
        }
        .padding()
    }

    // MARK: - Computed Properties

    private var isFormValid: Bool {
        !email.isEmpty && email.isValidEmail
    }

    // MARK: - Actions

    private func sendResetRequest() {
        Task {
            do {
                try await viewModel.requestPasswordReset(email: email)
                requestSent = true
            } catch {
                // Error is handled by viewModel
            }
        }
    }
}

// MARK: - Preview

#Preview {
    PasswordResetView()
        .environmentObject(AuthenticationViewModel())
}
