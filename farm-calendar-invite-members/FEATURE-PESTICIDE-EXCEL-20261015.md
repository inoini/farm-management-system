# Excel Export Feature (2026-10-15)

## Current implementation

This repository retains the original stock, crop, field, schedule, profit and photo-management functionality.

The pesticide-use condition checker introduced in the original feature PR was removed on user request. The stock-list button, checker endpoint, checker page, checker service and its dedicated test have all been removed. The previously recorded pesticide data fields within the stock inventory are preserved (for example dilution rate and max use count); this does NOT imply pesticide-use suitability verification.

Excel export remains available as an additional feature on the original pages:
- `/management`: Download the currently selected year's field/crop-profit and sales/expense workbook.
- `/work-history`: Download the current year's work history.
- `/stock`: Download the inventory listing.
- `/crop`: Download crop listings.
- `/field`: Download field listings.

The endpoints are authenticated, scoped to the current farm, read-only and return .xlsx files. No database schema change is required. Values entered as spreadsheet text are never interpreted as spreadsheet formulas by the generator. Fields with identical names remain distinguished by their IDs.

## Restore a previous project state

The snapshot branch `backup/before-removing-pesticide-check-20261015` contains the commit before this removal.
Rollback the specific removal merge/revert through GitHub if needed. This change is intentionally narrowly scoped to the requested feature removal.

## Verification

From `farm-calendar-invite-members` run:

```bash
mvn test
mvn package
```

GitHub Actions verifies Excel export and existing app tests before applying changes.
