//
//  AppCard.swift
//  ChamChamCham
//
//  Created by iyungui on 7/7/26.
//

import SwiftUI

/// Figma `card`. A single card with two sizes:
/// - `small`: compact card (default width 258) — image with an overlaid white badge, title, and
///   up to two caption lines.
/// - `medium`: full-width post card — larger image, a badge row + date, title, a caption, and a
///   profile / reactions footer.
///
/// Images use ``AppImagePlaceholder`` by default; pass a `thumbnail` for real media.
struct AppCard<Thumbnail: View>: View {
    enum Size {
        case small
        case medium
    }

    let size: Size
    let title: String
    var captions: [String] = []
    var badges: [String] = []
    var dateText: String = "mm/dd"
    var nickname: String = "닉네임"
    var likeText: String = "nn"
    var commentText: String = "nn"
    var showsPostInfo: Bool = true
    /// Overrides width. `small` defaults to 258; `medium` fills its container.
    var width: CGFloat? = nil

    private let thumbnail: Thumbnail?

    init(
        size: Size,
        title: String,
        captions: [String] = [],
        badges: [String] = [],
        dateText: String = "mm/dd",
        nickname: String = "닉네임",
        likeText: String = "nn",
        commentText: String = "nn",
        showsPostInfo: Bool = true,
        width: CGFloat? = nil,
        @ViewBuilder thumbnail: () -> Thumbnail
    ) {
        self.size = size
        self.title = title
        self.captions = captions
        self.badges = badges
        self.dateText = dateText
        self.nickname = nickname
        self.likeText = likeText
        self.commentText = commentText
        self.showsPostInfo = showsPostInfo
        self.width = width
        self.thumbnail = thumbnail()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            imageArea

            if size == .small {
                smallContent
            } else {
                mediumContent
            }
        }
        .padding(size == .medium ? Spacing.lg : Spacing.md)
        .frame(width: size == .small ? (width ?? 258) : width)
        .frame(maxWidth: (size == .medium && width == nil) ? .infinity : nil)
        .background(Color.Object.default)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.Border.default, lineWidth: 1))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    // MARK: - Image

    private var imageArea: some View {
        media
            .frame(height: size == .medium ? 178 : 126)
            .frame(maxWidth: .infinity)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(alignment: .topLeading) {
                if size == .small, let badge = badges.first {
                    overlayBadge(badge)
                        .padding(Spacing.sm)
                }
            }
    }

    @ViewBuilder private var media: some View {
        if let thumbnail {
            thumbnail
        } else {
            AppImagePlaceholder(cornerRadius: 0)
        }
    }

    private func overlayBadge(_ label: String) -> some View {
        Text(label)
            .appTypography(.labelMedium)
            .foregroundStyle(Color.Text.subtle)
            .padding(Spacing.sm)
            .frame(minWidth: 36, minHeight: 28)
            .background(Color.Object.default)
            .clipShape(RoundedRectangle(cornerRadius: 8))
    }

    // MARK: - Small

    private var smallContent: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            Text(title)
                .appTypography(.titleLargeEmphasized)
                .foregroundStyle(Color.Text.subtle)
                .lineLimit(1)

            if !captions.isEmpty {
                VStack(alignment: .leading, spacing: 2) {
                    ForEach(Array(captions.prefix(2).enumerated()), id: \.offset) { _, caption in
                        Text(caption)
                            .appTypography(.bodyLarge)
                            .foregroundStyle(Color.Text.subtle)
                            .lineLimit(1)
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    // MARK: - Medium

    private var mediumContent: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top) {
                HStack(spacing: Spacing.xs) {
                    ForEach(Array(badges.prefix(2).enumerated()), id: \.offset) { _, badge in
                        AppBadge(label: badge, size: .small, style: .solidPastel, variant: .secondary)
                    }
                }
                Spacer(minLength: Spacing.md)
                Text(dateText)
                    .appTypography(.bodyMedium)
                    .foregroundStyle(Color.Text.subtle)
            }

            VStack(alignment: .leading, spacing: Spacing.xs) {
                Text(title)
                    .appTypography(.titleLargeEmphasized)
                    .foregroundStyle(Color.Text.subtle)
                    .lineLimit(1)
                if let caption = captions.first {
                    Text(caption)
                        .appTypography(.bodyLarge)
                        .foregroundStyle(Color.Text.muted)
                        .lineLimit(1)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            if showsPostInfo {
                HStack(spacing: Spacing.xs) {
                    HStack(spacing: Spacing.sm) {
                        AppAvatar(size: .small) {
                            AppImagePlaceholder(isCircle: true, squareSize: 6)
                        }
                        Text(nickname)
                            .appTypography(.bodyMedium)
                            .foregroundStyle(Color.Text.muted)
                            .lineLimit(1)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)

                    HStack(spacing: 12) {
                        reaction(systemImage: "heart", text: likeText)
                        reaction(systemImage: "bubble.left", text: commentText)
                    }
                }
            }
        }
    }

    private func reaction(systemImage: String, text: String) -> some View {
        HStack(spacing: 2) {
            Image(systemName: systemImage)
                .font(.system(size: 22))
                .foregroundStyle(Color.Icon.subtle)
                .frame(width: 24, height: 24)
            Text(text)
                .appTypography(.bodyMedium)
                .foregroundStyle(Color.Text.muted)
        }
    }
}

extension AppCard where Thumbnail == EmptyView {
    init(
        size: Size,
        title: String,
        captions: [String] = [],
        badges: [String] = [],
        dateText: String = "mm/dd",
        nickname: String = "닉네임",
        likeText: String = "nn",
        commentText: String = "nn",
        showsPostInfo: Bool = true,
        width: CGFloat? = nil
    ) {
        self.size = size
        self.title = title
        self.captions = captions
        self.badges = badges
        self.dateText = dateText
        self.nickname = nickname
        self.likeText = likeText
        self.commentText = commentText
        self.showsPostInfo = showsPostInfo
        self.width = width
        self.thumbnail = nil
    }
}

#Preview {
    ScrollView {
        VStack(spacing: Spacing.lg) {
            AppCard(
                size: .small,
                title: "타이틀",
                captions: ["캡션...", "캡션..."],
                badges: ["레이블"]
            )

            AppCard(
                size: .medium,
                title: "타이틀",
                captions: ["캡션..."],
                badges: ["레이블", "레이블"]
            )
        }
        .padding()
        .background(Color.Background.subtle)
    }
}
