
package com.rd.cropimagesingallery.cropImage;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import java.util.Arrays;

public class KeyboardCropOverlayView extends View {
  private ScaleGestureDetector mScaleDetector;
  private boolean mMultiTouchEnabled;
  private final KeyboardHandler mCropWindowHandler = new KeyboardHandler();
  private CropWindowChangeListener mCropWindowChangeListener;
  private final RectF mDrawRect = new RectF();
  private Paint mBorderPaint;
  private Paint mBorderCornerPaint;
  private Paint mGuidelinePaint;
  private Paint mBackgroundPaint;
  private Path mPath = new Path();
  private final float[] mBoundsPoints = new float[8];
  private final RectF mCalcBounds = new RectF();
  private int mViewWidth;
  private int mViewHeight;
  private float mBorderCornerOffset;
  private float mBorderCornerLength;
  private float mInitialCropWindowPaddingRatio;
  private float mTouchRadius;
  private float mSnapRadius;
  private KeyboardMoveHandler mMoveHandler;
  private boolean mFixAspectRatio;
  private int mAspectRatioX;
  private int mAspectRatioY;
  private float mTargetAspectRatio = ((float) mAspectRatioX) / mAspectRatioY;
  private KeyboardCropImageView.imageGuidelines mGuidelines;
  private KeyboardCropImageView.imageCropShape mCropShape;
  private final Rect mInitialCropWindowRect = new Rect();
  private boolean initializedCropWindow;
  private Integer mOriginalLayerType;
  public KeyboardCropOverlayView(Context context) {
    this(context, null);
  }
  public KeyboardCropOverlayView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }
  public void imageSetCropWindowChangeListener(CropWindowChangeListener listener) {
    mCropWindowChangeListener = listener;
  }
  public RectF imageGetCropWindowRect() {
    return mCropWindowHandler.imageGetRect();
  }
  public void imageSetCropWindowRect(RectF rect) {
    mCropWindowHandler.imageSetRect(rect);
  }
  public void imageFixCurrentCropWindowRect() {
    RectF rect = imageGetCropWindowRect();
    imageFixCropWindowRectByRules(rect);
    mCropWindowHandler.imageSetRect(rect);
  }
  public void imageSetBounds(float[] boundsPoints, int viewWidth, int viewHeight) {
    if (boundsPoints == null || !Arrays.equals(mBoundsPoints, boundsPoints)) {
      if (boundsPoints == null) {
        Arrays.fill(mBoundsPoints, 0);
      } else {
        System.arraycopy(boundsPoints, 0, mBoundsPoints, 0, boundsPoints.length);
      }
      mViewWidth = viewWidth;
      mViewHeight = viewHeight;
      RectF cropRect = mCropWindowHandler.imageGetRect();
      if (cropRect.width() == 0 || cropRect.height() == 0) {
        imageInitCropWindow();
      }
    }
  }
  public void imageResetCropOverlayView() {
    if (initializedCropWindow) {
      imageSetCropWindowRect(KeyboardBitmapUtils.EMPTY_RECT_F);
      imageInitCropWindow();
      invalidate();
    }
  }
  public KeyboardCropImageView.imageCropShape imageGetCropShape() {
    return mCropShape;
  }
  public void imageSsetCropShape(KeyboardCropImageView.imageCropShape cropShape) {
    if (mCropShape != cropShape) {
      mCropShape = cropShape;
        if (Build.VERSION.SDK_INT <= 17) {
        if (mCropShape == KeyboardCropImageView.imageCropShape.OVAL) {
          mOriginalLayerType = getLayerType();
          if (mOriginalLayerType != View.LAYER_TYPE_SOFTWARE) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
          } else {
            mOriginalLayerType = null;
          }
        } else if (mOriginalLayerType != null) {
          setLayerType(mOriginalLayerType, null);
          mOriginalLayerType = null;
        }
      }
      invalidate();
    }
  }
  public void imageSetGuidelines(KeyboardCropImageView.imageGuidelines guidelines) {
    if (mGuidelines != guidelines) {
      mGuidelines = guidelines;
      if (initializedCropWindow) {
        invalidate();
      }
    }
  }
  public boolean imageIsFixAspectRatio() {
    return mFixAspectRatio;
  }
  public void imageSetFixedAspectRatio(boolean fixAspectRatio) {
    if (mFixAspectRatio != fixAspectRatio) {
      mFixAspectRatio = fixAspectRatio;
      if (initializedCropWindow) {
        imageInitCropWindow();
        invalidate();
      }
    }
  }
  public int imageGetAspectRatioX() {
    return mAspectRatioX;
  }
  public void imageSetAspectRatioX(int aspectRatioX) {
    if (aspectRatioX <= 0) {
      throw new IllegalArgumentException(
          "Cannot set aspect ratio value to a number less than or equal to 0.");
    } else if (mAspectRatioX != aspectRatioX) {
      mAspectRatioX = aspectRatioX;
      mTargetAspectRatio = ((float) mAspectRatioX) / mAspectRatioY;

      if (initializedCropWindow) {
        imageInitCropWindow();
        invalidate();
      }
    }
  }
  public int imageGetAspectRatioY() {
    return mAspectRatioY;
  }
  public void imageSetAspectRatioY(int aspectRatioY) {
    if (aspectRatioY <= 0) {
      throw new IllegalArgumentException(
          "Cannot set aspect ratio value to a number less than or equal to 0.");
    } else if (mAspectRatioY != aspectRatioY) {
      mAspectRatioY = aspectRatioY;
      mTargetAspectRatio = ((float) mAspectRatioX) / mAspectRatioY;

      if (initializedCropWindow) {
        imageInitCropWindow();
        invalidate();
      }
    }
  }
  public void imageSetSnapRadius(float snapRadius) {
    mSnapRadius = snapRadius;
  }
  public boolean imageSetMultiTouchEnabled(boolean multiTouchEnabled) {
      if (mMultiTouchEnabled != multiTouchEnabled) {
      mMultiTouchEnabled = multiTouchEnabled;
      if (mMultiTouchEnabled && mScaleDetector == null) {
        mScaleDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
      }
      return true;
    }
    return false;
  }
  public void imageSetCropWindowLimits(
      float maxWidth, float maxHeight, float scaleFactorWidth, float scaleFactorHeight) {
    mCropWindowHandler.imageSetCropWindowLimits(
        maxWidth, maxHeight, scaleFactorWidth, scaleFactorHeight);
  }
  public Rect imageGetInitialCropWindowRect() {
    return mInitialCropWindowRect;
  }
  public void imageSetInitialCropWindowRect(Rect rect) {
    mInitialCropWindowRect.set(rect != null ? rect : KeyboardBitmapUtils.EMPTY_RECT);
    if (initializedCropWindow) {
      imageInitCropWindow();
      invalidate();
      imageCallOnCropWindowChanged(false);
    }
  }
  public void imageSetInitialAttributeValues(KeyboardCropImageOptions options) {

    mCropWindowHandler.imageSetInitialAttributeValues(options);

    imageSsetCropShape(options.cropShape);

    imageSetSnapRadius(options.snapRadius);

    imageSetGuidelines(options.guidelines);

    imageSetFixedAspectRatio(options.fixAspectRatio);

    imageSetAspectRatioX(options.aspectRatioX);

    imageSetAspectRatioY(options.aspectRatioY);

    imageSetMultiTouchEnabled(options.multiTouchEnabled);

    mTouchRadius = options.touchRadius;

    mInitialCropWindowPaddingRatio = options.initialCropWindowPaddingRatio;

    mBorderPaint = imageGetNewPaintOrNull(options.borderLineThickness, options.borderLineColor);

    mBorderCornerOffset = options.borderCornerOffset;
    mBorderCornerLength = options.borderCornerLength;
    mBorderCornerPaint =
        imageGetNewPaintOrNull(options.borderCornerThickness, options.borderCornerColor);

    mGuidelinePaint = imageGetNewPaintOrNull(options.guidelinesThickness, options.guidelinesColor);

    mBackgroundPaint = imageGetNewPaint(options.backgroundColor);
  }
  private void imageInitCropWindow() {

    float leftLimit = Math.max(KeyboardBitmapUtils.imageGetRectLeft(mBoundsPoints), 0);
    float topLimit = Math.max(KeyboardBitmapUtils.imageGetRectTop(mBoundsPoints), 0);
    float rightLimit = Math.min(KeyboardBitmapUtils.imageGetRectRight(mBoundsPoints), getWidth());
    float bottomLimit = Math.min(KeyboardBitmapUtils.imageGetRectBottom(mBoundsPoints), getHeight());

    if (rightLimit <= leftLimit || bottomLimit <= topLimit) {
      return;
    }

    RectF rect = new RectF();
    initializedCropWindow = true;

    float horizontalPadding = mInitialCropWindowPaddingRatio * (rightLimit - leftLimit);
    float verticalPadding = mInitialCropWindowPaddingRatio * (bottomLimit - topLimit);

    if (mInitialCropWindowRect.width() > 0 && mInitialCropWindowRect.height() > 0) {
      rect.left =
          leftLimit + mInitialCropWindowRect.left / mCropWindowHandler.imageGetScaleFactorWidth();
      rect.top = topLimit + mInitialCropWindowRect.top / mCropWindowHandler.imageGetScaleFactorHeight();
      rect.right =
          rect.left + mInitialCropWindowRect.width() / mCropWindowHandler.imageGetScaleFactorWidth();
      rect.bottom =
          rect.top + mInitialCropWindowRect.height() / mCropWindowHandler.imageGetScaleFactorHeight();
      rect.left = Math.max(leftLimit, rect.left);
      rect.top = Math.max(topLimit, rect.top);
      rect.right = Math.min(rightLimit, rect.right);
      rect.bottom = Math.min(bottomLimit, rect.bottom);

    } else if (mFixAspectRatio && rightLimit > leftLimit && bottomLimit > topLimit) {
      float bitmapAspectRatio = (rightLimit - leftLimit) / (bottomLimit - topLimit);
      if (bitmapAspectRatio > mTargetAspectRatio) {

        rect.top = topLimit + verticalPadding;
        rect.bottom = bottomLimit - verticalPadding;

        float centerX = getWidth() / 2f;
        mTargetAspectRatio = (float) mAspectRatioX / mAspectRatioY;
        float cropWidth =
            Math.max(mCropWindowHandler.imageGetMinCropWidth(), rect.height() * mTargetAspectRatio);

        float halfCropWidth = cropWidth / 2f;
        rect.left = centerX - halfCropWidth;
        rect.right = centerX + halfCropWidth;

      } else {

        rect.left = leftLimit + horizontalPadding;
        rect.right = rightLimit - horizontalPadding;

        float centerY = getHeight() / 2f;
        float cropHeight =
            Math.max(mCropWindowHandler.imageGetMinCropHeight(), rect.width() / mTargetAspectRatio);

        float halfCropHeight = cropHeight / 2f;
        rect.top = centerY - halfCropHeight;
        rect.bottom = centerY + halfCropHeight;
      }
    } else {
      rect.left = leftLimit + horizontalPadding;
      rect.top = topLimit + verticalPadding;
      rect.right = rightLimit - horizontalPadding;
      rect.bottom = bottomLimit - verticalPadding;
    }

    imageFixCropWindowRectByRules(rect);

    mCropWindowHandler.imageSetRect(rect);
  }
  private void imageFixCropWindowRectByRules(RectF rect) {
    if (rect.width() < mCropWindowHandler.imageGetMinCropWidth()) {
      float adj = (mCropWindowHandler.imageGetMinCropWidth() - rect.width()) / 2;
      rect.left -= adj;
      rect.right += adj;
    }
    if (rect.height() < mCropWindowHandler.imageGetMinCropHeight()) {
      float adj = (mCropWindowHandler.imageGetMinCropHeight() - rect.height()) / 2;
      rect.top -= adj;
      rect.bottom += adj;
    }
    if (rect.width() > mCropWindowHandler.imageGetMaxCropWidth()) {
      float adj = (rect.width() - mCropWindowHandler.imageGetMaxCropWidth()) / 2;
      rect.left += adj;
      rect.right -= adj;
    }
    if (rect.height() > mCropWindowHandler.imageGetMaxCropHeight()) {
      float adj = (rect.height() - mCropWindowHandler.imageGetMaxCropHeight()) / 2;
      rect.top += adj;
      rect.bottom -= adj;
    }

    imageCalculateBounds(rect);
    if (mCalcBounds.width() > 0 && mCalcBounds.height() > 0) {
      float leftLimit = Math.max(mCalcBounds.left, 0);
      float topLimit = Math.max(mCalcBounds.top, 0);
      float rightLimit = Math.min(mCalcBounds.right, getWidth());
      float bottomLimit = Math.min(mCalcBounds.bottom, getHeight());
      if (rect.left < leftLimit) {
        rect.left = leftLimit;
      }
      if (rect.top < topLimit) {
        rect.top = topLimit;
      }
      if (rect.right > rightLimit) {
        rect.right = rightLimit;
      }
      if (rect.bottom > bottomLimit) {
        rect.bottom = bottomLimit;
      }
    }
    if (mFixAspectRatio && Math.abs(rect.width() - rect.height() * mTargetAspectRatio) > 0.1) {
      if (rect.width() > rect.height() * mTargetAspectRatio) {
        float adj = Math.abs(rect.height() * mTargetAspectRatio - rect.width()) / 2;
        rect.left += adj;
        rect.right -= adj;
      } else {
        float adj = Math.abs(rect.width() / mTargetAspectRatio - rect.height()) / 2;
        rect.top += adj;
        rect.bottom -= adj;
      }
    }
  }
  @Override
  protected void onDraw(Canvas canvas) {

    super.onDraw(canvas);
    imageDrawBackground(canvas);

    if (mCropWindowHandler.imageShowGuidelines()) {
      if (mGuidelines == KeyboardCropImageView.imageGuidelines.ON) {
        imageDrawGuidelines(canvas);
      } else if (mGuidelines == KeyboardCropImageView.imageGuidelines.ON_TOUCH && mMoveHandler != null) {
        imageDrawGuidelines(canvas);
      }
    }

    imageDrawBorders(canvas);

    imageDrawCorners(canvas);
  }
  private void imageDrawBackground(Canvas canvas) {

    RectF rect = mCropWindowHandler.imageGetRect();

    float left = Math.max(KeyboardBitmapUtils.imageGetRectLeft(mBoundsPoints), 0);
    float top = Math.max(KeyboardBitmapUtils.imageGetRectTop(mBoundsPoints), 0);
    float right = Math.min(KeyboardBitmapUtils.imageGetRectRight(mBoundsPoints), getWidth());
    float bottom = Math.min(KeyboardBitmapUtils.imageGetRectBottom(mBoundsPoints), getHeight());

    if (mCropShape == KeyboardCropImageView.imageCropShape.RECTANGLE) {
      if (!imageIsNonStraightAngleRotated() || Build.VERSION.SDK_INT <= 17) {
        canvas.drawRect(left, top, right, rect.top, mBackgroundPaint);
        canvas.drawRect(left, rect.bottom, right, bottom, mBackgroundPaint);
        canvas.drawRect(left, rect.top, rect.left, rect.bottom, mBackgroundPaint);
        canvas.drawRect(rect.right, rect.top, right, rect.bottom, mBackgroundPaint);
      } else {
        mPath.reset();
        mPath.moveTo(mBoundsPoints[0], mBoundsPoints[1]);
        mPath.lineTo(mBoundsPoints[2], mBoundsPoints[3]);
        mPath.lineTo(mBoundsPoints[4], mBoundsPoints[5]);
        mPath.lineTo(mBoundsPoints[6], mBoundsPoints[7]);
        mPath.close();

        canvas.save();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          canvas.clipOutPath(mPath);
        } else {
          canvas.clipPath(mPath, Region.Op.INTERSECT);
        }
        canvas.clipRect(rect, Region.Op.XOR);
        canvas.drawRect(left, top, right, bottom, mBackgroundPaint);
        canvas.restore();
      }
    } else {
      mPath.reset();
        if (Build.VERSION.SDK_INT <= 17 && mCropShape == KeyboardCropImageView.imageCropShape.OVAL) {
        mDrawRect.set(rect.left + 2, rect.top + 2, rect.right - 2, rect.bottom - 2);
      } else {
        mDrawRect.set(rect.left, rect.top, rect.right, rect.bottom);
      }
      mPath.addOval(mDrawRect, Path.Direction.CW);
      canvas.save();
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        canvas.clipOutPath(mPath);
      } else {
        canvas.clipPath(mPath, Region.Op.XOR);
      }
      canvas.drawRect(left, top, right, bottom, mBackgroundPaint);
      canvas.restore();
    }
  }
  private void imageDrawGuidelines(Canvas canvas) {
    if (mGuidelinePaint != null) {
      float sw = mBorderPaint != null ? mBorderPaint.getStrokeWidth() : 0;
      RectF rect = mCropWindowHandler.imageGetRect();
      rect.inset(sw, sw);

      float oneThirdCropWidth = rect.width() / 3;
      float oneThirdCropHeight = rect.height() / 3;

      if (mCropShape == KeyboardCropImageView.imageCropShape.OVAL) {

        float w = rect.width() / 2 - sw;
        float h = rect.height() / 2 - sw;

        float x1 = rect.left + oneThirdCropWidth;
        float x2 = rect.right - oneThirdCropWidth;
        float yv = (float) (h * Math.sin(Math.acos((w - oneThirdCropWidth) / w)));
        canvas.drawLine(x1, rect.top + h - yv, x1, rect.bottom - h + yv, mGuidelinePaint);
        canvas.drawLine(x2, rect.top + h - yv, x2, rect.bottom - h + yv, mGuidelinePaint);

        float y1 = rect.top + oneThirdCropHeight;
        float y2 = rect.bottom - oneThirdCropHeight;
        float xv = (float) (w * Math.cos(Math.asin((h - oneThirdCropHeight) / h)));
        canvas.drawLine(rect.left + w - xv, y1, rect.right - w + xv, y1, mGuidelinePaint);
        canvas.drawLine(rect.left + w - xv, y2, rect.right - w + xv, y2, mGuidelinePaint);
      } else {

        float x1 = rect.left + oneThirdCropWidth;
        float x2 = rect.right - oneThirdCropWidth;
        canvas.drawLine(x1, rect.top, x1, rect.bottom, mGuidelinePaint);
        canvas.drawLine(x2, rect.top, x2, rect.bottom, mGuidelinePaint);

        float y1 = rect.top + oneThirdCropHeight;
        float y2 = rect.bottom - oneThirdCropHeight;
        canvas.drawLine(rect.left, y1, rect.right, y1, mGuidelinePaint);
        canvas.drawLine(rect.left, y2, rect.right, y2, mGuidelinePaint);
      }
    }
  }
  private void imageDrawBorders(Canvas canvas) {
    if (mBorderPaint != null) {
      float w = mBorderPaint.getStrokeWidth();
      RectF rect = mCropWindowHandler.imageGetRect();
      rect.inset(w / 2, w / 2);

      if (mCropShape == KeyboardCropImageView.imageCropShape.RECTANGLE) {
        canvas.drawRect(rect, mBorderPaint);
      } else {
        canvas.drawOval(rect, mBorderPaint);
      }
    }
  }
  private void imageDrawCorners(Canvas canvas) {
    if (mBorderCornerPaint != null) {

      float lineWidth = mBorderPaint != null ? mBorderPaint.getStrokeWidth() : 0;
      float cornerWidth = mBorderCornerPaint.getStrokeWidth();

      float w =
          cornerWidth / 2
              + (mCropShape == KeyboardCropImageView.imageCropShape.RECTANGLE ? mBorderCornerOffset : 0);

      RectF rect = mCropWindowHandler.imageGetRect();
      rect.inset(w, w);

      float cornerOffset = (cornerWidth - lineWidth) / 2;
      float cornerExtension = cornerWidth / 2 + cornerOffset;

      canvas.drawLine(
          rect.left - cornerOffset,
          rect.top - cornerExtension,
          rect.left - cornerOffset,
          rect.top + mBorderCornerLength,
          mBorderCornerPaint);
      canvas.drawLine(
          rect.left - cornerExtension,
          rect.top - cornerOffset,
          rect.left + mBorderCornerLength,
          rect.top - cornerOffset,
          mBorderCornerPaint);

      canvas.drawLine(
          rect.right + cornerOffset,
          rect.top - cornerExtension,
          rect.right + cornerOffset,
          rect.top + mBorderCornerLength,
          mBorderCornerPaint);
      canvas.drawLine(
          rect.right + cornerExtension,
          rect.top - cornerOffset,
          rect.right - mBorderCornerLength,
          rect.top - cornerOffset,
          mBorderCornerPaint);

      canvas.drawLine(
          rect.left - cornerOffset,
          rect.bottom + cornerExtension,
          rect.left - cornerOffset,
          rect.bottom - mBorderCornerLength,
          mBorderCornerPaint);
      canvas.drawLine(
          rect.left - cornerExtension,
          rect.bottom + cornerOffset,
          rect.left + mBorderCornerLength,
          rect.bottom + cornerOffset,
          mBorderCornerPaint);

      canvas.drawLine(
          rect.right + cornerOffset,
          rect.bottom + cornerExtension,
          rect.right + cornerOffset,
          rect.bottom - mBorderCornerLength,
          mBorderCornerPaint);
      canvas.drawLine(
          rect.right + cornerExtension,
          rect.bottom + cornerOffset,
          rect.right - mBorderCornerLength,
          rect.bottom + cornerOffset,
          mBorderCornerPaint);
    }
  }
  private static Paint imageGetNewPaint(int color) {
    Paint paint = new Paint();
    paint.setColor(color);
    return paint;
  }
  private static Paint imageGetNewPaintOrNull(float thickness, int color) {
    if (thickness > 0) {
      Paint borderPaint = new Paint();
      borderPaint.setColor(color);
      borderPaint.setStrokeWidth(thickness);
      borderPaint.setStyle(Paint.Style.STROKE);
      borderPaint.setAntiAlias(true);
      return borderPaint;
    } else {
      return null;
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (isEnabled()) {
      if (mMultiTouchEnabled) {
        mScaleDetector.onTouchEvent(event);
      }

      switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
          imageOnActionDown(event.getX(), event.getY());
          return true;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
          getParent().requestDisallowInterceptTouchEvent(false);
          imageOnActionUp();
          return true;
        case MotionEvent.ACTION_MOVE:
          imageOnActionMove(event.getX(), event.getY());
          getParent().requestDisallowInterceptTouchEvent(true);
          return true;
        default:
          return false;
      }
    } else {
      return false;
    }
  }
  private void imageOnActionDown(float x, float y) {
    mMoveHandler = mCropWindowHandler.imageGetMoveHandler(x, y, mTouchRadius, mCropShape);
    if (mMoveHandler != null) {
      invalidate();
    }
  }
  private void imageOnActionUp() {
    if (mMoveHandler != null) {
      mMoveHandler = null;
      imageCallOnCropWindowChanged(false);
      invalidate();
    }
  }
  private void imageOnActionMove(float x, float y) {
    if (mMoveHandler != null) {
      float snapRadius = mSnapRadius;
      RectF rect = mCropWindowHandler.imageGetRect();

      if (imageCalculateBounds(rect)) {
        snapRadius = 0;
      }

      mMoveHandler.move(
          rect,
          x,
          y,
          mCalcBounds,
          mViewWidth,
          mViewHeight,
          snapRadius,
          mFixAspectRatio,
          mTargetAspectRatio);
      mCropWindowHandler.imageSetRect(rect);
      imageCallOnCropWindowChanged(true);
      invalidate();
    }
  }
  private boolean imageCalculateBounds(RectF rect) {

    float left = KeyboardBitmapUtils.imageGetRectLeft(mBoundsPoints);
    float top = KeyboardBitmapUtils.imageGetRectTop(mBoundsPoints);
    float right = KeyboardBitmapUtils.imageGetRectRight(mBoundsPoints);
    float bottom = KeyboardBitmapUtils.imageGetRectBottom(mBoundsPoints);

    if (!imageIsNonStraightAngleRotated()) {
      mCalcBounds.set(left, top, right, bottom);
      return false;
    } else {
      float x0 = mBoundsPoints[0];
      float y0 = mBoundsPoints[1];
      float x2 = mBoundsPoints[4];
      float y2 = mBoundsPoints[5];
      float x3 = mBoundsPoints[6];
      float y3 = mBoundsPoints[7];

      if (mBoundsPoints[7] < mBoundsPoints[1]) {
        if (mBoundsPoints[1] < mBoundsPoints[3]) {
          x0 = mBoundsPoints[6];
          y0 = mBoundsPoints[7];
          x2 = mBoundsPoints[2];
          y2 = mBoundsPoints[3];
          x3 = mBoundsPoints[4];
          y3 = mBoundsPoints[5];
        } else {
          x0 = mBoundsPoints[4];
          y0 = mBoundsPoints[5];
          x2 = mBoundsPoints[0];
          y2 = mBoundsPoints[1];
          x3 = mBoundsPoints[2];
          y3 = mBoundsPoints[3];
        }
      } else if (mBoundsPoints[1] > mBoundsPoints[3]) {
        x0 = mBoundsPoints[2];
        y0 = mBoundsPoints[3];
        x2 = mBoundsPoints[6];
        y2 = mBoundsPoints[7];
        x3 = mBoundsPoints[0];
        y3 = mBoundsPoints[1];
      }

      float a0 = (y3 - y0) / (x3 - x0);
      float a1 = -1f / a0;
      float b0 = y0 - a0 * x0;
      float b1 = y0 - a1 * x0;
      float b2 = y2 - a0 * x2;
      float b3 = y2 - a1 * x2;

      float c0 = (rect.centerY() - rect.top) / (rect.centerX() - rect.left);
      float c1 = -c0;
      float d0 = rect.top - c0 * rect.left;
      float d1 = rect.top - c1 * rect.right;

      left = Math.max(left, (d0 - b0) / (a0 - c0) < rect.right ? (d0 - b0) / (a0 - c0) : left);
      left = Math.max(left, (d0 - b1) / (a1 - c0) < rect.right ? (d0 - b1) / (a1 - c0) : left);
      left = Math.max(left, (d1 - b3) / (a1 - c1) < rect.right ? (d1 - b3) / (a1 - c1) : left);
      right = Math.min(right, (d1 - b1) / (a1 - c1) > rect.left ? (d1 - b1) / (a1 - c1) : right);
      right = Math.min(right, (d1 - b2) / (a0 - c1) > rect.left ? (d1 - b2) / (a0 - c1) : right);
      right = Math.min(right, (d0 - b2) / (a0 - c0) > rect.left ? (d0 - b2) / (a0 - c0) : right);

      top = Math.max(top, Math.max(a0 * left + b0, a1 * right + b1));
      bottom = Math.min(bottom, Math.min(a1 * left + b3, a0 * right + b2));

      mCalcBounds.left = left;
      mCalcBounds.top = top;
      mCalcBounds.right = right;
      mCalcBounds.bottom = bottom;
      return true;
    }
  }
  private boolean imageIsNonStraightAngleRotated() {
    return mBoundsPoints[0] != mBoundsPoints[6] && mBoundsPoints[1] != mBoundsPoints[7];
  }
  private void imageCallOnCropWindowChanged(boolean inProgress) {
    try {
      if (mCropWindowChangeListener != null) {
        mCropWindowChangeListener.imageOnCropWindowChanged(inProgress);
      }
    } catch (Exception e) {
      Log.e("AIC", "Exception in crop window changed", e);
    }
  }
  public interface CropWindowChangeListener {
    void imageOnCropWindowChanged(boolean inProgress);
  }
  private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public boolean onScale(ScaleGestureDetector detector) {
      RectF rect = mCropWindowHandler.imageGetRect();

      float x = detector.getFocusX();
      float y = detector.getFocusY();
      float dY = detector.getCurrentSpanY() / 2;
      float dX = detector.getCurrentSpanX() / 2;

      float newTop = y - dY;
      float newLeft = x - dX;
      float newRight = x + dX;
      float newBottom = y + dY;

      if (newLeft < newRight
          && newTop <= newBottom
          && newLeft >= 0
          && newRight <= mCropWindowHandler.imageGetMaxCropWidth()
          && newTop >= 0
          && newBottom <= mCropWindowHandler.imageGetMaxCropHeight()) {

        rect.set(newLeft, newTop, newRight, newBottom);
        mCropWindowHandler.imageSetRect(rect);
        invalidate();
      }

      return true;
    }
  }
}
