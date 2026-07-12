//
//  CommunityComposeView.swift
//  ChamChamCham
//
//  Created by iyungui on 7/7/26.
//

import PhotosUI
import SwiftUI

/// "게시물 작성하기" composer. Calls `onCreated` with the new post id after a successful submit and
/// dismisses itself.
struct CommunityComposeView: View {
    enum Layout {
        static let horizontalInset: CGFloat = 20
        static let textAreaContentInset: CGFloat = 20
        static let titleHeight: CGFloat = 38
        static let minimumBodyLines = 9
        static let maximumBodyLines = 21
        static let descriptionSpacing: CGFloat = 12
        static let imageSpacing: CGFloat = 12
        static let sectionTopInset: CGFloat = 16
        static let majorSectionGap: CGFloat = 24
    }

    private enum FocusedField {
        case title
        case body
    }

    private let container: DIContainer
    private let onCreated: (UUID) -> Void

    @State private var viewModel: CommunityComposeViewModel
    @State private var pickerItems: [PhotosPickerItem] = []
    @State private var showCropPicker = false
    @State private var showRecordPicker = false
    /// Presentation-only selection until the farming-record data source is wired; submit still sends nil.
    @State private var selectedFarmingRecord: FarmingRecordPreview?
    @FocusState private var focusedField: FocusedField?
    @Environment(\.dismiss) private var dismiss

    init(container: DIContainer, onCreated: @escaping (UUID) -> Void) {
        self.container = container
        self.onCreated = onCreated
        _viewModel = State(
            initialValue: CommunityComposeViewModel(
                repository: container.makeCommunityRepository(),
                cropCatalog: container.makeCropCatalogService(),
                mediaRepository: container.makeMediaUploadRepository()
            )
        )
    }

    var body: some View {
        VStack(spacing: 0) {
            AppTopAppBar(
                title: "게시물 작성하기",
                isDetail: true,
                showBorder: false,
                leading: .init("chevron.left") { dismiss() }
            )

            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    cropSection
                    composeTextArea
                    sectionDivider
                    farmingRecordSection
                    imageSection
                    sectionDivider
                    questionToggle
                    errorMessage
                }
                .padding(.bottom, Spacing.lg)
            }
            .scrollDismissesKeyboard(.interactively)
        }
        .background(Color.Background.default)
        .safeAreaInset(edge: .bottom) {
            submitBar
        }
        .task { await viewModel.loadBoards() }
        .sheet(isPresented: $showCropPicker) {
            CropPickerSheet(loadCrops: viewModel.catalogCrops) { crops in
                viewModel.addBoards(from: crops)
            }
        }
        .fullScreenCover(isPresented: $showRecordPicker) {
            FarmingRecordPickerView(selectedRecord: $selectedFarmingRecord)
        }
        .onChange(of: pickerItems) { _, items in
            guard !items.isEmpty else { return }
            Task { await attach(items) }
        }
    }

    // MARK: - Crop board selection

    private var cropSection: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 2) {
                Text("작물 게시판")
                    .appTypography(.bodyMedium)
                    .foregroundStyle(Color.Text.default)
                Text("*")
                    .appTypography(.bodyMedium)
                    .foregroundStyle(Color.Text.red)
            }
            .frame(height: 24)
            .padding(.horizontal, Layout.horizontalInset)

            HStack(spacing: 0) {
                Button {
                    showCropPicker = true
                } label: {
                    Image(systemName: "plus")
                        .font(.system(size: 24, weight: .medium))
                        .foregroundStyle(Color.Icon.subtle)
                        .frame(width: 32, height: 32)
                        .background(Color.Object.default)
                        .overlay(Circle().stroke(Color.Border.default, lineWidth: 1))
                        .clipShape(Circle())
                }
                .buttonStyle(.plain)
                .frame(width: 60, height: 48)
                .overlay(alignment: .trailing) {
                    Rectangle()
                        .fill(Color.Border.default)
                        .frame(width: 1, height: 24)
                }

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: Spacing.sm) {
                        ForEach(viewModel.boards) { board in
                            AppChip(
                                label: board.cropName,
                                isSelected: viewModel.selectedCropId == board.cropId,
                                style: .solid
                            ) {
                                viewModel.selectCrop(board.cropId)
                            }
                        }
                    }
                    .padding(.leading, Spacing.sm)
                    .padding(.trailing, Layout.horizontalInset)
                    .frame(height: 48)
                }
            }
            .frame(height: 48)
        }
        .padding(.top, Layout.sectionTopInset)
    }

    // MARK: - Text area

    private var composeTextArea: some View {
        VStack(alignment: .leading, spacing: Spacing.md) {
            TextField(
                "",
                text: $viewModel.title,
                prompt: Text("제목을 입력해주세요.")
                    .foregroundStyle(Color.Text.muted)
            )
                .appTypography(.titleMedium)
                .foregroundStyle(viewModel.isTitleOverLimit ? Color.Text.red : Color.Text.default)
                .lineLimit(1)
                .focused($focusedField, equals: .title)
                .frame(height: Layout.titleHeight, alignment: .topLeading)
                .overlay(alignment: .bottom) {
                    Rectangle()
                        .fill(Color.Border.default)
                        .frame(height: 1)
                }

            VStack(alignment: .leading, spacing: Layout.descriptionSpacing) {
                TextField(
                    "",
                    text: $viewModel.body,
                    prompt: Text("농사와 관련해 이야기하고 싶은 내용을 자유롭게 작성해보세요.")
                        .foregroundStyle(Color.Text.muted),
                    axis: .vertical
                )
                .appTypography(.bodyLarge)
                .foregroundStyle(Color.Text.subtle)
                .lineLimit(Layout.minimumBodyLines...Layout.maximumBodyLines)
                .focused($focusedField, equals: .body)

                validationRow
            }
        }
        .padding(Layout.textAreaContentInset)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.Background.subtle)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .padding(.horizontal, Layout.horizontalInset)
        .padding(.top, Spacing.md)
        .padding(.bottom, Layout.majorSectionGap)
    }

    private var validationRow: some View {
        HStack(alignment: .top, spacing: Spacing.sm) {
            if let inputValidationMessage = viewModel.inputValidationMessage {
                Text(inputValidationMessage)
                    .appTypography(.labelMedium)
                    .foregroundStyle(Color.Text.red)
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)
            }

            Spacer(minLength: Spacing.sm)

            Text("\(viewModel.body.count)/\(CommunityComposeViewModel.bodyLimit)")
                .appTypography(.labelMedium)
                .foregroundStyle(viewModel.isBodyOverLimit ? Color.Text.red : Color.Text.muted)
                .lineLimit(1)
        }
    }

    // MARK: - Farming record

    private var farmingRecordSection: some View {
        VStack(alignment: .leading, spacing: Layout.imageSpacing) {
            Button {
                showRecordPicker = true
            } label: {
                HStack {
                    Text("영농 기록 첨부하기")
                        .appTypography(.bodyMedium)
                        .foregroundStyle(Color.Text.default)
                    Spacer()
                    Image(systemName: "chevron.right")
                        .font(.system(size: 24))
                        .foregroundStyle(Color.Icon.subtle)
                }
                .frame(height: 24)
            }
            .buttonStyle(.plain)
            .padding(.horizontal, Layout.horizontalInset)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: Spacing.sm) {
                    ForEach(FarmingRecordPreview.samples.prefix(3)) { record in
                        Button {
                            selectedFarmingRecord = record
                        } label: {
                            AppCard(
                                size: .xsmall,
                                title: record.title,
                                captions: [record.cropName, record.caption],
                                dateText: record.dateText,
                                isSelected: selectedFarmingRecord?.id == record.id
                            ) {
                                FarmingRecordImage(record: record, height: 84)
                            }
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal, Layout.horizontalInset)
            }
        }
        .padding(.top, Layout.majorSectionGap)
        .padding(.bottom, Layout.majorSectionGap)
    }

    // MARK: - Images

    private var imageSection: some View {
        VStack(alignment: .leading, spacing: Layout.imageSpacing) {
            Text("사진 첨부하기")
                .appTypography(.bodyMedium)
                .foregroundStyle(Color.Text.default)
                .padding(.horizontal, Layout.horizontalInset)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: Layout.imageSpacing) {
                    if viewModel.canAddImage {
                        PhotosPicker(
                            selection: $pickerItems,
                            maxSelectionCount: CommunityComposeViewModel.maxImages - viewModel.attachments.count,
                            matching: .images
                        ) {
                            AppImageUploadSlot(label: "\(viewModel.attachments.count)/\(CommunityComposeViewModel.maxImages)")
                        }
                    }

                    ForEach(viewModel.attachments) { attachment in
                        AppImageUploadSlot(
                            onRemove: { viewModel.removeImage(id: attachment.id) }
                        ) {
                            thumbnail(for: attachment)
                        }
                    }
                }
                .padding(.horizontal, Layout.horizontalInset)
            }
        }
        .padding(.bottom, Layout.majorSectionGap)
    }

    /// The picked image is shown immediately; while its upload is in flight a dimmed spinner overlays it.
    @ViewBuilder private func thumbnail(for attachment: CommunityComposeViewModel.Attachment) -> some View {
        ZStack {
            if let uiImage = UIImage(data: attachment.previewData) {
                Image(uiImage: uiImage).resizable().scaledToFill()
            } else {
                Color.Object.muted
            }
            if attachment.isUploading {
                Color.black.opacity(0.3)
                ProgressView()
                    .tint(Color.Icon.inverse)
            }
        }
    }

    // MARK: - Q&A toggle

    private var questionToggle: some View {
        HStack {
            Text("질문으로 올리기")
                .appTypography(.bodyMedium)
                .foregroundStyle(Color.Text.default)
            Spacer()
            AppToggle(isOn: $viewModel.isQuestion)
        }
        .padding(.horizontal, Layout.horizontalInset)
        .frame(height: 28)
        .padding(.top, Layout.majorSectionGap)
        .padding(.bottom, 36)
    }

    private var submitBar: some View {
        VStack(spacing: 0) {
            Rectangle()
                .fill(Color.Border.subtle)
                .frame(height: 1)

            AppButton(
                viewModel.isSubmitting ? nil : "완료",
                variant: .primary,
                size: .medium,
                fullWidth: true,
                action: submit
            )
            .disabled(!viewModel.canSubmit)
            .overlay {
                if viewModel.isSubmitting {
                    ProgressView()
                        .tint(Color.Text.inverse)
                }
            }
            .padding(.horizontal, Layout.horizontalInset)
            .padding(.top, 12)
            .padding(.bottom, 12)
        }
        .background(Color.Background.default)
    }

    @ViewBuilder private var errorMessage: some View {
        if let errorMessage = viewModel.errorMessage {
            Text(errorMessage)
                .appTypography(.labelMedium)
                .foregroundStyle(Color.Text.red)
                .padding(.horizontal, Layout.horizontalInset)
                .padding(.bottom, Spacing.md)
        }
    }

    private var sectionDivider: some View {
        AppDivider(size: .small)
    }

    private func submit() {
        Task {
            if let id = await viewModel.submit() {
                onCreated(id)
                dismiss()
            }
        }
    }

    private func attach(_ items: [PhotosPickerItem]) async {
        for item in items {
            guard let data = try? await item.loadTransferable(type: Data.self) else { continue }
            await viewModel.addImage(data)
        }
        pickerItems = []
    }
}

private struct FarmingRecordPickerView: View {
    @Binding var selectedRecord: FarmingRecordPreview?
    @Environment(\.dismiss) private var dismiss
    @State private var searchText = ""
    @State private var selectedCropName: String?
    @State private var selectedRecordID: UUID?

    var body: some View {
        VStack(spacing: 0) {
            AppTopAppBar(
                title: "영농 기록 첨부하기",
                isDetail: true,
                showBorder: false,
                leading: .init("chevron.left") { dismiss() }
            )

            filterArea

            ScrollView {
                LazyVStack(spacing: Spacing.md) {
                    if filteredRecords.isEmpty {
                        EmptyStateView(message: "조건에 맞는 기록이 없어요.")
                            .padding(.top, Spacing.xl)
                    } else {
                        ForEach(filteredRecords) { record in
                            Button {
                                selectedRecordID = record.id
                            } label: {
                                FarmingRecordPickerCard(record: record, isSelected: selectedRecordID == record.id)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, Spacing.md)
                .padding(.bottom, Spacing.xl)
            }
        }
        .background(Color.Background.default)
        .safeAreaInset(edge: .bottom) {
            selectBar
        }
        .onAppear {
            selectedRecordID = selectedRecord?.id
            selectedCropName = selectedRecord?.cropName
        }
        .onChange(of: searchText) { _, _ in
            clearSelectionIfNeeded()
        }
    }

    private var filterArea: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            HStack(spacing: Spacing.sm) {
                Image(systemName: "magnifyingglass")
                    .font(.system(size: 24))
                    .foregroundStyle(Color.Icon.subtle)
                TextField("어떤 기록을 올릴까요?", text: $searchText)
                    .appTypography(.bodyLarge)
                    .foregroundStyle(Color.Text.default)
            }
            .padding(.horizontal, Spacing.md)
            .frame(height: 56)
            .background(Color.Background.subtle)
            .clipShape(RoundedRectangle(cornerRadius: 8))
            .padding(.horizontal, 20)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: Spacing.sm) {
                    AppChip(label: "전체", isSelected: selectedCropName == nil, style: .solid) {
                        selectCrop(nil)
                    }
                    ForEach(cropNames, id: \.self) { cropName in
                        AppChip(label: cropName, isSelected: selectedCropName == cropName, style: .solid) {
                            selectCrop(cropName)
                        }
                    }
                }
                .padding(.horizontal, 20)
                .frame(height: 48)
            }
        }
        .padding(.top, Spacing.sm)
        .padding(.bottom, Spacing.sm)
    }

    private var selectBar: some View {
        VStack(spacing: 0) {
            Rectangle()
                .fill(Color.Border.subtle)
                .frame(height: 1)

            Button {
                selectedRecord = FarmingRecordPreview.samples.first { $0.id == selectedRecordID }
                dismiss()
            } label: {
                Text("선택")
                    .appTypography(.bodyLarge)
                    .foregroundStyle(selectedRecordID == nil ? Color.Text.muted : Color.Text.inverse)
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .background(selectedRecordID == nil ? Color.Object.disabled : Color.Object.bold)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .buttonStyle(.plain)
            .disabled(selectedRecordID == nil)
            .padding(.horizontal, 20)
            .padding(.top, 12)
            .padding(.bottom, 12)
        }
        .background(Color.Background.default)
    }

    private var cropNames: [String] {
        Array(Set(FarmingRecordPreview.samples.map(\.cropName))).sorted()
    }

    private var filteredRecords: [FarmingRecordPreview] {
        let keyword = searchText.trimmingCharacters(in: .whitespacesAndNewlines)
        return FarmingRecordPreview.samples.filter { record in
            let cropMatches = selectedCropName == nil || record.cropName == selectedCropName
            let keywordMatches = keyword.isEmpty
                || record.title.localizedCaseInsensitiveContains(keyword)
                || record.caption.localizedCaseInsensitiveContains(keyword)
                || record.cropName.localizedCaseInsensitiveContains(keyword)
            return cropMatches && keywordMatches
        }
    }

    private func selectCrop(_ cropName: String?) {
        selectedCropName = cropName
        clearSelectionIfNeeded()
    }

    private func clearSelectionIfNeeded() {
        guard let selectedRecordID else { return }
        if !filteredRecords.contains(where: { $0.id == selectedRecordID }) {
            self.selectedRecordID = nil
        }
    }
}

private struct FarmingRecordPickerCard: View {
    let record: FarmingRecordPreview
    let isSelected: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.md) {
            HStack(alignment: .center) {
                HStack(spacing: Spacing.xs) {
                    recordBadge(record.cropName)
                    recordBadge(record.category)
                }
                Spacer()
                Text(record.dateText)
                    .appTypography(.labelMedium)
                    .foregroundStyle(Color.Text.muted)
            }

            HStack(spacing: Spacing.md) {
                VStack(alignment: .leading, spacing: Spacing.xs) {
                    Text(record.title)
                        .appTypography(.titleLargeEmphasized)
                        .foregroundStyle(isSelected ? Color.Text.default : Color.Text.subtle)
                        .lineLimit(2)
                    Text(record.caption)
                        .appTypography(.bodyLarge)
                        .foregroundStyle(isSelected ? Color.Text.subtle : Color.Text.muted)
                        .lineLimit(2)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                FarmingRecordImage(record: record, height: 96)
                    .frame(width: 96)
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, minHeight: 180, alignment: .leading)
        .background(isSelected ? Color.Object.primarySubtle : Color.Object.default)
        .overlay {
            RoundedRectangle(cornerRadius: 16)
                .stroke(isSelected ? Color.Border.primary : Color.Border.default, lineWidth: 1)
        }
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func recordBadge(_ label: String) -> some View {
        Text(label)
            .appTypography(.labelMedium)
            .foregroundStyle(isSelected ? Color.Text.primary : Color.Text.subtle)
            .lineLimit(1)
            .padding(.horizontal, Spacing.sm)
            .frame(height: 32)
            .background(isSelected ? Color.Object.default : Color.Object.muted)
            .clipShape(RoundedRectangle(cornerRadius: 8))
    }
}

private struct FarmingRecordImage: View {
    let record: FarmingRecordPreview
    let height: CGFloat

    var body: some View {
        RoundedRectangle(cornerRadius: 12)
            .fill(
                LinearGradient(
                    colors: [Color(hex: record.imageStartHex), Color(hex: record.imageEndHex)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            )
            .overlay {
                Image(systemName: "leaf.fill")
                    .font(.system(size: height > 90 ? 34 : 28))
                    .foregroundStyle(Color.Text.inverse.opacity(0.8))
            }
            .overlay(alignment: .top) {
                LinearGradient(
                    colors: [.black.opacity(0.28), .clear],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .frame(height: height * 0.42)
            }
            .frame(maxWidth: .infinity)
            .frame(height: height)
            .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

private struct FarmingRecordPreview: Identifiable, Hashable, Sendable {
    let id: UUID
    let dateText: String
    let cropName: String
    let category: String
    let title: String
    let caption: String
    let imageStartHex: UInt32
    let imageEndHex: UInt32

    static let samples: [FarmingRecordPreview] = [
        .init(
            id: UUID(uuidString: "0FC55A80-93F2-4C74-939B-B424842AD35C")!,
            dateText: "07/08",
            cropName: "딸기",
            category: "필수 데이터",
            title: "영농 활동",
            caption: "관수량과 생육 상태 기록",
            imageStartHex: 0x7FE19E,
            imageEndHex: 0x27865C
        ),
        .init(
            id: UUID(uuidString: "5C2877A9-0EA3-4851-860B-E33942D6D97E")!,
            dateText: "07/07",
            cropName: "토마토",
            category: "필수 데이터",
            title: "타이틀",
            caption: "병해충 확인 및 방제 메모",
            imageStartHex: 0xBAED4F,
            imageEndHex: 0x699018
        ),
        .init(
            id: UUID(uuidString: "2F41D692-A887-422E-9DDF-0EEAD2C63C8D")!,
            dateText: "07/06",
            cropName: "오이",
            category: "필수 데이터",
            title: "영농 활동 카테고리",
            caption: "작성 내용은 최대 2줄입니다.",
            imageStartHex: 0xC6F1CB,
            imageEndHex: 0x38C284
        ),
        .init(
            id: UUID(uuidString: "A0833192-76DF-48C4-AFCB-38EF4F36E3FE")!,
            dateText: "07/05",
            cropName: "상추",
            category: "필수 데이터",
            title: "토양 관리",
            caption: "멀칭 상태와 수분 보유량 확인",
            imageStartHex: 0xE6F7BF,
            imageEndHex: 0x8CC610
        ),
    ]
}
