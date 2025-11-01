//
//  APIClient.swift
//  PersonalDiary
//
//  Created by Claude Code on 2025-10-31.
//  Copyright © 2025 Personal Diary Platform. All rights reserved.
//

import Foundation

/// Main API client for backend communication
final class APIClient {
    // MARK: - Singleton

    static let shared = APIClient()

    // MARK: - Properties

    private let baseURL: URL
    private let session: URLSession
    private let keychainService: KeychainService
    private var isRefreshingToken = false
    private var tokenRefreshCallbacks: [(Result<String, Error>) -> Void] = []

    // MARK: - Initialization

    private init(
        baseURL: URL? = nil,
        session: URLSession = .shared,
        keychainService: KeychainService = KeychainService()
    ) {
        #if DEBUG
        self.baseURL = baseURL ?? URL(string: Constants.API.debugBaseURL)!
        #else
        self.baseURL = baseURL ?? URL(string: Constants.API.releaseBaseURL)!
        #endif

        self.session = session
        self.keychainService = keychainService
    }

    // MARK: - Request Methods

    /// Performs a GET request
    func get<T: Decodable>(
        _ endpoint: APIEndpoint,
        parameters: [String: Any]? = nil
    ) async throws -> T {
        try await request(endpoint, method: "GET", parameters: parameters)
    }

    /// Performs a POST request
    func post<T: Decodable>(
        _ endpoint: APIEndpoint,
        body: Encodable? = nil
    ) async throws -> T {
        try await request(endpoint, method: "POST", body: body)
    }

    /// Performs a PUT request
    func put<T: Decodable>(
        _ endpoint: APIEndpoint,
        body: Encodable? = nil
    ) async throws -> T {
        try await request(endpoint, method: "PUT", body: body)
    }

    /// Performs a DELETE request
    func delete<T: Decodable>(
        _ endpoint: APIEndpoint
    ) async throws -> T {
        try await request(endpoint, method: "DELETE")
    }

    /// Performs a request without expecting a response body
    func perform(
        _ endpoint: APIEndpoint,
        method: String,
        body: Encodable? = nil
    ) async throws {
        let _: EmptyResponse = try await request(endpoint, method: method, body: body)
    }

    // MARK: - Core Request

    private func request<T: Decodable>(
        _ endpoint: APIEndpoint,
        method: String,
        parameters: [String: Any]? = nil,
        body: Encodable? = nil,
        retryCount: Int = 0
    ) async throws -> T {
        // Build URL
        var urlComponents = URLComponents(url: baseURL.appendingPathComponent(endpoint.path), resolvingAgainstBaseURL: false)!

        // Add query parameters
        if let parameters = parameters {
            urlComponents.queryItems = parameters.map { URLQueryItem(name: $0.key, value: "\($0.value)") }
        }

        guard let url = urlComponents.url else {
            throw AppError.network(.invalidURL)
        }

        // Create request
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        // Add authentication header if required
        if endpoint.requiresAuth {
            let token = try await getAccessToken()
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        // Add body if present
        if let body = body {
            request.httpBody = try JSONEncoder().encode(body)
        }

        // Perform request
        let (data, response) = try await session.data(for: request)

        // Handle response
        guard let httpResponse = response as? HTTPURLResponse else {
            throw AppError.network(.invalidResponse)
        }

        // Handle token refresh for 401 errors
        if httpResponse.statusCode == 401, endpoint.requiresAuth, retryCount < 1 {
            do {
                _ = try await refreshAccessToken()
                return try await self.request(endpoint, method: method, parameters: parameters, body: body, retryCount: retryCount + 1)
            } catch {
                throw AppError.authentication(.tokenExpired)
            }
        }

        // Check for errors
        try handleHTTPError(httpResponse, data: data)

        // Decode response
        if T.self == EmptyResponse.self {
            return EmptyResponse() as! T
        }

        do {
            let decoder = JSONDecoder()
            decoder.keyDecodingStrategy = .convertFromSnakeCase
            decoder.dateDecodingStrategy = .iso8601
            return try decoder.decode(T.self, from: data)
        } catch {
            throw AppError.network(.decodingFailed)
        }
    }

    // MARK: - Token Management

    private func getAccessToken() async throws -> String {
        if let token = try keychainService.retrieveAccessToken() {
            return token
        }

        throw AppError.authentication(.notAuthenticated)
    }

    private func refreshAccessToken() async throws -> String {
        // Prevent multiple simultaneous refresh requests
        if isRefreshingToken {
            return try await withCheckedThrowingContinuation { continuation in
                tokenRefreshCallbacks.append { result in
                    continuation.resume(with: result)
                }
            }
        }

        isRefreshingToken = true
        defer {
            isRefreshingToken = false
        }

        do {
            guard let refreshToken = try keychainService.retrieveRefreshToken() else {
                throw AppError.authentication(.tokenExpired)
            }

            let response: TokenRefreshResponse = try await request(
                .refreshToken,
                method: "POST",
                body: TokenRefreshRequest(refreshToken: refreshToken)
            )

            // Save new tokens
            try keychainService.saveAccessToken(response.accessToken)
            try keychainService.saveRefreshToken(response.refreshToken)

            // Notify waiting requests
            let callbacks = tokenRefreshCallbacks
            tokenRefreshCallbacks.removeAll()
            callbacks.forEach { $0(.success(response.accessToken)) }

            return response.accessToken
        } catch {
            // Notify waiting requests of failure
            let callbacks = tokenRefreshCallbacks
            tokenRefreshCallbacks.removeAll()
            callbacks.forEach { $0(.failure(error)) }

            throw error
        }
    }

    // MARK: - Error Handling

    private func handleHTTPError(_ response: HTTPURLResponse, data: Data) throws {
        guard (200...299).contains(response.statusCode) else {
            // Try to decode error response
            if let errorResponse = try? JSONDecoder().decode(APIErrorResponse.self, from: data) {
                throw AppError.network(.serverError(message: errorResponse.message))
            }

            // Fallback to HTTP status codes
            switch response.statusCode {
            case 400:
                throw AppError.network(.badRequest)
            case 401:
                throw AppError.authentication(.tokenExpired)
            case 403:
                throw AppError.network(.forbidden)
            case 404:
                throw AppError.network(.notFound)
            case 409:
                throw AppError.network(.conflict)
            case 429:
                throw AppError.network(.rateLimitExceeded)
            case 500...599:
                throw AppError.network(.serverError(message: "Server error occurred"))
            default:
                throw AppError.network(.unknownError)
            }
        }
    }

    // MARK: - Upload Methods

    /// Uploads a file with multipart form data
    func uploadFile(
        _ endpoint: APIEndpoint,
        fileData: Data,
        fileName: String,
        mimeType: String,
        parameters: [String: String]? = nil
    ) async throws -> MediaUploadResponse {
        guard let url = URL(string: endpoint.path, relativeTo: baseURL) else {
            throw AppError.network(.invalidURL)
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"

        let boundary = UUID().uuidString
        request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")

        if endpoint.requiresAuth {
            let token = try await getAccessToken()
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        var body = Data()

        // Add parameters
        if let parameters = parameters {
            for (key, value) in parameters {
                body.append("--\(boundary)\r\n".data(using: .utf8)!)
                body.append("Content-Disposition: form-data; name=\"\(key)\"\r\n\r\n".data(using: .utf8)!)
                body.append("\(value)\r\n".data(using: .utf8)!)
            }
        }

        // Add file
        body.append("--\(boundary)\r\n".data(using: .utf8)!)
        body.append("Content-Disposition: form-data; name=\"file\"; filename=\"\(fileName)\"\r\n".data(using: .utf8)!)
        body.append("Content-Type: \(mimeType)\r\n\r\n".data(using: .utf8)!)
        body.append(fileData)
        body.append("\r\n".data(using: .utf8)!)
        body.append("--\(boundary)--\r\n".data(using: .utf8)!)

        request.httpBody = body

        let (data, response) = try await session.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw AppError.network(.invalidResponse)
        }

        try handleHTTPError(httpResponse, data: data)

        let decoder = JSONDecoder()
        decoder.keyDecodingStrategy = .convertFromSnakeCase
        return try decoder.decode(MediaUploadResponse.self, from: data)
    }

    /// Downloads a file
    func downloadFile(_ url: URL) async throws -> Data {
        var request = URLRequest(url: url)

        if let token = try? await getAccessToken() {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        let (data, response) = try await session.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw AppError.network(.invalidResponse)
        }

        try handleHTTPError(httpResponse, data: data)

        return data
    }
}

// MARK: - API Endpoint

enum APIEndpoint {
    // Authentication
    case register
    case login
    case logout
    case refreshToken
    case verifyEmail
    case requestPasswordReset
    case resetPassword

    // Entries
    case entries
    case entry(String)
    case createEntry
    case updateEntry(String)
    case deleteEntry(String)

    // Sync
    case syncEntries
    case syncStatus

    // Media
    case uploadMedia
    case downloadMedia(String)

    // Search
    case search

    // Social
    case facebookConnect
    case facebookImport

    var path: String {
        switch self {
        case .register: return "/auth/register"
        case .login: return "/auth/login"
        case .logout: return "/auth/logout"
        case .refreshToken: return "/auth/refresh"
        case .verifyEmail: return "/auth/verify-email"
        case .requestPasswordReset: return "/auth/request-password-reset"
        case .resetPassword: return "/auth/reset-password"

        case .entries: return "/entries"
        case .entry(let id): return "/entries/\(id)"
        case .createEntry: return "/entries"
        case .updateEntry(let id): return "/entries/\(id)"
        case .deleteEntry(let id): return "/entries/\(id)"

        case .syncEntries: return "/sync/entries"
        case .syncStatus: return "/sync/status"

        case .uploadMedia: return "/media/upload"
        case .downloadMedia(let id): return "/media/\(id)"

        case .search: return "/search"

        case .facebookConnect: return "/social/facebook/connect"
        case .facebookImport: return "/social/facebook/import"
        }
    }

    var requiresAuth: Bool {
        switch self {
        case .register, .login, .refreshToken, .requestPasswordReset, .resetPassword, .verifyEmail:
            return false
        default:
            return true
        }
    }
}

// MARK: - Supporting Types

private struct EmptyResponse: Decodable {}

private struct APIErrorResponse: Decodable {
    let message: String
    let code: String?
}

struct TokenRefreshRequest: Encodable {
    let refreshToken: String
}

struct TokenRefreshResponse: Decodable {
    let accessToken: String
    let refreshToken: String
}

struct MediaUploadResponse: Decodable {
    let id: String
    let url: String
    let thumbnailUrl: String?
}
