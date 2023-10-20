package com.rd.cropimagesingallery.cropImage;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import com.rd.cropimagesingallery.R;
import java.lang.ref.WeakReference;
import java.util.UUID;

public class KeyboardCropImageView extends FrameLayout {
  private final ImageView mImageView;
  private final KeyboardCropOverlayView mCropOverlayView;
  private final Matrix mImageMatrix = new Matrix();
  private final Matrix mImageInverseMatrix = new Matrix();
  private final ProgressBar mProgressBar;
  private final float[] mImagePoints = new float[8];
  private final float[] mScaleImagePoints = new float[8];
  private KeyboardCropImageAnimation mAnimation;
  private Bitmap mBitmap;
  private int mInitialDegreesRotated;
  private int mDegreesRotated;
  private boolean mFlipHorizontally;
  private boolean mFlipVertically;
  private int mLayoutWidth;
  private int mLayoutHeight;
  private int mImageResource;
  private imageScaleType mScaleType;
  private boolean mSaveBitmapToInstanceState = false;
  private boolean mShowCropOverlay = true;
  private boolean mShowProgressBar = true;
  private boolean mAutoZoomEnabled = true;
  private int mMaxZoom;
  private imageOnSetCropOverlayReleasedListener mOnCropOverlayReleasedListener;
  private imageOnSetCropOverlayMovedListener mOnSetCropOverlayMovedListener;
  private imageOnSetCropWindowChangeListener mOnSetCropWindowChangeListener;
  private imageOnSetImageUriCompleteListener mOnSetImageUriCompleteListener;
  private imageOnCropImageCompleteListener mOnCropImageCompleteListener;
  private Uri mLoadedImageUri;
  private int mLoadedSampleSize = 1;
  private float mZoom = 1;
  private float mZoomOffsetX;
  private float mZoomOffsetY;
  private RectF mRestoreCropWindowRect;
  private int mRestoreDegreesRotated;
  private boolean mSizeChanged;
  private Uri mSaveInstanceStateBitmapUri;
  private WeakReference<KeyboardBitmapWorkerTask> mBitmapLoadingWorkerTask;
  private WeakReference<KeyboardBitmapTask> mBitmapCroppingWorkerTask;
  public KeyboardCropImageView(Context context) {
    this(context, null);
  }

  public KeyboardCropImageView(Context context, AttributeSet attrs) {
    super(context, attrs);
    KeyboardCropImageOptions options = null;
    Intent intent = context instanceof Activity ? ((Activity) context).getIntent() : null;
    if (intent != null) {
      Bundle bundle = intent.getBundleExtra(KeyboardCropImage.CROP_IMAGE_EXTRA_BUNDLE);
      if (bundle != null) {
        options = bundle.getParcelable(KeyboardCropImage.CROP_IMAGE_EXTRA_OPTIONS);
      }
    }

    if (options == null) {
      options = new KeyboardCropImageOptions();
      if (attrs != null) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.CropImageView, 0, 0);
        try {
          options.fixAspectRatio =
              ta.getBoolean(R.styleable.CropImageView_cropFixAspectRatio, options.fixAspectRatio);
          options.aspectRatioX =
              ta.getInteger(R.styleable.CropImageView_cropAspectRatioX, options.aspectRatioX);
          options.aspectRatioY =
              ta.getInteger(R.styleable.CropImageView_cropAspectRatioY, options.aspectRatioY);
          options.scaleType =
              imageScaleType.values()[
                  ta.getInt(R.styleable.CropImageView_cropScaleType, options.scaleType.ordinal())];
          options.autoZoomEnabled =
              ta.getBoolean(R.styleable.CropImageView_cropAutoZoomEnabled, options.autoZoomEnabled);
          options.multiTouchEnabled =
              ta.getBoolean(
                  R.styleable.CropImageView_cropMultiTouchEnabled, options.multiTouchEnabled);
          options.maxZoom = ta.getInteger(R.styleable.CropImageView_cropMaxZoom, options.maxZoom);
          options.cropShape =
              imageCropShape.values()[
                  ta.getInt(R.styleable.CropImageView_cropShape, options.cropShape.ordinal())];
          options.guidelines =
              imageGuidelines.values()[
                  ta.getInt(
                      R.styleable.CropImageView_cropGuidelines, options.guidelines.ordinal())];
          options.snapRadius =
              ta.getDimension(R.styleable.CropImageView_cropSnapRadius, options.snapRadius);
          options.touchRadius =
              ta.getDimension(R.styleable.CropImageView_cropTouchRadius, options.touchRadius);
          options.initialCropWindowPaddingRatio =
              ta.getFloat(
                  R.styleable.CropImageView_cropInitialCropWindowPaddingRatio,
                  options.initialCropWindowPaddingRatio);
          options.borderLineThickness =
              ta.getDimension(
                  R.styleable.CropImageView_cropBorderLineThickness, options.borderLineThickness);
          options.borderLineColor =
              ta.getInteger(R.styleable.CropImageView_cropBorderLineColor, options.borderLineColor);
          options.borderCornerThickness =
              ta.getDimension(
                  R.styleable.CropImageView_cropBorderCornerThickness,
                  options.borderCornerThickness);
          options.borderCornerOffset =
              ta.getDimension(
                  R.styleable.CropImageView_cropBorderCornerOffset, options.borderCornerOffset);
          options.borderCornerLength =
              ta.getDimension(
                  R.styleable.CropImageView_cropBorderCornerLength, options.borderCornerLength);
          options.borderCornerColor =
              ta.getInteger(
                  R.styleable.CropImageView_cropBorderCornerColor, options.borderCornerColor);
          options.guidelinesThickness =
              ta.getDimension(
                  R.styleable.CropImageView_cropGuidelinesThickness, options.guidelinesThickness);
          options.guidelinesColor =
              ta.getInteger(R.styleable.CropImageView_cropGuidelinesColor, options.guidelinesColor);
          options.backgroundColor =
              ta.getInteger(R.styleable.CropImageView_cropBackgroundColor, options.backgroundColor);
          options.showCropOverlay =
              ta.getBoolean(R.styleable.CropImageView_cropShowCropOverlay, mShowCropOverlay);
          options.showProgressBar =
              ta.getBoolean(R.styleable.CropImageView_cropShowProgressBar, mShowProgressBar);
          options.borderCornerThickness =
              ta.getDimension(
                  R.styleable.CropImageView_cropBorderCornerThickness,
                  options.borderCornerThickness);
          options.minCropWindowWidth =
              (int)
                  ta.getDimension(
                      R.styleable.CropImageView_cropMinCropWindowWidth, options.minCropWindowWidth);
          options.minCropWindowHeight =
              (int)
                  ta.getDimension(
                      R.styleable.CropImageView_cropMinCropWindowHeight,
                      options.minCropWindowHeight);
          options.minCropResultWidth =
              (int)
                  ta.getFloat(
                      R.styleable.CropImageView_cropMinCropResultWidthPX,
                      options.minCropResultWidth);
          options.minCropResultHeight =
              (int)
                  ta.getFloat(
                      R.styleable.CropImageView_cropMinCropResultHeightPX,
                      options.minCropResultHeight);
          options.maxCropResultWidth =
              (int)
                  ta.getFloat(
                      R.styleable.CropImageView_cropMaxCropResultWidthPX,
                      options.maxCropResultWidth);
          options.maxCropResultHeight =
              (int)
                  ta.getFloat(
                      R.styleable.CropImageView_cropMaxCropResultHeightPX,
                      options.maxCropResultHeight);
          options.flipHorizontally =
              ta.getBoolean(
                  R.styleable.CropImageView_cropFlipHorizontally, options.flipHorizontally);
          options.flipVertically =
              ta.getBoolean(R.styleable.CropImageView_cropFlipHorizontally, options.flipVertically);

          mSaveBitmapToInstanceState =
              ta.getBoolean(
                  R.styleable.CropImageView_cropSaveBitmapToInstanceState,
                  mSaveBitmapToInstanceState);

          if (ta.hasValue(R.styleable.CropImageView_cropAspectRatioX)
              && ta.hasValue(R.styleable.CropImageView_cropAspectRatioX)
              && !ta.hasValue(R.styleable.CropImageView_cropFixAspectRatio)) {
            options.fixAspectRatio = true;
          }
        } finally {
          ta.recycle();
        }
      }
    }

    options.imageValidate();

    mScaleType = options.scaleType;
    mAutoZoomEnabled = options.autoZoomEnabled;
    mMaxZoom = options.maxZoom;
    mShowCropOverlay = options.showCropOverlay;
    mShowProgressBar = options.showProgressBar;
    mFlipHorizontally = options.flipHorizontally;
    mFlipVertically = options.flipVertically;

    LayoutInflater inflater = LayoutInflater.from(context);
    View v = inflater.inflate(R.layout.crop_image_view, this, true);

    mImageView = v.findViewById(R.id.ImageView_image);
    mImageView.setScaleType(ImageView.ScaleType.MATRIX);

    mCropOverlayView = v.findViewById(R.id.CropOverlayView);
    mCropOverlayView.imageSetCropWindowChangeListener(
            inProgress -> {
              imageHandleCropWindowChanged(inProgress, true);
              imageOnSetCropOverlayReleasedListener listener = mOnCropOverlayReleasedListener;
              if (listener != null && !inProgress) {
                listener.imageOnCropOverlayReleased(imageGetCropRect());
              }
              imageOnSetCropOverlayMovedListener movedListener = mOnSetCropOverlayMovedListener;
              if (movedListener != null && inProgress) {
                movedListener.imageOnCropOverlayMoved(imageGetCropRect());
              }
            });
    mCropOverlayView.imageSetInitialAttributeValues(options);

    mProgressBar = v.findViewById(R.id.CropProgressBar);
    imageSetProgressBarVisibility();
  }
  public int imageGetRotatedDegrees() {
    return mDegreesRotated;
  }
  public void imageSetFixedAspectRatio(boolean fixAspectRatio) {
    mCropOverlayView.imageSetFixedAspectRatio(fixAspectRatio);
  }
  public void imageSetAspectRatio(int aspectRatioX, int aspectRatioY) {
    mCropOverlayView.imageSetAspectRatioX(aspectRatioX);
    mCropOverlayView.imageSetAspectRatioY(aspectRatioY);
    imageSetFixedAspectRatio(true);
  }
  public Rect imageGetWholeImageRect() {
    int loadedSampleSize = mLoadedSampleSize;
    Bitmap bitmap = mBitmap;
    if (bitmap == null) {
      return null;
    }
    int orgWidth = bitmap.getWidth() * loadedSampleSize;
    int orgHeight = bitmap.getHeight() * loadedSampleSize;
    return new Rect(0, 0, orgWidth, orgHeight);
  }
  public Rect imageGetCropRect() {
    int loadedSampleSize = mLoadedSampleSize;
    Bitmap bitmap = mBitmap;
    if (bitmap == null) {
      return null;
    }
    float[] points = imageGetCropPoints();

    int orgWidth = bitmap.getWidth() * loadedSampleSize;
    int orgHeight = bitmap.getHeight() * loadedSampleSize;
    return KeyboardBitmapUtils.imageGetRectFromPoints(
        points,
        orgWidth,
        orgHeight,
        mCropOverlayView.imageIsFixAspectRatio(),
        mCropOverlayView.imageGetAspectRatioX(),
        mCropOverlayView.imageGetAspectRatioY());
  }
  public float[] imageGetCropPoints() {
    RectF cropWindowRect = mCropOverlayView.imageGetCropWindowRect();

    float[] points =
        new float[] {
          cropWindowRect.left,
          cropWindowRect.top,
          cropWindowRect.right,
          cropWindowRect.top,
          cropWindowRect.right,
          cropWindowRect.bottom,
          cropWindowRect.left,
          cropWindowRect.bottom
        };

    mImageMatrix.invert(mImageInverseMatrix);
    mImageInverseMatrix.mapPoints(points);

    for (int i = 0; i < points.length; i++) {
      points[i] *= mLoadedSampleSize;
    }

    return points;
  }
  public Bitmap imageGetCroppedImage() {
    return imageGetCroppedImage(0, 0, imageRequestSizeOptions.NONE);
  }
  public Bitmap imageGetCroppedImage(int reqWidth, int reqHeight, imageRequestSizeOptions options) {
    Bitmap croppedBitmap = null;
    if (mBitmap != null) {
      mImageView.clearAnimation();

      reqWidth = options != imageRequestSizeOptions.NONE ? reqWidth : 0;
      reqHeight = options != imageRequestSizeOptions.NONE ? reqHeight : 0;

      if (mLoadedImageUri != null
          && (mLoadedSampleSize > 1 || options == imageRequestSizeOptions.SAMPLING)) {
        int orgWidth = mBitmap.getWidth() * mLoadedSampleSize;
        int orgHeight = mBitmap.getHeight() * mLoadedSampleSize;
        KeyboardBitmapUtils.imageBitmapSampled bitmapSampled =
            KeyboardBitmapUtils.imageCropBitmap(
                getContext(),
                mLoadedImageUri,
                imageGetCropPoints(),
                mDegreesRotated,
                orgWidth,
                orgHeight,
                mCropOverlayView.imageIsFixAspectRatio(),
                mCropOverlayView.imageGetAspectRatioX(),
                mCropOverlayView.imageGetAspectRatioY(),
                reqWidth,
                reqHeight,
                mFlipHorizontally,
                mFlipVertically);
        croppedBitmap = bitmapSampled.bitmap;
      } else {
        croppedBitmap =
            KeyboardBitmapUtils.imageCropBitmapObjectHandleOOM(
                    mBitmap,
                    imageGetCropPoints(),
                    mDegreesRotated,
                    mCropOverlayView.imageIsFixAspectRatio(),
                    mCropOverlayView.imageGetAspectRatioX(),
                    mCropOverlayView.imageGetAspectRatioY(),
                    mFlipHorizontally,
                    mFlipVertically)
                .bitmap;
      }

      croppedBitmap = KeyboardBitmapUtils.imageResizeBitmap(croppedBitmap, reqWidth, reqHeight, options);
    }

    return croppedBitmap;
  }
  public void imageSetImageResource(int resId) {
    if (resId != 0) {
      mCropOverlayView.imageSetInitialCropWindowRect(null);
      Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId);
      imageSetBitmap(bitmap, resId, null, 1, 0);
    }
  }
  public void imageSetImageUriAsync(Uri uri) {
    if (uri != null) {
      KeyboardBitmapWorkerTask currentTask =
          mBitmapLoadingWorkerTask != null ? mBitmapLoadingWorkerTask.get() : null;
      if (currentTask != null) {
        currentTask.cancel(true);
      }
      imageClearImageInt();
      mRestoreCropWindowRect = null;
      mRestoreDegreesRotated = 0;
      mCropOverlayView.imageSetInitialCropWindowRect(null);
      mBitmapLoadingWorkerTask = new WeakReference<>(new KeyboardBitmapWorkerTask(this, uri));
      mBitmapLoadingWorkerTask.get().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
      imageSetProgressBarVisibility();
    }
  }
  public void imageRotateImage(int degrees) {
    if (mBitmap != null) {
      if (degrees < 0) {
        degrees = (degrees % 360) + 360;
      } else {
        degrees = degrees % 360;
      }

      boolean flipAxes =
          !mCropOverlayView.imageIsFixAspectRatio()
              && ((degrees > 45 && degrees < 135) || (degrees > 215 && degrees < 305));
      KeyboardBitmapUtils.RECT.set(mCropOverlayView.imageGetCropWindowRect());
      float halfWidth = (flipAxes ? KeyboardBitmapUtils.RECT.height() : KeyboardBitmapUtils.RECT.width()) / 2f;
      float halfHeight = (flipAxes ? KeyboardBitmapUtils.RECT.width() : KeyboardBitmapUtils.RECT.height()) / 2f;
      if (flipAxes) {
        boolean isFlippedHorizontally = mFlipHorizontally;
        mFlipHorizontally = mFlipVertically;
        mFlipVertically = isFlippedHorizontally;
      }

      mImageMatrix.invert(mImageInverseMatrix);

      KeyboardBitmapUtils.POINTS[0] = KeyboardBitmapUtils.RECT.centerX();
      KeyboardBitmapUtils.POINTS[1] = KeyboardBitmapUtils.RECT.centerY();
      KeyboardBitmapUtils.POINTS[2] = 0;
      KeyboardBitmapUtils.POINTS[3] = 0;
      KeyboardBitmapUtils.POINTS[4] = 1;
      KeyboardBitmapUtils.POINTS[5] = 0;
      mImageInverseMatrix.mapPoints(KeyboardBitmapUtils.POINTS);
      mDegreesRotated = (mDegreesRotated + degrees) % 360;
      imageApplyImageMatrix(getWidth(), getHeight(), true, false);
      mImageMatrix.mapPoints(KeyboardBitmapUtils.POINTS2, KeyboardBitmapUtils.POINTS);
      mZoom /=
          Math.sqrt(
              Math.pow(KeyboardBitmapUtils.POINTS2[4] - KeyboardBitmapUtils.POINTS2[2], 2)
                  + Math.pow(KeyboardBitmapUtils.POINTS2[5] - KeyboardBitmapUtils.POINTS2[3], 2));
      mZoom = Math.max(mZoom, 1);
      imageApplyImageMatrix(getWidth(), getHeight(), true, false);
      mImageMatrix.mapPoints(KeyboardBitmapUtils.POINTS2, KeyboardBitmapUtils.POINTS);
      double change =
          Math.sqrt(
              Math.pow(KeyboardBitmapUtils.POINTS2[4] - KeyboardBitmapUtils.POINTS2[2], 2)
                  + Math.pow(KeyboardBitmapUtils.POINTS2[5] - KeyboardBitmapUtils.POINTS2[3], 2));
      halfWidth *= change;
      halfHeight *= change;
      KeyboardBitmapUtils.RECT.set(
          KeyboardBitmapUtils.POINTS2[0] - halfWidth,
          KeyboardBitmapUtils.POINTS2[1] - halfHeight,
          KeyboardBitmapUtils.POINTS2[0] + halfWidth,
          KeyboardBitmapUtils.POINTS2[1] + halfHeight);

      mCropOverlayView.imageResetCropOverlayView();
      mCropOverlayView.imageSetCropWindowRect(KeyboardBitmapUtils.RECT);
      imageApplyImageMatrix(getWidth(), getHeight(), true, false);
      imageHandleCropWindowChanged(false, false);
      mCropOverlayView.imageFixCurrentCropWindowRect();
    }
  }
  void imageOnSetImageUriAsyncComplete(KeyboardBitmapWorkerTask.Result result) {

    mBitmapLoadingWorkerTask = null;
    imageSetProgressBarVisibility();

    if (result.error == null) {
      mInitialDegreesRotated = result.degreesRotated;
      imageSetBitmap(result.bitmap, 0, result.uri, result.loadSampleSize, result.degreesRotated);
    }

    imageOnSetImageUriCompleteListener listener = mOnSetImageUriCompleteListener;
    if (listener != null) {
      listener.imageOnSetImageUriComplete(this, result.uri, result.error);
    }
  }
  void imageOnImageCroppingAsyncComplete(KeyboardBitmapTask.Result result) {

    mBitmapCroppingWorkerTask = null;
    imageSetProgressBarVisibility();

    imageOnCropImageCompleteListener listener = mOnCropImageCompleteListener;
    if (listener != null) {
      CropResult cropResult =
          new CropResult(
              mBitmap,
              mLoadedImageUri,
              result.bitmap,
              result.uri,
              result.error,
              imageGetCropPoints(),
              imageGetCropRect(),
              imageGetWholeImageRect(),
              imageGetRotatedDegrees(),
              result.sampleSize);
      listener.imageOnCropImageComplete(this, cropResult);
    }
  }
  private void imageSetBitmap(
      Bitmap bitmap, int imageResource, Uri imageUri, int loadSampleSize, int degreesRotated) {
    if (mBitmap == null || !mBitmap.equals(bitmap)) {

      mImageView.clearAnimation();
      imageClearImageInt();
      mBitmap = bitmap;
      mImageView.setImageBitmap(mBitmap);

      mLoadedImageUri = imageUri;
      mImageResource = imageResource;
      mLoadedSampleSize = loadSampleSize;
      mDegreesRotated = degreesRotated;

      imageApplyImageMatrix(getWidth(), getHeight(), true, false);

      if (mCropOverlayView != null) {
        mCropOverlayView.imageResetCropOverlayView();
        imageSetCropOverlayVisibility();
      }
    }
  }
  private void imageClearImageInt() {
    if (mBitmap != null && (mImageResource > 0 || mLoadedImageUri != null)) {
      mBitmap.recycle();
    }
    mBitmap = null;
    mImageResource = 0;
    mLoadedImageUri = null;
    mLoadedSampleSize = 1;
    mDegreesRotated = 0;
    mZoom = 1;
    mZoomOffsetX = 0;
    mZoomOffsetY = 0;
    mImageMatrix.reset();
    mSaveInstanceStateBitmapUri = null;

    mImageView.setImageBitmap(null);

    imageSetCropOverlayVisibility();
  }
  @Override
  public Parcelable onSaveInstanceState() {
    if (mLoadedImageUri == null && mBitmap == null && mImageResource < 1) {
      return super.onSaveInstanceState();
    }

    Bundle bundle = new Bundle();
    Uri imageUri = mLoadedImageUri;
    if (mSaveBitmapToInstanceState && imageUri == null && mImageResource < 1) {
      mSaveInstanceStateBitmapUri =
          imageUri =
              KeyboardBitmapUtils.imageWriteTempStateStoreBitmap(
                  getContext(), mBitmap, mSaveInstanceStateBitmapUri);
    }
    if (imageUri != null && mBitmap != null) {
      String key = UUID.randomUUID().toString();
      KeyboardBitmapUtils.mStateBitmap = new Pair<>(key, new WeakReference<>(mBitmap));
      bundle.putString("LOADED_IMAGE_STATE_BITMAP_KEY", key);
    }
    if (mBitmapLoadingWorkerTask != null) {
      KeyboardBitmapWorkerTask task = mBitmapLoadingWorkerTask.get();
      if (task != null) {
        bundle.putParcelable("LOADING_IMAGE_URI", task.imageGetUri());
      }
    }
    bundle.putParcelable("instanceState", super.onSaveInstanceState());
    bundle.putParcelable("LOADED_IMAGE_URI", imageUri);
    bundle.putInt("LOADED_IMAGE_RESOURCE", mImageResource);
    bundle.putInt("LOADED_SAMPLE_SIZE", mLoadedSampleSize);
    bundle.putInt("DEGREES_ROTATED", mDegreesRotated);
    bundle.putParcelable("INITIAL_CROP_RECT", mCropOverlayView.imageGetInitialCropWindowRect());

    KeyboardBitmapUtils.RECT.set(mCropOverlayView.imageGetCropWindowRect());

    mImageMatrix.invert(mImageInverseMatrix);
    mImageInverseMatrix.mapRect(KeyboardBitmapUtils.RECT);

    bundle.putParcelable("CROP_WINDOW_RECT", KeyboardBitmapUtils.RECT);
    bundle.putString("CROP_SHAPE", mCropOverlayView.imageGetCropShape().name());
    bundle.putBoolean("CROP_AUTO_ZOOM_ENABLED", mAutoZoomEnabled);
    bundle.putInt("CROP_MAX_ZOOM", mMaxZoom);
    bundle.putBoolean("CROP_FLIP_HORIZONTALLY", mFlipHorizontally);
    bundle.putBoolean("CROP_FLIP_VERTICALLY", mFlipVertically);

    return bundle;
  }

  @Override
  public void onRestoreInstanceState(Parcelable state) {

    if (state instanceof Bundle) {
      Bundle bundle = (Bundle) state;
      if (mBitmapLoadingWorkerTask == null
          && mLoadedImageUri == null
          && mBitmap == null
          && mImageResource == 0) {

        Uri uri = bundle.getParcelable("LOADED_IMAGE_URI");
        if (uri != null) {
          String key = bundle.getString("LOADED_IMAGE_STATE_BITMAP_KEY");
          if (key != null) {
            Bitmap stateBitmap =
                KeyboardBitmapUtils.mStateBitmap != null && KeyboardBitmapUtils.mStateBitmap.first.equals(key)
                    ? KeyboardBitmapUtils.mStateBitmap.second.get()
                    : null;
            KeyboardBitmapUtils.mStateBitmap = null;
            if (stateBitmap != null && !stateBitmap.isRecycled()) {
              imageSetBitmap(stateBitmap, 0, uri, bundle.getInt("LOADED_SAMPLE_SIZE"), 0);
            }
          }
          if (mLoadedImageUri == null) {
            imageSetImageUriAsync(uri);
          }
        } else {
          int resId = bundle.getInt("LOADED_IMAGE_RESOURCE");
          if (resId > 0) {
            imageSetImageResource(resId);
          } else {
            uri = bundle.getParcelable("LOADING_IMAGE_URI");
            if (uri != null) {
              imageSetImageUriAsync(uri);
            }
          }
        }

        mDegreesRotated = mRestoreDegreesRotated = bundle.getInt("DEGREES_ROTATED");

        Rect initialCropRect = bundle.getParcelable("INITIAL_CROP_RECT");
        if (initialCropRect != null
            && (initialCropRect.width() > 0 || initialCropRect.height() > 0)) {
          mCropOverlayView.imageSetInitialCropWindowRect(initialCropRect);
        }

        RectF cropWindowRect = bundle.getParcelable("CROP_WINDOW_RECT");
        if (cropWindowRect != null && (cropWindowRect.width() > 0 || cropWindowRect.height() > 0)) {
          mRestoreCropWindowRect = cropWindowRect;
        }

        mCropOverlayView.imageSsetCropShape(imageCropShape.valueOf(bundle.getString("CROP_SHAPE")));

        mAutoZoomEnabled = bundle.getBoolean("CROP_AUTO_ZOOM_ENABLED");
        mMaxZoom = bundle.getInt("CROP_MAX_ZOOM");

        mFlipHorizontally = bundle.getBoolean("CROP_FLIP_HORIZONTALLY");
        mFlipVertically = bundle.getBoolean("CROP_FLIP_VERTICALLY");
      }

      super.onRestoreInstanceState(bundle.getParcelable("instanceState"));
    } else {
      super.onRestoreInstanceState(state);
    }
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    int widthMode = MeasureSpec.getMode(widthMeasureSpec);
    int widthSize = MeasureSpec.getSize(widthMeasureSpec);
    int heightMode = MeasureSpec.getMode(heightMeasureSpec);
    int heightSize = MeasureSpec.getSize(heightMeasureSpec);

    if (mBitmap != null) {
      if (heightSize == 0) {
        heightSize = mBitmap.getHeight();
      }

      int desiredWidth;
      int desiredHeight;

      double viewToBitmapWidthRatio = Double.POSITIVE_INFINITY;
      double viewToBitmapHeightRatio = Double.POSITIVE_INFINITY;
      if (widthSize < mBitmap.getWidth()) {
        viewToBitmapWidthRatio = (double) widthSize / (double) mBitmap.getWidth();
      }
      if (heightSize < mBitmap.getHeight()) {
        viewToBitmapHeightRatio = (double) heightSize / (double) mBitmap.getHeight();
      }
      if (viewToBitmapWidthRatio != Double.POSITIVE_INFINITY
          || viewToBitmapHeightRatio != Double.POSITIVE_INFINITY) {
        if (viewToBitmapWidthRatio <= viewToBitmapHeightRatio) {
          desiredWidth = widthSize;
          desiredHeight = (int) (mBitmap.getHeight() * viewToBitmapWidthRatio);
        } else {
          desiredHeight = heightSize;
          desiredWidth = (int) (mBitmap.getWidth() * viewToBitmapHeightRatio);
        }
      } else {
        desiredWidth = mBitmap.getWidth();
        desiredHeight = mBitmap.getHeight();
      }

      int width = imageGetOnMeasureSpec(widthMode, widthSize, desiredWidth);
      int height = imageGetOnMeasureSpec(heightMode, heightSize, desiredHeight);

      mLayoutWidth = width;
      mLayoutHeight = height;

      setMeasuredDimension(mLayoutWidth, mLayoutHeight);

    } else {
      setMeasuredDimension(widthSize, heightSize);
    }
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    super.onLayout(changed, l, t, r, b);
    if (mLayoutWidth > 0 && mLayoutHeight > 0) {
      ViewGroup.LayoutParams origParams = this.getLayoutParams();
      origParams.width = mLayoutWidth;
      origParams.height = mLayoutHeight;
      setLayoutParams(origParams);

      if (mBitmap != null) {
        imageApplyImageMatrix(r - l, b - t, true, false);
        if (mRestoreCropWindowRect != null) {
          if (mRestoreDegreesRotated != mInitialDegreesRotated) {
            mDegreesRotated = mRestoreDegreesRotated;
            imageApplyImageMatrix(r - l, b - t, true, false);
          }
          mImageMatrix.mapRect(mRestoreCropWindowRect);
          mCropOverlayView.imageSetCropWindowRect(mRestoreCropWindowRect);
          imageHandleCropWindowChanged(false, false);
          mCropOverlayView.imageFixCurrentCropWindowRect();
          mRestoreCropWindowRect = null;
        } else if (mSizeChanged) {
          mSizeChanged = false;
          imageHandleCropWindowChanged(false, false);
        }
      } else {
        imageUpdateImageBounds(true);
      }
    } else {
      imageUpdateImageBounds(true);
    }
  }
  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    mSizeChanged = oldw > 0 && oldh > 0;
  }
  private void imageHandleCropWindowChanged(boolean inProgress, boolean animate) {
    int width = getWidth();
    int height = getHeight();
    if (mBitmap != null && width > 0 && height > 0) {

      RectF cropRect = mCropOverlayView.imageGetCropWindowRect();
      if (inProgress) {
        if (cropRect.left < 0
            || cropRect.top < 0
            || cropRect.right > width
            || cropRect.bottom > height) {
          imageApplyImageMatrix(width, height, false, false);
        }
      } else if (mAutoZoomEnabled || mZoom > 1) {
        float newZoom = 0;
        if (mZoom < mMaxZoom
            && cropRect.width() < width * 0.5f
            && cropRect.height() < height * 0.5f) {
          newZoom =
              Math.min(
                  mMaxZoom,
                  Math.min(
                      width / (cropRect.width() / mZoom / 0.64f),
                      height / (cropRect.height() / mZoom / 0.64f)));
        }
        if (mZoom > 1 && (cropRect.width() > width * 0.65f || cropRect.height() > height * 0.65f)) {
          newZoom =
              Math.max(
                  1,
                  Math.min(
                      width / (cropRect.width() / mZoom / 0.51f),
                      height / (cropRect.height() / mZoom / 0.51f)));
        }
        if (!mAutoZoomEnabled) {
          newZoom = 1;
        }

        if (newZoom > 0 && newZoom != mZoom) {
          if (animate) {
            if (mAnimation == null) {
              mAnimation = new KeyboardCropImageAnimation(mImageView, mCropOverlayView);
            }
            mAnimation.imageSetStartState(mImagePoints, mImageMatrix);
          }

          mZoom = newZoom;

          imageApplyImageMatrix(width, height, true, animate);
        }
      }
      if (mOnSetCropWindowChangeListener != null && !inProgress) {
        mOnSetCropWindowChangeListener.imageOnCropWindowChanged();
      }
    }
  }
  private void imageApplyImageMatrix(float width, float height, boolean center, boolean animate) {
    if (mBitmap != null && width > 0 && height > 0) {

      mImageMatrix.invert(mImageInverseMatrix);
      RectF cropRect = mCropOverlayView.imageGetCropWindowRect();
      mImageInverseMatrix.mapRect(cropRect);

      mImageMatrix.reset();
      mImageMatrix.postTranslate(
          (width - mBitmap.getWidth()) / 2, (height - mBitmap.getHeight()) / 2);
      imageMapImagePointsByImageMatrix();
      if (mDegreesRotated > 0) {
        mImageMatrix.postRotate(
            mDegreesRotated,
            KeyboardBitmapUtils.imageGetRectCenterX(mImagePoints),
            KeyboardBitmapUtils.imageGetRectCenterY(mImagePoints));
        imageMapImagePointsByImageMatrix();
      }
      float scale =
          Math.min(
              width / KeyboardBitmapUtils.imageGetRectWidth(mImagePoints),
              height / KeyboardBitmapUtils.imageGetRectHeight(mImagePoints));
      if (mScaleType == imageScaleType.FIT_CENTER
          || (mScaleType == imageScaleType.CENTER_INSIDE && scale < 1)
          || (scale > 1 && mAutoZoomEnabled)) {
        mImageMatrix.postScale(
            scale,
            scale,
            KeyboardBitmapUtils.imageGetRectCenterX(mImagePoints),
            KeyboardBitmapUtils.imageGetRectCenterY(mImagePoints));
        imageMapImagePointsByImageMatrix();
      }
      float scaleX = mFlipHorizontally ? -mZoom : mZoom;
      float scaleY = mFlipVertically ? -mZoom : mZoom;
      mImageMatrix.postScale(
          scaleX,
          scaleY,
          KeyboardBitmapUtils.imageGetRectCenterX(mImagePoints),
          KeyboardBitmapUtils.imageGetRectCenterY(mImagePoints));
      imageMapImagePointsByImageMatrix();
      mImageMatrix.mapRect(cropRect);
      if (center) {
        mZoomOffsetX =
            width > KeyboardBitmapUtils.imageGetRectWidth(mImagePoints)
                ? 0
                : Math.max(
                        Math.min(
                            width / 2 - cropRect.centerX(), -KeyboardBitmapUtils.imageGetRectLeft(mImagePoints)),
                        getWidth() - KeyboardBitmapUtils.imageGetRectRight(mImagePoints))
                    / scaleX;
        mZoomOffsetY =
            height > KeyboardBitmapUtils.imageGetRectHeight(mImagePoints)
                ? 0
                : Math.max(
                        Math.min(
                            height / 2 - cropRect.centerY(), -KeyboardBitmapUtils.imageGetRectTop(mImagePoints)),
                        getHeight() - KeyboardBitmapUtils.imageGetRectBottom(mImagePoints))
                    / scaleY;
      } else {
        mZoomOffsetX =
            Math.min(Math.max(mZoomOffsetX * scaleX, -cropRect.left), -cropRect.right + width)
                / scaleX;
        mZoomOffsetY =
            Math.min(Math.max(mZoomOffsetY * scaleY, -cropRect.top), -cropRect.bottom + height)
                / scaleY;
      }
      mImageMatrix.postTranslate(mZoomOffsetX * scaleX, mZoomOffsetY * scaleY);
      cropRect.offset(mZoomOffsetX * scaleX, mZoomOffsetY * scaleY);
      mCropOverlayView.imageSetCropWindowRect(cropRect);
      imageMapImagePointsByImageMatrix();
      mCropOverlayView.invalidate();

      if (animate) {
        mAnimation.imageSetEndState(mImagePoints, mImageMatrix);
        mImageView.startAnimation(mAnimation);
      } else {
        mImageView.setImageMatrix(mImageMatrix);
      }
      imageUpdateImageBounds(false);
    }
  }
  private void imageMapImagePointsByImageMatrix() {
    mImagePoints[0] = 0;
    mImagePoints[1] = 0;
    mImagePoints[2] = mBitmap.getWidth();
    mImagePoints[3] = 0;
    mImagePoints[4] = mBitmap.getWidth();
    mImagePoints[5] = mBitmap.getHeight();
    mImagePoints[6] = 0;
    mImagePoints[7] = mBitmap.getHeight();
    mImageMatrix.mapPoints(mImagePoints);
    mScaleImagePoints[0] = 0;
    mScaleImagePoints[1] = 0;
    mScaleImagePoints[2] = 100;
    mScaleImagePoints[3] = 0;
    mScaleImagePoints[4] = 100;
    mScaleImagePoints[5] = 100;
    mScaleImagePoints[6] = 0;
    mScaleImagePoints[7] = 100;
    mImageMatrix.mapPoints(mScaleImagePoints);
  }
  private static int imageGetOnMeasureSpec(int measureSpecMode, int measureSpecSize, int desiredSize) {
    int spec;
    if (measureSpecMode == MeasureSpec.EXACTLY) {
      spec = measureSpecSize;
    } else if (measureSpecMode == MeasureSpec.AT_MOST) {
      spec = Math.min(desiredSize, measureSpecSize);
    } else {
      spec = desiredSize;
    }

    return spec;
  }
  private void imageSetCropOverlayVisibility() {
    if (mCropOverlayView != null) {
      mCropOverlayView.setVisibility(mShowCropOverlay && mBitmap != null ? VISIBLE : INVISIBLE);
    }
  }
  private void imageSetProgressBarVisibility() {
    boolean visible =
        mShowProgressBar
            && (mBitmap == null && mBitmapLoadingWorkerTask != null
                || mBitmapCroppingWorkerTask != null);
    mProgressBar.setVisibility(visible ? VISIBLE : INVISIBLE);
  }
  private void imageUpdateImageBounds(boolean clear) {
    if (mBitmap != null && !clear) {
      float scaleFactorWidth =
          100f * mLoadedSampleSize / KeyboardBitmapUtils.imageGetRectWidth(mScaleImagePoints);
      float scaleFactorHeight =
          100f * mLoadedSampleSize / KeyboardBitmapUtils.imageGetRectHeight(mScaleImagePoints);
      mCropOverlayView.imageSetCropWindowLimits(
          getWidth(), getHeight(), scaleFactorWidth, scaleFactorHeight);
    }
    mCropOverlayView.imageSetBounds(clear ? null : mImagePoints, getWidth(), getHeight());
  }
  public enum imageCropShape {
    RECTANGLE,
    OVAL
  }
  public enum imageScaleType {
    FIT_CENTER,
    CENTER,
    CENTER_CROP,
    CENTER_INSIDE
  }
  public enum imageGuidelines {
    OFF,
    ON_TOUCH,
    ON
  }
  public enum imageRequestSizeOptions {
    NONE,
    SAMPLING,
    RESIZE_INSIDE,
    RESIZE_FIT,
    RESIZE_EXACT
  }
  public interface imageOnSetCropOverlayReleasedListener {
    void imageOnCropOverlayReleased(Rect rect);
  }
  public interface imageOnSetCropOverlayMovedListener {
    void imageOnCropOverlayMoved(Rect rect);
  }
  public interface imageOnSetCropWindowChangeListener {
    void imageOnCropWindowChanged();
  }
  public interface imageOnSetImageUriCompleteListener {
    void imageOnSetImageUriComplete(KeyboardCropImageView view, Uri uri, Exception error);
  }
  public interface imageOnCropImageCompleteListener {
    void imageOnCropImageComplete(KeyboardCropImageView view, CropResult result);
  }
  public static class CropResult {
    private final Bitmap mOriginalBitmap;
    private final Uri mOriginalUri;
    private final Bitmap mBitmap;
    private final Uri mUri;
    private final Exception mError;
    private final float[] mCropPoints;
    private final Rect mCropRect;
    private final Rect mWholeImageRect;
    private final int mRotation;
    private final int mSampleSize;

    CropResult(
        Bitmap originalBitmap,
        Uri originalUri,
        Bitmap bitmap,
        Uri uri,
        Exception error,
        float[] cropPoints,
        Rect cropRect,
        Rect wholeImageRect,
        int rotation,
        int sampleSize) {
      mOriginalBitmap = originalBitmap;
      mOriginalUri = originalUri;
      mBitmap = bitmap;
      mUri = uri;
      mError = error;
      mCropPoints = cropPoints;
      mCropRect = cropRect;
      mWholeImageRect = wholeImageRect;
      mRotation = rotation;
      mSampleSize = sampleSize;
    }
    public Uri imageGetOriginalUri() {
      return mOriginalUri;
    }
    public Uri imageGetUri() {
      return mUri;
    }
    public Exception imageGetError() {
      return mError;
    }
    public float[] imageGetCropPoints() {
      return mCropPoints;
    }
    public Rect imageGetCropRect() {
      return mCropRect;
    }
    public Rect imageGetWholeImageRect() {
      return mWholeImageRect;
    }
    public int imageGetRotation() {
      return mRotation;
    }
    public int imageGetSampleSize() {
      return mSampleSize;
    }
  }
}
