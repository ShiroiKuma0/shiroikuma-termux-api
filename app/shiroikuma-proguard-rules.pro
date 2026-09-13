# shiroikuma-termux-api fork (Phase 4): rules for our own layer, applied from shiroikuma.gradle.
# The page XML (res/xml/preferences_shiroikuma_ui.xml) instantiates this Preference by class name;
# aapt2 normally emits the keep rule itself, this makes it explicit.
-keep class com.termux.api.shiroikuma.ui.AutomationTokenPreference { <init>(...); }
