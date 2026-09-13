package com.termux.api.shiroikuma;

import androidx.annotation.NonNull;

import com.termux.shared.markdown.MarkdownUtils;
import com.termux.shared.termux.TermuxConstants;

/**
 * 白い熊 Termux API — the fork's user-visible identity.
 *
 * Everything the app shows or links out to under its own name (toolbar title, error notification
 * titles, toasts, the main page's GitHub links, the About page) reads these instead of the upstream
 * values in {@link TermuxConstants}, which arrive unchanged from the JitPack {@code termux-shared}
 * library and cannot be overridden there. Package name, shared user id, log tags, intent actions
 * and every other internal identifier stay upstream's.
 *
 * The resource-side twins are the {@code TERMUX_APP_NAME} / {@code TERMUX_API_APP_NAME} entities in
 * {@code res/values/strings.xml}; keep the two in step.
 */
public final class ShiroikumaConstants {

    /** Label of this app (replaces {@link TermuxConstants#TERMUX_API_APP_NAME}). */
    public static final String APP_NAME = "白い熊 Termux API";

    /** Label of the host app, our fork of Termux (replaces {@link TermuxConstants#TERMUX_APP_NAME}). */
    public static final String TERMUX_APP_NAME = "白い熊 Termux";

    /** This fork's GitHub repo (replaces {@link TermuxConstants#TERMUX_API_GITHUB_REPO_URL}). */
    public static final String GITHUB_REPO_URL = "https://github.com/ShiroiKuma0/shiroikuma-termux-api";
    public static final String GITHUB_ISSUES_REPO_URL = GITHUB_REPO_URL + "/issues";
    public static final String GITHUB_RELEASES_URL = GITHUB_REPO_URL + "/releases";

    /** The host app's fork (replaces {@link TermuxConstants#TERMUX_GITHUB_REPO_URL}). */
    public static final String TERMUX_GITHUB_REPO_URL = "https://github.com/ShiroiKuma0/shiroikuma-termux";

    private ShiroikumaConstants() {}

    /**
     * The About page's "Important Links" — upstream's
     * {@code TermuxUtils.getImportantLinksMarkdownString()} with the Termux and Termux:API rows
     * pointing at the 白い熊 forks (plus this fork's releases and issues). The upstream plugins we
     * do not fork, the packages repo, email, reddit and the wiki stay as upstream lists them: they
     * are the manual and the community, not branding.
     */
    @NonNull
    public static String getImportantLinksMarkdownString() {
        StringBuilder markdownString = new StringBuilder();

        markdownString.append("## Important Links");

        markdownString.append("\n\n### GitHub\n");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(TERMUX_APP_NAME, TERMUX_GITHUB_REPO_URL)).append("  ");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(APP_NAME, GITHUB_REPO_URL)).append("  ");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(APP_NAME + " releases", GITHUB_RELEASES_URL)).append("  ");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(APP_NAME + " issues", GITHUB_ISSUES_REPO_URL)).append("  ");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(TermuxConstants.TERMUX_BOOT_APP_NAME, TermuxConstants.TERMUX_BOOT_GITHUB_REPO_URL)).append("  ");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(TermuxConstants.TERMUX_FLOAT_APP_NAME, TermuxConstants.TERMUX_FLOAT_GITHUB_REPO_URL)).append("  ");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(TermuxConstants.TERMUX_STYLING_APP_NAME, TermuxConstants.TERMUX_STYLING_GITHUB_REPO_URL)).append("  ");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(TermuxConstants.TERMUX_TASKER_APP_NAME, TermuxConstants.TERMUX_TASKER_GITHUB_REPO_URL)).append("  ");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(TermuxConstants.TERMUX_WIDGET_APP_NAME, TermuxConstants.TERMUX_WIDGET_GITHUB_REPO_URL)).append("  ");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(TermuxConstants.TERMUX_PACKAGES_GITHUB_REPO_NAME, TermuxConstants.TERMUX_PACKAGES_GITHUB_REPO_URL)).append("  ");

        markdownString.append("\n\n### Email\n");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(TermuxConstants.TERMUX_SUPPORT_EMAIL_URL, TermuxConstants.TERMUX_SUPPORT_EMAIL_MAILTO_URL)).append("  ");

        markdownString.append("\n\n### Reddit\n");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(TermuxConstants.TERMUX_REDDIT_SUBREDDIT, TermuxConstants.TERMUX_REDDIT_SUBREDDIT_URL)).append("  ");

        markdownString.append("\n\n### Wiki\n");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(TermuxConstants.TERMUX_WIKI, TermuxConstants.TERMUX_WIKI_URL)).append("  ");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(TermuxConstants.TERMUX_APP_NAME, TermuxConstants.TERMUX_GITHUB_WIKI_REPO_URL)).append("  ");
        markdownString.append("\n").append(MarkdownUtils.getLinkMarkdownString(TermuxConstants.TERMUX_PACKAGES_GITHUB_REPO_NAME, TermuxConstants.TERMUX_PACKAGES_GITHUB_WIKI_REPO_URL)).append("  ");

        markdownString.append("\n##\n");

        return markdownString.toString();
    }

}
