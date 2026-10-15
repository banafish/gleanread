# browser-scripts

A collection of browser userscripts, including web text selection and capture scripts for mainstream browsers (X-Browser, Microsoft Edge, Mozilla Firefox, Lemur Browser, Kiwi Browser, etc.).

[简体中文](./README.md) | [English](./README_EN.md)

---

## 📌 glean-web-capture.user.js — Web Selection Capture

When you select text on any webpage (via long-press on mobile or mouse selection on desktop), a floating circular button labeled **"摘" (Extract)** will automatically appear near the selection. Tapping this button instantly launches the GleanRead Fast Capture dialog (`FastCaptureActivity`) on Android via Deep Link.

### 🌟 Key Advantages

- **Atomic Data Delivery**: The current page URL (`location.href`), page title (`document.title`), and selected text are packaged directly into the launch Intent / Deep Link, eliminating any reliance on accessibility services or background web scraping.
- **Distraction-Free Dedicated Task**: The capture dialog operates in an independent temporary task (`taskAffinity=""` + `excludeFromRecents`). Closing the dialog returns you immediately to your reading page without pulling the background GleanRead main interface to the foreground.
- **Immune to Background Process Freezing**: Bypasses aggressive background process freezing policies on various Android ROMs (such as Honor, Huawei, Xiaomi, etc.) by relying purely on system-level Deep Link protocol invocation.

---

## 🛠️ Installation & Usage Guide by Browser

### 1. X-Browser (Android)

X-Browser features a built-in lightweight userscript manager without needing extra extensions:

1. Open X-Browser → Tap the bottom menu → **Toolbox → Scripts** (named "User Scripts" or "Extensions" in some versions).
2. Tap **Add Script / New Script**, paste the full code of [`glean-web-capture.user.js`](./glean-web-capture.user.js), or import the file directly.
3. Save and make sure the script is enabled. Verify that the match pattern covers all websites (`@match *://*/*`).
4. Open any news or article page, long-press to select text → The button appears below the selection → Tap "摘" to launch the GleanRead capture dialog.

---

### 2. Microsoft Edge

#### Edge for Android
1. Ensure your Edge for Android version supports extensions (e.g., Edge Canary/Dev or stable releases with extension flags enabled).
2. Open the menu → **Extensions**, search and install **Tampermonkey** or **Violentmonkey**.
3. Open the Tampermonkey dashboard, click "+" to create a new script, paste `glean-web-capture.user.js`, and save.
4. Select text on any web page and tap the floating "摘" button. Edge natively supports the Chromium standard intent URI (`useIntentUri: true`) for seamless app triggering.

#### Edge for Desktop (Windows / macOS)
1. Install **Tampermonkey** or **Violentmonkey** from the [Microsoft Edge Add-ons Store](https://microsoftedge.microsoft.com/addons).
2. Create or import `glean-web-capture.user.js` and ensure it is enabled.
3. Select text with your mouse on any page, then click "摘".
4. The browser will invoke the custom protocol `gleanread://capture`. When paired with Windows Subsystem for Android (WSA) or cross-device integrations, it directly triggers the capture window.

---

### 3. Mozilla Firefox

#### Firefox for Android
Firefox for Android provides first-class support for extensions:

1. Open Firefox for Android → Tap the three-dot menu → **Add-ons**.
2. Find and install **Tampermonkey** or **Violentmonkey**.
3. Open extension settings, create a new script, paste `glean-web-capture.user.js`, and save/enable it.
4. **Deep Link Protocol Tuning (Important)**:
   - Firefox's Gecko engine handles `intent://` syntax slightly differently than Chromium. If clicking the "摘" button shows "Cannot open this address" or does nothing, edit the top configuration in the script:
     ```javascript
     var CONFIG = {
         useIntentUri: false, // Set to false to use direct gleanread:// scheme
         ...
     };
     ```
   - After setting it to `false`, Firefox will prompt to "Open in external app" or allow the system to route directly to GleanRead.

#### Firefox for Desktop
1. Install **Tampermonkey** from the [Firefox Add-ons Store](https://addons.mozilla.org).
2. Import and save the script. Select text and click "摘", and Firefox will prompt for permission to open the `gleanread://` protocol link.

---

### 4. Lemur Browser / Kiwi Browser (Android)

Both browsers offer full Chrome Web Store extension support on Android:

1. Open the browser's extension store or visit the Chrome Web Store, then install **Tampermonkey**.
2. Open the Tampermonkey dashboard, create a new script, and paste `glean-web-capture.user.js`.
3. Open any webpage and select text to trigger the "摘" button and quick capture flow.

---

## ⚙️ Deep Link Routing: Intent URI vs Scheme

The `CONFIG.useIntentUri` variable at the top of the script controls the URL scheme dispatched to Android:

| Format | Config Value | Description & Use Case |
|---|---|---|
| **Intent URI (Default)** | `useIntentUri: true` | Chromium standard syntax: `intent://capture?url=...#Intent;scheme=gleanread;package=com.gleanread.android;end`.<br>Includes explicit `package=` declaration to guard against intent spoofing. Preferred for Chromium-based browsers (Chrome, Edge, Kiwi, X-Browser). |
| **Direct Scheme** | `useIntentUri: false` | Standard custom scheme: `location.href = "gleanread://capture?..."`.<br>Fallback for browsers that fail to parse `intent:` syntax, report invalid URLs, or for **Firefox** on Android. |

> **Recommendation**: Keep `useIntentUri: true` by default. If tapping the button produces no response or an invalid address error, change it to `useIntentUri: false` and refresh the page.

---

## 🔔 External App Confirmation Prompt

- **Security Mechanism**: Modern Android browsers generally display a prompt when a script attempts to launch an external app (e.g., "Allow opening external app: GleanRead?").
- **Expected Action**: Tap "Allow". Some browsers allow checking "Always allow for this site" or "Remember my choice".
- **X-Browser Behavior**: X-Browser always displays a bottom confirmation bar for script-triggered navigation without persistent whitelist capability. Simply tap "Allow" to proceed; capture data and reliability remain 100% intact.

---

## 🔧 Script Configuration (`CONFIG`)

You can customize runtime parameters at the top of `glean-web-capture.user.js`:

```javascript
var CONFIG = {
    useIntentUri: true,                   // true: intent URI; false: gleanread:// scheme
    packageName: 'com.gleanread.android', // Android target package name
    maxTextLength: 6000,                  // Maximum character count before truncation
    buttonOffsetY: 24,                    // Vertical distance (px) between button and selection bottom
    debounceMillis: 250,                  // Selection change debounce time (ms)
    debug: false,                         // Console debug log toggle (set true when troubleshooting)
};
```

---

## ❓ Troubleshooting & FAQ

If the button does not appear after selecting text on a regular webpage:

1. **Verify Script Injection**:
   - Open developer console and check for `[GleanRead] script v... installed`, or run `window.__gleanWebCaptureInstalled` in console (should return `true`).
   - If `undefined`: Check if the script is enabled and if the site matches `@match *://*/*`.
   - Note: Built-in reader modes, novel modes, or `file://` pages in some browsers block userscript injection. Test on standard `https://` articles first.
2. **Verify Selection Event Pipeline**:
   - Selecting text should trigger `[GleanRead] first selection-related event received`.
   - Text inside cross-origin `<iframe>` tags is intentionally ignored for security and sandboxing reasons.
3. **No Response After Tapping Button**:
   - Check the console for `[GleanRead] navigate: ...`.
   - If an error or invalid URL warning occurs, toggle `CONFIG.useIntentUri = false`.
4. **Known Limitations**:
   - Page scrolling hides the button; slightly re-adjusting selection restores it.
   - Text selected inside form fields (`<input>`, `<textarea>`, `contenteditable`) will not trigger the capture button.
