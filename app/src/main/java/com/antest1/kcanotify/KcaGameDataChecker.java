package com.antest1.kcanotify;

import android.content.Context;

import retrofit2.Response;

import static com.antest1.kcanotify.KcaConstants.PREF_KCA_DATA_VERSION;
import static com.antest1.kcanotify.KcaUtils.compareVersion;
import static com.antest1.kcanotify.KcaUtils.getStringPreferences;
import static com.antest1.kcanotify.KcaUtils.setPreferences;

// Game data (api_start2) versions on the update server. The server keeps a file for every version,
// and X-Api-Hash is the md5 of the stored file, so the same hash means the same content.
// The methods here make network requests: call them off the main thread.
public class KcaGameDataChecker {
    public static final String HEADER_VERSION = "X-Api-Version";
    public static final String HEADER_HASH = "X-Api-Hash";

    public static class Info {
        public final String version;
        public final String hash;

        Info(String version, String hash) {
            this.version = version;
            this.hash = hash;
        }
    }

    // version and hash of the given version ("recent" for the latest), or null if not available
    public static Info getInfo(KcaDownloader downloader, String version) {
        try {
            Response<Void> response = downloader.getGameDataInfo(version).execute();
            if (response.code() != 200) return null;
            String serverVersion = response.headers().get(HEADER_VERSION);
            if (!isVersion(serverVersion)) return null;
            return new Info(serverVersion.trim(), response.headers().get(HEADER_HASH));
        } catch (Exception e) {
            return null;
        }
    }

    // Returns the latest game data on the server, or null if it cannot be checked. When only the
    // version changed (same content as the recorded version), records the latest version, so no
    // download is needed.
    public static Info syncVersion(Context context, KcaDownloader downloader) {
        Info latest = getInfo(downloader, "recent");
        if (latest == null) return null;
        String current = getStringPreferences(context, PREF_KCA_DATA_VERSION);
        if (!isVersion(current) || compareVersion(current, latest.version)) return latest;
        if (latest.hash == null || latest.hash.isEmpty()) return latest;
        Info recorded = getInfo(downloader, current);
        if (recorded != null && latest.hash.equals(recorded.hash)) {
            setPreferences(context, PREF_KCA_DATA_VERSION, latest.version);
        }
        return latest;
    }

    // the newer of two versions; either may be null
    public static String newerVersion(String a, String b) {
        if (!isVersion(a)) return isVersion(b) ? b.trim() : null;
        if (!isVersion(b)) return a.trim();
        return compareVersion(a.trim(), b.trim()) ? a.trim() : b.trim();
    }

    public static boolean isVersion(String value) {
        // each part fits in an int, as compareVersion parses them
        return value != null && value.trim().matches("\\d{1,9}(\\.\\d{1,9})+");
    }
}
