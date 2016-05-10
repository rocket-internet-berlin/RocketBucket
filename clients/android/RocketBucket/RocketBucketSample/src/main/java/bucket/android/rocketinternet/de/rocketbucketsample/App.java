package bucket.android.rocketinternet.de.rocketbucketsample;

import android.app.Application;

import de.rocketinternet.android.bucket.RocketBucket;

/**
 * Created by mohamed.elawadi on 09/05/16.
 */
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        RocketBucket.initialize(this, "http://10.24.18.45:8080/split", "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ", null, true);
    }
}
