# Data Safety draft

Confirm these answers against the deployed production services and every third-party SDK before submitting them in Play Console.

## Data collected

| Play data category | ToolTrack use | Required | Handling |
| --- | --- | --- | --- |
| Name | Account identity and activity attribution | Yes | Encrypted in transit; removed or anonymized on deletion |
| Email address | Login, employee accounts, and support | Yes | Encrypted in transit; removed or anonymized on deletion |
| User IDs | Company-scoped authentication | Yes | Encrypted in transit |
| Photos | User-selected tool inventory photos | No | Encrypted in transit; owner company deletion removes local stored files |
| Other user-generated content | Tool notes, condition notes, job and location labels | No | Encrypted in transit; used only for inventory workflows |
| App interactions | Tool checkout, transfer, return, and history events | Yes | Encrypted in transit; used to provide the service |

## Expected declarations

- Data is collected for app functionality and account management.
- Data is not sold.
- Data is not used for advertising or marketing.
- Data is shared only with infrastructure providers acting as service providers. List the actual production database, hosting, storage, monitoring, and email vendors before submission.
- All production traffic must use HTTPS.
- Users can request deletion in the app and through the deployed `/delete-account` page.
- The app is not directed to children.
- The app does not collect precise or approximate device location; user-entered job/location text is inventory content, not device location data.

## Permissions

- Camera: QR scanning initiated by the user.
- Photo access: selecting a tool photo initiated by a manager. Android may use the system photo picker rather than broad library access depending on OS behavior.
- Microphone and device location are not requested.

## Before submission

- Re-run an Android App Bundle permission report in Play Console.
- Include crash-reporting or analytics collection here if either service is added.
- Ensure the public policy names every data category and reflects the production retention and backup schedule.
