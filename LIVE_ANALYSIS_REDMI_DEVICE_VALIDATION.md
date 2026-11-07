# Redmi Physical Device Validation

**Date:** 2026-11-05
**Target serial:** `JZZTAYWGSGWSE6A6`
**Result:** **PARTIAL PHYSICAL VALIDATION — STOPPED AFTER AUTH/POSE FAILURE REPORT**

## Device and APK audit

| Item | Result | Evidence |
|---|---|---|
| ADB target | **PASS** | Explicit serial reported `device`; no other connected devices were listed |
| Model | **PASS** | `M2101K7BG` (Xiaomi) |
| Android | **PASS** | Android 13 / SDK 33 |
| Display | **PASS** | 1080 x 2400, density 440 dpi |
| APK identity | **PASS** | `com.calisthenics.ai`, version code 1, version name 1.0 |
| Production API | **PASS** | APK contains `https://api.sky-enterprise.id.vn/`; prod BuildConfig matches |
| Production cleartext policy | **PASS** | Main production network security config denies cleartext; dev-only loopback config is flavor-scoped |
| Existing Calisthenics package | **PASS** | Package was not installed before the test; no user app data was overwritten |
| APK signature compatibility | **PASS** | Fresh install succeeded; there was no existing app signature to compare |

The APK audited was `app/build/outputs/apk/prod/debug/app-prod-debug.apk` (23,485,357 bytes).

## Installation and launch

The initial standard install returned `INSTALL_FAILED_USER_RESTRICTED`. After the user approved the install prompt on the Redmi, a single retry of:

```text
adb -s JZZTAYWGSGWSE6A6 install -r app/build/outputs/apk/prod/debug/app-prod-debug.apk
```

returned `Success`. The package was absent before installation. Package metadata confirmed version code 1/version 1.0, and the app launched successfully. No uninstall, app-data clear, broad permission grant, or device security-setting change was performed.

The user signed in through the app UI and confirmed successful authentication. Filtered app logs showed `POST /api/v1/auth/login` and subsequent authenticated account endpoints returning HTTP 200. Authorization headers and token response data were not retained or reported.

## Live camera and VPS evidence

- The user navigated to Live Analysis, approved camera permission through the normal UI, and pressed Start.
- Camera permission was granted. CameraX Preview and ImageAnalysis use cases became active, and Camera2 opened camera 0 for `com.calisthenics.ai`.
- The on-screen live view rendered and displayed “Tracking body pose...” with a score of `--` before Start. This is a UI state, not proof of detected landmarks.
- The user then reported repeated navigation back to Authentication after starting and requested that the issue be noted. I stopped live testing: no relogin, additional start, or further frame submission was attempted.
- VPS FastAPI logs contained **18** `POST /api/v1/analysis/live/frame` requests with HTTP **200**. This is consistent with the user's Start action and proves the real AI endpoint was reached during the device session.
- All 18 corresponding inference logs reported no bounding boxes and `keypoint_output_shape=(0, 17, 3)`. Therefore, these frames produced **zero detected people/landmarks**; no posture metrics or skeleton coordinates were available to validate.
- The request image shape in the AI logs was 720x720. No uploaded frame or video was exported or saved by this validation.
- The captured screen after the user's report showed the app's Messages tab, not Authentication. The available Android HTTP log sample showed 16 response entries, all HTTP 200, and no captured 401/403. This does **not** disprove the reported transient auth redirect: the exact transition was not captured in app logs, and I did not repeat the test to reproduce it.
- Camera2 logs later showed a disconnect for this app. No additional camera session was started.

## Performance snapshot

These are one-time process/device observations, not a benchmark:

| Metric | Observation | Limitation |
|---|---:|---|
| App memory | 224,486 KiB PSS; 347,646 KiB RSS | Single snapshot while Live Analysis was open |
| App CPU | 30.3% in one `top` sample | Transient sample, not average load |
| VPS live endpoint responses | 18/18 HTTP 200 | Server-side count during the session |
| Successful person detections | 0/18 | No pose landmarks were produced |
| Camera capture FPS | **NOT MEASURED** | UI displayed a fixed “5”; it is not an actual measured FPS |
| Processed FPS / dropped frames | **NOT MEASURED** | No frame counters/timing trace available |
| Preprocessing / WAN / end-to-end latency | **NOT MEASURED** | No per-frame device timestamps were captured |
| AI inference latency for these 18 requests | **NOT MEASURED** | These log entries did not expose per-request timing values |

## Validation checklist

| Check | Result | Evidence type / notes |
|---|---|---|
| Redmi identity and display audit | **PASS** | Actual device read-only ADB properties |
| APK package/version/production endpoint | **PASS** | APK manifest, DEX string, and generated production BuildConfig |
| APK installation and launch | **PASS** | Successful targeted `install -r`; package metadata verified |
| User authentication | **PASS (initial login)** | User confirmed; login and account endpoint responses showed 200 |
| Camera permission / CameraX initialization | **PASS** | User granted permission; Preview/ImageAnalysis active in Camera2 logs |
| Camera frame capture and preprocessing | **PARTIAL** | VPS received 18 processed 720x720 frame requests; no person detected |
| Authenticated Android-to-VPS request | **PARTIAL** | AI endpoint received 18 forwarded requests; no device-side request/response trace for the frame calls |
| Actual VPS YOLO inference during device session | **PASS (inference executed)** | 18 real route requests; each completed HTTP 200 |
| Person/pose/17-landmark detection | **FAIL** | All 18 inference logs showed no boxes and zero keypoints |
| Metrics parsing / skeleton overlay alignment | **NOT TESTED** | No landmarks or metrics were returned to render/align |
| Reported authentication redirect | **REPORTED; NOT REPRODUCED** | User reported repeated redirects; captured state/logs showed Messages and successful API responses, not a captured 401 |
| Start/pause/resume/stop/reset and lifecycle | **PARTIAL** | Start reached inference; camera disconnect was observed after leaving the live session; other controls not tested |
| Captured/processed FPS, dropped frames, end-to-end latency | **NOT TESTED** | Fixed UI “5” is not a measurement; no frame-level device timing |
| App CPU/RAM during live analysis | **PARTIAL** | One-time snapshot only; not a benchmark |
| Network failure, timeout, backend unavailable, recovery | **NOT TESTED** | Stopped after the reported auth issue; no system network changes |

## Conclusion

Physical device validation is **partial and not production sign-off**. Installation, user-confirmed initial login, CameraX startup, and real VPS inference requests were observed. However, the camera frames did not contain a detected person, no landmarks/metrics/skeleton were validated, the user's reported repeated auth navigation was not captured in the available logs, and performance/lifecycle benchmarks were not completed. Testing was stopped without relogin or another camera run. Existing phone data and settings were preserved.
