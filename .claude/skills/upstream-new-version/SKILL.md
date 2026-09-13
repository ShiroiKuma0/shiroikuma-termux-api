---
name: upstream-new-version
description: Sync the shiroikuma-termux-api fork onto new upstream commits of termux/termux-api master — fast-forward our master mirror, rebase custom onto it, keep BUILD_NUMBER counting (never reset), build the next +NNN. Use when 白い熊 says upstream has moved, asks to check/update/sync to upstream, or to rebase custom onto the latest Termux:API. ALWAYS present the proceed-gated upstream-changes table BEFORE rebasing.
---

# Sync shiroikuma-termux-api onto new upstream Termux:API commits

This fork tracks [termux/termux-api](https://github.com/termux/termux-api) — the Termux plugin
app behind the `termux-api` command-line package. `master` mirrors **`upstream/master`** (the
branch tip, fast-forward only); `custom` carries our patches and is rebased onto it.

**We follow the branch tip, not release tags** (`Upstream tracking: git`, 白い熊, 2026-09-13).
Termux tags a release rarely — `v0.53.0` sits far behind `master`, which carries the fixes 白い熊
actually runs — so a sync happens whenever **commits land on `upstream/master`**, and the fork
versionName pins the base commit (`<upstream>+<date>.<HH-MM>.g<sha8>+<NNN>`, global
`git-versioning` skill). Upstream's own `versionCode` literal mostly stands still across syncs,
which is exactly why `BUILD_NUMBER` is **never reset** (see step 5).

> **Never `git push` or `git commit` unprompted.** After the rebase + build you stop and let 白い熊
> test; you push only on their explicit **"Push"** (`custom` needs `--force-with-lease` after a
> rebase; `master` is a fast-forward, a plain push).

## Branch / remote model

| Branch | Role | Update mode |
| --- | --- | --- |
| `master` | Mirrors `upstream/master`. No fork work here. | `git merge --ff-only upstream/master` |
| `custom` | Our patches; the working/dev branch and the GitHub default branch. | rebased onto `master` each sync |

`origin` = `git@github.com:ShiroiKuma0/shiroikuma-termux-api.git` (push). `upstream` =
`https://github.com/termux/termux-api.git` (fetch only; push URL `DISABLED`).

## Steps

1. **Check whether upstream moved.** The trigger is `upstream/master` no longer being an ancestor
   of our `master` — not a version-literal change (it rarely moves):
   ```bash
   git fetch upstream
   git fetch origin
   if git merge-base --is-ancestor upstream/master master; then
     echo ">>> No new upstream commits. master is at or above upstream/master. Nothing to do."
   else
     old=$(git rev-parse master)      # capture BEFORE any fast-forward — the gate range starts here
     old_vn=$(git show master:app/build.gradle          | grep -oP 'versionName "\K[^"]+' | head -1)
     new_vn=$(git show upstream/master:app/build.gradle | grep -oP 'versionName "\K[^"]+' | head -1)
     old_vc=$(git show master:app/build.gradle          | grep -oP 'versionCode \K[0-9]+'  | head -1)
     new_vc=$(git show upstream/master:app/build.gradle | grep -oP 'versionCode \K[0-9]+'  | head -1)
     echo ">>> New upstream commits: $(git rev-list --count master..upstream/master); versionName ${old_vn} -> ${new_vn}, versionCode ${old_vc} -> ${new_vc}; old base ${old:0:8}"
   fi
   ```
   If nothing is newer, stop and report "already current" with the current version.

2. **⛔ PROCEED GATE — present the upstream changes as a table, then STOP.** 白い熊's standing
   requirement: **before** anything is rebased, show what the new upstream commits actually bring.

   Gather the material from all of these — they complement each other:
   ```bash
   git log --oneline --no-merges "$old"..upstream/master        # what really landed
   git log --merges --format='%s' "$old"..upstream/master       # which PRs were merged
   git log --stat --format='%n### %h  %s%n%b' "$old"..upstream/master   # full messages + files, to judge relevance
   git diff --stat "$old"..upstream/master                      # where the weight is
   git tag --contains "$old" --sort=-version:refname | head     # did a release tag land in the span?
   gh release view <tag> -R termux/termux-api                   # only when a tag landed: its notes
   ```
   Dependabot bumps (`.github/`, Gradle/AGP) get **one** row; translation-only commits likewise.

   Present a **descriptive markdown table** — one row per feature/change, in plain language, not raw
   commit subjects:

   | Area | Change | What it means for us |
   | --- | --- | --- |
   | APIs (`apis/*API.java`) | … | … |
   | Receiver / socket / keep-alive | … | … |
   | UI (activities, settings, strings) | … | … |
   | Manifest / permissions | … | … |
   | Build / deps (`termux-shared` sha, AGP, Gradle) | … | … |

   Cover new/changed/removed APIs, fixes, permission changes, and anything touching files our
   patches own — **flag those rows**, they are the likely conflict sites:
   `app/build.gradle` (the one appended `apply from:` line), `gradle.properties`, `.gitignore`,
   `app/src/main/AndroidManifest.xml`, `app/src/main/res/values/strings.xml` (the `<!ENTITY>`
   block), `app/src/main/res/drawable*/ic_launcher*`, `TermuxAPIConstants.java`, `README.md`, and
   whatever de-branding / UI-page hooks we hold in the activities. A **bump of the `termux-shared`
   sha** in upstream's `app/build.gradle` is informational for us — we build against the fork's own
   `termux-shared` from mavenLocal (`SHIROIKUMA_TERMUX_SHARED_VERSION`); when upstream's bump
   signals new shared APIs, re-publish from `~/git/shiroikuma-termux` after its own sync and bump
   the `-skN` suffix in both repos — say so. A move of **`versionCode`** in `app/build.gradle` matters for step 5.

   Also state the stack size (`git rev-list --count master..custom`) and the plan.

   **Then stop and wait for 白い熊's explicit go-ahead.** Do not move `master`, do not rebase, do not
   build until they say proceed. If they decline, nothing has been touched.

3. **Fast-forward `master`** (mirror; no fork work lives here) and **bank a safety branch**:
   ```bash
   git status --short                              # the working tree must be clean (unsandboxed!)
   git checkout master
   git merge --ff-only upstream/master             # master only ever fast-forwards
   git branch custom-pre-$(date +%Y-%m-%d) custom  # the untouched stack, in case the rebase goes wrong
   ```
   Do **not** push `master` yet — every push waits for "Push" (step 8).

4. **Rebase `custom`:**
   ```bash
   git checkout custom
   git rebase master
   ```
   Resolve conflicts so **all** our customizations survive (table below). Reconcile, don't drop: if
   upstream restructured a file we patch, port our change to the new structure rather than forcing
   the old diff. Keep **upstream's** `versionCode` / `versionName` literals — `app/shiroikuma.gradle`
   reads and overrides them, so they are never edited by hand. If upstream touched the end of
   `app/build.gradle`, our `apply from: 'shiroikuma.gradle'` line must end up **last** again
   (after `validateVersionName`). **If conflicts are significant, stop and plan with 白い熊**
   before continuing. `git rebase --abort` (or the `custom-pre-<date>` branch) is the way back.

5. **Do NOT reset the build tail.** `BUILD_NUMBER` in `gradle.properties` keeps counting — it runs
   monotonically across syncs, and `buildFork` refuses any `versionCode` at or below
   `LAST_BUILT_VERSION_CODE`. The only exception: upstream's own `versionCode` literal moved
   (`1002` → `1003`, say). Then `<new code> * 10000 + 1` already exceeds every code built on the
   old line, so `BUILD_NUMBER=1` is allowed — but never required; leaving it counting is always
   correct. **Never lower it, never "tidy" it.**

6. **Verify our customizations are intact after the rebase:**

   | What | Expected | Where |
   | --- | --- | --- |
   | Installed app id | `com.termux.api` (unchanged from upstream) | `app/build.gradle` → `defaultConfig.applicationId` |
   | Code namespace | `com.termux.api` (unchanged from upstream) | `app/build.gradle` → `namespace` |
   | sharedUserId | `com.termux` (`${TERMUX_PACKAGE_NAME}`, unchanged) | `AndroidManifest.xml`, `manifestPlaceholders` |
   | Fork script hook | `apply from: 'shiroikuma.gradle'` is the **last** line | `app/build.gradle` |
   | Fork version block | pin (`forkGit`, `upstreamPin`), `forkVersionName` / `forkVersionCode`, floor guard | `app/shiroikuma.gradle` |
   | Signing | `signingConfigs.create('release')` from `keystore.properties`, assigned to `buildTypes.release` | `app/shiroikuma.gradle` |
   | Build task | `buildFork` → `~/tmp/shiroikuma-termux-api_<versionName>_universal.apk`, bump + floor | `app/shiroikuma.gradle` |
   | Build tail | `BUILD_NUMBER` **unchanged** by the sync, `LAST_BUILT_VERSION_CODE` present | `gradle.properties` |
   | App label | `白い熊 Termux API` (once Phase 3 landed) | `TERMUX_API_APP_NAME` entity in `strings.xml` + `manifestPlaceholders` |
   | Black-yellow icon | yellow line-art on black (once Phase 2 landed) | `res/drawable/ic_launcher.xml`, `drawable-anydpi-v26/` |
   | De-branding / links | our name + `https://github.com/ShiroiKuma0/shiroikuma-termux-api` in every user-visible string and root doc (Phase 3) | `strings.xml`, `TermuxAPIConstants.java`, `README.md` |
   | 白い熊 Termux API UI | the page + Export/Import + automation rows (once Phase 4 landed) | `com/termux/api/shiroikuma/` |
   | Committed agent files | `CLAUDE.md`, `.claude/skills/` tracked; `keystore.properties`, `*.jks`, `.claude/settings.local.json`, `.scratch/` ignored | `.gitignore` |

   Watch for **new upstream strings that reintroduce "Termux:API"** or upstream's GitHub links —
   grep after the rebase and re-de-brand:
   ```bash
   grep -rn 'Termux:API\|termux/termux-api' app/src/main/res app/src/main/java README.md | grep -v 'com.termux.api' | head -40
   ```
   Sanity check the script still evaluates and prints the new pin:
   `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ANDROID_HOME=/home/shiroikuma/android-sdk ./gradlew -q versionName`
   — the `.g<sha8>` must now be the first 8 chars of `git merge-base HEAD master`.

7. **Build the next `+NNN`** via the **build-apk** skill
   (`JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ANDROID_HOME=/home/shiroikuma/android-sdk ./gradlew buildFork --console=plain < /dev/null`),
   then deliver it via the global **`/after-build`** skill (no transfer prompt). Its name carries
   the new pin and the *next* counter value, e.g. `0.53.0+<new date>.<HH-MM>.g<new sha>+007`.

8. **Stop.** Let 白い熊 test. Commit/push only on their explicit **"Push"** — then, and only then:
   ```bash
   git push origin master                        # fast-forward, safe
   git push --force-with-lease origin custom     # rebased history
   ```
   The `custom-pre-<date>` safety branch stays local; delete it only when 白い熊 says so.

## Notes

- Keep our changes a **small, legible layer** on top of upstream — prefer rebasing (linear history)
  over merging, so the customization set stays easy to audit and replay. Everything build-related
  is in the additive `app/shiroikuma.gradle`; upstream's `app/build.gradle` carries one appended line.
- **Never rename `applicationId` / `namespace` / `sharedUserId`** while resolving anything — the
  `termux-api` CLI in the prefix hardcodes `com.termux.api/.TermuxApiReceiver`.
- **Signing never changes on a sync.** The family key
  (`~/.android-keystores/shiroikuma-emacs-termux.jks`) is shared with every `com.termux` shared-UID
  member; a different key = `INSTALL_FAILED_SHARED_USER_INCOMPATIBLE`.
- Upstream's release CI is `assembleDebug` + `testkey_untrusted.jks`; ours is the release build
  type. If upstream ever adds its own `signingConfigs.release`, keep ours winning (it is assigned
  after theirs, in `shiroikuma.gradle`).

---

**Commit convention — no Claude attribution.** Never add a `Co-Authored-By: Claude …` /
"Generated with Claude" trailer to commit messages or PR bodies; end the message at the last line of
the body. This overrides the harness default. (Global rule: `~/.claude/CLAUDE.md`.)
