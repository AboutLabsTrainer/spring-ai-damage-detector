# DESIGN upstream changes: transistor-damage-detection

Date 2026-10-30. All four changes (UC-1 to UC-4) were **confirmed by the user on 2026-10-30**. Neither one changes a user-visible outcome of a UAT scenario.

## UC-1: US-03 AC 1, which errors are retried (DDD-6, ADR-004)

- **Current AC (US-03):** "A failed Jev call (timeout, connection error, HTTP error, unreadable answer) is retried with increasing waits, default 3 attempts in total, configurable."
- **Proposed AC:** "A failed Jev call is retried with increasing waits if it is transient (timeout, connection error, HTTP 408/429/5xx, unreadable answer), default 3 attempts in total, configurable. Other HTTP 4xx errors (e.g. 401, 403, 400) are not retried; the image becomes REVIEW with `Jev call failed after 1 attempt: HTTP <code> <reason>`."
- **Why:** retrying a revoked key or a malformed request cannot succeed. Retrying would add ≥ 3 s of backoff per image (≥ 12 min for 253 images), which breaks K5. The US-03 technical note leaves "which errors are retried" to DESIGN.
- **Scenario impact:** none. "401 on every call → 4 REVIEW, 0 of 4 judged, non-zero exit" still holds. The 503 scenarios still show 3 attempts.

## UC-3: Precise C11 assertion (US-01 scenario "Folder names are only used for scoring", INV-2; DDD-24)

- **Current:** "no request to Jev contains a file name or a folder name such as "damaged_case" or "good"".
- **Problem:** every request legitimately contains the fixed class vocabulary `GOOD`, `DAMAGED_CASE`, … (`DefectQuestion` criteria). These match the folder names case-insensitively, so the literal scenario cannot pass.
- **Proposed:** "No request to Jev contains an image's file name, its relative path, a folder label followed by a path separator (e.g. `damaged_case/`), or the input folder name. Apart from the image data, every request is identical for every image." The fixed class vocabulary is allowed because it is the same for every image and carries no ground truth.

## UC-4: Jev read timeout 30 s (US-03 example 2; DDD-23)

- **Current example:** "`good/042.png` times out on all 3 attempts … `timeout after 120 s`".
- **Proposed:** "`timeout after 30 s`". The default read timeout is 30 s, configurable as `detector.jev.read-timeout`. With 120 s, a single hanging image breaks K5 (≤ 10 min per 253 images); with 30 s, K5 holds for up to 3 hanging images.

## Note: Spring Boot 4.1 upgrade (ADR-006, user decision 2026-10-30)

This is not an upstream change: no story, AC or scenario changes. It is listed here only so that DISTILL and DELIVER know that DELIVER step 1 is the framework upgrade.

## UC-2: Exit codes are distinct (DDD-9, ADR-005)

- **Current:** D14, D18 and INV-5 say only "non-zero".
- **Proposed:**

  | Code | Meaning |
  |---|---|
  | 1 | Refused or unexpected error |
  | 2 | False pass(es) at the configured threshold (D18) |
  | 3 | 0 images judged by Jev (D14) |

  Precedence is 1 > 3 > 2.
- **Why:** a demo script can tell "unsafe threshold" apart from "misconfigured run". This is a refinement only, and every existing "non-zero" assertion stays true.
