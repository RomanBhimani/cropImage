package com.rd.cropimagesingallery.cropImage;

import android.graphics.Rect;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public final class KeyboardCropImage {
  public static final String CROP_IMAGE_EXTRA_OPTIONS = "CROP_IMAGE_EXTRA_OPTIONS";
  public static final String CROP_IMAGE_EXTRA_BUNDLE = "CROP_IMAGE_EXTRA_BUNDLE";

  private KeyboardCropImage() {}
  public static final class ActivityResult extends KeyboardCropImageView.CropResult implements Parcelable {
    public static final Creator<ActivityResult> CREATOR =
        new Creator<ActivityResult>() {
          @Override
          public ActivityResult createFromParcel(Parcel in) {
            return new ActivityResult(in);
          }

          @Override
          public ActivityResult[] newArray(int size) {
            return new ActivityResult[size];
          }
        };
    protected ActivityResult(Parcel in) {
      super(
          null,
          (Uri) in.readParcelable(Uri.class.getClassLoader()),
          null,
          (Uri) in.readParcelable(Uri.class.getClassLoader()),
          (Exception) in.readSerializable(),
          in.createFloatArray(),
          (Rect) in.readParcelable(Rect.class.getClassLoader()),
          (Rect) in.readParcelable(Rect.class.getClassLoader()),
          in.readInt(),
          in.readInt());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
      dest.writeParcelable(imageGetOriginalUri(), flags);
      dest.writeParcelable(imageGetUri(), flags);
      dest.writeSerializable(imageGetError());
      dest.writeFloatArray(imageGetCropPoints());
      dest.writeParcelable(imageGetCropRect(), flags);
      dest.writeParcelable(imageGetWholeImageRect(), flags);
      dest.writeInt(imageGetRotation());
      dest.writeInt(imageGetSampleSize());
    }
    @Override
    public int describeContents() {
      return 0;
    }
  }
}
