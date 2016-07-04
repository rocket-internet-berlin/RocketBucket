package bucket.android.rocketinternet.de.rocketbucketsample;

import android.app.Application;

import java.util.concurrent.TimeUnit;

import de.rocketinternet.android.bucket.Config;
import de.rocketinternet.android.bucket.RocketBucket;
import de.rocketinternet.android.bucket.RocketBucketContainer;

/**
 * Created by mohamed.elawadi on 09/05/16.
 */
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        /**
         url which client can call, this provided by RocketBucket server side for more info @url https://github.com/rocket-internet-berlin/RocketBucket
         in this example we used local host IP which running Bucket Server .
         */
        String endpoint = "http://10.24.18.45:8080/split";
        /**
         * provided by RocketBucket server for more info https://github.com/rocket-internet-berlin/RocketBucket
         */
        String apiKey = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        /**
         * boolean value to indicate whither or not to show debugging view on different activities in order to mannually test different buckets
          while running the app without server code change
         */
        boolean isDebug = true;
        /**
         * (optional) call back to be notified when request successfully served by backend
         */
        RocketBucketContainer callback = null;
        Config config = new Config.Builder()
                            .apiKey(apiKey)
                                .endpoint(endpoint)
                             .blockAppTillExperimentsLoaded(2, TimeUnit.SECONDS)
                              .debugMode(isDebug)
                              .build();
        RocketBucket.initialize(this, config, callback);
    }
}
