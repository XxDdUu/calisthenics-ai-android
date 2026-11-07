# Live Analysis VPS Deployment and Validation

**Date:** 2026-11-05  
**Overall result:** VPS services deployed and AI inference verified; Android-to-VPS end-to-end validation is **NOT COMPLETE**.

## Deployment architecture

```text
Android prodDebug / release
  -> https://api.sky-enterprise.id.vn
  -> Cloudflare HTTPS edge
  -> Spring Boot backend (VPS host port 8082 -> container 8080)
  -> Docker network: http://ai-service:8000
  -> FastAPI live frame endpoint -> YOLOv8n-Pose -> biomechanics
```

PostgreSQL and MinIO remain in the existing Compose deployment. No Compose environment values or secrets were changed. The AI container has no published host port (`8000/tcp` has no host binding); the backend resolves the AI service at `http://ai-service:8000`, with a configured 60-second timeout.

The public health request returned HTTP 200 over verified HTTPS/HTTP2. Cloudflare response headers were observed. No Nginx site configuration was present; Apache is active on port 80 with only its default virtual host and no observed proxy rules. Therefore the public TLS edge is verified, but the origin-side proxy/TLS configuration was not verified. The backend is bound to `0.0.0.0:8082`; this exposure was not changed because its existing network/firewall policy is unknown.

## Deployment and service status

| Component | Result | Evidence |
|---|---|---|
| FastAPI AI service | **PASS** | Deployed image `sha256:8a3d2e8016b6655aac853076f24f4d9f55d06465e8fcefc91f4dd6503d63ebaf`; container healthy |
| Spring Boot backend | **PASS** | Deployed image `sha256:3019664d887e73afe13606bf727de9013e11a74922272fb8c00fb9eeae28682e`; container healthy; live controller/service/executor classes present |
| PostgreSQL | **PASS** | Container healthy; backend actuator reports database UP |
| MinIO | **PASS** | Container healthy |
| Docker internal AI connectivity | **PASS** | Backend container fetched `http://ai-service:8000/health` successfully |
| FastAPI public port | **PASS** | No host port binding on AI container |
| Public backend HTTPS health | **PASS** | HTTP 200, TLS verification result 0 |
| Live API without JWT | **PASS (auth gate)** | Public live catalog request returns expected HTTP 401 |
| Authenticated Android request through backend | **NOT TESTED** | No device/session credentials available in this environment |

The backend image was built from the committed backend source plus only the three live-gateway classes/configuration. Other dirty backend changes, including unrelated authentication work, were excluded. The AI image was built from the current AI-service working tree after its live tests passed.

## AI inference validation

The deployed private FastAPI endpoint accepted a multipart JPEG at `/api/v1/analysis/live/frame` and returned HTTP 200. This was an actual YOLO inference, not a mocked response. The input was the real `bus.jpg` image bundled with Ultralytics, **not an Android camera frame or a calisthenics recording**.

Observed response:

```json
{
  "status": "VALID",
  "exercise": "FULL_PLANCHE",
  "score": 55.0,
  "confidence": 0.891,
  "poseDetected": true,
  "isValidHold": false,
  "measurements": {
    "elbow_angle": 98.1,
    "shoulder_lean_angle": 41.4,
    "hip_angle": 172.5,
    "knee_angle": 174.4,
    "body_line_angle": 86.3,
    "leg_spread_ratio": 2.0
  },
  "feedback": [
    {"code": "EXTEND_ELBOWS", "severity": "ERROR"},
    {"code": "ALIGN_BODY", "severity": "ERROR"}
  ],
  "landmark_count": 17,
  "timing": {
    "preprocessing_ms": 3.94,
    "inference_ms": 36.38,
    "analysis_ms": 0.30,
    "total_ms": 40.63
  }
}
```

These are **one warm VPS service request**, not a throughput benchmark. The measured AI-container loopback HTTP time was 50.806 ms. No Android/WAN round-trip latency, average FPS, sustained load, or failure rate was measured.

## Performance and hardware snapshot

Measured after deployment with `docker stats --no-stream`:

| Container | CPU | Memory |
|---|---:|---:|
| Backend | 2.47% | 673.3 MiB |
| AI service | 3.66% | 351.8 MiB |
| PostgreSQL | 0.00% | 40.52 MiB |

The VPS reported 12 CPUs and 62 GiB RAM (55 GiB available at the snapshot). A Tesla M40 24 GB GPU is present, but the AI container reports `DEVICE=cpu`; **GPU acceleration is not enabled or claimed**. Network bandwidth was not measured.

## Android integration

- `prodDebug` and `prodRelease` both generated `API_BASE_URL = https://api.sky-enterprise.id.vn/`.
- Gradle now rejects non-HTTPS production URLs and production URLs that do not use the configured VPS host.
- Production network security configuration disallows cleartext; emulator loopback allowances are isolated to the dev flavor.
- Multipart text fields now use Retrofit `RequestBody` parts.
- Live response DTOs now parse the AI service's actual snake_case landmark coordinates and timing fields.
- `./gradlew :app:compileProdDebugKotlin :app:compileProdReleaseKotlin :app:processDevDebugResources --no-daemon` — **PASS**
- `./gradlew :app:testProdDebugUnitTest --tests com.calisthenics.ai.data.remote.dto.LiveAnalysisDtosTest --no-daemon` — **PASS**
- `./gradlew :app:assembleProdDebug --no-daemon` — **PASS**
- A Gradle configuration check with `API_BASE_URL=http://localhost:8080/` failed with the expected HTTPS validation message.

`adb devices -l` returned no attached devices. The production APK was built but not installed or exercised.

## PASS / FAIL / NOT TESTED checklist

| Check | Result | Notes |
|---|---|---|
| AI service deployed in Docker and healthy | **PASS** | Restart policy and health check retained |
| YOLO model available and loads for inference | **PASS** | `yolov8n-pose.pt`, 6,832,633 bytes |
| Real multipart inference returns landmarks/metrics | **PASS** | 17 landmarks; real bundled image; one request |
| Backend healthy and connected to PostgreSQL | **PASS** | Actuator reports UP |
| Backend-to-AI Docker networking | **PASS** | Internal AI health check succeeds |
| FastAPI port not published publicly | **PASS** | No host binding |
| Public HTTPS API health | **PASS** | HTTPS verified at the public edge |
| Android production API points to VPS | **PASS** | prodDebug/release BuildConfig verified |
| Production-debug APK builds | **PASS** | Assemble completed |
| Android authentication and camera permission flow | **NOT TESTED** | Requires app launch and user interaction |
| Android camera frames reach authenticated backend | **NOT TESTED** | No physical device or authenticated session |
| Backend forwards an authenticated frame to AI | **NOT TESTED** | AI was tested directly inside its container |
| Skeleton alignment on a real preview | **NOT TESTED** | Requires physical camera/device validation |
| Start/pause/resume/stop/reset and background lifecycle | **NOT TESTED** | Requires Android runtime |
| Network-loss, timeout, reconnect, camera-switch cases | **NOT TESTED** | Requires Android runtime |
| Sustained FPS / latency / request-failure benchmark | **NOT TESTED** | Only one warm AI request was timed |
| Origin reverse-proxy/TLS policy | **NOT TESTED** | Public Cloudflare HTTPS verified; origin proxy rules not established |

## Known issues and remaining work

1. Full Android-to-VPS-to-AI validation remains outstanding. The 401 response is the expected authentication gate, not evidence of an authenticated frame request.
2. The Android live-analysis entry currently selects the first parent exercise, while the AI-supported catalog is not wired into that selection flow. Exercise-specific camera setup is not yet verified.
3. Camera frame conversion, rotation/mirroring, and overlay scaling have not been verified on hardware. The current analyzer reports `side_view=true` and the overlay uses normalized coordinates against the full Compose canvas.
4. Camera analyzer executor shutdown, in-flight request bounds/cancellation, and lifecycle behavior need device-level validation before production readiness can be claimed.
5. Backend host port 8082 is bound on all interfaces. Confirm origin firewall/Cloudflare restrictions and whether the intended architecture should instead use a loopback-only origin proxy before changing it.
6. Exercise session persistence and a saved live-session summary were not verified.

**Conclusion:** the VPS AI and backend services are deployed and healthy, and real VPS inference is confirmed. The feature is **not production-validated end-to-end** until an authenticated Android device sends camera frames through the public backend and lifecycle/performance tests pass.
