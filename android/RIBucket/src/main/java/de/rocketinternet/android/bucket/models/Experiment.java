package de.rocketinternet.android.bucket.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * Created by mohamed.elawadi on 12/04/16.
 */
public class Experiment implements Parcelable {
    String name;
    List<Bucket> buckets;
    boolean isEnabled;

    protected Experiment(Parcel in) {
        name = in.readString();
        buckets = in.createTypedArrayList(Bucket.CREATOR);
        isEnabled = in.readByte() != 0;
    }

    public static final Creator<Experiment> CREATOR = new Creator<Experiment>() {
        @Override
        public Experiment createFromParcel(Parcel in) {
            return new Experiment(in);
        }

        @Override
        public Experiment[] newArray(int size) {
            return new Experiment[size];
        }
    };

    public String getName() {
        return name;
    }


    public List<Bucket> getBuckets() {
        return buckets;
    }

    public Experiment(String experimentName, boolean isEnabled, List<Bucket> buckets) {
        this.name = experimentName;
        this.isEnabled = isEnabled;
        this.buckets = buckets;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeTypedList(buckets);
        dest.writeByte((byte) (isEnabled ? 1 : 0));
    }
}
