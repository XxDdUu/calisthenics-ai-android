# Workout-plan and Today's Workout data contract

The Android app uses authenticated `api/v1/workout-plans` endpoints for workout plan schedules, exercise assignments, and today's source resolution. The bearer token added by the existing auth interceptor identifies the user; the client does not send a user ID.

Available endpoints used by the app:

- `GET /api/v1/workout-plans` — list the authenticated user's plans and weekly day assignments.
- `GET /api/v1/workout-plans/today?date=YYYY-MM-DD&activePlanId={id}` — resolve today's primary source and candidates. An active Coach assignment with a scheduled day takes priority; otherwise the backend returns the Client's personal plans scheduled for that weekday. `PENDING` relationships do not count as active Coaches. The app-provided active plan ID only disambiguates candidates from the already-resolved source; it cannot override Coach priority. Multiple valid candidates require an explicit plan choice.
- `GET /api/v1/workout-plans/{planId}` — retrieve one plan and its assigned days.
- `GET /api/v1/workout-plans/{planId}/days/{dayId}/exercises` — retrieve the day's ordered exercise assignments.
- Existing create, update, delete, template, and exercise-assignment routes support plan management.

Weekday values in plan responses are names such as `MONDAY` through `SUNDAY`; the app maps them to Java `DayOfWeek`/ISO ordering, where Monday is 1 and Sunday is 7. `sessionType: REST` represents an intentional rest day. A missing day assignment is not a rest day.

The selected active plan remains a device preference scoped to the authenticated account. Today's source priority and applicable candidates are resolved by the backend, not inferred from a merged plan list in Compose. A Coach rest-day assignment is represented as an explicit rest day; when no applicable Coach assignment exists, the resolver checks personal plans before returning an empty state.

## Workout session and history endpoints

The Android client uses the authenticated principal for every operation; user IDs are never sent in request bodies:

- `POST /api/v1/workout-sessions` — start or resume a scheduled workout. Send `planId`, `dayId`, ISO `workoutDate`, and `assignmentId` for a Coach-assigned plan (null for a personal plan). The backend revalidates the resolved source before creating a session and returns an existing unfinished session for that date instead of duplicating it.
- `GET /api/v1/workout-sessions/active` — list all unfinished sessions, including earlier dates, so the Client chooses which one to resume.
- `GET /api/v1/workout-sessions/{sessionId}` — fetch current state or completed workout detail.
- `PUT /api/v1/workout-sessions/{sessionId}/exercises/{exerciseId}/sets/{setId}` — idempotently update one set's reps, hold seconds, external/assistance weight, notes, and completion state.
- `PUT /api/v1/workout-sessions/{sessionId}/exercises/{exerciseId}` — explicitly skip or unskip an exercise. Completion is derived from all prescribed sets being complete.
- `POST /api/v1/workout-sessions/{sessionId}/finish` — finish a full workout or confirm a partial workout with `{"confirmPartial": true}`.
- `GET /api/v1/workout-sessions?page=0&size=20` — paginated completed-session history.

The session response persists `source` (`COACH` or `PERSONAL`) and the applicable assignment reference. Session exercises and prescribed set rows are snapshotted from the scheduled plan at start, preserving their order, targets, and progress if the plan, assignment, or Coach connection changes later. Session updates operate on this snapshot and do not re-resolve today's plan. A partial finish marks remaining exercises `INCOMPLETE`; skipped exercises stay distinct from completed work. Completed sessions are immutable through active-session operations.

Edits are persisted to a separate, account-scoped device preference queue before the debounced backend request. Updates carry the entity version; a stale edit receives HTTP 409 and remains queued without automatic stale retries. The current server value is fetched and shown alongside explicit conflict feedback. The Client can explicitly reapply the local value against the latest version. Other failed requests remain queued and are retried when connectivity returns or the user requests synchronization. These queued edits are the limited offline-edit support; starting a new session still requires the backend.
