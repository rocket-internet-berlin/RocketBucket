package bucket.android.rocketinternet.de.rocketbucketsample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import de.rocketinternet.android.bucket.RocketBucket;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String bucketA_OR_B = RocketBucket.getBucketName("customer_support_tool_integration");//usually bucket name is enough to decide to make the decision
        //Optional Extra info about the bucket
        String buttonColor = RocketBucket.getExtraByName("checkout button colors", "color", "#FFFFF");
        String sectionVisibility = RocketBucket.getExtraByName("ExperimentName2", "SectionVisibility", "false");
    }
}
