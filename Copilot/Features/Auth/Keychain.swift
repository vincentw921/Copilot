//
//  Keychain.swift
//  Copilot
//
//  Created by Vincent Wang on 9/14/25.
//

import Foundation
import Security

final class Keychain {
    private let account = "CloudID"
    private let service = Bundle.main.bundleIdentifier ?? "app.cloudid"

    var cloudID: String? {
        get { read() }
        set {
            if let v = newValue { write(v) } else { delete() }
        }
    }

    private func query() -> [String: Any] {
        [kSecClass as String: kSecClassGenericPassword,
         kSecAttrService as String: service,
         kSecAttrAccount as String: account]
    }

    private func read() -> String? {
        var q = query()
        q[kSecReturnData as String] = kCFBooleanTrue
        q[kSecMatchLimit as String] = kSecMatchLimitOne
        var item: CFTypeRef?
        let status = SecItemCopyMatching(q as CFDictionary, &item)
        guard status == errSecSuccess, let data = item as? Data else { return nil }
        return String(data: data, encoding: .utf8)
    }

    private func write(_ value: String) {
        let data = Data(value.utf8)
        var q = query()
        SecItemDelete(q as CFDictionary) // replace if exists
        q[kSecValueData as String] = data
        SecItemAdd(q as CFDictionary, nil)
    }

    private func delete() {
        SecItemDelete(query() as CFDictionary)
    }
}
