//
//  MockCropCatalog.swift
//  ChamChamCham
//
//  Created by iyungui on 7/3/26.
//

import Foundation

// Placeholder data standing in for `GET /api/v1/crops` (CROP-001) until that
// endpoint is wired up. IDs are fixed literals, not `UUID()`, so a crop
// selection saved to the onboarding draft still resolves after relaunch.
enum MockCropCatalog {
    static let categories = ["인기", "약초류", "근채류", "화훼·열매", "허브·잎"]

    static let crops: [Crop] = [
        Crop(id: UUID(uuidString: "00000000-0000-0000-0000-000000000001")!, name: "황기", category: "약초류"),
        Crop(id: UUID(uuidString: "00000000-0000-0000-0000-000000000002")!, name: "당귀", category: "약초류"),
        Crop(id: UUID(uuidString: "00000000-0000-0000-0000-000000000003")!, name: "작약", category: "약초류"),
        Crop(id: UUID(uuidString: "00000000-0000-0000-0000-000000000004")!, name: "인삼", category: "약초류"),
        Crop(id: UUID(uuidString: "00000000-0000-0000-0000-000000000005")!, name: "도라지", category: "약초류"),
        Crop(id: UUID(uuidString: "00000000-0000-0000-0000-000000000006")!, name: "더덕", category: "약초류"),
        Crop(id: UUID(uuidString: "00000000-0000-0000-0000-000000000007")!, name: "강황", category: "약초류"),
        Crop(id: UUID(uuidString: "00000000-0000-0000-0000-000000000008")!, name: "감초", category: "약초류"),
        Crop(id: UUID(uuidString: "00000000-0000-0000-0000-000000000009")!, name: "천궁", category: "약초류"),
        Crop(id: UUID(uuidString: "00000000-0000-0000-0000-00000000000a")!, name: "백출", category: "약초류"),
        Crop(id: UUID(uuidString: "00000000-0000-0000-0000-00000000000b")!, name: "오미자", category: "약초류"),
        Crop(id: UUID(uuidString: "00000000-0000-0000-0000-00000000000c")!, name: "구기자", category: "약초류"),
        Crop(id: UUID(uuidString: "00000000-0000-0000-0000-00000000000d")!, name: "쑥", category: "약초류"),
        Crop(id: UUID(uuidString: "00000000-0000-0000-0000-00000000000e")!, name: "뽕잎", category: "약초류"),
        Crop(id: UUID(uuidString: "00000000-0000-0000-0000-00000000000f")!, name: "결명자", category: "약초류"),
        Crop(id: UUID(uuidString: "00000000-0000-0000-0000-000000000010")!, name: "산수유", category: "약초류"),
        Crop(id: UUID(uuidString: "00000000-0000-0000-0000-000000000011")!, name: "택사", category: "약초류"),
        Crop(id: UUID(uuidString: "00000000-0000-0000-0000-000000000012")!, name: "복령", category: "약초류"),
        Crop(id: UUID(uuidString: "00000000-0000-0000-0000-000000000013")!, name: "지황", category: "약초류"),
        Crop(id: UUID(uuidString: "00000000-0000-0000-0000-000000000014")!, name: "시호", category: "약초류"),
    ]
}
