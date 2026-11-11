package com.antest1.kcanotify;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.icu.text.RelativeDateTimeFormatter;
import android.icu.util.ULocale;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.MenuCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import retrofit2.Response;

import static com.antest1.kcanotify.InitStartActivity.ACTION_RESET;
import static com.antest1.kcanotify.KcaConstants.KCANOTIFY_DB_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_CHECK_UPDATE_START;
import static com.antest1.kcanotify.KcaConstants.PREF_KCARESOURCE_VERSION;
import static com.antest1.kcanotify.KcaConstants.PREF_KCA_DATA_VERSION;
import static com.antest1.kcanotify.KcaDataUpdateAdapter.PROGRESS_HIDDEN;
import static com.antest1.kcanotify.KcaDataUpdateAdapter.PROGRESS_INDETERMINATE;
import static com.antest1.kcanotify.KcaDataUpdateAdapter.Row;
import static com.antest1.kcanotify.KcaDataUpdateAdapter.Trailing;
import static com.antest1.kcanotify.KcaDataUpdateAdapter.getThemeColor;
import static com.antest1.kcanotify.KcaUtils.compareVersion;
import static com.antest1.kcanotify.KcaUtils.getBooleanPreferences;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.setPreferences;

public class UpdateCheckActivity extends BaseActivity {
    private static final String TAG = "UpdateCheckActivity";
    public static final String EXTRA_MAIN_FLAG = "main_flag";
    // starts downloading all updates once the check is done
    public static final String EXTRA_AUTO_UPDATE = "auto_update";
    private static final int LOAD_DELAY = 500;
    private static final String GAME_ROW_KEY = "game";

    private enum CheckState { CHECKING, FAILED_SERVER, FAILED_OFFLINE, READY }

    private enum Category {
        TRANSLATION(R.string.data_update_cat_translation, R.drawable.ic_du_translate),
        MAP(R.string.data_update_cat_map, R.drawable.ic_du_map),
        AKASHI(R.string.data_update_cat_akashi, R.drawable.ic_du_build),
        QUEST(R.string.data_update_cat_quest, R.drawable.ic_du_quest),
        SHIP(R.string.data_update_cat_ship, R.drawable.ic_du_ship),
        EXPEDITION(R.string.data_update_cat_expedition, R.drawable.ic_du_timer),
        FAIRY(R.string.data_update_cat_fairy, R.drawable.ic_du_face),
        OTHER(R.string.data_update_cat_other, R.drawable.ic_du_folder);

        final int title;
        final int icon;

        Category(int title, int icon) {
            this.title = title;
            this.icon = icon;
        }

        static Category of(String name) {
            if (name.equals("icon_info.json")) return FAIRY;
            if (name.startsWith("akashi_")) return AKASHI;
            if (name.equals("edges.json") || name.equals("map_sub.json") || name.equals("world_list.json")) return MAP;
            if (name.equals("quest_track.json")) return QUEST;
            if (name.startsWith("ships_")) return SHIP;
            if (name.equals("expedition.json")) return EXPEDITION;
            if (KcaDataUpdateManager.isTranslationFile(name)) return TRANSLATION;
            return OTHER;
        }
    }

    private static class ResourceFile {
        final String name;
        final Category category;
        int current;
        int latest = -1; // -1: not on the server list
        String url;

        ResourceFile(String name) {
            this.name = name;
            this.category = Category.of(name);
        }
    }

    private final List<ResourceFile> files = new ArrayList<>();
    private final Set<Category> expanded = EnumSet.noneOf(Category.class);
    private final Map<String, Integer> progress = new HashMap<>();

    LinearProgressIndicator topProgress;
    RecyclerView listView;
    KcaDataUpdateAdapter adapter;
    KcaDBHelper dbHelper;
    KcaDownloader downloader;
    KcaFairyDownloader fairyDownloader;
    KcaDataUpdateManager updateManager;

    boolean mainFlag = false;
    boolean autoUpdate = false;
    CheckState checkState = CheckState.CHECKING;
    int checkId = 0;
    String latestGameDataVersion = null;
    // Last-Modified of list.json, to detect resource files served from an older CDN copy
    String listLastModified = null;

    private final KcaDataUpdateManager.Listener updateListener = new KcaDataUpdateManager.Listener() {
        @Override
        public void onItemChanged(String key, KcaDataUpdateManager.State state, int value) {
            onUpdateItemChanged(key, state, value);
        }

        @Override
        public void onFinished(KcaDataUpdateManager.Summary summary) {
            onUpdateFinished(summary);
        }
    };

    private final KcaDataUpdateAdapter.SummaryActions summaryActions = new KcaDataUpdateAdapter.SummaryActions() {
        @Override
        public void onCancel() {
            updateManager.cancelAll();
        }

        @Override
        public void onRetry() {
            if (checkState == CheckState.READY && retryFailed()) return;
            checkUpdates();
        }

        @Override
        public void onUpdateAll() {
            updateAll();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rescheck);

        Intent intent = getIntent();
        mainFlag = intent.getBooleanExtra(EXTRA_MAIN_FLAG, false);
        autoUpdate = savedInstanceState == null && intent.getBooleanExtra(EXTRA_AUTO_UPDATE, false);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.data_update_title));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        dbHelper = new KcaDBHelper(getApplicationContext(), null, KCANOTIFY_DB_VERSION);
        KcaApiData.setDBHelper(dbHelper);
        downloader = KcaUtils.getInfoDownloader(getApplicationContext());
        fairyDownloader = new KcaFairyDownloader(this);
        updateManager = KcaDataUpdateManager.getInstance(this);
        updateManager.addListener(updateListener);

        for (String name : KcaUtils.getResourceFilter(getApplicationContext())) {
            files.add(new ResourceFile(name));
        }
        loadCurrentVersions();

        topProgress = findViewById(R.id.update_progress);
        listView = findViewById(R.id.update_list);
        listView.setLayoutManager(new LinearLayoutManager(this));
        listView.setItemAnimator(null);
        adapter = new KcaDataUpdateAdapter(summaryActions);
        listView.setAdapter(adapter);

        checkUpdates();
    }

    @Override
    protected void onDestroy() {
        // downloads keep going; the manager shows one result toast when no screen is listening
        updateManager.removeListener(updateListener);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.data_update, menu);
        MenuCompat.setGroupDividerEnabled(menu, true);
        menu.findItem(R.id.action_data_check_at_start)
                .setChecked(getBooleanPreferences(getApplicationContext(), PREF_CHECK_UPDATE_START));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            goBack();
            return true;
        } else if (id == R.id.action_data_refresh) {
            checkUpdates();
            return true;
        } else if (id == R.id.action_data_check_at_start) {
            boolean checked = !item.isChecked();
            item.setChecked(checked);
            setPreferences(getApplicationContext(), PREF_CHECK_UPDATE_START, checked);
            return true;
        } else if (id == R.id.action_data_reset) {
            showResetDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mainFlag) {
            goBack();
        } else {
            super.onBackPressed();
        }
    }

    private void goBack() {
        if (mainFlag) startActivity(new Intent(getApplicationContext(), MainActivity.class));
        finish();
    }

    private void loadCurrentVersions() {
        for (ResourceFile file : files) file.current = dbHelper.getResVer(file.name);
    }

    // checks the latest game data version and the resource list on the update server
    private void checkUpdates() {
        if (updateManager.isRunning()) return; // the list is checked again after the downloads
        updateManager.clearFinishedStates();
        progress.clear();
        loadCurrentVersions();
        final int id = ++checkId;
        if (!KcaUtils.checkOnline(getApplicationContext())) {
            checkState = CheckState.FAILED_OFFLINE;
            render();
            return;
        }
        checkState = CheckState.CHECKING;
        render();

        final Context context = getApplicationContext();
        new Thread(() -> {
            String infoVersion = null;
            try {
                Response<String> response = downloader.getRecentVersion().execute();
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject info = JsonParser.parseString(response.body()).getAsJsonObject();
                    if (info.has("data_version")) infoVersion = info.get("data_version").getAsString();
                }
            } catch (Exception e) {
                Log.w(TAG, "version info: " + e);
            }
            // the update server itself is the source of truth (KCAINFO can lag behind on the CDN);
            // a version-only update (same content) is recorded here without a download
            KcaGameDataChecker.Info latest = KcaGameDataChecker.syncVersion(context, downloader);
            String gameVersion = KcaGameDataChecker.newerVersion(infoVersion, latest == null ? null : latest.version);

            JsonArray list = null;
            String lastModified = null;
            try {
                Response<String> response = downloader.getResourceList().execute();
                if (response.isSuccessful() && response.body() != null) {
                    list = JsonParser.parseString(response.body()).getAsJsonArray();
                    lastModified = response.headers().get("Last-Modified");
                }
            } catch (Exception e) {
                Log.w(TAG, "resource list: " + e);
            }

            final JsonArray resourceList = list;
            final String listModified = lastModified;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed() || id != checkId) return;
                if (gameVersion == null || resourceList == null) {
                    checkState = CheckState.FAILED_SERVER;
                    render();
                } else {
                    applyCheckResult(gameVersion, resourceList, listModified);
                }
            });
        }).start();
    }

    private void applyCheckResult(String gameVersion, JsonArray list, String lastModified) {
        latestGameDataVersion = gameVersion;
        listLastModified = lastModified;
        Map<String, JsonObject> items = new HashMap<>();
        for (JsonElement element : list) {
            if (!element.isJsonObject()) continue;
            JsonObject item = element.getAsJsonObject();
            if (item.has("name") && item.has("version") && item.has("url")) {
                items.put(item.get("name").getAsString(), item);
            }
        }
        for (ResourceFile file : files) {
            JsonObject item = items.get(file.name);
            file.latest = item != null ? item.get("version").getAsInt() : -1;
            file.url = item != null ? item.get("url").getAsString() : null;
        }
        loadCurrentVersions();
        checkState = CheckState.READY;
        KcaDataUpdateManager.saveCheckResult(getApplicationContext(),
                (isGameDataOutdated() ? 1 : 0) + countOutdatedFiles());
        render();

        if (autoUpdate) {
            autoUpdate = false;
            updateAll();
        }
    }

    private String getCurrentGameDataVersion() {
        return getStringPreferences(getApplicationContext(), PREF_KCA_DATA_VERSION);
    }

    private boolean isGameDataOutdated() {
        if (checkState != CheckState.READY || !KcaGameDataChecker.isVersion(latestGameDataVersion)) return false;
        String current = getCurrentGameDataVersion();
        return !KcaGameDataChecker.isVersion(current) || !compareVersion(current, latestGameDataVersion);
    }

    private boolean isOutdated(ResourceFile file) {
        return checkState == CheckState.READY && file.url != null && file.latest > file.current;
    }

    private int countOutdatedFiles() {
        int count = 0;
        for (ResourceFile file : files) if (isOutdated(file)) count += 1;
        return count;
    }

    private void enqueueGameData() {
        if (isGameDataOutdated()) updateManager.enqueueGameData(latestGameDataVersion);
        render();
    }

    private void enqueueFiles(List<ResourceFile> targets) {
        for (ResourceFile file : targets) {
            if (isOutdated(file)) updateManager.enqueueResource(file.name, file.url, file.latest, listLastModified);
        }
        render();
    }

    private void updateAll() {
        if (isGameDataOutdated()) updateManager.enqueueGameData(latestGameDataVersion);
        enqueueFiles(files);
    }

    // downloads the failed ones again; false if nothing failed
    private boolean retryFailed() {
        boolean retried = false;
        if (isGameDataOutdated() && isFailed(KcaDataUpdateManager.GAME_DATA_KEY)) {
            updateManager.enqueueGameData(latestGameDataVersion);
            retried = true;
        }
        List<ResourceFile> targets = new ArrayList<>();
        for (ResourceFile file : files) {
            if (isOutdated(file) && isFailed(file.name)) targets.add(file);
        }
        if (!targets.isEmpty()) {
            enqueueFiles(targets);
            retried = true;
        }
        render();
        return retried;
    }

    private boolean isFailed(String key) {
        KcaDataUpdateManager.State state = updateManager.getState(key);
        return state == KcaDataUpdateManager.State.FAILED || state == KcaDataUpdateManager.State.NOT_READY;
    }

    private void onUpdateItemChanged(String key, KcaDataUpdateManager.State state, int value) {
        if (state == KcaDataUpdateManager.State.DOWNLOADING) {
            progress.put(key, value);
        } else {
            progress.remove(key);
        }
        if (state == KcaDataUpdateManager.State.DONE) {
            for (ResourceFile file : files) {
                if (file.name.equals(key)) file.current = dbHelper.getResVer(key);
            }
            if (key.equals("icon_info.json")) {
                Toast.makeText(getApplicationContext(), "Retrieving Fairy Images..", Toast.LENGTH_LONG).show();
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (!isFinishing() && !isDestroyed()) fairyDownloader.run(false);
                }, LOAD_DELAY);
            }
        }
        render();
    }

    private void onUpdateFinished(KcaDataUpdateManager.Summary summary) {
        render();
        String message = KcaDataUpdateManager.getSummaryText(getApplicationContext(), summary);
        if (message == null) return;
        Snackbar snackbar = Snackbar.make(findViewById(R.id.update_root), message, Snackbar.LENGTH_LONG);
        if (summary.failed + summary.notReady > 0) {
            snackbar.setAction(R.string.data_update_retry, v -> retryFailed());
        }
        snackbar.show();
    }

    private void render() {
        List<Row> rows = new ArrayList<>();
        rows.add(Row.summary(buildSummary()));
        boolean ready = checkState == CheckState.READY;
        if (ready) {
            rows.add(Row.header("game", getString(R.string.data_update_section_game)));
            rows.add(buildGameRow());
            rows.add(Row.header("resource", getString(R.string.data_update_section_resource)));
        } else {
            // the latest versions are not known: shows what is installed
            rows.add(Row.header("installed", getString(R.string.data_update_section_installed)));
            rows.add(buildGameRow());
        }
        for (Category category : Category.values()) {
            List<ResourceFile> targets = new ArrayList<>();
            for (ResourceFile file : files) if (file.category == category) targets.add(file);
            if (targets.isEmpty()) continue;
            rows.add(buildCategoryRow(category, targets));
            if (targets.size() > 1 && expanded.contains(category)) {
                for (ResourceFile file : targets) rows.add(buildFileRow(file));
            }
        }
        adapter.setRows(rows);

        int barProgress = PROGRESS_HIDDEN;
        if (checkState == CheckState.CHECKING) {
            barProgress = PROGRESS_INDETERMINATE;
        } else if (updateManager.isRunning()) {
            barProgress = getBatchProgress();
        }
        KcaDataUpdateAdapter.setProgress(topProgress, barProgress);
    }

    // 0-100 over the downloads requested since the queue was last empty
    private int getBatchProgress() {
        List<String> batch = updateManager.getBatchKeys();
        if (batch.isEmpty()) return PROGRESS_INDETERMINATE;
        int total = 0;
        for (String key : batch) {
            if (isFinished(updateManager.getState(key))) {
                total += 100;
            } else {
                Integer value = progress.get(key);
                if (value != null && value > 0) total += value;
            }
        }
        // nothing to show yet (e.g. the game data, whose size is not known)
        if (total == 0) return PROGRESS_INDETERMINATE;
        return total / batch.size();
    }

    private static boolean isFinished(KcaDataUpdateManager.State state) {
        return state == null || state == KcaDataUpdateManager.State.DONE
                || state == KcaDataUpdateManager.State.FAILED || state == KcaDataUpdateManager.State.NOT_READY;
    }

    private KcaDataUpdateAdapter.Summary buildSummary() {
        KcaDataUpdateAdapter.Summary summary = new KcaDataUpdateAdapter.Summary();
        long lastCheck = KcaDataUpdateManager.getLastCheckTime(getApplicationContext());
        String lastChecked = lastCheck > 0
                ? KcaUtils.format(getString(R.string.data_update_last_checked), getRelativeTime(lastCheck)) : "";
        switch (checkState) {
            case CHECKING:
                summary.title = getString(R.string.data_update_checking);
                summary.body = lastChecked;
                return summary;
            case FAILED_SERVER:
            case FAILED_OFFLINE:
                summary.icon = R.drawable.ic_du_error;
                summary.iconTint = androidx.appcompat.R.attr.colorError;
                summary.title = getString(R.string.data_update_check_failed);
                summary.body = joinLines(getString(checkState == CheckState.FAILED_OFFLINE
                        ? R.string.data_update_check_failed_offline
                        : R.string.data_update_check_failed_server), lastChecked);
                summary.retry = true;
                return summary;
            default:
                break;
        }

        List<String> batch = updateManager.getBatchKeys();
        if (updateManager.isRunning() && !batch.isEmpty()) {
            int finished = 0;
            for (String key : batch) if (isFinished(updateManager.getState(key))) finished += 1;
            summary.style = KcaDataUpdateAdapter.CardStyle.PROGRESS;
            summary.title = KcaUtils.format(getString(R.string.data_update_in_progress), finished, batch.size());
            summary.body = getString(R.string.data_update_in_progress_hint);
            summary.progress = getBatchProgress();
            summary.cancel = true;
            return summary;
        }

        int gameCount = isGameDataOutdated() ? 1 : 0;
        int fileCount = countOutdatedFiles();
        int done = 0, failed = 0, notReady = 0;
        List<String> keys = new ArrayList<>();
        keys.add(KcaDataUpdateManager.GAME_DATA_KEY);
        for (ResourceFile file : files) keys.add(file.name);
        for (String key : keys) {
            KcaDataUpdateManager.State state = updateManager.getState(key);
            boolean outdated = key.equals(KcaDataUpdateManager.GAME_DATA_KEY) ? gameCount > 0 : isOutdated(findFile(key));
            if (state == KcaDataUpdateManager.State.DONE) done += 1;
            else if (state == KcaDataUpdateManager.State.FAILED && outdated) failed += 1;
            else if (state == KcaDataUpdateManager.State.NOT_READY && outdated) notReady += 1;
        }

        if (failed > 0) {
            summary.style = KcaDataUpdateAdapter.CardStyle.ERROR;
            summary.icon = R.drawable.ic_du_error;
            summary.iconTint = androidx.appcompat.R.attr.colorError;
            summary.title = KcaUtils.format(getString(R.string.data_update_failed_title), failed + notReady);
            summary.body = KcaDataUpdateManager.getSummaryText(getApplicationContext(),
                    new KcaDataUpdateManager.Summary(done, failed, notReady));
            summary.retry = true;
        } else if (notReady > 0) {
            summary.icon = R.drawable.ic_du_timer;
            summary.iconTint = com.google.android.material.R.attr.colorOnSurfaceVariant;
            summary.title = getString(R.string.data_update_not_ready_title);
            summary.body = getString(R.string.data_update_not_ready_hint);
            summary.retry = true;
        } else if (gameCount + fileCount > 0) {
            summary.title = KcaUtils.format(getString(R.string.data_update_available), gameCount + fileCount);
            summary.body = joinLines(KcaUtils.format(getString(R.string.data_update_available_detail),
                    gameCount, fileCount), lastChecked);
            summary.updateAll = true;
        } else {
            summary.icon = R.drawable.ic_du_check_circle;
            summary.iconSuccess = true;
            summary.title = getString(R.string.data_update_all_latest);
            summary.body = lastChecked;
        }
        return summary;
    }

    private ResourceFile findFile(String name) {
        for (ResourceFile file : files) if (file.name.equals(name)) return file;
        return null;
    }

    private Row buildGameRow() {
        String current = getCurrentGameDataVersion();
        if (current == null || current.isEmpty()) current = "-";
        boolean outdated = isGameDataOutdated();
        String supporting = outdated ? current + " → " + latestGameDataVersion : current;
        Row row = Row.entry(GAME_ROW_KEY, R.drawable.ic_du_storage, getString(R.string.data_update_cat_game), supporting)
                .noExpandSpace();
        if (checkState == CheckState.READY) {
            Trailing trailing = getTrailing(KcaDataUpdateManager.GAME_DATA_KEY, outdated);
            row.trailing(trailing, PROGRESS_INDETERMINATE, isActionable(trailing) ? this::enqueueGameData : null);
            if (trailing == Trailing.DOWNLOADING) row.progress(PROGRESS_INDETERMINATE);
        }
        return row;
    }

    private Row buildCategoryRow(Category category, List<ResourceFile> targets) {
        boolean ready = checkState == CheckState.READY;
        int outdatedCount = 0;
        int latestInstalled = 0;
        ResourceFile downloading = null;
        Trailing trailing = Trailing.LATEST;
        for (ResourceFile file : targets) {
            boolean outdated = isOutdated(file);
            if (outdated) outdatedCount += 1;
            latestInstalled = Math.max(latestInstalled, file.current);
            Trailing fileTrailing = getTrailing(file.name, outdated);
            if (fileTrailing == Trailing.DOWNLOADING) downloading = file;
            if (getPriority(fileTrailing) > getPriority(trailing)) trailing = fileTrailing;
        }

        String supporting;
        if (downloading != null) {
            supporting = downloading.name;
        } else if (targets.size() == 1) {
            supporting = getVersionText(targets.get(0));
        } else if (outdatedCount > 0) {
            supporting = KcaUtils.format(getString(R.string.data_update_files_outdated), outdatedCount, targets.size());
        } else {
            supporting = formatResourceVersion(latestInstalled);
        }

        Row row = Row.entry("category:" + category.name(), category.icon, getString(category.title), supporting);
        if (targets.size() > 1) {
            row.expandable(expanded.contains(category), () -> {
                if (!expanded.remove(category)) expanded.add(category);
                render();
            });
        }
        if (ready) {
            int value = getProgress(downloading);
            row.trailing(trailing, value, isActionable(trailing) ? () -> enqueueFiles(targets) : null);
            if (trailing == Trailing.DOWNLOADING) row.progress(value);
        }
        return row;
    }

    private Row buildFileRow(ResourceFile file) {
        Row row = Row.file("file:" + file.name, file.name, getVersionText(file));
        if (checkState == CheckState.READY) {
            Trailing trailing = getTrailing(file.name, isOutdated(file));
            List<ResourceFile> target = new ArrayList<>();
            target.add(file);
            row.trailing(trailing, getProgress(file), isActionable(trailing) ? () -> enqueueFiles(target) : null);
        }
        return row;
    }

    private Trailing getTrailing(String key, boolean outdated) {
        KcaDataUpdateManager.State state = updateManager.getState(key);
        if (state == KcaDataUpdateManager.State.DOWNLOADING) return Trailing.DOWNLOADING;
        if (state == KcaDataUpdateManager.State.QUEUED) return Trailing.QUEUED;
        if (outdated) {
            if (state == KcaDataUpdateManager.State.FAILED) return Trailing.FAILED;
            if (state == KcaDataUpdateManager.State.NOT_READY) return Trailing.NOT_READY;
            return Trailing.UPDATE;
        }
        return state == KcaDataUpdateManager.State.DONE ? Trailing.DONE : Trailing.LATEST;
    }

    // the state shown for a category: the most urgent one of its files
    private static int getPriority(Trailing trailing) {
        switch (trailing) {
            case DOWNLOADING: return 6;
            case QUEUED: return 5;
            case FAILED: return 4;
            case NOT_READY: return 3;
            case UPDATE: return 2;
            case DONE: return 1;
            default: return 0;
        }
    }

    private static boolean isActionable(Trailing trailing) {
        return trailing == Trailing.UPDATE || trailing == Trailing.FAILED || trailing == Trailing.NOT_READY;
    }

    private int getProgress(ResourceFile file) {
        if (file == null) return PROGRESS_INDETERMINATE;
        Integer value = progress.get(file.name);
        return value == null || value < 0 ? PROGRESS_INDETERMINATE : value;
    }

    private String getVersionText(ResourceFile file) {
        String current = formatResourceVersion(file.current);
        if (!isOutdated(file)) return current;
        String latest = formatResourceVersion(file.latest);
        if (current.equals(latest)) {
            // updated again on the same day: only the revision (NN of yyyyMMddNN) differs
            return KcaUtils.format("%s (rev %d → %d)", current, file.current % 100, file.latest % 100);
        }
        return current + " → " + latest;
    }

    // resource versions are yyyyMMddNN
    private static String formatResourceVersion(int version) {
        if (version <= 0) return "-";
        String value = String.valueOf(version);
        if (value.length() != 10) return value;
        return value.substring(0, 4) + "-" + value.substring(4, 6) + "-" + value.substring(6, 8);
    }

    private static String joinLines(String first, String second) {
        if (second == null || second.isEmpty()) return first;
        return first + "\n" + second;
    }

    private String getRelativeTime(long time) {
        Locale locale = LocaleUtils.getLocale();
        if (locale == null) locale = getResources().getConfiguration().getLocales().get(0);
        RelativeDateTimeFormatter formatter = RelativeDateTimeFormatter.getInstance(ULocale.forLocale(locale));
        long diff = Math.max(0, System.currentTimeMillis() - time);
        if (diff < DateUtils.MINUTE_IN_MILLIS) {
            return formatter.format(RelativeDateTimeFormatter.Direction.PLAIN, RelativeDateTimeFormatter.AbsoluteUnit.NOW);
        } else if (diff < DateUtils.HOUR_IN_MILLIS) {
            return formatter.format(diff / DateUtils.MINUTE_IN_MILLIS,
                    RelativeDateTimeFormatter.Direction.LAST, RelativeDateTimeFormatter.RelativeUnit.MINUTES);
        } else if (diff < DateUtils.DAY_IN_MILLIS) {
            return formatter.format(diff / DateUtils.HOUR_IN_MILLIS,
                    RelativeDateTimeFormatter.Direction.LAST, RelativeDateTimeFormatter.RelativeUnit.HOURS);
        }
        return formatter.format(diff / DateUtils.DAY_IN_MILLIS,
                RelativeDateTimeFormatter.Direction.LAST, RelativeDateTimeFormatter.RelativeUnit.DAYS);
    }

    private void showResetDialog() {
        float density = getResources().getDisplayMetrics().density;
        MaterialCheckBox downloadAfter = new MaterialCheckBox(this);
        downloadAfter.setText(R.string.data_reset_download_after);
        downloadAfter.setChecked(KcaUtils.checkOnline(getApplicationContext()));
        FrameLayout container = new FrameLayout(this);
        container.setPadding((int) (18 * density), (int) (8 * density), (int) (24 * density), 0);
        container.addView(downloadAfter);

        Drawable icon = AppCompatResources.getDrawable(this, R.drawable.ic_backup_restore);
        if (icon != null) {
            icon = icon.mutate();
            icon.setTint(getThemeColor(this, com.google.android.material.R.attr.colorSecondary));
        }
        AlertDialog dialog = new MaterialAlertDialogBuilder(this,
                com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
                .setIcon(icon)
                .setTitle(R.string.data_reset_title)
                .setMessage(R.string.data_reset_message)
                .setView(container)
                .setNegativeButton(R.string.dialog_cancel, null)
                .setPositiveButton(R.string.data_reset_confirm, (d, which) -> resetData(downloadAfter.isChecked()))
                .show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(getThemeColor(this, androidx.appcompat.R.attr.colorError));
    }

    private void resetData(boolean updateAfter) {
        updateManager.cancelAll();
        resetDownloadedData(getApplicationContext(), dbHelper);
        Intent intent = new Intent(this, InitStartActivity.class);
        intent.putExtra(ACTION_RESET, true);
        intent.putExtra(InitStartActivity.EXTRA_UPDATE_AFTER_RESET, updateAfter);
        startActivity(intent);
        finish();
    }

    // Remove downloaded resources and game data, so that InitStartActivity (ACTION_RESET) restores
    // the bundled ones. Fairy images (separate folder), logs and settings are kept.
    private static void resetDownloadedData(Context context, KcaDBHelper helper) {
        File[] dataFiles = context.getDir("data", Context.MODE_PRIVATE).listFiles();
        if (dataFiles != null) {
            for (File file : dataFiles) {
                if (file.isFile()) file.delete();
            }
        }
        helper.clearResVer();
        setPreferences(context, PREF_KCARESOURCE_VERSION, 0);
        KcaGameDataStore.delete(context);
        setPreferences(context, PREF_KCA_DATA_VERSION, "");
    }
}
