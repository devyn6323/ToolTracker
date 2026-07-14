# ToolTrack Google Play release checklist

## Code and hosting

- [ ] Deploy `tooltrack-backend` with the `prod` profile behind HTTPS.
- [ ] Provision managed PostgreSQL with automated backups.
- [ ] Attach a backed-up persistent volume at `/data`, or replace local photos with object storage.
- [ ] Set `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_SECRET`, `UPLOAD_DIR`, and a monitored `SUPPORT_EMAIL`.
- [ ] Confirm `/actuator/health`, `/privacy`, and `/delete-account` are publicly reachable.
- [ ] Configure the EAS `production` environment with the deployed HTTPS `EXPO_PUBLIC_API_URL` and support email.
- [ ] Run backend tests, `npm run typecheck`, and `npm run export:android`.
- [ ] Generate a signed production `.aab` and enroll it in Play App Signing.

## Real-device acceptance

- [ ] Register a company, add an employee and tool, scan, checkout, transfer, return, and inspect history.
- [ ] Verify camera denial/retry, invalid QR codes, offline API errors, and interrupted requests.
- [ ] Verify two companies cannot access each other's tools, QR records, users, uploads, or activity.
- [ ] Verify concurrent checkout attempts permit only one holder.
- [ ] Verify employee deletion anonymizes history and owner deletion removes the full company.
- [ ] Restore a database/volume backup in a non-production environment.

## Play Console

- [ ] Replace Expo placeholder icons and prepare the 512×512 icon, feature graphic, and phone screenshots.
- [ ] Add descriptions, category, support contact, and the deployed `/privacy` URL.
- [ ] Complete Data Safety, app access, content rating, target audience, ads, and account deletion declarations.
- [ ] Provide permanent reviewer credentials for a representative non-empty company account.
- [ ] Confirm the uploaded bundle's target API level satisfies the current Play requirement.
- [ ] Complete the required closed-test period if the developer account is subject to it.
- [ ] Launch free, or implement Google Play Billing and plan enforcement before selling digital subscriptions in-app.
