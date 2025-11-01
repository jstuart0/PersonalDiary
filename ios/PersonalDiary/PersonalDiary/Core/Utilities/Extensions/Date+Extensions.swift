//
//  Date+Extensions.swift
//  PersonalDiary
//
//  Created by Claude Code on 2025-10-31.
//  Copyright © 2025 Personal Diary Platform. All rights reserved.
//

import Foundation

extension Date {
    /// Format date for display
    var displayDate: String {
        let formatter = DateFormatter()
        formatter.dateFormat = Constants.DateFormat.displayDate
        return formatter.string(from: self)
    }

    /// Format date and time for display
    var displayDateTime: String {
        let formatter = DateFormatter()
        formatter.dateFormat = Constants.DateFormat.displayDateTime
        return formatter.string(from: self)
    }

    /// Format time for display
    var displayTime: String {
        let formatter = DateFormatter()
        formatter.dateFormat = Constants.DateFormat.displayTime
        return formatter.string(from: self)
    }

    /// Convert to ISO 8601 string
    var iso8601String: String {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter.string(from: self)
    }

    /// Check if date is today
    var isToday: Bool {
        Calendar.current.isDateInToday(self)
    }

    /// Check if date is yesterday
    var isYesterday: Bool {
        Calendar.current.isDateInYesterday(self)
    }

    /// Get relative time description (e.g., "2 hours ago", "Yesterday", "Last week")
    var relativeTimeString: String {
        let calendar = Calendar.current
        let now = Date()

        if calendar.isDateInToday(self) {
            let components = calendar.dateComponents([.hour, .minute], from: self, to: now)
            if let hours = components.hour, hours > 0 {
                return hours == 1 ? "1 hour ago" : "\(hours) hours ago"
            } else if let minutes = components.minute, minutes > 0 {
                return minutes == 1 ? "1 minute ago" : "\(minutes) minutes ago"
            } else {
                return "Just now"
            }
        } else if calendar.isDateInYesterday(self) {
            return "Yesterday"
        } else if let days = calendar.dateComponents([.day], from: self, to: now).day, days < 7 {
            return "\(days) days ago"
        } else if let weeks = calendar.dateComponents([.weekOfYear], from: self, to: now).weekOfYear, weeks < 4 {
            return weeks == 1 ? "Last week" : "\(weeks) weeks ago"
        } else if let months = calendar.dateComponents([.month], from: self, to: now).month, months < 12 {
            return months == 1 ? "Last month" : "\(months) months ago"
        } else if let years = calendar.dateComponents([.year], from: self, to: now).year {
            return years == 1 ? "Last year" : "\(years) years ago"
        }

        return displayDate
    }

    /// Start of day
    var startOfDay: Date {
        Calendar.current.startOfDay(for: self)
    }

    /// End of day
    var endOfDay: Date {
        var components = DateComponents()
        components.day = 1
        components.second = -1
        return Calendar.current.date(byAdding: components, to: startOfDay) ?? self
    }
}

extension String {
    /// Parse ISO 8601 date string
    var iso8601Date: Date? {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter.date(from: self)
    }
}
