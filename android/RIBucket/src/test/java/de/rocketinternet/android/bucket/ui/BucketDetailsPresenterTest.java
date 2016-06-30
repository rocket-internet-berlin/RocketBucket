package de.rocketinternet.android.bucket.ui;

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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.Map;

import de.rocketinternet.android.bucket.RocketBucket;
import de.rocketinternet.android.bucket.RocketBucketContainer;
import de.rocketinternet.android.bucket.core.Config;
import de.rocketinternet.android.bucket.core.EditableBucketsProvider;
import de.rocketinternet.android.bucket.models.Bucket;
import de.rocketinternet.android.bucket.network.BucketService;
import de.rocketinternet.android.bucket.test.MockBuilder;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for the implementation of {@link BucketDetailsPresenter}
 */
@RunWith(MockitoJUnitRunner.class)
public class BucketDetailsPresenterTest {


    @Mock Context context;
    @Mock Application application;
    @Mock BucketService bucketService;
    @Mock BucketDetailsContract.View viewController;
    @Mock RocketBucket rocketBucket;
    @Mock EditableBucketsProvider bucketsProvider;

    BucketsActivity.BucketPJO defaultPjo;
    Map<String, String> resultCallbackExpirment;
    Throwable resultCallbackThrowable;


    BucketDetailsPresenter presenter;

    @Mock Intent intent;
    @Captor ArgumentCaptor<RocketBucketContainer> apiResultCaptor;

    @After
    public void tearDown() throws Exception {
        presenter = null;
    }

    /**
        assuming Editable bucket provider initialization, and assuming that all tests under this default bucket details
     */
    @Before
    public void initialize() {
        when(bucketsProvider.getBucket(anyString())).thenAnswer(new Answer<Bucket>() {
            @Override
            public Bucket answer(InvocationOnMock invocation) throws Throwable {
                String bucketName = (String) invocation.getArguments()[0];
                return MockBuilder.getMockedDefaultLatestBucket().get(bucketName);
            }
        });

        Config config = new Config.Builder()
                .apiKey(MockBuilder.getApiKey())
                .endpoint(MockBuilder.getEndpoint())
                .debugMode(true)
                .build();


        resultCallbackThrowable = null;
        resultCallbackExpirment = null;
        defaultPjo = MockBuilder.getBucketPJO(null, null, true);

        presenter = new BucketDetailsPresenter(context, defaultPjo, viewController, bucketsProvider);
    }

    @Test
    public void testOnUnexpectedError() throws Exception {
    }


    @Test
    public void testPresentDisplayCorrectValues() throws Exception {
        assertEquals(defaultPjo.getExperimentName(), presenter.getCurrentExperiment().getName());
        assertEquals(defaultPjo.getExperimentName(), presenter.getCurrentExperimentName());
        assertEquals(defaultPjo.isAutomatic(), !presenter.isManualExpirment());
    }

    @Test
    public void testOnExperimentDataReady_selectionModeAutomatic_shouldAskUIToMakeItAutomatic() throws Exception {
        verify(viewController).updateSelectionMethod(eq(false), anyString());
    }
    @Test(expected = RuntimeException.class)
    public void testOnExperimentDataReady_whenExperimentNull_shouldThrowException() throws Exception {
        //presenter = new BucketDetailsPresenter(context, new BucketsActivity.BucketPJO(null,MockBuilder.getDefaultBucket(),false), viewController);
    }

}