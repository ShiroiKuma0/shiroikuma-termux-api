# Changelog

This file carries the history of **白い熊 Termux API**, 白い熊's fork of
[Termux:API](https://github.com/termux/termux-api). Upstream ships no `CHANGELOG.md` (its history
is its GitHub release notes), so this file is the fork's alone: newest release first, each section
naming the upstream base it is built on and then everything built on top of stock. Only the first
release lists everything; later sections are per-release deltas, each with an “Upstream since …”
part distilled from the upstream commits the base moved over. Should upstream ever add a
`CHANGELOG.md`, the fork's sections stay strictly above their content.

## 白い熊 Termux API 0.53.0+2026-09-06.10-33.g44dff893+003 — 2026-09-13

**Upstream base:** [`termux/termux-api`](https://github.com/termux/termux-api) branch `master` at
[`44dff893`](https://github.com/termux/termux-api/commit/44dff8932c23c37531bcc930d8a916f81bf8d43a)
(2026-09-06 10:33 UTC), i.e. upstream `0.53.0` (versionCode 1002) plus every `master` commit up to
that one. Fork versionCode `10020003`. First release of the fork — this section lists everything
built on top of stock.

### Major features

- **The 白い熊 Termux API UI page** — a black/yellow page of the fork's own (`ShiroikumaUiActivity`
  + `ShiroikumaUiFragment`, `res/xml/preferences_shiroikuma_ui.xml`), titled `白い熊 Termux API UI`,
  with three entry points: a **long-press on the settings icon** of the main toolbar
  (`ShiroikumaUiActivity.installSettingsLongPress`, one line in
  `TermuxAPIMainActivity.onCreateOptionsMenu`), a **static launcher shortcut** 「白い熊 UI」 on the
  app icon (`res/xml/shortcuts.xml`, attached to the launcher activity-alias in the manifest), and
  the **first row of the Settings root** (`res/xml/sets__termux.xml`, an `<intent>` preference).
- **Export / Import of the app's preferences** as one ZIP into a folder of your choice, with a
  「Last export」 readout — see *Backup & automation*.
- **The sister-app backup-automation contract v2** (broadcast door, provider data door, foreground
  data service, progress broadcasts, capability meta-data) so 白い熊 応用管理 and 白い熊 自由作業盤
  (the 保存復元 batch) can back the app up and restore it without touching the screen — see
  *Backup & automation*.
- **The black/yellow traced launcher icon and the 白い熊 name** on everything user-visible — see
  *Identity & packaging*.
- **A reproducible signed release build** (`buildFork`) that pins the upstream commit in the
  version — see *Build pipeline*.

### UI & theming

- House look of the UI page: black `#000000`, yellow `#FFFF00`, dim yellow `#C8C800`, warning red
  `#FF5252` (`res/values/shiroikuma_colors.xml`); theme `Theme.Shiroikuma.Ui`
  (`res/values/shiroikuma_styles.xml`); layout `res/layout/activity_shiroikuma_ui.xml`.
- Section headings 20 sp bold with a text-wide 2.5 dp yellow underline, a 1 px hairline above every
  section but the first; rows at the 72 dp indent with 5 dp vertical padding and no dividers; pill
  buttons (`res/drawable/shiroikuma_pill.xml`); bordered dialogs. Row layouts in the kxkb style:
  `preference_category_shiroikuma{,_first}.xml`, `preference_shiroikuma_{indent1,indent2,subheader}.xml`,
  `preference_widget_shiroikuma_regenerate.xml`; helpers `ShiroikumaViews`,
  `AutomationTokenPreference`.
- **Export / Import section**: 「Export / Import…」 (opens the panel) · 「Export directory」 (the
  absolute path when resolvable, red *not set* otherwise; tap opens `ACTION_OPEN_DOCUMENT_TREE` and
  the tree Uri is persisted) · 「Last export」 (queried on `onResume` off the main thread: yellow
  `date time · size`, red *none* / *no directory*) · switch 「Automation export」 (default **ON**) ·
  switch 「Use authorization token?」 (default **OFF**) · 「Automation token」 (shown only while the
  switch above is on; abbreviated, tap copies the full token to the clipboard, a 「Regenerate」 pill
  with a confirm dialog).
- **Reset section**: 「Reset the 白い熊 UI settings」 — confirm dialog, then the export directory is
  forgotten and `shiroikuma_automation` is cleared (switch back to ON, token no longer required, a
  new token generated on next read). Backups already written are kept.
- No appearance sections on purpose — Termux:API draws almost nothing of its own.
- Every string of the page, the panel and the automation surface lives in
  `res/values/shiroikuma_strings.xml`, apart from upstream's `strings.xml`, so a rebase never
  touches them.

### Backup & automation

- **Export engine** (`shiroikuma/backup/ShiroikumaExport.java`): one ZIP named
  `shiroikuma-termux-api_<yyyy-MM-dd_HH-mm-ss>.zip` (family convention — no version, no infix, so
  every sister app's backups sort uniformly in one directory), written as `<name>.part` and renamed
  only once complete, into the SAF tree directory. Layout: `manifest.json` first (`format` =
  `shiroikuma-termux-api`, `version` 1, `app`, `appVersion`, `createdTs`, `categories[]`), then
  `settings.json` = `{"<prefs file>":{"<key>":{"t":"int|long|float|bool|string|set","v":…}}}` —
  every SharedPreferences file under `shared_prefs/` (in practice `com.termux.api_preferences`, the
  app's one real file: the log level) except the device-local `shiroikuma_automation` and
  `shiroikuma_eximport`, `_has_set_default_values` and `WebViewChromiumPrefs`, and minus the key
  `last_pending_intent_request_code` (a per-device counter). One category, `settings` — “Settings
  (log level · app preferences)”, on by default.
- **Import**: per-key merge with `commit()`, only the categories present in the archive, per-category
  counts reported (`N keys in M files`), then 「Later」 / 「Restart now」. A foreign ZIP is refused
  with “Not a 白い熊 Termux API backup.”; an empty one with “Nothing to restore.”.
- **The panel** (`shiroikuma/ui/ExportImportPanel.java`, ported from raikidoban): bordered box,
  red/yellow directory box (tap to choose), the last-export line, 「全選択」 plus the category
  checkboxes, 「Cancel」 ‖ 「Import」 「Export」 pill buttons; every info dialog closes the whole
  chain; export with nothing ticked is refused. Export directory kept in the device-local prefs file
  `shiroikuma_eximport` (`dir_uri`).
- **Contract v2 §1 — the broadcast door** (`shiroikuma/automation/StateExportReceiver.java`, exported
  with no permission by design): `com.termux.api.action.EXPORT_STATE` runs the category ZIP
  headlessly, `LIST_CATEGORIES` answers one `id<TAB>label<TAB>parent<TAB>on|off` line per category,
  `CANCEL_EXPORT` stops a running export (fire-and-forget; the export unwinds at the next entry
  boundary, deletes the half-written archive and answers its original request with
  `ERROR:cancelled`). Extras: `token` (optional, checked only while 「Use authorization token?」 is
  on), `path` (optional absolute directory), `items` (optional comma list of category ids),
  `progress_action`, and the reply trio `reply_action` / `reply_package` / `reply_id`. Reply: a
  fresh broadcast to `reply_package` (with `FLAG_INCLUDE_STOPPED_PACKAGES`) echoing `reply_id`,
  `result` = `OK:<path>|<bytes>|<human size>|<n> categories`, `OK:` + the category lines, or
  `ERROR:<reason>` — exactly one terminal reply per request. The export runs inside the receiver's
  `goAsync()` window (a settings-only ZIP finishes in milliseconds), so no foreground service is
  started and the cold unattended batch can never be refused a foreground start.
- **Storage rule of §1**: the app declares `MANAGE_EXTERNAL_STORAGE` (upstream, for its storage
  APIs), so an absolute `path` is honoured only while All-files access is actually held
  (`Environment.isExternalStorageManager()`) and refused with exactly `ERROR:no-storage-access`
  otherwise; no `path` → the configured SAF directory; none configured → `ERROR:no-directory`.
- **Contract v2 §2a — the data door** (`AutomationProvider`, authority `com.termux.api.automation`):
  `describe` (answers from the manifest, the category enum and SharedPreferences only —
  `requires_permissions: []`, `contains: ["Settings (log level · app preferences)"]`, `format` 1,
  `min_format` 1), `export` and `import` (the bytes go through a file descriptor the caller opened,
  never a path; the call validates, starts the service and returns `OK:<job_id>`; the terminal
  answer comes back on a broadcast), `cancel`. `import` exists only here, never as a broadcast
  action. A refusal is returned in the result bundle, never thrown.
- **Caller verification** (`AutomationCallers`, ported from 自由作業盤): an exact package name
  (`shiroikuma.oyokanri`, `shiroikuma.jiyusagyoban` — never a prefix), the UID confirmed through
  `PackageManager.getPackagesForUid`, and the signing certificate matched against a pinned SHA-256.
- **The foreground data service** (`AutomationDataService`, `foregroundServiceType="dataSync"`):
  goes foreground first, drains the in-process descriptor hand-over inside the same
  `try`/`finally`, closes the caller's file on every path; job ids and cancellation flags in
  `AutomationJobs` (process-local, polled at write boundaries, never persisted). Notification
  channel “Automation data” with “Writing the settings out…” / “Restoring the settings…” /
  “Reading the backup…”.
- **Refused foreground starts** (`AutomationForeground`): `ERROR:no-foreground-start` only when the
  throwable is a `ForegroundServiceStartNotAllowedException` (matched by class name, so it loads on
  older devices) and the app is not battery-exempt — the keyed line that earns a 「電池最適化を除外」
  button on the caller's failed row; a descriptive one-line `ERROR:` otherwise.
- **Contract v2 §3 — progress** (`AutomationProgress`, one sender for both doors): a broadcast per
  category at ≥ 500 ms intervals, the correlation id written under both `reply_id` and `job_id`, and
  a 20 s heartbeat re-sending the last true line while a write into the caller's pipe blocks (the
  caller presumes two minutes of silence to be death). Inert without a `progress_action`.
- **Contract v2 §4 — discovery**: manifest `<meta-data>` `shiroikuma.automation.contract` = 2,
  `shiroikuma.automation.format` = 1, `shiroikuma.automation.min_format` = 1, readable without
  waking the app; `<queries>` for `shiroikuma.oyokanri` and `shiroikuma.jiyusagyoban` so the
  replies' `setPackage()` works on Android 11+.
- **Authorization** (`AutomationAuth`, prefs file `shiroikuma_automation`, device-local and never
  exported): master switch `automation_enabled` default **ON**, `automation_require_token` default
  **OFF**, a 48-hex token (24 random bytes) generated lazily; every write is `commit()` because the
  gate fails open. Refusals: `ERROR:automation disabled` / `ERROR:bad token`; a token sent to an app
  that does not require one is ignored, never refused.
- New manifest permissions: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`. The whole
  automation block is additive at the end of `AndroidManifest.xml`; nothing above it is touched.

### Identity & packaging

- **App label `白い熊 Termux API`**, host app `白い熊 Termux`: the `TERMUX_API_APP_NAME` /
  `TERMUX_APP_NAME` `<!ENTITY>` pair in `res/values/strings.xml` (every resource string, including
  `app_name` and the launcher label, and now `keep_alive_service` too) and the matching
  `manifestPlaceholders` in `app/build.gradle`.
- **`ShiroikumaConstants`** (`shiroikuma/ShiroikumaConstants.java`), the Java twin of those entities,
  replaces the JitPack `termux-shared` values wherever the app shows its own name or links: the
  toolbar title and the launcher-icon enable/disable toasts (`TermuxAPIMainActivity`), the error
  notification titles (`TermuxApiReceiver`, `ResultReturner`), the two permission toasts
  (`TermuxApiReceiver`), the notification channel title (`NotificationAPI`).
- **Links**: the main page's `plugin_info` links go to this fork and to
  `ShiroiKuma0/shiroikuma-termux` (the `termux-api-package` docs link stays upstream's); the About
  page's “Important Links” come from `ShiroikumaConstants.getImportantLinksMarkdownString()` — the
  Termux and Termux:API rows point at the forks, plus this fork's releases and issues; the other
  plugins, the packages repo, email, reddit and the wiki stay as upstream lists them.
- **Donate row removed** from the Settings root (`link__termux_donate` preference and its
  `link__donate___val__title` string).
- **Launcher icon**: the family's black/yellow traced line art — the `>_` prompt inside the disc
  outline, yellow `#FFFF00` strokes on `#000000` — as `drawable/ic_foreground.xml` (adaptive
  foreground) and the legacy pre-26 `drawable/ic_launcher.xml`, both emitted by
  `tools/icon/emit_launcher.py` from one geometry model; SVG master
  `design/shiroikuma-termux-api-icon.svg`. Upstream's adaptive wrapper
  (`drawable-anydpi-v26/ic_launcher.xml`, black background + `@drawable/ic_foreground`) untouched.
- **App id `com.termux.api`, namespace and `sharedUserId` `com.termux` unchanged** — the
  `termux-api` CLI binary hardcodes `com.termux.api/.TermuxApiReceiver` and the
  `com.termux.api://listen` socket. The fork therefore installs **over** stock Termux:API (same id,
  same shared UID, higher versionCode) and is signed with the one key of the whole 白い熊
  `com.termux` family (白い熊 Termux, Termux API, Termux X11, Termux GUI, 白い熊 GNU Emacs).
- Deliberately left as upstream: `LOG_TAG`s, the `termux-notification` channel id, internal names
  such as `TermuxAudioRecording_` / `TermuxFingerprintAPIKey`, `ResultReturner`'s developer-facing
  exception texts, `SECURITY.md`, and upstream's README body (kept verbatim below the fork's own).
- **Known remainder (honest note):** the “Where To Report An Issue” footer that
  `TermuxPluginUtils.sendPluginCommandErrorNotification()` appends to a plugin crash report still
  names upstream's Termux / Termux:API issue trackers. It is built inside the JitPack
  `termux-shared` library from that library's own `TermuxConstants`, which this repo cannot
  override; it will be replaced once the dependency points at a `termux-shared` published from the
  fork's own `ShiroiKuma0/shiroikuma-termux`.

### Build pipeline

- **`app/shiroikuma.gradle`**, applied by the last line of `app/build.gradle` (after upstream's
  inline SemVer check has accepted its own literal): versionName
  `<upstream version>+<base date>.<HH-MM>.g<sha8>+<NNN>` where the pin is `git merge-base HEAD
  master` with that commit's UTC committer time (`0.53.0+2026-09-06.10-33.g44dff893+003` here),
  versionCode = upstream code × 10000 + `BUILD_NUMBER` (`1002 × 10000 + 3 = 10020003`). Upstream's
  own `versionCode 1002` / `versionName "0.53.0"` literals are read, never edited, so a rebase
  carries the new base in by itself.
- **Release signing** from the gitignored `keystore.properties` (`signingConfigs.release`); upstream
  signs only debug builds (its releases are CI `assembleDebug` outputs signed with the tracked
  `app/testkey_untrusted.jks`, left as is). The fork ships the **release** build type: R8 minified,
  `-dontobfuscate`, `shrinkResources false` as upstream leaves it.
- **`buildFork` task**: `assembleRelease` → copy to
  `~/tmp/shiroikuma-termux-api_<versionName>_universal.apk` → bump `BUILD_NUMBER` and record
  `LAST_BUILT_VERSION_CODE` in `gradle.properties`. Fails fast at configuration time when
  `keystore.properties` is missing (the APK would be unsigned) or the versionCode would not exceed
  the last built one; refuses to overwrite an existing file of the same name. `./gradlew -q
  versionName` prints the versionName the next build will carry.
- **Universal APK**: the app has no native code (the JNI libs `termux-shared` pulls in are excluded
  by upstream's `packagingOptions`), so one APK serves every ABI.
- `BUILD_NUMBER` runs monotonically and is never reset — `master` follows upstream's branch tip,
  whose versionCode stands still between releases, and an installer compares versionCode alone.
- `androidx.documentfile:documentfile:1.0.1` declared for the SAF-directory code;
  `app/shiroikuma-proguard-rules.pro` keeps `AutomationTokenPreference` (instantiated by name from
  the page XML).
- `.gitignore`: `keystore.properties`, `*.jks` (upstream's tracked debug key stays tracked),
  `.claude/settings.local.json`, `.scratch/`. `keystore.properties_sample` documents the family
  keystore's fields.
- Repository docs: `CLAUDE.md` (the fork's customization table, the UI page, the automation
  specifics, versioning, hard rules), `.claude/skills/build-apk/SKILL.md` (build + delivery),
  `.claude/skills/upstream-new-version/SKILL.md` (upstream sync with the proceed-gated
  upstream-changes table). Fork README with the upstream README kept verbatim beneath it; this
  `CHANGELOG.md`.
