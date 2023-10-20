
package com.rd.cropimagesingallery.cropImage;

import android.graphics.RectF;
final class KeyboardHandler {
  private final RectF mEdges = new RectF();
  private final RectF mGetEdges = new RectF();
  private float mMinCropWindowWidth;
  private float mMinCropWindowHeight;
  private float mMaxCropWindowWidth;
  private float mMaxCropWindowHeight;
  private float mMinCropResultWidth;
  private float mMinCropResultHeight;
  private float mMaxCropResultWidth;
  private float mMaxCropResultHeight;
  private float mScaleFactorWidth = 1;
  private float mScaleFactorHeight = 1;
  public RectF imageGetRect() {
    mGetEdges.set(mEdges);
    return mGetEdges;
  }
  public float imageGetMinCropWidth() {
    return Math.max(mMinCropWindowWidth, mMinCropResultWidth / mScaleFactorWidth);
  }
  public float imageGetMinCropHeight() {
    return Math.max(mMinCropWindowHeight, mMinCropResultHeight / mScaleFactorHeight);
  }
  public float imageGetMaxCropWidth() {
    return Math.min(mMaxCropWindowWidth, mMaxCropResultWidth / mScaleFactorWidth);
  }
  public float imageGetMaxCropHeight() {
    return Math.min(mMaxCropWindowHeight, mMaxCropResultHeight / mScaleFactorHeight);
  }
  public float imageGetScaleFactorWidth() {
    return mScaleFactorWidth;
  }
  public float imageGetScaleFactorHeight() {
    return mScaleFactorHeight;
  }
  public void imageSetCropWindowLimits(
      float maxWidth, float maxHeight, float scaleFactorWidth, float scaleFactorHeight) {
    mMaxCropWindowWidth = maxWidth;
    mMaxCropWindowHeight = maxHeight;
    mScaleFactorWidth = scaleFactorWidth;
    mScaleFactorHeight = scaleFactorHeight;
  }
  public void imageSetInitialAttributeValues(KeyboardCropImageOptions options) {
    mMinCropWindowWidth = options.minCropWindowWidth;
    mMinCropWindowHeight = options.minCropWindowHeight;
    mMinCropResultWidth = options.minCropResultWidth;
    mMinCropResultHeight = options.minCropResultHeight;
    mMaxCropResultWidth = options.maxCropResultWidth;
    mMaxCropResultHeight = options.maxCropResultHeight;
  }
  public void imageSetRect(RectF rect) {
    mEdges.set(rect);
  }
  public boolean imageShowGuidelines() {
    return !(mEdges.width() < 100 || mEdges.height() < 100);
  }
  public KeyboardMoveHandler imageGetMoveHandler(
      float x, float y, float targetRadius, KeyboardCropImageView.imageCropShape cropShape) {
    KeyboardMoveHandler.Type type =
        cropShape == KeyboardCropImageView.imageCropShape.OVAL
            ? imageGetOvalPressedMoveType(x, y)
            : imageGetRectanglePressedMoveType(x, y, targetRadius);
    return type != null ? new KeyboardMoveHandler(type, this, x, y) : null;
  }
  private KeyboardMoveHandler.Type imageGetRectanglePressedMoveType(
      float x, float y, float targetRadius) {
    KeyboardMoveHandler.Type moveType = null;
    if (KeyboardHandler.imageIsInCornerTargetZone(x, y, mEdges.left, mEdges.top, targetRadius)) {
      moveType = KeyboardMoveHandler.Type.TOP_LEFT;
    } else if (KeyboardHandler.imageIsInCornerTargetZone(
        x, y, mEdges.right, mEdges.top, targetRadius)) {
      moveType = KeyboardMoveHandler.Type.TOP_RIGHT;
    } else if (KeyboardHandler.imageIsInCornerTargetZone(
        x, y, mEdges.left, mEdges.bottom, targetRadius)) {
      moveType = KeyboardMoveHandler.Type.BOTTOM_LEFT;
    } else if (KeyboardHandler.imageIsInCornerTargetZone(
        x, y, mEdges.right, mEdges.bottom, targetRadius)) {
      moveType = KeyboardMoveHandler.Type.BOTTOM_RIGHT;
    } else if (KeyboardHandler.imageIsInCenterTargetZone(
            x, y, mEdges.left, mEdges.top, mEdges.right, mEdges.bottom)
        && imageFocusCenter()) {
      moveType = KeyboardMoveHandler.Type.CENTER;
    } else if (KeyboardHandler.imageIsInHorizontalTargetZone(
        x, y, mEdges.left, mEdges.right, mEdges.top, targetRadius)) {
      moveType = KeyboardMoveHandler.Type.TOP;
    } else if (KeyboardHandler.imageIsInHorizontalTargetZone(
        x, y, mEdges.left, mEdges.right, mEdges.bottom, targetRadius)) {
      moveType = KeyboardMoveHandler.Type.BOTTOM;
    } else if (KeyboardHandler.imageIsInVerticalTargetZone(
        x, y, mEdges.left, mEdges.top, mEdges.bottom, targetRadius)) {
      moveType = KeyboardMoveHandler.Type.LEFT;
    } else if (KeyboardHandler.imageIsInVerticalTargetZone(
        x, y, mEdges.right, mEdges.top, mEdges.bottom, targetRadius)) {
      moveType = KeyboardMoveHandler.Type.RIGHT;
    } else if (KeyboardHandler.imageIsInCenterTargetZone(
            x, y, mEdges.left, mEdges.top, mEdges.right, mEdges.bottom)
        && !imageFocusCenter()) {
      moveType = KeyboardMoveHandler.Type.CENTER;
    }
    return moveType;
  }
  private KeyboardMoveHandler.Type imageGetOvalPressedMoveType(float x, float y) {
    float cellLength = mEdges.width() / 6;
    float leftCenter = mEdges.left + cellLength;
    float rightCenter = mEdges.left + (5 * cellLength);

    float cellHeight = mEdges.height() / 6;
    float topCenter = mEdges.top + cellHeight;
    float bottomCenter = mEdges.top + 5 * cellHeight;

    KeyboardMoveHandler.Type moveType;
    if (x < leftCenter) {
      if (y < topCenter) {
        moveType = KeyboardMoveHandler.Type.TOP_LEFT;
      } else if (y < bottomCenter) {
        moveType = KeyboardMoveHandler.Type.LEFT;
      } else {
        moveType = KeyboardMoveHandler.Type.BOTTOM_LEFT;
      }
    } else if (x < rightCenter) {
      if (y < topCenter) {
        moveType = KeyboardMoveHandler.Type.TOP;
      } else if (y < bottomCenter) {
        moveType = KeyboardMoveHandler.Type.CENTER;
      } else {
        moveType = KeyboardMoveHandler.Type.BOTTOM;
      }
    } else {
      if (y < topCenter) {
        moveType = KeyboardMoveHandler.Type.TOP_RIGHT;
      } else if (y < bottomCenter) {
        moveType = KeyboardMoveHandler.Type.RIGHT;
      } else {
        moveType = KeyboardMoveHandler.Type.BOTTOM_RIGHT;
      }
    }
    return moveType;
  }
  private static boolean imageIsInCornerTargetZone(
      float x, float y, float handleX, float handleY, float targetRadius) {
    return Math.abs(x - handleX) <= targetRadius && Math.abs(y - handleY) <= targetRadius;
  }
  private static boolean imageIsInHorizontalTargetZone(
      float x, float y, float handleXStart, float handleXEnd, float handleY, float targetRadius) {
    return x > handleXStart && x < handleXEnd && Math.abs(y - handleY) <= targetRadius;
  }
  private static boolean imageIsInVerticalTargetZone(
      float x, float y, float handleX, float handleYStart, float handleYEnd, float targetRadius) {
    return Math.abs(x - handleX) <= targetRadius && y > handleYStart && y < handleYEnd;
  }
  private static boolean imageIsInCenterTargetZone(
      float x, float y, float left, float top, float right, float bottom) {
    return x > left && x < right && y > top && y < bottom;
  }
  private boolean imageFocusCenter() {
    return !imageShowGuidelines();
  }
}
