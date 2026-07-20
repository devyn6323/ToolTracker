# ToolTrack production-readiness gate

Do not promote ToolTrack from testing to production until every required item below is verified.

## Required service configuration

- Render uses the production profile and its health check reports healthy.
- PostgreSQL automated backups are enabled with a documented retention period.
- Restore the latest PostgreSQL backup into a separate test database and verify company, user, tool, and transaction counts.
- Cloudinary is configured through `CLOUDINARY_URL`; its API secret is never placed in EAS or the mobile bundle.
- SendGrid HTTPS delivery is configured and password reset messages reach the inbox and spam folder tests.
- Google OAuth contains the Play app-signing and EAS signing SHA-1 certificates.
- Render alerts notify the monitored support address when the web service is unhealthy or repeatedly restarts.
- Render error logs are reviewed for `incidentId` entries and retained long enough to investigate tester reports.

## Account and authorization tests

- Owner registration and password login.
- New owner creation with Google.
- Manager-created employee must replace the temporary password before seeing inventory.
- Forgot-password codes are delivered, expire, work once, and revoke older sessions.
- Google login links only the exact verified employee email.
- Owner can transfer ownership after password or Google reauthentication.
- Previous owner becomes a manager and cannot delete the company.
- Account deletion works for password and Google-only accounts.
- Company deletion removes database records and cloud photos.

## Tool workflow tests

- Add and edit tools with camera and photo-library images.
- Replacing, removing, or cancelling a photo does not leave an accessible unused upload.
- Print and share a QR code, then scan the printed result on another device.
- Check out one tool and multiple tools to the same job.
- Rapidly tap checkout once and confirm only one open checkout exists.
- Return, transfer, damaged, lost, maintenance, retired, and overdue paths.
- Keyboard, date selectors, Android navigation insets, and denied camera/photo permissions.
- Slow network, airplane mode, Render cold start, and retry behavior do not create duplicate records.

## Google Play gates

- Upload only to internal or closed testing until this checklist passes.
- Review the latest pre-launch report: stability, performance, accessibility, and screenshots.
- Complete the Data safety and account-deletion declarations using the production behavior.
- Confirm the public privacy and deletion URLs load without signing in.
- If required for the developer account, keep at least 12 closed testers opted in continuously for 14 days and retain their feedback.
