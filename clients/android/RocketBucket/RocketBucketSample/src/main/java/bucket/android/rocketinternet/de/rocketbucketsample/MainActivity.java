package bucket.android.rocketinternet.de.rocketbucketsample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import de.rocketinternet.android.bucket.RocketBucket;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RocketBucket.initialize(this, "http://url.com/", "apkikey", null, false);

    }
}
