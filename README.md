<div align="center">

<img src="design/shiroikuma-termux-api-icon.svg" width="120" alt="白い熊 Termux API icon" />

# 白い熊 Termux API

**The Termux plugin that hands Android to the shell — camera, clipboard, notifications, sensors, SMS, TTS and more — the way 白い熊 runs it.**

A fork of [Termux:API](https://github.com/termux/termux-api) with **major additions**: the **白い熊 Termux API UI** page, Export / Import of the app's settings, the sister-app backup-automation contract (so 保存復元 backs it up headlessly), the black/yellow traced icon and the 白い熊 name everywhere, and a reproducible signed release build.

Installs **over** the stock Termux:API (app id `com.termux.api` kept, so the `termux-api` CLI package and the rest of the Termux package ecosystem keep working); the whole family — 白い熊 Termux, Termux API, Termux X11, Termux GUI and 白い熊 GNU Emacs — shares Android UID `com.termux` and is signed with one key, so every member must come from these forks.

**📥 Latest release: [`0.53.0+2026-09-06.10-33.g44dff893+003`](https://github.com/ShiroiKuma0/shiroikuma-termux-api/releases/latest)** — [all releases & APK downloads »](https://github.com/ShiroiKuma0/shiroikuma-termux-api/releases)

</div>

---

## 🐻 The 白い熊 Termux API UI page

Stock Termux:API has a settings screen with one real option. This fork adds a page of its own in
the house black/yellow look — **白い熊 Termux API UI** — reachable three ways: **long-press the
settings icon** in the app's toolbar, the **「白い熊 UI」 shortcut** on the launcher icon, or the
**first row of the Settings screen**. Section headings with the text-wide yellow underline, rows at
the 72 dp indent, pill buttons, bordered dialogs — the same page every 白い熊 app carries, so you
never have to learn a second layout.

---

## 📦 Export / Import — the app's settings as one ZIP

Pick an export directory once (any folder, through the system picker), then **Export** writes
`shiroikuma-termux-api_<yyyy-MM-dd_HH-mm-ss>.zip` there — the app's preferences, type-tagged, in one
archive that reads and sorts uniformly beside every sister app's backups. **Import** restores just
the categories the archive holds and offers to restart the app on the spot. The page shows the
directory (red until set) and the **last export** with its time and size, so you can see at a
glance whether a backup exists.

---

## 🤖 Backup automation for 保存復元

The app speaks the family's **backup-automation contract v2**: a broadcast door
(`com.termux.api.action.EXPORT_STATE` / `LIST_CATEGORIES` / `CANCEL_EXPORT`) that writes the same
ZIP headlessly, and a content-provider data door (`com.termux.api.automation`, with `describe` /
`export` / `import` / `cancel`) that streams the backup into a file descriptor the caller opens —
so 白い熊 応用管理 can back the app up and restore it onto a clean phone, and 白い熊 自由作業盤 can run
the 保存復元 batch across the whole family in one go. Callers are verified by package name, UID and
pinned signing certificate; an optional authorization token can be switched on; progress broadcasts
with a heartbeat keep the batch informed; and the whole thing can be closed off with one switch.

---

## 🎨 The traced icon and the name

The launcher icon is the family's **black/yellow traced line art** — the `>_` prompt inside the
disc, yellow `#FFFF00` on black — generated from one geometry model
(`tools/icon/emit_launcher.py`, SVG master under `design/`). The app is **白い熊 Termux API** in the
toolbar, the notifications, the toasts and the launcher; its links point at this fork and at
[白い熊 Termux](https://github.com/ShiroiKuma0/shiroikuma-termux); the donate row is gone.

---

## 🔑 One family, one key

The app id **stays `com.termux.api`** — the `termux-api` CLI binary in the Termux prefix hardcodes
`com.termux.api/.TermuxApiReceiver` and the `com.termux.api://listen` socket, so this build
**upgrades stock Termux:API in place** (same id, same shared UID `com.termux`, higher versionCode).
It is signed with the one key of the whole 白い熊 `com.termux` family, which is what a shared UID
demands: install the sister forks from their own release pages, never a mix of stock and fork.

The version pins the upstream commit each release is built on:
`<upstream version>+<upstream base date>.<HH-MM>.g<sha8>+<NNN>` — the fork tracks upstream's
`master` tip, not its rare release tags, and `versionCode` is upstream's code × 10000 + the build
counter, so a rebase is never a downgrade.

---

## 👪 Family

- [shiroikuma-termux](https://github.com/ShiroiKuma0/shiroikuma-termux) — 白い熊 Termux, the terminal itself (Termux:Boot, Widget, Float and Styling absorbed).
- [shiroikuma-termux-api](https://github.com/ShiroiKuma0/shiroikuma-termux-api) — this repo.
- [shiroikuma-termux-x11](https://github.com/ShiroiKuma0/shiroikuma-termux-x11) — 白い熊 Termux X11.
- [shiroikuma-termux-gui](https://github.com/ShiroiKuma0/shiroikuma-termux-gui) — 白い熊 Termux GUI.
- [shiroikuma-emacs](https://github.com/ShiroiKuma0/shiroikuma-emacs) — 白い熊 GNU Emacs (`shiroikuma.emacs`, installs side-by-side with stock `org.gnu.emacs`).

---

## Built on Termux:API

A fork of [Termux:API](https://github.com/termux/termux-api) (app id `com.termux.api` kept, so it
installs over the official build and the `termux-api` package keeps addressing it). Termux:API is
the bridge that lets scripts in Termux reach the Android APIs — the camera, the clipboard,
notifications, sensors, telephony, TTS and the rest — and this fork changes nothing about how those
calls are made. The code remains under the [GPL-3.0](http://www.gnu.org/licenses/gpl-3.0.en.html).

The app builds against the fork's own `termux-shared` library (published to mavenLocal by
[shiroikuma-termux](https://github.com/ShiroiKuma0/shiroikuma-termux)), so even the “Where To Report
An Issue” footer of a plugin crash report names this fork, not upstream.

## Building

```bash
git clone https://github.com/ShiroiKuma0/shiroikuma-termux-api.git
cd shiroikuma-termux-api            # branch `custom` — the fork; `master` mirrors upstream

# Release signing: `keystore.properties` at the repo root (gitignored) — copy
# keystore.properties_sample and fill in the family keystore. Without it buildFork refuses to run
# (the APK would come out unsigned), and a build signed with any other key cannot install into
# the com.termux shared UID (INSTALL_FAILED_SHARED_USER_INCOMPATIBLE).
cp keystore.properties_sample keystore.properties && $EDITOR keystore.properties

# JDK 21 and the Android SDK (compileSdk 35); no NDK — the app has no native code.
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME="$HOME/android-sdk"

# The fork build: signed release APK → ~/tmp/shiroikuma-termux-api_<versionName>_universal.apk,
# then BUILD_NUMBER in gradle.properties is bumped for the next build.
./gradlew buildFork --console=plain < /dev/null

# Toolchain check only (no copy, no bump):
./gradlew :app:assembleRelease --console=plain < /dev/null
# → app/build/outputs/apk/release/termux-api-app_v<versionName>+release.apk
```

`./gradlew -q versionName` prints the exact versionName the next build will carry. Verify a
build with `apksigner verify --print-certs <apk>` (family certificate SHA-256
`50b47e8f09b8781fccc998df3fc5c02de0dd9670a3d37e6cacba9f4e76319604`) and
`aapt dump badging <apk> | head -1` (package, versionCode, versionName).

---

Upstream's README follows unchanged.

---

# Termux API

[![Build status](https://github.com/termux/termux-api/workflows/Build/badge.svg)](https://github.com/termux/termux-api/actions)
[![Join the chat at https://gitter.im/termux/termux](https://badges.gitter.im/termux/termux.svg)](https://gitter.im/termux/termux)

This is an app exposing Android API to command line usage and scripts or programs.

When developing or packaging, note that this app needs to be signed with the same
key as the main Termux app for permissions to work (only the main Termux app are
allowed to call the API methods in this app).

## Installation

Latest version is `v0.53.0`.

Termux:API application can be obtained from [F-Droid](https://f-droid.org/en/packages/com.termux.api/).

Additionally we provide per-commit debug builds for those who want to try
out the latest features or test their pull request. This build can be obtained
from one of the workflow runs listed on [Github Actions](https://github.com/termux/termux-api/actions/workflows/github_action_build.yml?query=branch%3Amaster+event%3Apush)
page.

Signature keys of all offered builds are different. Before you switch the
installation source, you will have to uninstall the Termux application and
all currently installed plugins. Check https://github.com/termux/termux-app#Installation for more info.

## License

Released under the [GPLv3 license](http://www.gnu.org/licenses/gpl-3.0.en.html).

## How API calls are made through the termux-api helper binary

The [termux-api](https://github.com/termux/termux-api-package/blob/master/termux-api.c)
client binary in the `termux-api` package generates two linux anonymous namespace
sockets, and passes their address to the [TermuxApiReceiver broadcast receiver](https://github.com/termux/termux-api/blob/master/app/src/main/java/com/termux/api/TermuxApiReceiver.java)
as in:

```
/system/bin/am broadcast ${BROADCAST_RECEIVER} --es socket_input ${INPUT_SOCKET} --es socket_output ${OUTPUT_SOCKET}
```

The two sockets are used to forward stdin from `termux-api` to the relevant API
class and output from the API class to the stdout of `termux-api`.

## Client scripts

Client scripts which processes command line arguments before calling the
`termux-api` helper binary are available in the [termux-api package](https://github.com/termux/termux-api-package).

## Ideas

- Wifi network search and connect.
- Add extra permissions to the app to (un)install apps, stop processes etc.
