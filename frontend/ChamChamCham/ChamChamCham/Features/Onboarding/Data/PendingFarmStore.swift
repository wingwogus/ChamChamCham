//
//  PendingFarmStore.swift
//  ChamChamCham
//
//  Created by iyungui on 7/12/26.
//

import Foundation

actor PendingFarmStore {
    private let defaults: UserDefaults
    private let key = "onboarding.pending-extra-farms"

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    func replace(with requests: [SaveFarmRequestDTO]) {
        guard let data = try? JSONEncoder().encode(requests) else { return }
        defaults.set(data, forKey: key)
    }

    func load() -> [SaveFarmRequestDTO] {
        guard let data = defaults.data(forKey: key),
              let requests = try? JSONDecoder().decode([SaveFarmRequestDTO].self, from: data) else {
            return []
        }
        return requests
    }

    func removeFirst() {
        var requests = load()
        guard !requests.isEmpty else { return }
        requests.removeFirst()
        replace(with: requests)
    }
}
