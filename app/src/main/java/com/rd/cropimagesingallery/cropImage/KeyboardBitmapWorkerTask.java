package com.rd.cropimagesingallery.cropImage;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import java.lang.ref.WeakReference;
final class KeyboardBitmapWorkerTask extends AsyncTask<Void, Void, KeyboardBitmapWorkerTask.Result> {
  private final WeakReference<KeyboardCropImageView> mCropImageViewReference;
  private final Uri mUri;

  private final Context mContext;
  private final int mWidth;
  private final int mHeight;

  public KeyboardBitmapWorkerTask(KeyboardCropImageView cropImageView, Uri uri) {
    mUri = uri;
    mCropImageViewReference = new WeakReference<>(cropImageView);

    mContext = cropImageView.getContext();

    DisplayMetrics metrics = cropImageView.getResources().getDisplayMetrics();
    double densityAdj = metrics.density > 1 ? 1 / metrics.density : 1;
    mWidth = (int) (metrics.widthPixels * densityAdj);
    mHeight = (int) (metrics.heightPixels * densityAdj);
  }
  public Uri imageGetUri() {
    return mUri;
  }
  @Override
  protected Result doInBackground(Void... params) {
    try {
      if (!isCancelled()) {

        KeyboardBitmapUtils.imageBitmapSampled decodeResult =
            KeyboardBitmapUtils.imageDecodeSampledBitmap(mContext, mUri, mWidth, mHeight);

        if (!isCancelled()) {

          KeyboardBitmapUtils.imageRotateBitmapResult rotateResult =
              KeyboardBitmapUtils.imageRotateBitmapByExif(decodeResult.bitmap, mContext, mUri);

          return new Result(
              mUri, rotateResult.bitmap, decodeResult.sampleSize, rotateResult.degrees);
        }
      }
      return null;
    } catch (Exception e) {
      return new Result(mUri, e);
    }
  }
  @Override
  protected void onPostExecute(Result result) {
    if (result != null) {
      boolean completeCalled = false;
      if (!isCancelled()) {
        KeyboardCropImageView cropImageView = mCropImageViewReference.get();
        if (cropImageView != null) {
          completeCalled = true;
          cropImageView.imageOnSetImageUriAsyncComplete(result);
        }
      }
      if (!completeCalled && result.bitmap != null) {
        result.bitmap.recycle();
      }
    }
  }
  public static final class Result {
    public final Uri uri;
    public final Bitmap bitmap;
    public final int loadSampleSize;
    public final int degreesRotated;
    public final Exception error;

    Result(Uri uri, Bitmap bitmap, int loadSampleSize, int degreesRotated) {
      this.uri = uri;
      this.bitmap = bitmap;
      this.loadSampleSize = loadSampleSize;
      this.degreesRotated = degreesRotated;
      this.error = null;
    }

    Result(Uri uri, Exception error) {
      this.uri = uri;
      this.bitmap = null;
      this.loadSampleSize = 0;
      this.degreesRotated = 0;
      this.error = error;
    }
  }
}
