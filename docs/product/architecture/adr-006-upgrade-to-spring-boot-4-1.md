# ADR-006: Upgrade to Spring Boot 4.1

## Status
Accepted (user decision, 2026-10-30). The upgrade is **the first DELIVER step**, before any story work.

## Context
- The walking skeleton (`89c9f4d`) is on Spring Boot 3.5.16 with Java 21.
- **Support dates** (as stated in the user decision of 2026-10-30):
  - Spring Boot 3.5: OSS support ended 2026-06-30, and 3.5.16 was its last free release.
  - Spring Boot 4.0: OSS support ends 2026-12-31.
  - Spring Boot 4.1: OSS support runs until 2027-07-31.
- C1 asks for Spring Boot best practices. A showcase shown and shared from late 2026 onward should not run on an unpatched framework.
- The codebase is small (about 12 production classes), so migration cost is lowest now, before the 4 stories add code.

## Decision
- Move to **Spring Boot 4.1.x**, taking the latest 4.1 patch available at DELIVER step 1. Java 21 stays.
- **Verify on Maven Central in DELIVER step 1** and pin in `pom.xml`:
  - the exact Spring Boot 4.1 patch;
  - a Cucumber version whose `cucumber-junit-platform-engine` supports the JUnit Platform managed by Boot 4.1 (today 7.20.1);
  - a WireMock standalone version that runs on that stack (today 3.13.2);
  - the ArchUnit 1.4.x patch.
- **Boot 4 changes that matter here** (the crafter checks each one; the walking-skeleton acceptance test must stay green after the upgrade):
  - **Spring Framework 7.** `RestClient` stays the Jev client. Core retry now exists in the framework, but it is not used (ADR-004, alternative 1).
  - **Jackson 3** is the default (`tools.jackson.*` packages, `JsonMapper`). `ReportWriter` uses `com.fasterxml.jackson.databind.ObjectMapper` today and migrates to it. Annotations stay in `com.fasterxml.jackson.annotation`. No Jackson 2 compatibility module is used.
  - **Modularised starters.** `RestClient.Builder` auto-configuration comes from the RestClient starter (`spring-boot-starter-restclient`) instead of bare `spring-web`. Test support moves to per-module test starters where needed.
  - **HTTP client property names changed in Boot 4.** The Jev timeouts therefore move to `detector.jev.connect-timeout` and `detector.jev.read-timeout`, applied by the composition root (ADR-004). They do not rely on Boot's global property names.
  - **JUnit 6 / JUnit Platform** managed by Boot 4.1. Check the Cucumber engine and the `junit-platform-suite` runner against it.

## Alternatives considered
1. **Stay on 3.5.16 without patches.** Rejected: no OSS security or bug fixes since 2026-06-30, which contradicts C1. Every later upgrade gets more expensive as the stories add code.
2. **Stay on 3.5 with commercial extended support** (e.g. VMware Tanzu Spring enterprise support). Rejected: proprietary and paid. The open-source-first principle forbids it without an explicit request, and a showcase has no budget for it.
3. **Upgrade to 4.0.** Rejected: it brings the same migration effort as 4.1 (Framework 7, Jackson 3), but OSS support ends 2026-12-31, about two months away. That would force a second upgrade almost at once.

## Consequences
- Positive:
  - Supported framework until 2027-07-31.
  - Framework 7 core retry and Jackson 3 are available for later work.
  - Migration happens while the code is smallest.
- Negative:
  - DELIVER step 1 costs extra effort: Jackson 3 migration, starter changes, test-stack compatibility.
  - Test-library compatibility (Cucumber, WireMock) is unverified until DELIVER step 1. Fallback: the newest compatible versions, pinned explicitly.
  - DEVOPS must use a Java 21 base image, which is unchanged.
