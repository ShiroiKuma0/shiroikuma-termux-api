package com.termux.api.shiroikuma.backup;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.documentfile.provider.DocumentFile;

import com.termux.api.R;
import com.termux.api.shiroikuma.ShiroikumaConstants;
import com.termux.shared.termux.TermuxConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * The Export / Import engine of 白い熊 Termux API — the category ZIP the UI page writes and the
 * automation contract triggers, in the family's shared shape (ported from raikidoban's
 * {@code RkbExport} / ArcaneChat's {@code ShiroikumaExport}).
 *
 * <h3>What this app has to back up</h3>
 *
 * <p>Termux:API keeps no user data of its own — it is a broadcast-driven bridge from the
 * {@code termux-api} CLI to Android APIs. Its whole persistent state is <b>one</b> SharedPreferences
 * file, {@code com.termux.api_preferences} (the log level, written through termux-shared's
 * {@code TermuxAPIAppSharedPreferences}), so the ZIP carries a single category, {@link Cat#SETTINGS}:
 * a type-tagged dump of every SharedPreferences file under {@code shared_prefs/} except the two
 * device-local ones this fork adds ({@link #PREFS_EXIMPORT}, holding the export directory, and
 * {@code shiroikuma_automation}, holding the automation switch and token — see
 * {@link #EXCLUDED_FILES}). {@link #EXCLUDED_KEYS} drops {@code last_pending_intent_request_code}:
 * a monotonic per-device counter whose restore could only ever hand a live PendingIntent's request
 * code to a new one.
 *
 * <h3>ZIP layout</h3>
 *
 * <pre>
 * manifest.json   {"format":"shiroikuma-termux-api","version":1,"app":"com.termux.api",
 *                  "appVersion":"…","createdTs":…,"categories":["settings"]}
 * settings.json   {"&lt;prefs file&gt;":{"&lt;key&gt;":{"t":"int|long|float|bool|string|set","v":…}}}
 * </pre>
 *
 * <p>Import is a per-key <b>merge</b> with {@code commit()} — never a clear — restricted to the
 * categories present in the archive, and it reports per-category counts.
 *
 * <h3>Atomic writes</h3>
 *
 * <p>Every archive is written as {@code <name>.part} and renamed to its final name only once the
 * ZIP is closed and complete; any failure or cancellation deletes the partial in the same
 * {@code finally}. A killed export otherwise leaves a file indistinguishable from a real backup
 * until someone restores it — and 白い熊 keeps every app's backups in one directory sorted by date.
 */
public final class ShiroikumaExport {

    /** The archive format tag in {@code manifest.json}; an archive without it is not ours. */
    public static final String FORMAT = ShiroikumaConstants.EXPORT_SLUG;
    /** Bumped when an older build could no longer read what we write. */
    public static final int VERSION = 1;

    /** Family-wide backup-name convention: {@code shiroikuma-termux-api_<yyyy-MM-dd_HH-mm-ss>.zip}. */
    public static final String EXPORT_PREFIX = ShiroikumaConstants.EXPORT_SLUG + "_";
    /** An archive in flight; never counted as a backup, never offered for import. */
    public static final String PART_SUFFIX = ".part";

    public static final String MANIFEST_ENTRY = "manifest.json";
    public static final String SETTINGS_ENTRY = "settings.json";

    /** Device-local prefs holding the export directory (a SAF tree Uri); deliberately never exported. */
    public static final String PREFS_EXIMPORT = "shiroikuma_eximport";
    private static final String KEY_DIR_URI = "dir_uri";

    /** The app's own preferences file — {@code com.termux.api_preferences}. */
    public static final String PRIMARY_PREFS = TermuxConstants.TERMUX_API_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION;

    /**
     * SharedPreferences files that must never travel: this fork's two device-local files (the
     * automation token would otherwise sit in every backup; a SAF grant cannot be restored anyway),
     * plus androidx.preference's defaults marker and the WebView's own cache prefs.
     */
    private static final Set<String> EXCLUDED_FILES = new HashSet<>();
    /** Keys that are device state, not settings — see the class comment. */
    private static final Set<String> EXCLUDED_KEYS = new HashSet<>();

    static {
        EXCLUDED_FILES.add(PREFS_EXIMPORT);
        EXCLUDED_FILES.add("shiroikuma_automation");
        EXCLUDED_FILES.add("_has_set_default_values");
        EXCLUDED_FILES.add("WebViewChromiumPrefs");
        EXCLUDED_KEYS.add("last_pending_intent_request_code");
    }

    private static final String EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents";

    /** One export at a time, process-wide — released in a {@code finally}, never persisted. */
    private static final AtomicBoolean EXPORT_RUNNING = new AtomicBoolean(false);

    private ShiroikumaExport() {
    }

    // ---------------------------------------------------------------------------------------------
    // categories
    // ---------------------------------------------------------------------------------------------

    /**
     * The selectable categories. {@code id} is the ZIP entry name (without {@code .json}) and the
     * id accepted in the automation {@code items} extra; {@code defaultSelected} is what
     * {@code LIST_CATEGORIES} reports as its fourth field and what the panel starts ticked with.
     */
    public enum Cat {
        SETTINGS("settings", R.string.shiroikuma_eim_cat_settings, null, true);

        public final String id;
        @StringRes
        public final int labelRes;
        /** The parent's id for a sub-option, null for a top-level category. */
        @Nullable
        public final String parentId;
        public final boolean defaultSelected;

        Cat(String id, @StringRes int labelRes, @Nullable String parentId, boolean defaultSelected) {
            this.id = id;
            this.labelRes = labelRes;
            this.parentId = parentId;
            this.defaultSelected = defaultSelected;
        }

        @Nullable
        public static Cat byId(@Nullable String id) {
            if (id == null) return null;
            for (Cat c : values()) {
                if (c.id.equals(id)) return c;
            }
            return null;
        }

        public static Set<Cat> all() {
            Set<Cat> out = new LinkedHashSet<>();
            Collections.addAll(out, values());
            return out;
        }

        /** The default set — the ones reported {@code on}; what {@code items} absent means. */
        public static Set<Cat> defaults() {
            Set<Cat> out = new LinkedHashSet<>();
            for (Cat c : values()) {
                if (c.defaultSelected) out.add(c);
            }
            return out;
        }
    }

    /** Real counts, never a percentage: {@code done} is the POSITION of the category being written. */
    public interface Progress {
        void onProgress(int done, int total, String categoryLabel);
    }

    /** Polled between ZIP entries — never mid-write — so a cancel unwinds at the next boundary. */
    public interface Cancel {
        boolean isCancelled();
    }

    /** Thrown out of {@link #export} once a cancel has been seen; the caller deletes its partial. */
    public static class CancelledException extends IOException {
        public CancelledException() {
            super("cancelled");
        }
    }

    /** What an export wrote: the path to show, and the real byte length of the finished file. */
    public static final class Written {
        public final String path;
        public final long bytes;

        Written(String path, long bytes) {
            this.path = path;
            this.bytes = bytes;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // file name + export directory
    // ---------------------------------------------------------------------------------------------

    /** The name of the ZIP to write now — identical for the UI panel and the automation doors. */
    public static String exportFileName() {
        return EXPORT_PREFIX
                + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ROOT).format(new Date())
                + ".zip";
    }

    /** True if this is one of our finished backups (the shared directory also holds sister apps'). */
    public static boolean isExportFileName(@Nullable String name) {
        return name != null && name.startsWith(EXPORT_PREFIX) && name.endsWith(".zip");
    }

    private static SharedPreferences eximportPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_EXIMPORT, Context.MODE_PRIVATE);
    }

    /** The tree Uri 白い熊 last chose, grant or no grant — so the picker can open right there. */
    @Nullable
    public static Uri exportDirUri(Context context) {
        String raw = eximportPrefs(context).getString(KEY_DIR_URI, null);
        if (raw == null || raw.isEmpty()) return null;
        try {
            return Uri.parse(raw);
        } catch (Exception e) {
            return null;
        }
    }

    /** Persist a tree picked with {@code ACTION_OPEN_DOCUMENT_TREE}, with its read+write grant. */
    public static void setExportDirUri(Context context, @Nullable Uri uri) {
        if (uri != null) {
            try {
                context.getContentResolver().takePersistableUriPermission(uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                | android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            } catch (Exception e) {
                // Some providers refuse a persistable grant; the session grant still works for now.
            }
        }
        // commit(): the gate and the directory are tiny, infrequent writes that must reach disk.
        eximportPrefs(context).edit().putString(KEY_DIR_URI, uri == null ? null : uri.toString()).commit();
    }

    /** The configured export directory, or null when unset / no longer reachable. */
    @Nullable
    public static DocumentFile exportDir(Context context) {
        Uri uri = exportDirUri(context);
        if (uri == null) return null;
        try {
            DocumentFile dir = DocumentFile.fromTreeUri(context, uri);
            return (dir != null && dir.isDirectory()) ? dir : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Best-effort real filesystem path of a SAF tree (primary storage only), so the page and the
     * automation reply show an absolute path rather than a bare folder label; null otherwise.
     */
    @Nullable
    public static String absolutePathOf(@Nullable Uri treeUri, @Nullable String fileName) {
        if (treeUri == null || !EXTERNAL_STORAGE_AUTHORITY.equals(treeUri.getAuthority())) return null;
        String docId;
        try {
            docId = DocumentsContract.getTreeDocumentId(treeUri);
        } catch (Exception e) {
            return null;
        }
        if (docId == null || !docId.startsWith("primary:")) return null;
        String rel = docId.substring("primary:".length());
        while (rel.startsWith("/")) rel = rel.substring(1);
        while (rel.endsWith("/")) rel = rel.substring(0, rel.length() - 1);
        String base = Environment.getExternalStorageDirectory().getAbsolutePath();
        String path = rel.isEmpty() ? base : base + "/" + rel;
        return fileName == null ? path : path + "/" + fileName;
    }

    /** The directory as shown on the page: its absolute path when resolvable, its name otherwise, null when unset. */
    @Nullable
    public static String dirLabel(Context context) {
        DocumentFile dir = exportDir(context);
        if (dir == null) return null;
        String abs = absolutePathOf(dir.getUri(), null);
        return abs != null ? abs : dir.getName();
    }

    /** Our newest finished backup in the configured directory, or null. Lists the directory — call off the main thread. */
    @Nullable
    public static DocumentFile newestExport(Context context) {
        DocumentFile dir = exportDir(context);
        if (dir == null) return null;
        DocumentFile newest = null;
        try {
            for (DocumentFile f : dir.listFiles()) {
                if (f.isFile() && isExportFileName(f.getName())
                        && (newest == null || f.lastModified() > newest.lastModified())) {
                    newest = f;
                }
            }
        } catch (Exception e) {
            return null;
        }
        return newest;
    }

    public static String humanSize(long bytes) {
        if (bytes >= (1L << 30)) return String.format(Locale.ROOT, "%.2f GB", bytes / (double) (1L << 30));
        if (bytes >= (1L << 20)) return String.format(Locale.ROOT, "%.1f MB", bytes / (double) (1L << 20));
        if (bytes >= (1L << 10)) return String.format(Locale.ROOT, "%.1f KB", bytes / (double) (1L << 10));
        return bytes + " B";
    }

    // ---------------------------------------------------------------------------------------------
    // EXPORT
    // ---------------------------------------------------------------------------------------------

    /**
     * Write a ZIP of the selected categories to {@code out} (which the caller closes). This is the
     * headless core: the panel, the broadcast receiver and the data-door service are thin callers.
     *
     * @throws CancelledException when {@code cancel} said stop at an entry boundary
     * @throws IllegalStateException when another export is already running
     */
    public static void export(@NonNull Context context, @NonNull Set<Cat> cats, @NonNull OutputStream out,
                              @Nullable Progress progress, @Nullable Cancel cancel) throws IOException {
        if (!EXPORT_RUNNING.compareAndSet(false, true)) {
            throw new IllegalStateException("export already running");
        }
        try {
            Context app = context.getApplicationContext();
            List<Cat> ordered = new ArrayList<>();
            for (Cat c : Cat.values()) {
                if (cats.contains(c)) ordered.add(c);
            }
            ZipOutputStream zip = new ZipOutputStream(out);
            boolean complete = false;
            try {
                writeEntry(zip, MANIFEST_ENTRY, manifest(app, ordered));
                int total = ordered.size();
                int n = 0;
                for (Cat cat : ordered) {
                    throwIfCancelled(cancel);
                    n++;
                    if (progress != null) progress.onProgress(n, total, app.getString(cat.labelRes));
                    if (cat == Cat.SETTINGS) {
                        writeEntry(zip, SETTINGS_ENTRY, dumpAllPrefs(app).toString(2));
                    }
                }
                throwIfCancelled(cancel);
                // The end-of-central-directory record is what tells a complete archive from a
                // truncated one; it is written here, and only a stream that got here is renamed.
                zip.finish();
                zip.flush();
                complete = true;
            } catch (JSONException e) {
                throw new IOException("cannot serialise: " + e.getMessage(), e);
            } finally {
                if (complete) {
                    zip.close(); // releases the deflater; closes `out` too, which the caller may close again harmlessly
                } else {
                    try {
                        zip.close();
                    } catch (IOException ignored) {
                        // the stream may already be broken; the caller deletes the partial anyway
                    }
                }
            }
        } finally {
            EXPORT_RUNNING.set(false);
        }
    }

    /**
     * Write {@code <name>.part} into a SAF directory, then rename it to the final name. The partial
     * is deleted on every path that does not end in a finished, renamed archive.
     */
    @NonNull
    public static Written exportToDirectory(@NonNull Context context, @NonNull DocumentFile dir, @NonNull Set<Cat> cats,
                                            @Nullable Progress progress, @Nullable Cancel cancel) throws IOException {
        String name = exportFileName();
        DocumentFile partial = dir.createFile("application/octet-stream", name + PART_SUFFIX);
        if (partial == null) throw new IOException("cannot create " + name + PART_SUFFIX + " in the export directory");
        boolean done = false;
        try {
            OutputStream os = context.getContentResolver().openOutputStream(partial.getUri(), "w");
            if (os == null) throw new IOException("cannot open " + name + PART_SUFFIX + " for writing");
            try {
                export(context, cats, os, progress, cancel);
                os.flush();
            } finally {
                os.close();
            }
            DocumentFile finished = renameOrCopy(context, dir, partial, name);
            done = true;
            long bytes = finished.length();
            String abs = absolutePathOf(dir.getUri(), finished.getName() == null ? name : finished.getName());
            return new Written(abs != null ? abs : (dir.getName() + "/" + name), bytes);
        } finally {
            if (!done) deleteQuietly(partial);
        }
    }

    /**
     * {@code renameTo} is the one SAF verb a provider may refuse; when it does, the finished bytes
     * are copied under the final name and the partial removed, so the archive is never lost to a
     * naming quirk. The rename path is the normal one on the external-storage provider.
     */
    private static DocumentFile renameOrCopy(Context context, DocumentFile dir, DocumentFile partial, String name)
            throws IOException {
        boolean renamed;
        try {
            renamed = partial.renameTo(name);
        } catch (Exception e) {
            renamed = false;
        }
        if (renamed && name.equals(partial.getName())) {
            return partial;
        }
        DocumentFile target = dir.createFile("application/zip", name);
        if (target == null) throw new IOException("cannot create " + name + " in the export directory");
        boolean copied = false;
        try {
            InputStream in = context.getContentResolver().openInputStream(partial.getUri());
            OutputStream out = context.getContentResolver().openOutputStream(target.getUri(), "w");
            if (in == null || out == null) throw new IOException("cannot copy " + name + " into place");
            try {
                byte[] buf = new byte[64 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                out.flush();
            } finally {
                in.close();
                out.close();
            }
            copied = true;
        } finally {
            if (!copied) deleteQuietly(target);
        }
        deleteQuietly(partial);
        return target;
    }

    /**
     * The plain-{@code File} twin, for an automation {@code path} extra this app is allowed to
     * write (All-files access granted — the app declares {@code MANAGE_EXTERNAL_STORAGE}).
     */
    @NonNull
    public static Written exportToFile(@NonNull Context context, @NonNull File dir, @NonNull Set<Cat> cats,
                                       @Nullable Progress progress, @Nullable Cancel cancel) throws IOException {
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        if (!dir.isDirectory()) throw new IOException("not a directory: " + dir.getAbsolutePath());
        String name = exportFileName();
        File target = new File(dir, name);
        File partial = new File(dir, name + PART_SUFFIX);
        boolean done = false;
        try {
            OutputStream os = new FileOutputStream(partial);
            try {
                export(context, cats, os, progress, cancel);
                os.flush();
            } finally {
                os.close();
            }
            if (!partial.renameTo(target)) {
                throw new IOException("cannot rename " + partial.getName() + " to " + target.getName());
            }
            done = true;
            return new Written(target.getAbsolutePath(), target.length());
        } finally {
            if (!done) //noinspection ResultOfMethodCallIgnored
                partial.delete();
        }
    }

    private static String manifest(Context app, List<Cat> cats) throws JSONException {
        JSONArray ids = new JSONArray();
        for (Cat c : cats) ids.put(c.id);
        String versionName = "";
        try {
            PackageInfo info = app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
            versionName = info.versionName == null ? "" : info.versionName;
        } catch (Exception ignored) {
            // a manifest without a version is still a usable manifest
        }
        return new JSONObject()
                .put("format", FORMAT)
                .put("version", VERSION)
                .put("app", app.getPackageName())
                .put("appVersion", versionName)
                .put("createdTs", System.currentTimeMillis())
                .put("categories", ids)
                .toString(2);
    }

    private static void writeEntry(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static void throwIfCancelled(@Nullable Cancel cancel) throws CancelledException {
        if (cancel != null && cancel.isCancelled()) throw new CancelledException();
    }

    private static void deleteQuietly(@Nullable DocumentFile doc) {
        try {
            if (doc != null) doc.delete();
        } catch (Exception ignored) {
            // nothing useful is left to do about it
        }
    }

    // ---------------------------------------------------------------------------------------------
    // prefs (type-tagged, every SharedPreferences file of the app)
    // ---------------------------------------------------------------------------------------------

    /**
     * The SharedPreferences files to carry: the app's own file always (so the schema is stable even
     * before it exists), plus whatever else sits in {@code shared_prefs/}, minus the excluded ones.
     */
    @NonNull
    public static List<String> prefsFiles(@NonNull Context context) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        names.add(PRIMARY_PREFS);
        File dir = new File(context.getApplicationContext().getDataDir(), "shared_prefs");
        File[] files = dir.listFiles();
        if (files != null) {
            List<String> found = new ArrayList<>();
            for (File f : files) {
                String n = f.getName();
                if (f.isFile() && n.endsWith(".xml")) found.add(n.substring(0, n.length() - 4));
            }
            Collections.sort(found);
            names.addAll(found);
        }
        List<String> out = new ArrayList<>();
        for (String n : names) {
            if (!EXCLUDED_FILES.contains(n)) out.add(n);
        }
        return out;
    }

    private static JSONObject dumpAllPrefs(Context app) throws JSONException {
        JSONObject obj = new JSONObject();
        for (String name : prefsFiles(app)) {
            obj.put(name, dumpPrefs(app.getSharedPreferences(name, Context.MODE_PRIVATE)));
        }
        return obj;
    }

    /** Every key with a type tag: {@code {"t":"int|long|float|bool|string|set","v":…}}. */
    private static JSONObject dumpPrefs(SharedPreferences sp) throws JSONException {
        JSONObject obj = new JSONObject();
        for (Map.Entry<String, ?> e : sp.getAll().entrySet()) {
            String key = e.getKey();
            if (EXCLUDED_KEYS.contains(key)) continue;
            Object v = e.getValue();
            JSONObject entry = new JSONObject();
            if (v instanceof Boolean) {
                entry.put("t", "bool").put("v", v);
            } else if (v instanceof Integer) {
                entry.put("t", "int").put("v", v);
            } else if (v instanceof Long) {
                entry.put("t", "long").put("v", v);
            } else if (v instanceof Float) {
                entry.put("t", "float").put("v", ((Float) v).doubleValue());
            } else if (v instanceof String) {
                entry.put("t", "string").put("v", v);
            } else if (v instanceof Set) {
                JSONArray a = new JSONArray();
                for (Object s : (Set<?>) v) a.put(String.valueOf(s));
                entry.put("t", "set").put("v", a);
            } else {
                continue;
            }
            obj.put(key, entry);
        }
        return obj;
    }

    /**
     * Merge a {@code settings.json} back in, file by file, key by key — never a wipe. The excluded
     * files and keys are skipped on the way in too, so an archive cannot smuggle an automation
     * token or a foreign SAF grant into this install. Returns {@code files:keys} counts.
     */
    private static int[] mergeAllPrefs(Context app, String json) throws JSONException {
        JSONObject obj = new JSONObject(json);
        int files = 0;
        int keys = 0;
        for (Iterator<String> it = obj.keys(); it.hasNext(); ) {
            String name = it.next();
            if (EXCLUDED_FILES.contains(name) || name.isEmpty() || name.contains("/")) continue;
            JSONObject dump = obj.optJSONObject(name);
            if (dump == null) continue;
            int n = mergePrefs(app.getSharedPreferences(name, Context.MODE_PRIVATE), dump);
            files++;
            keys += n;
        }
        return new int[]{files, keys};
    }

    private static int mergePrefs(SharedPreferences sp, JSONObject dump) {
        int n = 0;
        SharedPreferences.Editor editor = sp.edit();
        for (Iterator<String> it = dump.keys(); it.hasNext(); ) {
            String key = it.next();
            if (EXCLUDED_KEYS.contains(key)) continue;
            JSONObject entry = dump.optJSONObject(key);
            if (entry == null) continue;
            switch (entry.optString("t")) {
                case "bool":
                    editor.putBoolean(key, entry.optBoolean("v"));
                    break;
                case "int":
                    editor.putInt(key, entry.optInt("v"));
                    break;
                case "long":
                    editor.putLong(key, entry.optLong("v"));
                    break;
                case "float":
                    editor.putFloat(key, (float) entry.optDouble("v"));
                    break;
                case "string":
                    editor.putString(key, entry.optString("v"));
                    break;
                case "set": {
                    JSONArray a = entry.optJSONArray("v");
                    Set<String> set = new HashSet<>();
                    if (a != null) for (int i = 0; i < a.length(); i++) set.add(a.optString(i));
                    editor.putStringSet(key, set);
                    break;
                }
                default:
                    continue;
            }
            n++;
        }
        // commit(), NOT apply(): the data door answers OK the moment the import returns, and
        // 応用管理 then force-stops this app with a SIGKILL — an apply() still in flight would be
        // lost and the restore would report success over keys that never reached disk. Both
        // callers run off the main thread, so the synchronous write costs nothing that matters.
        editor.commit();
        return n;
    }

    // ---------------------------------------------------------------------------------------------
    // INSPECT + IMPORT
    // ---------------------------------------------------------------------------------------------

    /** The category ids an archive actually carries — empty if it is not one of ours. */
    @NonNull
    public static List<String> categoriesIn(@NonNull File file) {
        List<String> out = new ArrayList<>();
        try {
            ZipFile zip = new ZipFile(file);
            try {
                out.addAll(categoriesIn(zip));
            } finally {
                zip.close();
            }
        } catch (Exception ignored) {
            // not a zip, or not ours
        }
        return out;
    }

    @NonNull
    public static List<String> categoriesIn(@NonNull ZipFile zip) {
        List<String> out = new ArrayList<>();
        try {
            String manifest = readEntry(zip, MANIFEST_ENTRY);
            if (manifest == null) return out;
            JSONObject m = new JSONObject(manifest);
            if (!FORMAT.equals(m.optString("format"))) return out;
            for (Cat c : Cat.values()) {
                if (zip.getEntry(c.id + ".json") != null) out.add(c.id);
            }
        } catch (Exception ignored) {
            // a malformed manifest is "not ours"
        }
        return out;
    }

    /**
     * Apply the selected categories from an archive on disk; categories missing from it are
     * skipped. Returns the per-category summary, one line each.
     *
     * @throws IOException when the file is not one of our archives, or nothing in it was selected
     */
    @NonNull
    public static String importZip(@NonNull Context context, @NonNull File file, @NonNull Set<Cat> cats)
            throws IOException {
        Context app = context.getApplicationContext();
        ZipFile zip = new ZipFile(file);
        try {
            List<String> present = categoriesIn(zip);
            if (present.isEmpty()) throw new IOException(app.getString(R.string.shiroikuma_eim_import_none));
            StringBuilder summary = new StringBuilder();
            boolean any = false;
            for (Cat cat : Cat.values()) {
                if (!cats.contains(cat) || !present.contains(cat.id)) continue;
                String line;
                if (cat == Cat.SETTINGS) {
                    String json = readEntry(zip, SETTINGS_ENTRY);
                    if (json == null) continue;
                    int[] counts;
                    try {
                        counts = mergeAllPrefs(app, json);
                    } catch (JSONException e) {
                        throw new IOException("settings.json: " + e.getMessage(), e);
                    }
                    line = app.getString(R.string.shiroikuma_eim_settings_result, counts[1], counts[0]);
                } else {
                    continue;
                }
                any = true;
                if (summary.length() > 0) summary.append('\n');
                summary.append(app.getString(cat.labelRes)).append(": ").append(line);
            }
            if (!any) throw new IOException(app.getString(R.string.shiroikuma_eim_import_nothing));
            return summary.toString();
        } finally {
            zip.close();
        }
    }

    @Nullable
    private static String readEntry(ZipFile zip, String name) throws IOException {
        ZipEntry entry = zip.getEntry(name);
        if (entry == null) return null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        InputStream in = zip.getInputStream(entry);
        try {
            byte[] buf = new byte[16 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
        } finally {
            in.close();
        }
        return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }
}
