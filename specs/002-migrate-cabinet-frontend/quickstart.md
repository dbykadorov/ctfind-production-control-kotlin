# Quickstart: Migrate Cabinet Frontend

This quickstart describes the target validation workflow for the migrated cabinet frontend.

## 1. Prerequisites

Install and start a Docker Compose-compatible container runtime.

Verify it is available:

```bash
docker --version
docker compose version
```

Make sure local ports are available:

```text
8080  - backend API
15432 - local PostgreSQL diagnostics
5173  - frontend cabinet
```

## 2. Start Local Platform

From the repository root:

```bash
docker compose up --build --wait
```

Expected services:

```bash
docker compose ps
```

Expected result:

- `app` is healthy;
- `postgres` is healthy;
- `frontend` is healthy or running with successful login page response.

## 3. Check Backend Health

```bash
curl http://localhost:8080/actuator/health
```

Expected response includes:

```json
{"groups":["liveness","readiness"],"status":"UP"}
```

## 4. Open Frontend Login

Open in browser:

```text
http://localhost:5173/cabinet/login
```

Expected result:

- migrated cabinet login screen is visible;
- username and password fields are editable;
- required login logos/background/assets are visible;
- no browser Basic Auth popup appears;
- no old Frappe service is required.

## 5. Check Unauthenticated Redirect

Open in browser:

```text
http://localhost:5173/cabinet/orders
```

Expected result:

- app redirects/renders login screen;
- user does not see orders content.

## 6. Check Login Placeholder

Enter any non-empty username/password and submit.

Expected result:

- user remains on login screen;
- UI shows that authorization is not connected yet;
- no successful session is created.

## 7. Logs

Frontend logs:

```bash
docker compose logs -f frontend
```

Backend logs:

```bash
docker compose logs -f app
```

Database logs:

```bash
docker compose logs -f postgres
```

## 8. Stop

```bash
docker compose down
```

## 9. Troubleshooting

### Frontend Port Is Busy

```bash
ss -ltnp | rg ':5173'
```

Stop the conflicting process or adjust the port in a future planned change.

### Login Screen Is Blank

Check frontend logs and browser console. Common causes:

- missing migrated asset;
- Vite base path still points to old Frappe asset path;
- a Frappe-specific boot/API call runs before login render.

### Browser Shows Backend 401 At Root

This is expected for `http://localhost:8080/`; backend is API-only. Use the frontend URL:

```text
http://localhost:5173/cabinet/login
```
