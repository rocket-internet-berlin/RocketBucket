package de.rocketinternet.android.bucket;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Map;

import de.rocketinternet.android.bucket.ui.BucketDetailsContract;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Unit test for the implementation of {@link BucketDetailsPresenter}
 */
@RunWith(MockitoJUnitRunner.class)
public class BucketDetailsPresenterTest {
    @Mock Context mContext;
    @Mock Application mApplication;
    BucketDetailsPresenter mPresenter;
    @Mock BucketDetailsContract.View mViewController;
    private static final String DEFAULT_APIKEY = "298492849283932";
    private static final String DEFAULT_ENDPONT = "http://bucket.com";
    Map<String, String> mResultCallbackExpirment;
    Throwable mResultCallbackThrowable;
    RocketBucket mRocketBucket;


    @Mock Intent mIntent;
    @Captor ArgumentCaptor<RocketBucketContainer> apiResultCaptor;

    @After
    public void tearDown() throws Exception {
        mPresenter = null;
    }

    /**
        assuming Editable bucket provider initialization, and assuming that all tests under this default bucket details
     */
    @Before
    public void initialize() {
        EditableBucketsProvider mockData = new EditableBucketsProvider() {
            @Override
            public void loadBuckets(Context context, BucketsContainer container) {
                container.onBucketsRetrieved(context, MockBuilder.getMockedDefaultLatestBucket(), null);
            }
        };
        mRocketBucket = new RocketBucket(DEFAULT_ENDPONT, DEFAULT_APIKEY, mockData, null);
        mRocketBucket.updateLatestBuckets(mContext);
        RocketBucket.setInstance(mRocketBucket);//for setting provider manually !
        mResultCallbackThrowable = null;
        mResultCallbackExpirment = null;
        BucketsActivity.BucketPJO defaultPjo = MockBuilder.getBucketPJO(null, null, true);
        mPresenter = new BucketDetailsPresenter(mContext, defaultPjo, mViewController);
    }

    @Test
    public void testOnUnexpectedError() throws Exception {

    }

    @Test
    public void testGetCurrentExperimentName_givenExperimentName_PresenterShouldSetTheName() throws Exception {
        assertEquals(mPresenter.getCurrentExperimentName(), MockBuilder.getDefaultExpName());
    }

    @Test
    public void testOnGetCurrentExperiment_givenCurrentExpirment_PresenterShouldHaveCurrentExpirment() throws Exception {
        assertEquals(MockBuilder.getDefaultExpName(),mPresenter.getCurrentExperiment().getName());
    }


    @Test
    public void testIsManualExpirment_firstInitialization_selectionModeShouldBeAutomaticByDefault() throws Exception {
        assertFalse(mPresenter.isManualExpirment());
    }
    @Test
    public void testOnExperimentDataReady_selectionModeAutomatic_shouldAskUIToMakeItAutomatic() throws Exception {
        verify(mViewController).updateSelectionMethod(eq(false), anyString());
    }
    @Test(expected = RuntimeException.class)
    public void testOnExperimentDataReady_whenExperimentNull_shouldThrowException() throws Exception {
        mPresenter = new BucketDetailsPresenter(mContext, new BucketsActivity.BucketPJO(null,MockBuilder.getDefaultBucket(),false), mViewController);
    }

}