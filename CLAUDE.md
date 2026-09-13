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
| App label | `白い熊 Termux API` — **pending (Phase 3)**; today upstream's `Termux:API` | the `TERMUX_API_APP_NAME` `<!ENTITY>` in `app/src/main/res/values/strings.xml` **and** `manifestPlaceholders.TERMUX_API_APP_NAME` in `app/build.gradle` (both feed user-visible text) |
| App icon | black-yellow traced (yellow `#FFFF00` line-art on black) — **pending (Phase 2)** | `app/src/main/res/drawable/ic_launcher.xml`, `drawable-anydpi-v26/ic_launcher.xml`, cut by `tools/icon/emit_launcher.py` |
| Version tail | `versionName = "<upstream>+<base date>.<HH-MM>.g<sha8>+NNN"`, `versionCode = <upstream code>*10000+N` | `app/shiroikuma.gradle` (reads upstream's literals) |
| Signing | gitignored `keystore.properties` → `~/.android-keystores/shiroikuma-emacs-termux.jks` (alias `Emacs keystore`) — **the one key of the whole `com.termux` shared-UID family** | `app/shiroikuma.gradle` `signingConfigs.release` |
| Build task | `buildFork` (assembleRelease → copy to `~/tmp/` → bump `BUILD_NUMBER`) | `app/shiroikuma.gradle`, applied by the last line of `app/build.gradle` |
| Fork links | `https://github.com/ShiroiKuma0/shiroikuma-termux-api` everywhere the app links out — **pending (Phase 3)** | `plugin_info` in `strings.xml`, `TermuxAPIConstants.java`, `README.md`, fastlane |
| De-branding | our name + our GitHub links everywhere user-visible — **pending (Phase 3)** | `strings.xml` entities, main activity, README |
| 白い熊 Termux API UI | the black-yellow page: Export/Import (settings only) + automation rows + Reset — **pending (Phase 4)** | `app/src/main/java/com/termux/api/shiroikuma/` |

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
| Main / settings activities (the settings page is where our UI page will hang) | `activities/`, `settings/`, `res/xml/prefs__*.xml` |
| Constants (app name, GitHub links — Phase 3 de-branding targets) | `TermuxAPIConstants.java`, `res/values/strings.xml` (the `<!ENTITY>` block) |
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
  must show only what Phase 3 deliberately left (package-repo links, upstream attribution).
- Do not edit upstream's Java or resources for anything our own layer (`app/shiroikuma.gradle`, the
  `shiroikuma/` package, `shiroikuma_*` resources) can carry — the smaller the diff against
  `master`, the cleaner every rebase.

## Commit convention — no Claude attribution

Do **not** add any `Co-Authored-By: Claude …` trailer, nor a "🤖 Generated with Claude Code" /
Anthropic-attribution line, to commit messages or PR bodies in this repo. End the message at the last
line of the body. This overrides the harness default. (Global rule: `~/.claude/CLAUDE.md`.)
