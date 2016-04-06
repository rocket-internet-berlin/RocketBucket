package de.rocketinternet.android.bucket;

import android.util.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import de.rocketinternet.android.bucket.RocketBucket.Variant;

/**
 * @author Sameh Gerges
 */
class JsonParser {
    private final String JSON_ATT_EXPERIMENTS = "experiments";
    private final String JSON_ATT_NAME = "name";
    private final String JSON_ATT_BUCKET = "bucket";
    private final String JSON_ATT_DATA = "data";
    private final String JSON_ATT_VALUE = "value";

    public static Map<String, Variant> parseExperiments(InputStream jsonStream) throws IOException {
        return new JsonParser().parse(jsonStream);
    }
    /**
     * Using JsonReader is bit complicated to use and maintain but it much faster and efficient. Also current JSON structure is very simple.
     *
     * If JSON structure is getting more complicated, then it's recommend to switch to JSONObject and JSONArray
     *
     * @param jsonStream
     * @throws IOException
     */
    private Map<String, Variant> parse(InputStream jsonStream) throws IOException {
        Map<String, Variant> experimentMap = new HashMap<>();

        JsonReader reader = new JsonReader(new InputStreamReader(jsonStream, Charset.defaultCharset()));

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();

            if (JSON_ATT_EXPERIMENTS.equals(name)){
                readExperiments(reader, experimentMap);
            } else {
                reader.skipValue();
            }

        }
        reader.endObject();

        return experimentMap;
    }

    private void readExperiments(JsonReader reader, Map<String, Variant> experimentMap) throws IOException {
        reader.beginArray();
        while (reader.hasNext()){
            reader.beginObject();
            String experimentName = null;
            Variant variant = null;
            while (reader.hasNext()){
                String name = reader.nextName();
                if (JSON_ATT_NAME.equals(name)){
                    experimentName = reader.nextString();
                } else if (JSON_ATT_BUCKET.equals(name)) {
                    variant = readVariant(reader);
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();

            experimentMap.put(experimentName, variant);
        }
        reader.endArray();
    }

    private Variant readVariant(JsonReader reader) throws IOException {
        String vname = null;
        Map<String, String> data = new HashMap<>();

        reader.beginObject();
        while (reader.hasNext()){
            String name = reader.nextName();

            if (JSON_ATT_NAME.equals(name)){
                vname = reader.nextString();
            } else if (JSON_ATT_DATA.equals(name)) {
                reader.beginArray();
                while (reader.hasNext()){
                    readData(reader, data);
                }
                reader.endArray();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();

        if (vname != null){
            return new Variant(vname, data);
        }

        return null;
    }

    private void readData(JsonReader reader, Map<String, String> dataMap) throws IOException {
        String key = null, value =null;

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
}
