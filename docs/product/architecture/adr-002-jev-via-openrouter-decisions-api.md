# ADR-002: Jev via the OpenRouter Decisions API with the image in `state`

## Status
Accepted in the SPIKE (promotion 2026-10-29, variant A). This ADR records it. 2026-10-30.

## Context
- The brief asks for Jev / TypeSafe classification.
- No Jev Java SDK is documented (X3). OpenRouter accepts images for Jev as an alpha feature (E8a).
- SPIKE results:
  - Variant A (Jev on the image) was the only variant with 0 false passes: 40 defects, t ≥ 0.86, $0.00015 and 1.2 s per image.
  - **Caveat:** 0.86 was fitted in-sample on the same 66 images with a 0.01 margin (highest defect P(GOOD) = 0.85). The variant choice rests on the 0 vs 3–6 comparison. The threshold itself is **not validated**; US-01/US-02 (`--evaluate`) carry that validation.
  - Variants B and C used **Qwen** (`qwen3.6-35b-a3b`) as the vision step and let 3 to 6 defects through.
  - D12 removes Qwen entirely.
- C11 says no file name, folder name or label may reach the model. INV-3 says the key never appears in output.

## Decision
- Call `POST {detector.jev.base-url}/api/alpha/decisions` with Spring `RestClient` (spring-web; Boot 4.1 after ADR-006) and a Bearer key from `OPENROUTER_API_KEY`.
- The request has `model: typesafe/jev-1.13`, `state: [scene text, {type: image_url, image_url: {url: data:<media>;base64,...}}]`, and one `choice` question `defect` with five criteria (`DefectQuestion`, validated in the SPIKE).
- The `Jev` domain port accepts only image bytes and a media type. The adapter never receives a path or a label.

## Alternatives considered
1. **Vision LLM description, then Jev on text** (variant B). Rejected:
   - Measured only with Qwen: 3 false passes, and those descriptions were fully nominal for real defects.
   - D12 removes Qwen.
   - The same pipeline with a cloud vision model (e.g. `claude-haiku-5-5`) was **never measured**. Reopening it would need a new SPIKE, and a stronger vision model is out of scope (DISCUSS).
2. **Python/JS Jev SDK as a sidecar.** Rejected: it adds a second runtime and an inter-process call, which does not fit a single Docker jar. The SDK gives no functional gain over REST.
3. **Hand-built `java.net.http.HttpClient`.** Rejected: `RestClient` is already wired, takes its timeouts from the Jev settings (ADR-004), and maps JSON with Jackson.

## Consequences
- Positive:
  - Proven request shape, cheap, fast.
  - C11 is enforced by the port signature, by WireMock request assertions, and by the ArchUnit rule `jev..` ↛ `evaluate..`.
- Negative:
  - The alpha API format may change, and this is a risk to every slice. Mitigation:
    - The response mapping lives in one adapter.
    - Consumer-contract tests are recorded from the WireMock stubs (Spring Cloud Contract or Pact JVM; the DEVOPS handoff annotation).
    - A manual real-Jev smoke runs per slice.
  - The Jev class label is unreliable (20/40 exact), so descriptions say "Suspected".
