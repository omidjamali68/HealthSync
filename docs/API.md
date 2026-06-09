# Sample REST API contract for HealthSync

The Android app POSTs JSON to `{baseUrl}{ingestPath}` over HTTPS.

## Request

```
POST https://api.example.com/v1/health/ingest
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "deviceId": "8f3bf6e0-4b1a-4d6d-9c91-2a1c7f0d6f4b",
  "syncedAt": "2026-06-07T10:24:00Z",
  "window": {
    "from": "2026-06-07T09:00:00Z",
    "to":   "2026-06-07T10:24:00Z"
  },
  "steps": [
    { "date": "2026-06-07", "count": 4321 }
  ],
  "heartRate": [
    { "timestamp": "2026-06-07T10:11:23Z", "bpm": 72 },
    { "timestamp": "2026-06-07T10:12:23Z", "bpm": 74 }
  ]
}
```

## Response

`200 OK`
```json
{ "accepted": true }
```

Any non-2xx (4xx auth/validation, 5xx server) keeps the batch in the encrypted Room
queue and triggers WorkManager exponential backoff (starting at 30s).

## Device registration (optional)

If your backend wants to pre-register devices, expose `POST /v1/devices` returning a
token; paste that token into the Settings screen. The app does not need a separate
login endpoint — it relies on bearer-token authentication on every ingest call.

## Minimal server stub (Node/Express)

```js
import express from "express";
const app = express();
app.use(express.json({ limit: "1mb" }));

app.post("/v1/health/ingest", (req, res) => {
  const auth = req.header("authorization") || "";
  if (!auth.startsWith("Bearer ")) return res.sendStatus(401);
  console.log("batch", req.body.deviceId, {
    steps: req.body.steps?.length,
    hr: req.body.heartRate?.length,
  });
  res.json({ accepted: true });
});

app.listen(3000);
```
