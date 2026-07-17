#Report Detail — 비료 주기 (Fertilizing) Screen Capture

Captured 2026-07-18 via TalkToFigma. Figma node `1711:24886`, name
`비료주기 리포트`, type `FRAME`, size 390×1769. Third Report Detail workType
capture, following
[2026-07-18-report-detail-planting.md](2026-07-18-report-detail-planting.md)
(심기, findings 1–5 fixed in `3aa1457b`) and
[2026-07-18-report-detail-watering.md](2026-07-18-report-detail-watering.md)
(물주기).

Current implementation: shared chrome in `ReportDetailView.swift` +
`ReportMetricCard.swift` (already covered), charts via `ReportChartCard.swift`
(already covered by
[2026-07-18-report-detail-chart-spec.md](2026-07-18-report-detail-chart-spec.md)),
workType wiring in
[ReportPresentationModels.swift:75-103](../../../ChamChamCham/ChamChamCham/Features/Report/Presentation/Models/ReportPresentationModels.swift)
(`case .fertilizing`), backed by
[`FertilizingReportStatistics`](../../../ChamChamCham/ChamChamCham/Features/Report/Domain/FarmingWorkReportStatistics.swift)
(`totalAmountKg`, `averageAmountKg`, `amountCoverage`, `materialCategories`,
`methodDistribution`, `categoryMethods`).

## Structure (top to bottom) vs current code

1. **Top app bar** (`1711:24972`) — leading `icon/arrow_back_ios_new`,
   trailing `icon/more_vert`. Matches.
2. **Badges + period row** — same crop/farm badges, date range with `-`
   (hyphen) separator — same as 심기/물주기.
3. **WorkType title** ("비료주기", `1711:24898`) — SemiBold 28px `#1a1a1a`.
4. **Metric cards** (`1711:24899` `overview`, 2-up grid):
   - card-1: "총 작업 횟수" / "15회" (shared base metric).
   - card-2: "총 비료 사용량" / "100kg" — **matches code exactly**
     ([ReportPresentationModels.swift:78-81](../../../ChamChamCham/ChamChamCham/Features/Report/Presentation/Models/ReportPresentationModels.swift),
     title `"총 비료 사용량"`, value from `totalAmountKg`). No mismatch here,
     unlike the watering metric-title finding.
5. **"상세 정보" chart section** (`1711:24906`) — **3 cards this time**
   (fertilizing has 2 material-category donuts + 1 method stacked-bar, vs
   watering's 2 stacked bars):
   - card `1711:24908`: title **"진행한 비료주기 방식"**, stacked-bar graph
     (`1711:24912`) — "점적" 8번 (`#38c284`), unlabeled (`#c8f468`),
     unlabeled (`#f7dc11`). This is the `methodDistribution` chart.
   - card `1711:24918`: title **"각 비료 사용 횟수"**, **donut** graph
     (`1711:24922`, 6 slices: `#b1cbdf`, `#81dacb`, `#c8f468`, `#f7dc11`,
     `#a5e9b1`, `#38c284`), center label "A비료" / "12번". This is the
     `materialCategories` recordCount chart.
   - card `1711:24933`: title **"각 비료 사용량"**, donut graph (`1711:24937`,
     same 6-color palette), center label "A비료" / "500kg". This is the
     `materialCategories` amountKg chart.
6. **"참참참의 코칭"** (`1711:24948`) — same 4-card shape/copy as 심기/물주기
   (mock reuses identical placeholder text across workTypes), not compared
   in detail.
7. **Divider + "기록 내역 리스트"** (`1711:24964`) — `icon/arrow_forward_ios`
   + 3 inline preview rows, identical shape to prior captures.

## Confirmed matches

- **Findings 1–5 hold on this screen too** (icons, WorkType title font,
  metric label/value styling) — no regressions, same as the 물주기
  reconfirmation.
- **Base + fertilizing metric titles/values match code exactly** — no
  metric-title mismatch here (contrast with the 물주기 capture's finding 1).
- **Donut palette matches `Color.Chart.palette` exactly**: `#38c284`=primary,
  `#a5e9b1`=green300, `#f7dc11`=yellow, `#c8f468`=lime, `#81dacb`=turquoise
  (post-fix value), `#b1cbdf`=blue — all 6 palette colors appear in Figma's
  6-slice donut in the same
  [Color+App.swift:132-141](../../../ChamChamCham/ChamChamCham/Core/DesignSystem/Foundation/Color+App.swift)
  order. No color bug.
- Donut center-label layout ("A비료" + count/amount) matches the existing
  `ReportChartCard` donut pattern already confirmed in the chart-spec doc —
  no new issue (the known "center label hidden on expand" bug from that doc
  still applies here, not re-flagged).

## ⚠️ New findings (not fixed — documented only)

### 1. All 3 chart titles differ from Figma's copy
Code
([ReportPresentationModels.swift:83-102](../../../ChamChamCham/ChamChamCham/Features/Report/Presentation/Models/ReportPresentationModels.swift)):

```swift
Self.appendChart(title: "비료 종류별 작업 횟수", data: ..., to: &charts)   // count donut
Self.appendChart(title: "비료 종류별 사용량", data: ..., to: &charts)     // amount donut
Self.appendDistributionChart(title: "비료 주는 방법", values: statistics.methodDistribution, to: &charts)  // method bar
```

Figma's titles for the same three charts: **"진행한 비료주기 방식"** (method
bar), **"각 비료 사용 횟수"** (count donut), **"각 비료 사용량"** (amount
donut) — none of the three match the code's strings verbatim. This is the
same class of issue as the 물주기 capture's finding 2 (there, "물의 양"/"물
주는 방법" vs Figma's "물 준 양"/"진행한 물주기 방식") — **now confirmed
across two workTypes**, so this looks like a systemic chart-title copy drift
across the whole Report Detail feature, not a one-off per workType.

### 2. Chart order: method chart is always first in Figma, but code puts it in different positions per workType
Figma renders **method/style distribution first**, then the
material-specific charts (진행한 비료주기 방식 → 각 비료 사용 횟수 → 각 비료
사용량). Code appends **method chart last**
([ReportPresentationModels.swift:102](../../../ChamChamCham/ChamChamCham/Features/Report/Presentation/Models/ReportPresentationModels.swift),
after both material-category charts).

Cross-referencing the 물주기 capture: there, code puts the method chart
**second** (after the amount chart), while Figma puts it **first** too. So
across both workTypes captured so far, **Figma consistently orders the
"how it was done" method/style chart first**, while the code's append order
is inconsistent per workType (second for watering, last for fertilizing).
Worth checking the remaining workTypes' captures before deciding whether to
fix this as one systemic "move `appendDistributionChart(methodDistribution)`
first" change, or per-workType.

## Reconfirmed open items (not new)

- **Date-range separator** `-` vs `~` — same open question, recurs a third
  time.
- **Inline record-row preview** — same product-scope gap, recurs a third
  time; still just a placeholder link in code.

## Summary of open findings (not yet fixed)

1. Chart titles ("비료 종류별 작업 횟수"/"비료 종류별 사용량"/"비료 주는
   방법") don't match Figma's copy ("각 비료 사용 횟수"/"각 비료 사용량"/
   "진행한 비료주기 방식") — same systemic pattern as 물주기's chart-title
   finding.
2. Chart order: code puts the method-distribution chart last; Figma puts it
   first — same systemic pattern as 물주기's chart-order finding, now seen
   twice.
3. (Reconfirmed, not new) Date-range separator `-` vs `~`.
4. (Reconfirmed, not new) Inline record-row preview scope gap.
