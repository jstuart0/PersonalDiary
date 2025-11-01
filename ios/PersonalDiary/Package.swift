// swift-tools-version: 5.9
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "PersonalDiary",
    platforms: [
        .iOS(.v16)
    ],
    products: [
        .library(
            name: "PersonalDiary",
            targets: ["PersonalDiary"])
    ],
    dependencies: [
        // No external dependencies - using native frameworks
        // - CryptoKit (built-in)
        // - SwiftData (built-in, iOS 17+)
        // - Core Data (built-in, iOS 16+)
    ],
    targets: [
        .target(
            name: "PersonalDiary",
            dependencies: [],
            path: "PersonalDiary"),
        .testTarget(
            name: "PersonalDiaryTests",
            dependencies: ["PersonalDiary"],
            path: "PersonalDiaryTests")
    ]
)
