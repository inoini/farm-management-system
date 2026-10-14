# 2026-10-15 Additive feature update: pesticide check and Excel export

## Existing application preserved
- All changes are on `feature/pesticide-check-excel-export-20261015`, not on `main`.
- No existing table is dropped, no schema migration is required, and existing screens/routes remain available.
- Previous field-profit aggregation remains unchanged.
- Compare/review the draft pull request and merge only after tests and review.
- To discard the change, close the pull request; `main` remains unchanged.
- If already merged, revert the merge commit on GitHub and redeploy Render.

## Added menu actions (no new sidebar items)
- 在庫管理: `/stock` -> 「使用条件チェック」 for pesticide items.
- 経営管理: `/management` -> year-specific finance export.
- 作業履歴: `/work-history` -> current-year export.
- 在庫管理: `/stock` -> complete inventory export.
- 作物管理: `/crop` -> complete crop export.
- 圃場管理: `/field` -> complete field export.

## Data safety
- All Excel endpoints require login and only read current farm's records.
- Spreadsheet cells derived from user-entered strings are stored as strings, never formulas.
- Download responses use a private/no-store header.
- The pesticide check reads only inventory values and inputs from its screen; it never alters use counts or records a pesticide application.
- No integration with the official pesticide registry is present. This is a preliminary comparison of values manually registered by the user. Displayed results are **not** permission to use a pesticide. Applicability to pests, methods and active-ingredient totals is **not verified**.
- Verify up-to-date official registration and container label before pesticide use: https://pesticide.maff.go.jp/

## Deployment
Run Maven with Java 21:
```
cd farm-calendar-invite-members
mvn test
mvn package
```
Confirm build and feature tests on PR before merging. After merging, use existing Render deploy workflow and verify new buttons. Do not change Render settings or database environment variables.

## Excel sheets
- `management-YYYY.xlsx`: 圃場別収支, 作物別収支, 売上明細, 経費明細, filtered to YYYY.
- `work-history-YYYY.xlsx`: 作業履歴, filtered to YYYY.
- `farm-inventory.xlsx`: 在庫一覧.
- `farm-crops.xlsx`: 作物一覧.
- `farm-fields.xlsx`: 圃場一覧.

Finance export records without a plot appear as 「圃場未設定」, and same-named registered fields remain distinguishable by ID. Farm-wide shared costs are in the unassigned plot totals, not arbitrarily allocated.
