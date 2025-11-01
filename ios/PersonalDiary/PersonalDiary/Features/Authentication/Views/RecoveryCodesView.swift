//
//  RecoveryCodesView.swift
//  PersonalDiary
//
//  Created by Claude Code on 2025-10-31.
//  Copyright © 2025 Personal Diary Platform. All rights reserved.
//

import SwiftUI

struct RecoveryCodesView: View {
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var viewModel: AuthenticationViewModel

    let recoveryCodes: [String]

    @State private var hasAcknowledged = false
    @State private var hasCopied = false

    var body: some View {
        NavigationView {
            ZStack {
                Color(.systemGroupedBackground)
                    .ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 24) {
                        // Warning header
                        VStack(spacing: 12) {
                            Image(systemName: "exclamationmark.triangle.fill")
                                .font(.system(size: 50))
                                .foregroundColor(.orange)

                            Text("Save Your Recovery Codes")
                                .font(.title2)
                                .fontWeight(.bold)

                            Text("These codes are your only way to recover your account if you lose access. Save them in a secure place.")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                                .multilineTextAlignment(.center)
                        }
                        .padding()

                        // Recovery codes
                        VStack(alignment: .leading, spacing: 12) {
                            ForEach(Array(recoveryCodes.enumerated()), id: \.offset) { index, code in
                                HStack {
                                    Text("\(index + 1).")
                                        .foregroundColor(.secondary)
                                        .frame(width: 30, alignment: .leading)

                                    Text(formatRecoveryCode(code))
                                        .font(.system(.body, design: .monospaced))
                                        .fontWeight(.medium)

                                    Spacer()
                                }
                                .padding()
                                .background(Color(.systemBackground))
                                .cornerRadius(8)
                            }
                        }
                        .padding(.horizontal)

                        // Copy button
                        Button(action: copyAllCodes) {
                            Label(hasCopied ? "Copied!" : "Copy All Codes", systemImage: hasCopied ? "checkmark" : "doc.on.doc")
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(Color.blue.opacity(0.1))
                                .foregroundColor(.blue)
                                .cornerRadius(10)
                        }
                        .padding(.horizontal)

                        // Important notes
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Important:")
                                .font(.headline)

                            VStack(alignment: .leading, spacing: 8) {
                                BulletPoint(text: "Each code can only be used once")
                                BulletPoint(text: "Store them in a password manager or write them down")
                                BulletPoint(text: "Keep them in a safe, secure location")
                                BulletPoint(text: "We cannot recover your account without these codes")
                            }
                        }
                        .padding()
                        .background(Color.orange.opacity(0.1))
                        .cornerRadius(10)
                        .padding(.horizontal)

                        // Acknowledgement
                        Toggle(isOn: $hasAcknowledged) {
                            Text("I have saved my recovery codes")
                                .font(.subheadline)
                        }
                        .padding()
                        .background(Color(.systemBackground))
                        .cornerRadius(10)
                        .padding(.horizontal)

                        // Done button
                        Button(action: confirmAndDismiss) {
                            Text("I've Saved My Codes")
                                .fontWeight(.semibold)
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(hasAcknowledged ? Color.blue : Color.gray)
                                .foregroundColor(.white)
                                .cornerRadius(10)
                        }
                        .disabled(!hasAcknowledged)
                        .padding(.horizontal)

                        Spacer()
                    }
                    .padding(.vertical)
                }
            }
            .navigationBarHidden(true)
            .interactiveDismissDisabled(true)
        }
    }

    // MARK: - Helpers

    private func formatRecoveryCode(_ code: String) -> String {
        // Format as XXXX-XXXX-XXXX-XXXX
        let chunks = code.enumerated().reduce(into: [String]()) { result, element in
            if element.offset % 4 == 0 {
                result.append(String(element.element))
            } else {
                result[result.count - 1].append(element.element)
            }
        }
        return chunks.joined(separator: "-")
    }

    private func copyAllCodes() {
        let allCodes = recoveryCodes.map { formatRecoveryCode($0) }.joined(separator: "\n")
        UIPasteboard.general.string = allCodes
        hasCopied = true

        // Reset after 2 seconds
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
            hasCopied = false
        }
    }

    private func confirmAndDismiss() {
        Task {
            try? await viewModel.saveRecoveryCodesAcknowledged()
            viewModel.clearRecoveryCodes()
            dismiss()
        }
    }
}

struct BulletPoint: View {
    let text: String

    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            Image(systemName: "circle.fill")
                .font(.system(size: 6))
                .foregroundColor(.secondary)
                .padding(.top, 6)

            Text(text)
                .font(.footnote)
                .foregroundColor(.secondary)
        }
    }
}

// MARK: - Preview

#Preview {
    RecoveryCodesView(recoveryCodes: [
        "ABCD1234EFGH5678IJKL",
        "MNOP9012QRST3456UVWX",
        "YZAB7890CDEF1234GHIJ",
        "KLMN5678OPQR9012STUV",
        "WXYZ3456ABCD7890EFGH"
    ])
    .environmentObject(AuthenticationViewModel())
}
