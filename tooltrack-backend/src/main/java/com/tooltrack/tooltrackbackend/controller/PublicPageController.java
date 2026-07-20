package com.tooltrack.tooltrackbackend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicPageController {
    private final String supportEmail;

    public PublicPageController(@Value("${app.support.email}") String supportEmail) {
        this.supportEmail = HtmlUtils.htmlEscape(supportEmail);
    }

    @GetMapping(value = "/privacy", produces = MediaType.TEXT_HTML_VALUE)
    public String privacy() {
        return page("ToolTrack Privacy Policy", """
                <h1>ToolTrack Privacy Policy</h1>
                <p><strong>Effective July 13, 2026</strong></p>
                <p>ToolTrack provides tool inventory, QR scanning, employee checkout, and activity history for participating businesses.</p>
                <h2>Information we collect</h2>
                <p>We collect account and company information such as names, email addresses, roles, and encrypted password hashes. If you choose Google sign-in, we also receive your verified Google account name, email address, and stable account identifier for authentication. We also store tool records, selected tool photos, job or location labels, condition notes, and checkout activity entered by users.</p>
                <p>The Android camera is used only when a user chooses to scan a tool QR code. Tool photos are accessed only when a user selects or captures one for an inventory record. ToolTrack does not collect GPS location, contacts, advertising identifiers, or payment-card information.</p>
                <h2>How information is used and shared</h2>
                <p>Information is used to authenticate users, operate company inventories, show tool custody and history, provide support, prevent abuse, and maintain service reliability. Google processes the optional Google authentication flow, and Cloudinary stores user-requested tool photos. We do not sell personal information or use it for behavioral advertising. Data may be processed by hosting, database, file-storage, monitoring, and email providers solely to operate the service, or disclosed when legally required.</p>
                <h2>Retention and deletion</h2>
                <p>Company data is retained while the company account is active. An owner who deletes their account deletes the company, users, tools, and activity history. When another user deletes an account, identifying account details are removed while tool history retains an anonymous “Deleted user” label so the business audit trail remains accurate. Backup copies expire under the service backup schedule.</p>
                <h2>Security and choices</h2>
                <p>ToolTrack uses access controls, encrypted network connections in production, password hashing, and company-level data separation. No system is completely secure. Users can request deletion in the mobile app or at the account-deletion page linked below.</p>
                <p>ToolTrack is intended for business users and is not directed to children under 13.</p>
                <h2>Contact</h2>
                <p>Privacy and support questions: <a href="mailto:%1$s">%1$s</a></p>
                <p><a href="/delete-account">Request account deletion</a></p>
                """.formatted(supportEmail));
    }

    @GetMapping(value = "/delete-account", produces = MediaType.TEXT_HTML_VALUE)
    public String deleteAccount() {
        return page("Delete a ToolTrack account", """
                <h1>Delete a ToolTrack account</h1>
                <p>Enter the same email and password used in ToolTrack. Deletion is permanent. If you are the company owner, the company’s tools, users, photos, and activity history will also be deleted. Accounts created only with Google can delete directly in the app after re-confirming Google, or contact support below from the registered email address.</p>
                <form id="delete-form">
                  <label>Email<input id="email" type="email" autocomplete="username" required></label>
                  <label>Password<input id="password" type="password" autocomplete="current-password" required></label>
                  <button type="submit">Delete permanently</button>
                </form>
                <p id="result" role="status"></p>
                <p>If you cannot sign in, contact <a href="mailto:%1$s">%1$s</a>.</p>
                <script>
                document.getElementById('delete-form').addEventListener('submit', async (event) => {
                  event.preventDefault();
                  const result = document.getElementById('result');
                  result.textContent = 'Verifying account…';
                  try {
                    const credentials = { email: document.getElementById('email').value, password: document.getElementById('password').value };
                    const login = await fetch('/api/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(credentials) });
                    if (!login.ok) throw new Error('Email or password was incorrect.');
                    const session = await login.json();
                    const deletion = await fetch('/api/auth/account', { method: 'DELETE', headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + session.token }, body: JSON.stringify({ password: credentials.password }) });
                    if (!deletion.ok) throw new Error('The account could not be deleted. Please contact support.');
                    event.target.remove();
                    result.textContent = 'Your deletion request was completed.';
                  } catch (error) { result.textContent = error.message; }
                });
                </script>
                """.formatted(supportEmail));
    }

    private String page(String title, String content) {
        return """
                <!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
                <title>%s</title><style>body{font:16px/1.55 system-ui,sans-serif;color:#14212b;background:#f4f6f4;margin:0}main{max-width:720px;margin:auto;padding:32px 20px 64px}h1,h2{color:#17384f}h1{line-height:1.15}a{color:#23648a}form{display:grid;gap:16px;background:white;padding:20px;border:1px solid #dde3e2;border-radius:14px}label{display:grid;gap:6px;font-weight:700}input{font:inherit;padding:12px;border:1px solid #aab4b8;border-radius:8px}button{font:inherit;font-weight:800;color:white;background:#c8463a;border:0;border-radius:8px;padding:13px;cursor:pointer}#result{font-weight:700}</style></head>
                <body><main>%s</main></body></html>
                """.formatted(HtmlUtils.htmlEscape(title), content);
    }
}
