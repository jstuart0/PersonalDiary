//
//  SignupView.swift
//  PersonalDiary
//
//  Created by Claude Code on 2025-10-31.
//  Copyright © 2025 Personal Diary Platform. All rights reserved.
//

import SwiftUI

struct SignupView: View {
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var viewModel: AuthenticationViewModel

    @State private var currentStep = 1
    @State private var email = ""
    @State private var password = ""
    @State private var confirmPassword = ""
    @State private var name = ""
    @State private var selectedTier: EncryptionTier = .e2e
    @State private var isPasswordVisible = false
    @State private var isConfirmPasswordVisible = false
    @State private var showingRecoveryCodes = false

    var body: some View {
        NavigationView {
            ZStack {
                Color(.systemGroupedBackground)
                    .ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 24) {
                        // Progress indicator
                        HStack(spacing: 8) {
                            ForEach(1...3, id: \.self) { step in
                                Circle()
                                    .fill(step <= currentStep ? Color.blue : Color.gray.opacity(0.3))
                                    .frame(width: 10, height: 10)
                            }
                        }
                        .padding(.top, 20)

                        // Step content
                        Group {
                            switch currentStep {
                            case 1:
                                accountDetailsStep
                            case 2:
                                encryptionTierStep
                            case 3:
                                reviewStep
                            default:
                                EmptyView()
                            }
                        }

                        Spacer()
                    }
                    .padding()
                }
            }
            .navigationTitle("Create Account")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
            }
            .sheet(isPresented: $showingRecoveryCodes) {
                RecoveryCodesView(recoveryCodes: viewModel.recoveryCodes)
                    .environmentObject(viewModel)
            }
            .onChange(of: viewModel.isAuthenticated) { _, isAuth in
                if isAuth && selectedTier == .e2e && !viewModel.recoveryCodes.isEmpty {
                    showingRecoveryCodes = true
                } else if isAuth {
                    dismiss()
                }
            }
        }
    }

    // MARK: - Step 1: Account Details

    private var accountDetailsStep: some View {
        VStack(alignment: .leading, spacing: 20) {
            Text("Account Information")
                .font(.title2)
                .fontWeight(.bold)

            Text("Create your personal diary account")
                .font(.subheadline)
                .foregroundColor(.secondary)

            VStack(spacing: 16) {
                // Name
                TextField("Name (optional)", text: $name)
                    .textContentType(.name)
                    .padding()
                    .background(Color(.systemBackground))
                    .cornerRadius(10)

                // Email
                TextField("Email", text: $email)
                    .textContentType(.emailAddress)
                    .keyboardType(.emailAddress)
                    .autocapitalization(.none)
                    .padding()
                    .background(Color(.systemBackground))
                    .cornerRadius(10)

                // Password
                HStack {
                    Group {
                        if isPasswordVisible {
                            TextField("Password", text: $password)
                        } else {
                            SecureField("Password", text: $password)
                        }
                    }
                    .textContentType(.newPassword)

                    Button(action: { isPasswordVisible.toggle() }) {
                        Image(systemName: isPasswordVisible ? "eye.slash" : "eye")
                            .foregroundColor(.gray)
                    }
                }
                .padding()
                .background(Color(.systemBackground))
                .cornerRadius(10)

                // Confirm Password
                HStack {
                    Group {
                        if isConfirmPasswordVisible {
                            TextField("Confirm Password", text: $confirmPassword)
                        } else {
                            SecureField("Confirm Password", text: $confirmPassword)
                        }
                    }
                    .textContentType(.newPassword)

                    Button(action: { isConfirmPasswordVisible.toggle() }) {
                        Image(systemName: isConfirmPasswordVisible ? "eye.slash" : "eye")
                            .foregroundColor(.gray)
                    }
                }
                .padding()
                .background(Color(.systemBackground))
                .cornerRadius(10)

                // Password strength indicator
                if !password.isEmpty {
                    PasswordStrengthView(password: password)
                }
            }

            if let error = viewModel.errorMessage {
                Text(error)
                    .font(.footnote)
                    .foregroundColor(.red)
            }

            Button(action: { currentStep = 2 }) {
                Text("Next")
                    .fontWeight(.semibold)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(isStep1Valid ? Color.blue : Color.gray)
                    .foregroundColor(.white)
                    .cornerRadius(10)
            }
            .disabled(!isStep1Valid)
        }
    }

    // MARK: - Step 2: Encryption Tier

    private var encryptionTierStep: some View {
        VStack(alignment: .leading, spacing: 20) {
            Text("Choose Encryption")
                .font(.title2)
                .fontWeight(.bold)

            Text("Select how your data will be protected")
                .font(.subheadline)
                .foregroundColor(.secondary)

            VStack(spacing: 16) {
                // E2E Option
                TierSelectionCard(
                    tier: .e2e,
                    isSelected: selectedTier == .e2e,
                    action: { selectedTier = .e2e }
                )

                // UCE Option
                TierSelectionCard(
                    tier: .uce,
                    isSelected: selectedTier == .uce,
                    action: { selectedTier = .uce }
                )
            }

            HStack(spacing: 12) {
                Button(action: { currentStep = 1 }) {
                    Text("Back")
                        .fontWeight(.semibold)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color(.systemBackground))
                        .foregroundColor(.blue)
                        .cornerRadius(10)
                }

                Button(action: { currentStep = 3 }) {
                    Text("Next")
                        .fontWeight(.semibold)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.blue)
                        .foregroundColor(.white)
                        .cornerRadius(10)
                }
            }
        }
    }

    // MARK: - Step 3: Review and Submit

    private var reviewStep: some View {
        VStack(alignment: .leading, spacing: 20) {
            Text("Review")
                .font(.title2)
                .fontWeight(.bold)

            VStack(alignment: .leading, spacing: 12) {
                ReviewRow(title: "Name", value: name.isEmpty ? "Not provided" : name)
                ReviewRow(title: "Email", value: email)
                ReviewRow(title: "Encryption", value: selectedTier == .e2e ? "End-to-End" : "User-Controlled")
            }
            .padding()
            .background(Color(.systemBackground))
            .cornerRadius(10)

            if selectedTier == .e2e {
                VStack(alignment: .leading, spacing: 8) {
                    Label("Important", systemImage: "exclamationmark.triangle.fill")
                        .foregroundColor(.orange)
                        .font(.headline)

                    Text("You will receive recovery codes after signup. Save them securely - they're the only way to recover your account if you lose access.")
                        .font(.footnote)
                        .foregroundColor(.secondary)
                }
                .padding()
                .background(Color.orange.opacity(0.1))
                .cornerRadius(10)
            }

            if let error = viewModel.errorMessage {
                Text(error)
                    .font(.footnote)
                    .foregroundColor(.red)
            }

            HStack(spacing: 12) {
                Button(action: { currentStep = 2 }) {
                    Text("Back")
                        .fontWeight(.semibold)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color(.systemBackground))
                        .foregroundColor(.blue)
                        .cornerRadius(10)
                }

                Button(action: createAccount) {
                    HStack {
                        if viewModel.isLoading {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        } else {
                            Text("Create Account")
                                .fontWeight(.semibold)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(10)
                }
                .disabled(viewModel.isLoading)
            }
        }
    }

    // MARK: - Computed Properties

    private var isStep1Valid: Bool {
        !email.isEmpty &&
        email.isValidEmail &&
        !password.isEmpty &&
        password.passwordStrength() >= 3 &&
        password == confirmPassword
    }

    // MARK: - Actions

    private func createAccount() {
        Task {
            try? await viewModel.register(
                email: email,
                password: password,
                name: name.isEmpty ? nil : name,
                encryptionTier: selectedTier
            )
        }
    }
}

// MARK: - Supporting Views

struct TierSelectionCard: View {
    let tier: EncryptionTier
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Image(systemName: tier == .e2e ? "lock.shield.fill" : "key.fill")
                        .font(.title2)
                        .foregroundColor(tier == .e2e ? .green : .blue)

                    Spacer()

                    if isSelected {
                        Image(systemName: "checkmark.circle.fill")
                            .foregroundColor(.blue)
                    }
                }

                Text(tier == .e2e ? "End-to-End Encryption" : "User-Controlled Encryption")
                    .font(.headline)
                    .foregroundColor(.primary)

                Text(tier == .e2e ?
                    "Maximum security. Only you can read your entries. Requires recovery codes." :
                    "Balanced security. Password recovery available. Searchable on server.")
                    .font(.footnote)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.leading)
            }
            .padding()
            .background(Color(.systemBackground))
            .overlay(
                RoundedRectangle(cornerRadius: 10)
                    .stroke(isSelected ? Color.blue : Color.clear, lineWidth: 2)
            )
            .cornerRadius(10)
        }
    }
}

struct PasswordStrengthView: View {
    let password: String

    var body: some View {
        let strength = password.passwordStrength()

        VStack(alignment: .leading, spacing: 4) {
            HStack {
                ForEach(0..<4) { index in
                    Rectangle()
                        .fill(index < strength ? strengthColor : Color.gray.opacity(0.3))
                        .frame(height: 4)
                }
            }

            Text(strengthText)
                .font(.caption)
                .foregroundColor(strengthColor)
        }
    }

    private var strengthColor: Color {
        let strength = password.passwordStrength()
        switch strength {
        case 0...1: return .red
        case 2: return .orange
        case 3: return .yellow
        default: return .green
        }
    }

    private var strengthText: String {
        let strength = password.passwordStrength()
        switch strength {
        case 0...1: return "Weak password"
        case 2: return "Fair password"
        case 3: return "Good password"
        default: return "Strong password"
        }
    }
}

struct ReviewRow: View {
    let title: String
    let value: String

    var body: some View {
        HStack {
            Text(title)
                .foregroundColor(.secondary)

            Spacer()

            Text(value)
                .fontWeight(.medium)
        }
        .font(.subheadline)
    }
}

// MARK: - Preview

#Preview {
    SignupView()
        .environmentObject(AuthenticationViewModel())
}
