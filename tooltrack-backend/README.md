# ToolTrack backend

Spring Boot API for ToolTrack's first usable inventory and checkout workflow.

## What is implemented

- Company registration with an owner account
- JWT login and stateless authorization
- Company-scoped users, tools, and transaction history
- Owner/admin/manager inventory administration
- Employee and manager account creation
- Tool QR values and lookup after a scan
- Local-development tool photo uploads
- Single- and multi-tool checkout, return, current holder, due date, and checkout history
- Holder-to-holder tool transfer
- Dynamic overdue status and damaged/lost return handling
- H2 integration tests using PostgreSQL compatibility mode
- Flyway database migrations and production schema validation
- Health probes at `/actuator/health`
- In-app/API and public web account deletion
- Public privacy policy at `/privacy`

## Run locally

Requirement: Java 25.

The default `dev` profile uses a persistent local H2 database under `./data`, so no database setup or password is required. Run:

```powershell
.\mvnw.cmd spring-boot:run
```

To use a local PostgreSQL database instead, start with the `prod` profile and provide all required production-style variables:

```text
DATABASE_URL=jdbc:postgresql://localhost:5432/tooltrack
DATABASE_USERNAME=tooltrack
DATABASE_PASSWORD=tooltrack
JWT_SECRET=a-private-random-secret-with-at-least-32-bytes
```

Then run:

```powershell
$env:SPRING_PROFILES_ACTIVE='prod'
.\mvnw.cmd spring-boot:run
```

Flyway creates or migrates the database and Hibernate validates the resulting schema. Existing pre-Flyway development databases are baselined automatically.

## Production

Build the provided `Dockerfile`, attach a managed PostgreSQL database and a persistent volume mounted at `/data`, and set:

```text
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://database-host:5432/tooltrack
DATABASE_USERNAME=...
DATABASE_PASSWORD=...
JWT_SECRET=a-random-secret-of-at-least-32-bytes
UPLOAD_DIR=/data/uploads
SUPPORT_EMAIL=support@your-real-domain.com
```

Terminate TLS at the hosting provider and enable automated database and volume backups. Local files are suitable only when the host provides a durable volume; move photo storage to S3 or Cloudinary before deploying without one.

### Render

The root `render.yaml` can create and link the Docker web service and PostgreSQL database. Render's native `postgresql://` internal connection string is accepted directly and converted to JDBC configuration at startup.

If configuring the service manually, set:

```text
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=<Render Internal Database URL>
JWT_SECRET=<random secret of at least 32 bytes>
SUPPORT_EMAIL=<monitored address>
UPLOAD_DIR=/data/uploads
```

Use the database's internal URL when the web service and database are in the same Render account and region. Set the web service health check path to `/actuator/health`.

## API

All routes except registration and login require `Authorization: Bearer <token>`.

| Method | Route | Access |
| --- | --- | --- |
| POST | `/api/auth/register` | Public |
| POST | `/api/auth/login` | Public |
| DELETE | `/api/auth/account` | Current user; password confirmation required |
| GET | `/privacy` | Public privacy policy |
| GET | `/delete-account` | Public account deletion form |
| GET | `/api/employees` | Any active user |
| POST | `/api/employees` | Owner, admin, manager |
| GET | `/api/tools` | Any active user |
| GET | `/api/tools/{id}` | Any active user |
| GET | `/api/tools/by-qr/{qrCodeValue}` | Any active user |
| POST | `/api/tools` | Owner, admin, manager |
| PUT | `/api/tools/{id}` | Owner, admin, manager |
| POST | `/api/tools/{id}/checkout` | Any active user; checks out to self |
| POST | `/api/tools/checkout/batch` | Any active user; checks selected tools out to self with shared job details |
| POST | `/api/tools/{id}/return` | Current holder or management |
| POST | `/api/tools/{id}/transfer` | Current holder or management |
| GET | `/api/tools/{id}/history` | Any active user |
| GET | `/api/tools/my-tools` | Any active user |
| POST | `/api/uploads/tool-photo` | Owner, admin, manager |
| GET | `/api/dashboard` | Any active user |
| GET | `/api/activity` | Any active user |

The Expo app should render `qrCodeValue` with `react-native-qrcode-svg`. After `expo-camera` scans that value, call `/api/tools/by-qr/{qrCodeValue}` to resolve the tool.

### Example workflow

Register:

```json
{
  "companyName": "Demo Construction",
  "name": "Alex Owner",
  "email": "owner@example.com",
  "password": "ExamplePass1!"
}
```

Add a drill:

```json
{
  "assetNumber": "DRILL-001",
  "name": "Cordless Drill",
  "category": "Power Tools",
  "manufacturer": "DeWalt",
  "model": "DCD800",
  "serialNumber": "SN-1001",
  "condition": "GOOD",
  "currentLocation": "Main Shop"
}
```

Checkout:

```json
{
  "jobName": "Warehouse Remodel",
  "location": "Job 42",
  "expectedReturnAt": "2026-07-20T22:00:00Z",
  "conditionAtCheckout": "GOOD",
  "notes": "Battery and case included"
}
```

Return:

```json
{
  "conditionAtReturn": "GOOD",
  "location": "Main Shop",
  "notes": "Battery returned"
}
```

## Tests

```powershell
.\mvnw.cmd test
```

`ToolWorkflowIntegrationTests` exercises the complete demonstration workflow through the secured HTTP API.
