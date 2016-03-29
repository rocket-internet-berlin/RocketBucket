package de.rocketinternet.android.bucket;

import java.util.Map;

/**
 * @author Sameh Gerges
 */
public interface RocketBucketContainer {
    /**
     * Method is called whenever any unexpected error is caught. This is to enable apps reporting errors to external systems like crittercism, flurry, etc...
     * @param t
     */
    void onUnexpectedError(Throwable t);


    void onExperimentDataReady(Map<String, String> activeExperiments);
}
