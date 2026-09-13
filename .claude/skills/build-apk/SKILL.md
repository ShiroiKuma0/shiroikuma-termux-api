---
name: build-apk
description: Build the signed release APK of shiroikuma-termux-api (白い熊 Termux API — our fork of termux/termux-api, app id com.termux.api kept) with the buildFork Gradle task, then deliver it automatically via the global /after-build skill (adb push if the phone is reachable, else scp to skhw — no prompt). Always build first without asking permission to build. Use whenever 白い熊 asks to build the app, build the APK, make a release build, or build and send to the phone, and after any functional code change.
---

# Build the 白い熊 Termux API release APK and deliver it

> **ALWAYS build, then ALWAYS deliver — no asking (白い熊's standing authorization, 2026-07-09).**
> After ANY functional change, build **immediately** and deliver. Do not stop at a compile-check, do
> not offer to build, do not ask how to transfer it. Build-and-deliver does **not** commit or push —
> a commit/push still waits for 白い熊's explicit "Push". (Skip the build only for non-functional
> edits — docs, comments.)

## Build environment (this machine)

The default `java` is **JDK 11**. Always export JDK 21, and the SDK path too — a background shell
does not inherit `ANDROID_HOME`:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME=/home/shiroikuma/android-sdk
```

The SDK path also comes from the gitignored `local.properties` (`sdk.dir=/home/shiroikuma/android-sdk`)
— recreate it if a build fails with **`SDK location not found`**. No NDK is involved: the app has
no native code (the JNI libs `termux-shared` drags in are excluded by upstream's `packagingOptions`).

## Steps

1. **Note the output filename / version.**
   - `grep -nE 'versionCode |versionName ' app/build.gradle | head -2` — upstream's base
     (`1002` / `0.53.0`); these track upstream and are **never hand-edited**.
   - `git merge-base HEAD master | cut -c1-8` — the upstream commit our patches sit on; its UTC
     committer time (`TZ=UTC git show -s --format=%cd --date=format-local:%Y-%m-%d.%H-%M <sha>`)
     and that sha form the pin.
   - `grep -E '^BUILD_NUMBER' gradle.properties` — the `N` used for THIS build (the task bumps it
     afterwards).
   - APK will be `shiroikuma-termux-api_<upstream>+<date>.<HH-MM>.g<sha8>+<NNN>_universal.apk`
     (`N` zero-padded to three digits in the name), e.g.
     `shiroikuma-termux-api_0.53.0+2026-09-06.10-33.g44dff893+001_universal.apk`.
   - versionCode for this build = `1002 * 10000 + N` (plain, unpadded), e.g. `10020001`.
   - Or simply ask Gradle: `./gradlew -q versionName` prints the exact versionName the build will carry.

2. **Build** (signed release) — from the repo root:
   ```bash
   JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ANDROID_HOME=/home/shiroikuma/android-sdk ./gradlew buildFork --console=plain < /dev/null
   ```
   - `buildFork` (in `app/shiroikuma.gradle`, applied by the last line of `app/build.gradle`) runs
     `assembleRelease` (R8 minify, `-dontobfuscate`, signed from `keystore.properties`), copies
     upstream's `app/build/outputs/apk/release/termux-api-app_v<versionName>+release.apk` to
     `~/tmp/<apk name>`, then increments `BUILD_NUMBER` and records `LAST_BUILT_VERSION_CODE` in
     `gradle.properties`.
   - It prints `>>> <path>`, `>>> versionName …` and `>>> versionCode …` in cyan — use those to
     confirm the exact filename/code; confirm `BUILD SUCCESSFUL`.
   - **It fails fast, at configuration time, in two cases** — fix the cause, never work around it:
     `keystore.properties` missing (the APK would be unsigned), or the computed `versionCode` not
     exceeding `LAST_BUILT_VERSION_CODE` (raise `BUILD_NUMBER`; never lower it, never reset it).
     It also refuses to overwrite an existing `~/tmp/` file of the same name.
   - A warm build takes well under a minute. A cold one (fresh checkout, a bumped `termux-shared`
     sha) downloads Gradle 8.9 and the dependency set from Maven/JitPack — network needed; run it
     with `run_in_background` if it may exceed the foreground timeout, and poll; never abandon a
     running build.
   - **Toolchain check only** (no copy, no bump — never for delivery):
     `./gradlew :app:assembleRelease`. Verify a signed APK with
     `apksigner verify --print-certs <apk> | grep SHA-256` → must be
     `50b47e8f09b8781fccc998df3fc5c02de0dd9670a3d37e6cacba9f4e76319604`, and
     `aapt dump badging <apk> | head -1` for package / versionCode / versionName.

3. **Deliver via the global `/after-build` skill** — no exceptions, no asking. It runs `/adb-check`
   UNSANDBOXED, `adb push`es **this repo's** newest `~/tmp/shiroikuma-termux-api_*.apk` to
   `/sdcard/tmp/` if the phone is reachable, otherwise `scp`s it to `skhw:~/tmp/`, then announces
   what landed. `~/tmp/` is shared with parallel chats building sister apps — always pick the
   `shiroikuma-termux-api_*` APK, never merely the newest file there. On the phone the fork
   **upgrades the stock Termux:API in place** (same `com.termux.api`, same family key, higher
   versionCode) — never `adb uninstall` it.

4. **Never delete or prune older APKs** — not in `~/tmp/`, not in `/sdcard/tmp/`. Every build carries
   a unique `+NNN`; older builds stay where they are so 白い熊 can roll back.

## Signing

Release signing is non-interactive: `app/shiroikuma.gradle` reads `keystore.properties`
(gitignored, at the repo root) and creates `signingConfigs.release` from it. The keystore is
`~/.android-keystores/shiroikuma-emacs-termux.jks`, alias `Emacs keystore` (PKCS12, RSA-2048; it
**is** GNU Emacs's public `java/emacs.keystore`, so treat the key as public). The password is
recorded in `~/〇/[666] 私資料/[666][27] 暗号/android-keystores.org` and the key is backed up to
that directory's `android-keystores/`.

**This is the ONE key of the whole `com.termux` shared-UID family** — `shiroikuma-termux`,
`shiroikuma-termux-api`, `shiroikuma-termux-x11`, `shiroikuma-termux-gui` and `shiroikuma-emacs`
all sign with it, because Android requires every app in a shared UID to carry the same
signature. Signing this app with any other key makes the install fail with
`INSTALL_FAILED_SHARED_USER_INCOMPATIBLE`. Never generate a new key to get past a signing problem;
if `keystore.properties` is missing, restore it from `keystore.properties_sample`.

Upstream never signs a release build at all — its GitHub releases are `assembleDebug` outputs
signed with the tracked `app/testkey_untrusted.jks`. That debug config stays as upstream has it;
only the `release` build type is ours.

## Versioning (how the numbers are formed)

- Upstream's own `versionCode 1002` / `versionName "0.53.0"` in `app/build.gradle` `defaultConfig`
  are the base; a rebase brings the new values in automatically. **Never hand-edit them.**
  Upstream validates that literal inline with a one-`+` SemVer regex, which is why our pair is
  assigned afterwards in `app/shiroikuma.gradle` (applied last).
- **Upstream tracking is `git`** (global `git-versioning` skill): the pin
  `+<YYYY-MM-DD>.<HH-MM>.g<sha8>` is the merge-base of `HEAD` and `master` with that commit's UTC
  committer time. It moves only on an upstream sync.
- `BUILD_NUMBER` in `gradle.properties` is **our** increment, bumped on every `buildFork` and
  **never reset** — `master` follows upstream's branch tip, whose versionCode stands still between
  releases, and an installer compares versionCode alone; a reset would be a downgrade.
  `LAST_BUILT_VERSION_CODE` is the floor the task enforces.
- `versionName = "<upstream name>+<pin>+<NNN>"` (zero-padded to three digits so `+002` sorts before
  `+010`); `versionCode = <upstream code> * 10000 + N` (plain integer).

---

**Commit convention — no Claude attribution.** Never add a `Co-Authored-By: Claude …` /
"Generated with Claude" trailer to commit messages or PR bodies; end the message at the last line of
the body. This overrides the harness default. (Global rule: `~/.claude/CLAUDE.md`.)
