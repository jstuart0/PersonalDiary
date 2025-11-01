//
//  Data+Extensions.swift
//  PersonalDiary
//
//  Created by Claude Code on 2025-10-31.
//  Copyright © 2025 Personal Diary Platform. All rights reserved.
//

import Foundation
import CryptoKit

extension Data {
    /// Convert Data to hex string
    var hexString: String {
        map { String(format: "%02hhx", $0) }.joined()
    }

    /// Create Data from hex string
    init?(hexString: String) {
        let length = hexString.count / 2
        var data = Data(capacity: length)
        var index = hexString.startIndex

        for _ in 0..<length {
            let nextIndex = hexString.index(index, offsetBy: 2)
            guard let byte = UInt8(hexString[index..<nextIndex], radix: 16) else {
                return nil
            }
            data.append(byte)
            index = nextIndex
        }

        self = data
    }

    /// Generate SHA-256 hash of data
    var sha256Hash: String {
        let hash = SHA256.hash(data: self)
        return hash.compactMap { String(format: "%02x", $0) }.joined()
    }

    /// Generate SHA-256 hash as Data
    var sha256Data: Data {
        Data(SHA256.hash(data: self))
    }

    /// Convert to Base64 string
    var base64String: String {
        base64EncodedString()
    }

    /// Create Data from Base64 string
    init?(base64String: String) {
        guard let data = Data(base64Encoded: base64String) else {
            return nil
        }
        self = data
    }
}

extension String {
    /// Convert string to Data using UTF-8 encoding
    var data: Data? {
        data(using: .utf8)
    }

    /// Generate SHA-256 hash of string
    var sha256Hash: String {
        guard let data = data(using: .utf8) else {
            return ""
        }
        return data.sha256Hash
    }

    /// Validate email format
    var isValidEmail: Bool {
        let emailPredicate = NSPredicate(format: "SELF MATCHES %@", Constants.Validation.emailRegex)
        return emailPredicate.evaluate(with: self)
    }

    /// Check password strength
    var passwordStrength: PasswordStrength {
        let length = count

        if length < Constants.Validation.minPasswordLength {
            return .weak
        }

        var hasUppercase = false
        var hasLowercase = false
        var hasNumber = false
        var hasSpecialChar = false

        for char in self {
            if char.isUppercase {
                hasUppercase = true
            } else if char.isLowercase {
                hasLowercase = true
            } else if char.isNumber {
                hasNumber = true
            } else if !char.isLetter && !char.isNumber {
                hasSpecialChar = true
            }
        }

        let criteriaMet = [hasUppercase, hasLowercase, hasNumber, hasSpecialChar].filter { $0 }.count

        if criteriaMet >= 4 && length >= 16 {
            return .veryStrong
        } else if criteriaMet >= 3 && length >= 12 {
            return .strong
        } else if criteriaMet >= 2 {
            return .medium
        } else {
            return .weak
        }
    }
}

/// Password strength enum
enum PasswordStrength {
    case weak
    case medium
    case strong
    case veryStrong

    var displayName: String {
        switch self {
        case .weak:
            return "Weak"
        case .medium:
            return "Medium"
        case .strong:
            return "Strong"
        case .veryStrong:
            return "Very Strong"
        }
    }

    var color: String {
        switch self {
        case .weak:
            return "red"
        case .medium:
            return "orange"
        case .strong:
            return "green"
        case .veryStrong:
            return "blue"
        }
    }

    var progress: Double {
        switch self {
        case .weak:
            return 0.25
        case .medium:
            return 0.5
        case .strong:
            return 0.75
        case .veryStrong:
            return 1.0
        }
    }
}
