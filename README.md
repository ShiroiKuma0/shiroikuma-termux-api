# 白い熊 Termux API

白い熊's fork of [Termux:API](https://github.com/termux/termux-api), the Termux plugin app that
exposes Android APIs (camera, clipboard, notifications, sensors, SMS, TTS, …) to the `termux-api`
command-line package.

- **App id `com.termux.api` — unchanged.** The `termux-api` CLI hardcodes it, so the fork installs
  **over** the stock Termux:API (same id, same shared UID `com.termux`, higher versionCode). It is
  signed with the one key of the whole 白い熊 Termux family
  ([白い熊 Termux](https://github.com/ShiroiKuma0/shiroikuma-termux) and its plugins), which is what
  the shared UID requires.
- **What changes:** the label (**白い熊 Termux API**), the black-yellow icon, the links the app
  shows (this fork and the 白い熊 Termux fork instead of upstream's repos), the release signing key,
  and — in a coming version — a 白い熊 Termux API settings page. Everything else is upstream,
  rebased onto every upstream `master` commit; the version pins that commit
  (`0.53.0+<base date>.<HH-MM>.g<sha8>+<build>`).
- **Builds:** [Releases](https://github.com/ShiroiKuma0/shiroikuma-termux-api/releases) ·
  [Issues](https://github.com/ShiroiKuma0/shiroikuma-termux-api/issues).

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
