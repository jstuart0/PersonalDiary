//
//  LoginView.swift
//  PersonalDiary
//
//  Created by Claude Code on 2025-10-31.
//  Copyright © 2025 Personal Diary Platform. All rights reserved.
//

import SwiftUI

struct LoginView: View {
    @EnvironmentObject var viewModel: AuthenticationViewModel
    @State private var email = ""
    @State private var password = ""
    @State private var showingSignup = false
    @State private var showingPasswordReset = false
    @State private var isPasswordVisible = false

    var body: some View {
        NavigationView {
            ZStack {
                // Background
                Color(.systemGroupedBackground)
                    .ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 24) {
                        // Logo and title
                        VStack(spacing: 12) {
                            Image(systemName: "book.closed.fill")
                                .font(.system(size: 60))
                                .foregroundColor(.blue)

                            Text("Personal Diary")
                                .font(.largeTitle)
                                .fontWeight(.bold)

                            Text("Your private journal")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                        .padding(.top, 60)
                        .padding(.bottom, 20)

                        // Login form
                        VStack(spacing: 16) {
                            // Email field
                            TextField("Email", text: $email)
                                .textContentType(.emailAddress)
                                .keyboardType(.emailAddress)
                                .autocapitalization(.none)
                                .padding()
                                .background(Color(.systemBackground))
                                .cornerRadius(10)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 10)
                                        .stroke(Color.gray.opacity(0.2), lineWidth: 1)
                                )

                            // Password field
                            HStack {
                                Group {
                                    if isPasswordVisible {
                                        TextField("Password", text: $password)
                                    } else {
                                        SecureField("Password", text: $password)
                                    }
                                }
                                .textContentType(.password)

                                Button(action: { isPasswordVisible.toggle() }) {
                                    Image(systemName: isPasswordVisible ? "eye.slash" : "eye")
                                        .foregroundColor(.gray)
                                }
                            }
                            .padding()
                            .background(Color(.systemBackground))
                            .cornerRadius(10)
                            .overlay(
                                RoundedRectangle(cornerRadius: 10)
                                    .stroke(Color.gray.opacity(0.2), lineWidth: 1)
                            )

                            // Forgot password
                            Button(action: { showingPasswordReset = true }) {
                                Text("Forgot password?")
                                    .font(.footnote)
                                    .foregroundColor(.blue)
                            }
                            .frame(maxWidth: .infinity, alignment: .trailing)
                        }
                        .padding(.horizontal)

                        // Error message
                        if let error = viewModel.errorMessage {
                            Text(error)
                                .font(.footnote)
                                .foregroundColor(.red)
                                .multilineTextAlignment(.center)
                                .padding(.horizontal)
                        }

                        // Login button
                        Button(action: login) {
                            HStack {
                                if viewModel.isLoading {
                                    ProgressView()
                                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                } else {
                                    Text("Sign In")
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
                        .padding(.horizontal)

                        // Divider
                        HStack {
                            Rectangle()
                                .fill(Color.gray.opacity(0.3))
                                .frame(height: 1)

                            Text("or")
                                .font(.footnote)
                                .foregroundColor(.secondary)

                            Rectangle()
                                .fill(Color.gray.opacity(0.3))
                                .frame(height: 1)
                        }
                        .padding(.horizontal)
                        .padding(.vertical, 8)

                        // Sign up button
                        Button(action: { showingSignup = true }) {
                            Text("Create Account")
                                .fontWeight(.semibold)
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(Color(.systemBackground))
                                .foregroundColor(.blue)
                                .cornerRadius(10)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 10)
                                        .stroke(Color.blue, lineWidth: 1)
                                )
                        }
                        .padding(.horizontal)

                        Spacer()
                    }
                }
            }
            .navigationBarHidden(true)
            .sheet(isPresented: $showingSignup) {
                SignupView()
                    .environmentObject(viewModel)
            }
            .sheet(isPresented: $showingPasswordReset) {
                PasswordResetView()
                    .environmentObject(viewModel)
            }
        }
    }

    // MARK: - Computed Properties

    private var isFormValid: Bool {
        !email.isEmpty && !password.isEmpty && email.isValidEmail
    }

    // MARK: - Actions

    private func login() {
        Task {
            try? await viewModel.login(email: email, password: password)
        }
    }
}

// MARK: - Preview

#Preview {
    LoginView()
        .environmentObject(AuthenticationViewModel())
}
