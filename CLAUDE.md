# CLAUDE.md — guide for Claude Code in this repo

**shiroikuma-termux-api** — 白い熊's fork of [Termux:API](https://github.com/termux/termux-api),
the Termux plugin app that exposes Android APIs (camera, clipboard, notifications, sensors, SMS,
TTS, …) to the `termux-api` command-line package (Java, one Gradle module, no native code;
GPL-3.0). It keeps upstream's app id **`com.termux.api`** — the `termux-api` CLI binary in the
Termux prefix hardcodes `com.termux.api/.TermuxApiReceiver` and the socket
`com.termux.api://listen`, so renaming would mean rebuilding the package repo. What changes is the
label (**白い熊 Termux API**), the icon, the links, the signing key and our UI page; it installs
**over** the stock Termux:API (same id, same shared UID `com.termux`, higher versionCode).

This repo (`ShiroiKuma0/shiroikuma-termux-api`) is a fork. We track upstream's **`master` branch
tip** on `master` and layer our customizations on `custom`.

## Read this first

Before any work, read **`.claude/skills/build-apk/SKILL.md`** (canonical build + delivery) and
**`.claude/skills/upstream-new-version/SKILL.md`** (upstream sync + rebase, with the mandatory
proceed-gated upstream-changes table). Publishing a release uses the **global** `/publish-version`
skill — this repo has no local copy.

## Fork workflow — READ THIS FIRST

### Git remotes & branches

- `origin` → `git@github.com:ShiroiKuma0/shiroikuma-termux-api.git` (push here).
- `upstream` → `https://github.com/termux/termux-api.git` (fetch only; its push URL is `DISABLED`).
- `master` — mirrors `upstream/master`, **fast-forward only**. No fork work here.
- `custom` — all our work, rebased onto `master` on each sync, and the GitHub default branch so the
  repo page lands on the fork.

**Upstream tracking: `git`** — `custom` is rebased onto every upstream commit, so the fork
versionName pins the upstream base: `<upstream>+<base date>.<HH-MM>.g<sha>+<BUILD_NUMBER, 3 digits>`.
See the global **`git-versioning`** skill. Why git and not release tags (白い熊, 2026-09-13): termux
tags a release rarely (`v0.53.0` sits far behind `master`, which carries the fixes 白い熊 actually
runs), and the whole Termux family is synced the same way, so one uniform scheme beats a per-repo
case analysis.

### Our customizations (install identity + build)

| What | Value | Where |
| --- | --- | --- |
| applicationId | `com.termux.api` (**unchanged** — the CLI hardcodes it) | `app/build.gradle` → `defaultConfig` (upstream's line, untouched) |
| namespace (R/BuildConfig pkg) | `com.termux.api` (**never rename**) | `app/build.gradle` |
| sharedUserId | `com.termux` via the `${TERMUX_PACKAGE_NAME}` placeholder (**unchanged**) | `app/src/main/AndroidManifest.xml`, `manifestPlaceholders` in `app/build.gradle` |
| App label | `白い熊 Termux API` (host app: `白い熊 Termux`) — **done (Phase 3)** | the `TERMUX_API_APP_NAME` / `TERMUX_APP_NAME` `<!ENTITY>` pair in `app/src/main/res/values/strings.xml` (every resource string, incl. `app_name` → launcher `android:label`) **and** `manifestPlaceholders.TERMUX_API_APP_NAME` / `TERMUX_APP_NAME` in `app/build.gradle` (kept in step; the manifest itself only uses `${TERMUX_PACKAGE_NAME}`). Java-side twin: `ShiroikumaConstants.APP_NAME` / `TERMUX_APP_NAME` in `app/src/main/java/com/termux/api/shiroikuma/ShiroikumaConstants.java`, read by `TermuxAPIMainActivity` (toolbar title, launcher-icon toasts), `TermuxApiReceiver` (error-notification title, two permission toasts), `ResultReturner` (error-notification title), `NotificationAPI` (`CHANNEL_TITLE`) — the JitPack `termux-shared` `TermuxConstants.TERMUX_API_APP_NAME` still says `Termux:API` and is left for the log tag (`TermuxAPIApplication`) and the About-report file name (`TermuxAPISettingsActivity`) |
| App icon | black-yellow traced (yellow `#FFFF00` line-art on black) — **done (Phase 2, approved 2026-09-13)** | `app/src/main/res/drawable/ic_foreground.xml` (the traced vector) + `drawable/ic_launcher.xml` (legacy, pre-26), both emitted by `tools/icon/emit_launcher.py` from the geometry model, SVG master `design/shiroikuma-termux-api-icon.svg`; `drawable-anydpi-v26/ic_launcher.xml` is upstream's adaptive wrapper, untouched (black background + `@drawable/ic_foreground`) |
| Version tail | `versionName = "<upstream>+<base date>.<HH-MM>.g<sha8>+NNN"`, `versionCode = <upstream code>*10000+N` | `app/shiroikuma.gradle` (reads upstream's literals) |
| Signing | gitignored `keystore.properties` → `~/.android-keystores/shiroikuma-emacs-termux.jks` (alias `Emacs keystore`) — **the one key of the whole `com.termux` shared-UID family** | `app/shiroikuma.gradle` `signingConfigs.release` |
| Build task | `buildFork` (assembleRelease → copy to `~/tmp/` → bump `BUILD_NUMBER`) | `app/shiroikuma.gradle`, applied by the last line of `app/build.gradle` |
| Fork links | `https://github.com/ShiroiKuma0/shiroikuma-termux-api` (+ `/issues`, `/releases`) and `https://github.com/ShiroiKuma0/shiroikuma-termux` everywhere the app links out — **done (Phase 3)** | `ShiroikumaConstants.GITHUB_REPO_URL` / `TERMUX_GITHUB_REPO_URL` fed into `plugin_info` by `TermuxAPIMainActivity` (the `termux-api-package` docs link stays upstream's), `ShiroikumaConstants.getImportantLinksMarkdownString()` replaces `TermuxUtils.getImportantLinksMarkdownString()` on the About page (`TermuxAPISettingsActivity`; Termux + Termux:API rows → the forks, other plugins / packages / email / reddit / wiki as upstream), `README.md` fork header. `TermuxAPIConstants.java` holds no links (only the receiver name and the file-share authority) — untouched. No fastlane in this repo |
| De-branding | our name + our GitHub links everywhere user-visible; donate row removed — **done (Phase 3)** | the rows above, plus `keep_alive_service` in `strings.xml` now uses the entity, the `link__termux_donate` `<Preference>` is dropped from `res/xml/sets__termux.xml` and its `link__donate___val__title` string from `strings.xml` (`configureDonatePreference()` in `TermuxAPISettingsActivity` stays as dead code — `findPreference` returns null — to keep the upstream diff small). Deliberately left: `LOG_TAG`s, `CHANNEL_ID`, `TermuxAudioRecording_` / `TermuxFingerprintAPIKey` internal names, ResultReturner's "Termux app" exception texts (developer-facing), upstream's README body and `SECURITY.md` |
| 白い熊 Termux API UI | the black-yellow page (title `白い熊 Termux API UI`): Export / Import section → Reset section — **done (Phase 4)**; see "The UI page" below | host `shiroikuma/ui/ShiroikumaUiActivity.java` (theme `Theme.Shiroikuma.Ui` in `res/values/shiroikuma_styles.xml`, layout `res/layout/activity_shiroikuma_ui.xml`), page `shiroikuma/ui/ShiroikumaUiFragment.java` + `res/xml/preferences_shiroikuma_ui.xml`, kxkb-style rows `res/layout/preference_category_shiroikuma{,_first}.xml` / `preference_shiroikuma_{indent1,indent2,subheader}.xml` / `preference_widget_shiroikuma_regenerate.xml`, `shiroikuma/ui/{ShiroikumaViews,AutomationTokenPreference}.java`, strings/colours `res/values/shiroikuma_{strings,colors}.xml`, pill `res/drawable/shiroikuma_pill.xml`. Entry points: static shortcut `res/xml/shortcuts.xml` (`<meta-data android.app.shortcuts>` on the launcher alias in the manifest), long-press on the main toolbar's settings icon (`ShiroikumaUiActivity.installSettingsLongPress`, one line in `TermuxAPIMainActivity.onCreateOptionsMenu`), first row of `res/xml/sets__termux.xml` (an `<intent>` preference) |
| Export / Import | one ZIP `shiroikuma-termux-api_<yyyy-MM-dd_HH-mm-ss>.zip` (written as `.part`, renamed when complete) into a SAF tree directory; category `settings` only — **done (Phase 4)** | engine `shiroikuma/backup/ShiroikumaExport.java`, panel `shiroikuma/ui/ExportImportPanel.java` (raikidoban port: bordered box, red/yellow directory box, 全選択 + categories, Cancel ‖ Import Export pills, info dialog closes the chain, import → Later / Restart now); `androidx.documentfile` declared in `app/shiroikuma.gradle` |
| 保存復元 automation (contract v2 §1–§4) | receiver `com.termux.api.action.{EXPORT_STATE,CANCEL_EXPORT,LIST_CATEGORIES}`, provider `com.termux.api.automation` (describe / export / import / cancel), `dataSync` foreground service, progress + heartbeat, `<queries>` for both callers, `shiroikuma.automation.{contract=2,format=1,min_format=1}` — **done (Phase 4)** | `shiroikuma/automation/{AutomationAuth,AutomationCallers,AutomationJobs,AutomationProgress,AutomationForeground,StateExportReceiver,AutomationProvider,AutomationDataService}.java`; the additive block at the end of `AndroidManifest.xml` (+ `FOREGROUND_SERVICE{,_DATA_SYNC}` and the `<queries>` up top); `app/shiroikuma-proguard-rules.pro` |

### The UI page (Phase 4)

`白い熊 Termux API UI` — black `#000000`, yellow `#FFFF00`, dim `#C8C800`, warning red `#FF5252`;
section headings 20 sp bold with a text-wide 2.5 dp underline (1 px hairline above every section
but the first), rows at the 72 dp indent, 5 dp vertical padding, no dividers, pill buttons, bordered
dialogs. Sections and rows, top to bottom:

- **Export / Import** — 「Export / Import…」 (the panel) · 「Export directory」 (absolute path when
  resolvable, red *not set* otherwise; tap → `ACTION_OPEN_DOCUMENT_TREE`, persisted) · 「Last export」
  (queried on `onResume` off the main thread: yellow `date time · size`, red *none* / *no directory*)
  · switch 「Automation export」 (default **ON**) · switch 「Use authorization token?」 (default
  **OFF**) · 「Automation token」 (only while the switch above is on; abbreviated, tap copies,
  「Regenerate」 pill with a confirm).
- **Reset** — 「Reset the 白い熊 UI settings」: confirm dialog, then the export directory is forgotten
  and `shiroikuma_automation` is cleared (switch back ON, token not required, new token on next read).

No appearance sections: Termux:API draws almost nothing of its own.

**Categories** (the `Cat` enum in `ShiroikumaExport`; ids are the ZIP entry names and the
`items` ids): `settings` — "Settings (log level · app preferences)", default on. ZIP layout:
`manifest.json` first (`format` = `shiroikuma-termux-api`, `version` 1, `app`, `appVersion`,
`createdTs`, `categories[]`), then `settings.json` =
`{"<prefs file>":{"<key>":{"t":"int|long|float|bool|string|set","v":…}}}` — every file under
`shared_prefs/` (always `com.termux.api_preferences`, the app's one real file: `log_level`) except
`shiroikuma_automation`, `shiroikuma_eximport`, `_has_set_default_values`, `WebViewChromiumPrefs`,
and minus the key `last_pending_intent_request_code` (a per-device counter). Import = per-key merge
with `commit()`, only the categories present in the archive, per-category counts reported.

**Prefs files** (device-local, never exported): `shiroikuma_eximport` (`dir_uri` — the SAF tree
Uri), `shiroikuma_automation` (`automation_enabled` bool default true, `automation_require_token`
bool default false, `automation_token` 48-hex generated lazily; all writes `commit()`).

**Automation specifics of this app**: the §1 `EXPORT_STATE` runs inside the receiver's
`goAsync()` window (a settings-only ZIP finishes in milliseconds — no foreground service, so the
cold batch can never be refused a foreground start); the §2a data door runs in
`AutomationDataService` (`dataSync`), guarded at both start sites through
`AutomationForeground.refusal()` (`ERROR:no-foreground-start` only for
`ForegroundServiceStartNotAllowedException` by class name while not battery-exempt). The app
**declares** `MANAGE_EXTERNAL_STORAGE` (upstream), so an absolute `path` extra is honoured only
when `Environment.isExternalStorageManager()` and otherwise refused with exactly
`ERROR:no-storage-access`; no `path` → the SAF directory → `ERROR:no-directory`. `describe` answers
`requires_permissions: []` and `contains: ["Settings (log level · app preferences)"]`.

### CHANGELOG.md is unified — our sections on top

Upstream ships **no** `CHANGELOG.md` (its history is GitHub release notes). Ours is therefore a
root `CHANGELOG.md` we own outright, newest first, one section per release
(`## 白い熊 Termux API <tag> — <YYYY-MM-DD>`), and each section carries **two** parts: (a) our
changes since the previous release, and (b) an **“Upstream since `<previous base sha>`”**
subsection distilled from the proceed-gate table of the sync(s) that moved the base — that is the
“merged changelog” published with each release. Only the first release lists everything. The same
text goes in the GitHub release notes; the **global `/publish-version` skill** does both. Should
upstream ever add a `CHANGELOG.md`, keep ours strictly above their content, byte for byte.

### Versioning & APK naming

- The upstream base lives in `app/build.gradle` `defaultConfig` as upstream's own
  `versionCode 1002` / `versionName "0.53.0"` literals. **Never hand-edit them** — a rebase brings
  the new base in by itself. Upstream validates that literal inline with a strict SemVer regex
  (one `+` allowed), which is why our derivation lives in **`app/shiroikuma.gradle`, applied by
  the last line of `app/build.gradle`**: it runs after that check and overwrites the pair.
- The pin is `git merge-base HEAD master` (the upstream commit our patches sit on — not our HEAD,
  not `master`'s tip) shortened to 8 chars, plus that commit's own committer date **and time, in
  UTC** (`%ct` epoch → `yyyy-MM-dd.HH-mm`; never `--date=format:`, which renders the commit's own
  zone). It moves only on a sync.
- `BUILD_NUMBER` (in `gradle.properties`) is our per-build `N`:
  `versionName = "<base>+<YYYY-MM-DD>.<HH-MM>.g<sha8>+<NNN>"` (e.g.
  `0.53.0+2026-09-06.10-33.g44dff893+001`), `versionCode = 1002 * 10000 + N` (plain integer, e.g.
  `10020001`). Zero-padded to 3 digits **in the name only**. The `buildFork` task bumps it after
  every successful build.
- **`BUILD_NUMBER` runs MONOTONICALLY and is NEVER reset.** `master` follows upstream's branch
  tip, whose `versionCode` stands still between releases; an installer compares `versionCode` and
  nothing else, so resetting `N` on a sync that merely repinned `.g<sha>` would send `versionCode`
  backwards and make every sync a downgrade. Reset to `1` only if upstream's own `versionCode`
  literal moves — and even then only when the new line's codes all exceed the old one's.
- The `buildFork` task enforces this: it records `LAST_BUILT_VERSION_CODE` in `gradle.properties`
  and **refuses to build** a `versionCode` that does not exceed it. Raise `BUILD_NUMBER` past the
  last built tail; never lower it.
- APK: `shiroikuma-termux-api_<versionName>_universal.apk`, copied to `~/tmp/`. The app has no
  native code (the JNI libs pulled in by `termux-shared` are excluded), so one APK serves every
  ABI. The versionName contains no `_` and no `~` (Debian and `git check-ref-format` both reject
  them; see the global `git-versioning` skill). Upstream's own output under
  `app/build/outputs/apk/release/` is `termux-api-app_v<versionName>+release.apk` — `buildFork`
  copies whatever `assembleRelease` produced.

### Build commands

```bash
# Our build: signed release → ~/tmp + bump BUILD_NUMBER (use this)
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ANDROID_HOME=/home/shiroikuma/android-sdk ./gradlew buildFork --console=plain < /dev/null
# Release APK only (no copy / no bump) — toolchain proof, never for delivery
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ANDROID_HOME=/home/shiroikuma/android-sdk ./gradlew :app:assembleRelease --console=plain < /dev/null
```

Upstream's own release artefacts are **debug** builds signed with the tracked
`app/testkey_untrusted.jks`; we ship the **release** build type (R8 minified, `-dontobfuscate`,
`shrinkResources false` as upstream leaves it), signed with the family key.

### Toolchain

- JDK **21** at `/usr/lib/jvm/java-21-openjdk-amd64` (the host default `java` is JDK 11 — always
  set `JAVA_HOME`).
- Android SDK at `~/android-sdk`; `compileSdk 35`, `targetSdk 28`, `minSdk 24` (all from
  `gradle.properties`, upstream's). Gradle wrapper 8.9, AGP 8.7.3. No NDK.
- Gradle needs `local.properties` with `sdk.dir=/home/shiroikuma/android-sdk` (gitignored); a
  background shell does not inherit `ANDROID_HOME`, hence the explicit export above.
- The one external dependency, `com.termux.termux-app:termux-shared:<sha>`, comes from **JitPack**
  — the first build (and every bump of that sha) needs network.

## Architecture (upstream Termux:API)

One Gradle module, `:app`, package `com.termux.api`.

| Area | Where |
| --- | --- |
| Entry point: broadcast receiver the CLI targets (`com.termux.api/.TermuxApiReceiver`) | `app/src/main/java/com/termux/api/TermuxApiReceiver.java` |
| Socket listener (`com.termux.api://listen`) + keep-alive service | `SocketListener.java`, `KeepAliveService.java` |
| The 37 API implementations (`termux-camera-photo`, `termux-notification`, …) | `app/src/main/java/com/termux/api/apis/*API.java` |
| Main / settings activities (our UI page hangs off both — see the customization table) | `activities/`, `settings/`, `res/xml/prefs__*.xml`, `res/xml/sets__termux.xml` |
| Our UI page, Export / Import engine, automation contract | `shiroikuma/ui/`, `shiroikuma/backup/`, `shiroikuma/automation/` + the `shiroikuma_*` resources |
| Constants (app name, GitHub links) | ours: `shiroikuma/ShiroikumaConstants.java` + the `<!ENTITY>` block in `res/values/strings.xml`; upstream's `TermuxAPIConstants.java` (receiver name, share authority) and the library's `TermuxConstants` (log tag, paths) |
| Manifest: `sharedUserId`, the placeholders, every permission the APIs need | `app/src/main/AndroidManifest.xml` |
| Shared Termux library (constants, prefs, logger) | `com.termux.termux-app:termux-shared` (JitPack) |
| Our build layer | `app/shiroikuma.gradle`, `gradle.properties` (`BUILD_NUMBER`, `LAST_BUILT_VERSION_CODE`) |

## Hard rules

- **Never rename `applicationId`, `namespace` or `sharedUserId`.** All three stay `com.termux.api` /
  `com.termux.api` / `com.termux` — the `termux-api` CLI and every script in the prefix address
  this package by name, and the shared UID is what lets Termux talk to it at all.
- **One keystore for the whole `com.termux` shared-UID family** (`shiroikuma-termux`, `-termux-api`,
  `-termux-x11`, `-termux-gui`, `shiroikuma-emacs`): `~/.android-keystores/shiroikuma-emacs-termux.jks`.
  Signing any member with another key makes its install fail with
  `INSTALL_FAILED_SHARED_USER_INCOMPATIBLE`; changing the key later means uninstall + restore for
  everything. Never “fix” a signing problem by generating a new key.
- **Never commit/push unprompted.** Build, deliver, and stop; 白い熊 tests. Commit + push only on
  their explicit **"Push"** — that means commit + `git push --force-with-lease origin custom`
  (`master` fast-forwards with a plain push).
- `keystore.properties`, `local.properties` and `*.jks` are gitignored — never commit them.
  (Upstream's `app/testkey_untrusted.jks` is tracked and stays as upstream has it.)
- **Always run `adb`, `scp` and `git status`/`git diff` with `dangerouslyDisableSandbox: true`**
  (the sandbox blocks adb's server socket and invents phantom untracked files at the repo root).
- **After ANY functional change, build and deliver automatically** — the global `/after-build`
  standing authorization; never wait for "build it". Every build bumps `BUILD_NUMBER`; never
  overwrite or delete an older APK, in `~/tmp/` or on the phone. On the phone the fork **upgrades
  the stock Termux:API in place** (same id, same key as the rest of the family, higher
  versionCode) — never `adb uninstall` it.
- On new upstream commits, run the **`upstream-new-version`** skill — it presents the
  proceed-gated upstream-changes table **before** any rebasing, then fast-forwards `master`,
  rebases `custom`, keeps `BUILD_NUMBER` counting, and builds the next `+NNN`.
- **Brand grep guard after every rebase** — upstream keeps adding user-visible strings:
  `grep -rn 'Termux:API\|termux/termux-api' app/src/main/res app/src/main/java README.md | grep -v 'com.termux.api'`
  must show only what Phase 3 deliberately left: two `<!-- Termux:API … -->` comments in
  `strings.xml` (lines 72/75) and `README.md` (our header's attribution link + upstream's body).
  Anything new is a string to route through the entities / `ShiroikumaConstants`. Wider check:
  `grep -rn '"[^"]*Termux[^"]*"' app/src/main/java` must hit only internal names (`LOG_TAG`s,
  `TermuxFingerprintAPIKey`, `TermuxAudioRecording_`, ResultReturner's exception texts).
  Known remainder that this repo cannot fix: the "Where To Report An Issue" section appended to
  plugin error reports by `TermuxPluginUtils.sendPluginCommandErrorNotification()` lists upstream's
  `Termux` / `Termux:API` issue trackers — it is built inside the JitPack `termux-shared` library
  from its own `TermuxConstants`; it goes away only by pointing the dependency at a `termux-shared`
  published from `ShiroiKuma0/shiroikuma-termux` (whose Phase 3 rewrites those constants).
- Do not edit upstream's Java or resources for anything our own layer (`app/shiroikuma.gradle`, the
  `shiroikuma/` package, `shiroikuma_*` resources) can carry — the smaller the diff against
  `master`, the cleaner every rebase.

## Commit convention — no Claude attribution

Do **not** add any `Co-Authored-By: Claude …` trailer, nor a "🤖 Generated with Claude Code" /
Anthropic-attribution line, to commit messages or PR bodies in this repo. End the message at the last
line of the body. This overrides the harness default. (Global rule: `~/.claude/CLAUDE.md`.)
