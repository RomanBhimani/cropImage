package com.rd.cropimagesingallery.cropImage;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageView;

final class KeyboardCropImageAnimation extends Animation implements Animation.AnimationListener {
  private final ImageView mImageView;
  private final KeyboardCropOverlayView mCropOverlayView;
  private final float[] mStartBoundPoints = new float[8];
  private final float[] mEndBoundPoints = new float[8];
  private final RectF mStartCropWindowRect = new RectF();
  private final RectF mEndCropWindowRect = new RectF();
  private final float[] mStartImageMatrix = new float[9];
  private final float[] mEndImageMatrix = new float[9];
  private final RectF mAnimRect = new RectF();
  private final float[] mAnimPoints = new float[8];
  private final float[] mAnimMatrix = new float[9];
  public KeyboardCropImageAnimation(ImageView cropImageView, KeyboardCropOverlayView cropOverlayView) {
    mImageView = cropImageView;
    mCropOverlayView = cropOverlayView;

    setDuration(300);
    setFillAfter(true);
    setInterpolator(new AccelerateDecelerateInterpolator());
    setAnimationListener(this);
  }

  public void imageSetStartState(float[] boundPoints, Matrix imageMatrix) {
    reset();
    System.arraycopy(boundPoints, 0, mStartBoundPoints, 0, 8);
    mStartCropWindowRect.set(mCropOverlayView.imageGetCropWindowRect());
    imageMatrix.getValues(mStartImageMatrix);
  }

  public void imageSetEndState(float[] boundPoints, Matrix imageMatrix) {
    System.arraycopy(boundPoints, 0, mEndBoundPoints, 0, 8);
    mEndCropWindowRect.set(mCropOverlayView.imageGetCropWindowRect());
    imageMatrix.getValues(mEndImageMatrix);
  }

  @Override
  protected void applyTransformation(float interpolatedTime, Transformation t) {

    mAnimRect.left =
        mStartCropWindowRect.left
            + (mEndCropWindowRect.left - mStartCropWindowRect.left) * interpolatedTime;
    mAnimRect.top =
        mStartCropWindowRect.top
            + (mEndCropWindowRect.top - mStartCropWindowRect.top) * interpolatedTime;
    mAnimRect.right =
        mStartCropWindowRect.right
            + (mEndCropWindowRect.right - mStartCropWindowRect.right) * interpolatedTime;
    mAnimRect.bottom =
        mStartCropWindowRect.bottom
            + (mEndCropWindowRect.bottom - mStartCropWindowRect.bottom) * interpolatedTime;
    mCropOverlayView.imageSetCropWindowRect(mAnimRect);

    for (int i = 0; i < mAnimPoints.length; i++) {
      mAnimPoints[i] =
          mStartBoundPoints[i] + (mEndBoundPoints[i] - mStartBoundPoints[i]) * interpolatedTime;
    }
    mCropOverlayView.imageSetBounds(mAnimPoints, mImageView.getWidth(), mImageView.getHeight());

    for (int i = 0; i < mAnimMatrix.length; i++) {
      mAnimMatrix[i] =
          mStartImageMatrix[i] + (mEndImageMatrix[i] - mStartImageMatrix[i]) * interpolatedTime;
    }
    Matrix m = mImageView.getImageMatrix();
    m.setValues(mAnimMatrix);
    mImageView.setImageMatrix(m);

    mImageView.invalidate();
    mCropOverlayView.invalidate();
  }

  @Override
  public void onAnimationStart(Animation animation) {}

  @Override
  public void onAnimationEnd(Animation animation) {
    mImageView.clearAnimation();
  }

  @Override
  public void onAnimationRepeat(Animation animation) {}
}
