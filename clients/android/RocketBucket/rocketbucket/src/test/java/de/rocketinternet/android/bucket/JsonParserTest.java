package de.rocketinternet.android.bucket;

import android.content.Context;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

import de.rocketinternet.android.bucket.models.Bucket;
import de.rocketinternet.android.bucket.network.JsonParser;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * Created by mohamed.elawadi on 22/04/16.
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonParserTest {
    @Mock Context mContext;

    @Test
    public void testParseExperiments() throws Exception {

    }

    @Test
    public void testParseAllExperiments() throws Exception {

    }

    @Test
    public void testParseRocketBucketsExperiments() throws Exception {

    }

    @Test
    public void testConvertStreamToString() throws Exception {

    }

    @Test
    public void testConvertBucketToJSON() throws Exception {

    }

    @Test
    public void getBucketFromJSON_withValidBucket_shouldReturnValidJSON() throws Exception {
        Bucket bucket = new Bucket(MockBuilder.getDefaultBucketName(), 100, Collections.EMPTY_MAP);
        JSONObject jsonObject = JsonParser.convertBucketToJSON(bucket);
        assertEquals(jsonObject.get("name"), MockBuilder.getDefaultBucketName());
        assertEquals(jsonObject.get("percent"), 100);
        assertNotNull(jsonObject.get("data"));
    }
}