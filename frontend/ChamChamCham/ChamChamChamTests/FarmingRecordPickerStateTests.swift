//
//  FarmingRecordPickerStateTests.swift
//  ChamChamChamTests
//
//  Created by iyungui on 7/12/26.
//

import CoreGraphics
import Foundation
import Testing
@testable import ChamChamCham

@MainActor
@Suite("Farming record picker state")
struct FarmingRecordPickerStateTests {
    @Test("crop and search filters clear an invisible selection")
    func filteringClearsInvisibleSelection() {
        let records = FarmingRecordPreview.samples
        let state = FarmingRecordPickerState(records: records)

        state.selectRecord(records[0].id)
        #expect(state.selectedRecordID == records[0].id)

        state.selectCrop(records[1].cropName)
        #expect(state.selectedRecordID == nil)
        #expect(state.filteredRecords.allSatisfy { $0.cropName == records[1].cropName })

        state.selectRecord(records[1].id)
        state.searchText = "검색 결과 없음"
        #expect(state.selectedRecordID == nil)
        #expect(state.filteredRecords.isEmpty)
    }

    @Test("picker geometry matches the selected-state capture")
    func geometry() {
        #expect(FarmingRecordPickerView.Layout.horizontalInset == 20)
        #expect(FarmingRecordPickerView.Layout.filterTopInset == 16)
        #expect(FarmingRecordPickerView.Layout.chipAreaHeight == 64)
        #expect(FarmingRecordPickerView.Layout.cardSpacing == 16)
        #expect(FarmingRecordPickerView.Layout.listTopInset == 8)
    }
}
