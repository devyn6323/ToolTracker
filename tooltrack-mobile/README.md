# ToolTrack mobile

Expo SDK 57 and TypeScript mobile client for the ToolTrack Spring Boot API.

## Included workflows

- Login and company registration
- Secure JWT session storage
- Dashboard status totals and issue visibility
- Searchable and filterable tool inventory
- Add and edit tools for management roles
- Tool details, current holder, QR display, and full history
- QR scanning with camera permission handling
- Self-checkout with job, location, due date, and condition
- Tool return with damaged and missing-state warnings
- Employee-to-employee tool transfer with audit history
- Tool photo selection and authenticated local-development upload
- Current user's checked-out tools
- Employee and manager creation
- Company-wide activity feed
- Account, privacy, support, and permanent deletion settings
- EAS preview APK and production Android App Bundle profiles

## Configure the backend URL

Copy `.env.example` to `.env.local` and set `EXPO_PUBLIC_API_URL`.

```text
# Android emulator
EXPO_PUBLIC_API_URL=http://10.0.2.2:8080

# iOS simulator
EXPO_PUBLIC_API_URL=http://localhost:8080

# Physical phone on the same Wi-Fi network
EXPO_PUBLIC_API_URL=http://192.168.1.50:8080
```

Replace the physical-device example with the development computer's LAN address. Restart Expo after changing environment variables.

Production builds intentionally reject missing or non-HTTPS API configuration. Create the `production` EAS environment with `EXPO_PUBLIC_API_URL`, `EXPO_PUBLIC_SUPPORT_EMAIL`, and, if the backend pages are not used, explicit privacy/deletion URLs.

## Android release build

Link the project to an Expo account with `eas init`, configure the production environment variables, and run:

```powershell
npx eas-cli build --platform android --profile production
```

The production profile creates an `.aab` and automatically increments the remote Android build number. Upload the first bundle through the Play Console internal-testing track before enabling automated submission.

## Run

Start the Spring Boot backend first, then:

```powershell
npm install
npm start
```

Scan the terminal QR code with Expo Go or press `a` for an Android emulator.

## Verify

```powershell
npm run typecheck
npm run export:android
```

The app's QR renderer uses the backend-generated `qrCodeValue`. The scanner sends that exact value to `/api/tools/by-qr/{qrCodeValue}` and opens the resolved tool record.
