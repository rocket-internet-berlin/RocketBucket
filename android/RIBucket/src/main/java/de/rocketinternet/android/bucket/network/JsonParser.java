package de.rocketinternet.android.bucket.network;

import android.support.annotation.NonNull;
import android.util.JsonReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.rocketinternet.android.bucket.models.Bucket;
import de.rocketinternet.android.bucket.models.Experiment;

/**
 * @author Sameh Gerges
 */
public class JsonParser {
    private final String JSON_ATT_EXPERIMENTS = "experiments";
    private static final String JSON_ATT_NAME = "name";
    private final String JSON_ATT_ENABLED = "enabled";
    private static final String JSON_ATT_PERCENT = "percent";
    private final String JSON_ATT_BUCKET = "bucket";
    private final String JSON_ATT_BUCKETS = "buckets";
    private static final String JSON_ATT_DATA = "data";
    private static final String JSON_ATT_VALUE = "value";

    public static Map<String, Bucket> parseExperiments(Reader jsonStreamReader) throws IOException {
        return new JsonParser().parseUserExperiment(jsonStreamReader);
    }

    public static List<Experiment> parseAllExperiments(InputStream jsonStream) throws IOException, JSONException {
        return new JsonParser().parseRocketBucketsExperiments(jsonStream);
    }

    /**
     * Using JsonReader is bit complicated to use and maintain but it much faster and efficient. Also current JSON structure is very simple.
     * <p/>
     * If JSON structure is getting more complicated, then it's recommend to switch to JSONObject and JSONArray
     *
     * @param jsonStreamReader
     * @throws IOException
     */
    private Map<String, Bucket> parseUserExperiment(Reader jsonStreamReader) throws IOException {
        Map<String, Bucket> experimentMap = new HashMap<>();


        JsonReader reader = new JsonReader(jsonStreamReader);

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();

            if (JSON_ATT_EXPERIMENTS.equals(name)) {
                readExperiments(reader, experimentMap);
            } else {
                reader.skipValue();
            }

        }
        reader.endObject();

        return experimentMap;
    }

    public List<Experiment> parseRocketBucketsExperiments(InputStream jsonStream) throws IOException, JSONException {
        List<Experiment> experiments = new ArrayList<>();//better return value than pass it by reference
        readExperiments(jsonStream, experiments);

        return experiments;
    }

    private void readExperiments(JsonReader reader, Map<String, Bucket> experimentMap) throws IOException {
        reader.beginArray();
        while (reader.hasNext()) {
            reader.beginObject();
            String experimentName = null;
            Bucket bucket = null;
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (JSON_ATT_NAME.equals(name)) {
                    experimentName = reader.nextString();
                } else if (JSON_ATT_BUCKET.equals(name)) {
                    bucket = readVariant(reader);
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();

            experimentMap.put(experimentName, bucket);
        }
        reader.endArray();
    }

    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    private void readExperiments(InputStream reader, List<Experiment> experiments) throws IOException, JSONException {

        String theString = convertStreamToString(reader);
        JSONObject jonObject = new JSONObject(theString);
        JSONArray experimentsJSONArray = jonObject.getJSONArray(JSON_ATT_EXPERIMENTS);
        for (int i = 0; i < experimentsJSONArray.length(); i++) {
            JSONObject tempJSONExperiment = (JSONObject) experimentsJSONArray.get(i);
            List<Bucket> tempBuckets = getBuckets(tempJSONExperiment);
            experiments.add(new Experiment(tempJSONExperiment.getString(JSON_ATT_NAME), tempJSONExperiment.getBoolean(JSON_ATT_ENABLED), tempBuckets));
        }
    }

    private List<Bucket> getBuckets(JSONObject tempJSONExperiment) throws JSONException {
        List<Bucket> bucketList = new ArrayList<>();
        JSONArray buckts = tempJSONExperiment.getJSONArray(JSON_ATT_BUCKETS);//check for null TODO
        for (int i = 0; i < buckts.length(); i++) {
            JSONObject singleBucket = (JSONObject) buckts.get(i);
            Map<String, String> data = getDataFromBuckt(singleBucket);
            bucketList.add(new Bucket(singleBucket.getString(JSON_ATT_NAME), singleBucket.getInt(JSON_ATT_PERCENT), data));
        }
        return bucketList;
    }

    private static Map<String, String> getDataFromBuckt(JSONObject singleBuckt) {
        Map<String, String> data = new HashMap<>();

        try {
            JSONArray dataArray = singleBuckt.getJSONArray(JSON_ATT_DATA);
            for (int i = 0; i < dataArray.length(); i++) {
                JSONObject singleData = dataArray.getJSONObject(i);
                data.put(singleData.getString(JSON_ATT_NAME), JSON_ATT_VALUE);
            }
        } catch (JSONException ex) {
            return data;
        }
        return data;
    }


    private Bucket readVariant(JsonReader reader) throws IOException {
        String vname = null;
        Map<String, String> data = new HashMap<>();

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();

            if (JSON_ATT_NAME.equals(name)) {
                vname = reader.nextString();
            } else if (JSON_ATT_DATA.equals(name)) {
                reader.beginArray();
                while (reader.hasNext()) {
                    readData(reader, data);
                }
                reader.endArray();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();

        if (vname != null) {
            return new Bucket(vname, data);
        }

        return null;
    }

    private void readData(JsonReader reader, Map<String, String> dataMap) throws IOException {
        String key = null, value = null;

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (JSON_ATT_NAME.equals(name)) {
                key = reader.nextString();
            } else if (JSON_ATT_VALUE.equals(name)) {
                value = reader.nextString();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();

        if (key != null) {
            dataMap.put(key, value);
        }
    }

    public static JSONObject convertBucketToJSON(Bucket bucket) throws JSONException {
        JSONObject bucketJSON = new JSONObject();
        bucketJSON.put(JSON_ATT_NAME, bucket.getName());
        bucketJSON.put(JSON_ATT_PERCENT, bucket.getPercent());
        if (bucket.getData() != null && bucket.getData().size() > 0) {
            bucketJSON.put(JSON_ATT_DATA, getBucketDataArray(bucket));
        }
        return bucketJSON;
    }

    private static JSONArray getBucketDataArray(Bucket bucket) throws JSONException {
        JSONArray bucketData = new JSONArray();
        for (Map.Entry<String, String> dataItem :
                bucket.getData().entrySet()) {
            bucketData.put(getDataJSONItemFromBucket(dataItem));
        }
        return bucketData;
    }

    private static JSONObject getDataJSONItemFromBucket(@NonNull Map.Entry<String, String> dataItem) throws JSONException {
        JSONObject object = new JSONObject();
        object.put(dataItem.getKey(), dataItem.getValue());
        return object;
    }

    public static Bucket getBucketFromJSON(String jsonString) throws JSONException {
        JSONObject bucketJSON = new JSONObject(jsonString);
        return new Bucket(bucketJSON.getString(JSON_ATT_NAME), bucketJSON.getInt(JSON_ATT_PERCENT), getDataFromBuckt(bucketJSON));
    }
}
