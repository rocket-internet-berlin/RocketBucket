package de.rocketinternet.android.bucket.core;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;

import de.rocketinternet.android.bucket.models.Bucket;
import de.rocketinternet.android.bucket.network.JsonParser;

/**
 * Created by mohamed.elawadi on 22/04/16.
 */
public final class SharedPrefManager {
    private SharedPrefManager() {

    }

    private static final String KEY_PREFIX = "ROCKET_BUCKET_";

    private static SharedPreferences getSharePreferences(Context context) {
        return android.preference.PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static void saveCustomBucket(Context context, String experimentName, Bucket bucket) {
        String bucketAsString;
        try {
            bucketAsString = JsonParser.convertBucketToJSON(bucket).toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        SharedPreferences.Editor editor = getSharePreferences(context).edit();
        editor.putString(KEY_PREFIX.concat(experimentName), bucketAsString);
        editor.apply();
    }

    public static Bucket getCustomBucket(Context context, String experimentName) {
        String bucketString = getSharePreferences(context).getString(KEY_PREFIX.concat(experimentName), null);
        if (bucketString != null) {
            try {
                return JsonParser.getBucketFromJSON(bucketString);
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }

    public static void removeBucket(Context context, String experimentName) {
        getSharePreferences(context).edit().remove(KEY_PREFIX.concat(experimentName)).apply();
    }

    public static boolean isBucketExist(Context context, String experimentName) {
        return getSharePreferences(context).contains(KEY_PREFIX.concat(experimentName));
    }
}
