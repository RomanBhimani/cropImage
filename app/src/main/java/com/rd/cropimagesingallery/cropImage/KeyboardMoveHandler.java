
package com.rd.cropimagesingallery.cropImage;

import android.graphics.PointF;
import android.graphics.RectF;
final class KeyboardMoveHandler {
  private final float mMinCropWidth;
  private final float mMinCropHeight;
  private final float mMaxCropWidth;
  private final float mMaxCropHeight;
  private final Type mType;
  private final PointF mTouchOffset = new PointF();
  public KeyboardMoveHandler(
          Type type, KeyboardHandler cropWindowHandler, float touchX, float touchY) {
    mType = type;
    mMinCropWidth = cropWindowHandler.imageGetMinCropWidth();
    mMinCropHeight = cropWindowHandler.imageGetMinCropHeight();
    mMaxCropWidth = cropWindowHandler.imageGetMaxCropWidth();
    mMaxCropHeight = cropWindowHandler.imageGetMaxCropHeight();
    imageCalculateTouchOffset(cropWindowHandler.imageGetRect(), touchX, touchY);
  }
  public void move(
      RectF rect,
      float x,
      float y,
      RectF bounds,
      int viewWidth,
      int viewHeight,
      float snapMargin,
      boolean fixedAspectRatio,
      float aspectRatio) {
    float adjX = x + mTouchOffset.x;
    float adjY = y + mTouchOffset.y;

    if (mType == Type.CENTER) {
      imageMoveCenter(rect, adjX, adjY, bounds, viewWidth, viewHeight, snapMargin);
    } else {
      if (fixedAspectRatio) {
        imageMoveSizeWithFixedAspectRatio(
            rect, adjX, adjY, bounds, viewWidth, viewHeight, snapMargin, aspectRatio);
      } else {
        imageMoveSizeWithFreeAspectRatio(rect, adjX, adjY, bounds, viewWidth, viewHeight, snapMargin);
      }
    }
  }
  private void imageCalculateTouchOffset(RectF rect, float touchX, float touchY) {
    float touchOffsetX = 0;
    float touchOffsetY = 0;
    switch (mType) {
      case TOP_LEFT:
        touchOffsetX = rect.left - touchX;
        touchOffsetY = rect.top - touchY;
        break;
      case TOP_RIGHT:
        touchOffsetX = rect.right - touchX;
        touchOffsetY = rect.top - touchY;
        break;
      case BOTTOM_LEFT:
        touchOffsetX = rect.left - touchX;
        touchOffsetY = rect.bottom - touchY;
        break;
      case BOTTOM_RIGHT:
        touchOffsetX = rect.right - touchX;
        touchOffsetY = rect.bottom - touchY;
        break;
      case LEFT:
        touchOffsetX = rect.left - touchX;
        touchOffsetY = 0;
        break;
      case TOP:
        touchOffsetX = 0;
        touchOffsetY = rect.top - touchY;
        break;
      case RIGHT:
        touchOffsetX = rect.right - touchX;
        touchOffsetY = 0;
        break;
      case BOTTOM:
        touchOffsetX = 0;
        touchOffsetY = rect.bottom - touchY;
        break;
      case CENTER:
        touchOffsetX = rect.centerX() - touchX;
        touchOffsetY = rect.centerY() - touchY;
        break;
      default:
        break;
    }
    mTouchOffset.x = touchOffsetX;
    mTouchOffset.y = touchOffsetY;
  }
  private void imageMoveCenter(
      RectF rect, float x, float y, RectF bounds, int viewWidth, int viewHeight, float snapRadius) {
    float dx = x - rect.centerX();
    float dy = y - rect.centerY();
    if (rect.left + dx < 0
        || rect.right + dx > viewWidth
        || rect.left + dx < bounds.left
        || rect.right + dx > bounds.right) {
      dx /= 1.05f;
      mTouchOffset.x -= dx / 2;
    }
    if (rect.top + dy < 0
        || rect.bottom + dy > viewHeight
        || rect.top + dy < bounds.top
        || rect.bottom + dy > bounds.bottom) {
      dy /= 1.05f;
      mTouchOffset.y -= dy / 2;
    }
    rect.offset(dx, dy);
    imageSnapEdgesToBounds(rect, bounds, snapRadius);
  }
  private void imageMoveSizeWithFreeAspectRatio(
      RectF rect, float x, float y, RectF bounds, int viewWidth, int viewHeight, float snapMargin) {
    switch (mType) {
      case TOP_LEFT:
        imageAdjustTop(rect, y, bounds, snapMargin, 0, false, false);
        imageAdjustLeft(rect, x, bounds, snapMargin, 0, false, false);
        break;
      case TOP_RIGHT:
        imageAdjustTop(rect, y, bounds, snapMargin, 0, false, false);
        imageAdjustRight(rect, x, bounds, viewWidth, snapMargin, 0, false, false);
        break;
      case BOTTOM_LEFT:
        imageAdjustBottom(rect, y, bounds, viewHeight, snapMargin, 0, false, false);
        imageAdjustLeft(rect, x, bounds, snapMargin, 0, false, false);
        break;
      case BOTTOM_RIGHT:
        imageAdjustBottom(rect, y, bounds, viewHeight, snapMargin, 0, false, false);
        imageAdjustRight(rect, x, bounds, viewWidth, snapMargin, 0, false, false);
        break;
      case LEFT:
        imageAdjustLeft(rect, x, bounds, snapMargin, 0, false, false);
        break;
      case TOP:
        imageAdjustTop(rect, y, bounds, snapMargin, 0, false, false);
        break;
      case RIGHT:
        imageAdjustRight(rect, x, bounds, viewWidth, snapMargin, 0, false, false);
        break;
      case BOTTOM:
        imageAdjustBottom(rect, y, bounds, viewHeight, snapMargin, 0, false, false);
        break;
      default:
        break;
    }
  }
  private void imageMoveSizeWithFixedAspectRatio(
      RectF rect,
      float x,
      float y,
      RectF bounds,
      int viewWidth,
      int viewHeight,
      float snapMargin,
      float aspectRatio) {
    switch (mType) {
      case TOP_LEFT:
        if (imageCalculateAspectRatio(x, y, rect.right, rect.bottom) < aspectRatio) {
          imageAdjustTop(rect, y, bounds, snapMargin, aspectRatio, true, false);
          imageAdjustLeftByAspectRatio(rect, aspectRatio);
        } else {
          imageAdjustLeft(rect, x, bounds, snapMargin, aspectRatio, true, false);
          imageAdjustTopByAspectRatio(rect, aspectRatio);
        }
        break;
      case TOP_RIGHT:
        if (imageCalculateAspectRatio(rect.left, y, x, rect.bottom) < aspectRatio) {
          imageAdjustTop(rect, y, bounds, snapMargin, aspectRatio, false, true);
          imageAdjustRightByAspectRatio(rect, aspectRatio);
        } else {
          imageAdjustRight(rect, x, bounds, viewWidth, snapMargin, aspectRatio, true, false);
          imageAdjustTopByAspectRatio(rect, aspectRatio);
        }
        break;
      case BOTTOM_LEFT:
        if (imageCalculateAspectRatio(x, rect.top, rect.right, y) < aspectRatio) {
          imageAdjustBottom(rect, y, bounds, viewHeight, snapMargin, aspectRatio, true, false);
          imageAdjustLeftByAspectRatio(rect, aspectRatio);
        } else {
          imageAdjustLeft(rect, x, bounds, snapMargin, aspectRatio, false, true);
          imageAdjustBottomByAspectRatio(rect, aspectRatio);
        }
        break;
      case BOTTOM_RIGHT:
        if (imageCalculateAspectRatio(rect.left, rect.top, x, y) < aspectRatio) {
          imageAdjustBottom(rect, y, bounds, viewHeight, snapMargin, aspectRatio, false, true);
          imageAdjustRightByAspectRatio(rect, aspectRatio);
        } else {
          imageAdjustRight(rect, x, bounds, viewWidth, snapMargin, aspectRatio, false, true);
          imageAdjustBottomByAspectRatio(rect, aspectRatio);
        }
        break;
      case LEFT:
        imageAdjustLeft(rect, x, bounds, snapMargin, aspectRatio, true, true);
        imageAdjustTopBottomByAspectRatio(rect, bounds, aspectRatio);
        break;
      case TOP:
        imageAdjustTop(rect, y, bounds, snapMargin, aspectRatio, true, true);
        imageAdjustLeftRightByAspectRatio(rect, bounds, aspectRatio);
        break;
      case RIGHT:
        imageAdjustRight(rect, x, bounds, viewWidth, snapMargin, aspectRatio, true, true);
        imageAdjustTopBottomByAspectRatio(rect, bounds, aspectRatio);
        break;
      case BOTTOM:
        imageAdjustBottom(rect, y, bounds, viewHeight, snapMargin, aspectRatio, true, true);
        imageAdjustLeftRightByAspectRatio(rect, bounds, aspectRatio);
        break;
      default:
        break;
    }
  }
  private void imageSnapEdgesToBounds(RectF edges, RectF bounds, float margin) {
    if (edges.left < bounds.left + margin) {
      edges.offset(bounds.left - edges.left, 0);
    }
    if (edges.top < bounds.top + margin) {
      edges.offset(0, bounds.top - edges.top);
    }
    if (edges.right > bounds.right - margin) {
      edges.offset(bounds.right - edges.right, 0);
    }
    if (edges.bottom > bounds.bottom - margin) {
      edges.offset(0, bounds.bottom - edges.bottom);
    }
  }
  private void imageAdjustLeft(
      RectF rect,
      float left,
      RectF bounds,
      float snapMargin,
      float aspectRatio,
      boolean topMoves,
      boolean bottomMoves) {
    float newLeft = left;
    if (newLeft < 0) {
      newLeft /= 1.05f;
      mTouchOffset.x -= newLeft / 1.1f;
    }
    if (newLeft < bounds.left) {
      mTouchOffset.x -= (newLeft - bounds.left) / 2f;
    }
    if (newLeft - bounds.left < snapMargin) {
      newLeft = bounds.left;
    }
    if (rect.right - newLeft < mMinCropWidth) {
      newLeft = rect.right - mMinCropWidth;
    }
    if (rect.right - newLeft > mMaxCropWidth) {
      newLeft = rect.right - mMaxCropWidth;
    }
    if (newLeft - bounds.left < snapMargin) {
      newLeft = bounds.left;
    }
    if (aspectRatio > 0) {
      float newHeight = (rect.right - newLeft) / aspectRatio;
      if (newHeight < mMinCropHeight) {
        newLeft = Math.max(bounds.left, rect.right - mMinCropHeight * aspectRatio);
        newHeight = (rect.right - newLeft) / aspectRatio;
      }
      if (newHeight > mMaxCropHeight) {
        newLeft = Math.max(bounds.left, rect.right - mMaxCropHeight * aspectRatio);
        newHeight = (rect.right - newLeft) / aspectRatio;
      }
      if (topMoves && bottomMoves) {
        newLeft =
            Math.max(newLeft, Math.max(bounds.left, rect.right - bounds.height() * aspectRatio));
      } else {
        if (topMoves && rect.bottom - newHeight < bounds.top) {
          newLeft = Math.max(bounds.left, rect.right - (rect.bottom - bounds.top) * aspectRatio);
          newHeight = (rect.right - newLeft) / aspectRatio;
        }
        if (bottomMoves && rect.top + newHeight > bounds.bottom) {
          newLeft =
              Math.max(
                  newLeft,
                  Math.max(bounds.left, rect.right - (bounds.bottom - rect.top) * aspectRatio));
        }
      }
    }
    rect.left = newLeft;
  }
  private void imageAdjustRight(
      RectF rect,
      float right,
      RectF bounds,
      int viewWidth,
      float snapMargin,
      float aspectRatio,
      boolean topMoves,
      boolean bottomMoves) {

    float newRight = right;

    if (newRight > viewWidth) {
      newRight = viewWidth + (newRight - viewWidth) / 1.05f;
      mTouchOffset.x -= (newRight - viewWidth) / 1.1f;
    }

    if (newRight > bounds.right) {
      mTouchOffset.x -= (newRight - bounds.right) / 2f;
    }
    if (bounds.right - newRight < snapMargin) {
      newRight = bounds.right;
    }
    if (newRight - rect.left < mMinCropWidth) {
      newRight = rect.left + mMinCropWidth;
    }
    if (newRight - rect.left > mMaxCropWidth) {
      newRight = rect.left + mMaxCropWidth;
    }
    if (bounds.right - newRight < snapMargin) {
      newRight = bounds.right;
    }
    if (aspectRatio > 0) {
      float newHeight = (newRight - rect.left) / aspectRatio;
      if (newHeight < mMinCropHeight) {
        newRight = Math.min(bounds.right, rect.left + mMinCropHeight * aspectRatio);
        newHeight = (newRight - rect.left) / aspectRatio;
      }
      if (newHeight > mMaxCropHeight) {
        newRight = Math.min(bounds.right, rect.left + mMaxCropHeight * aspectRatio);
        newHeight = (newRight - rect.left) / aspectRatio;
      }
      if (topMoves && bottomMoves) {
        newRight =
            Math.min(newRight, Math.min(bounds.right, rect.left + bounds.height() * aspectRatio));
      } else {
        if (topMoves && rect.bottom - newHeight < bounds.top) {
          newRight = Math.min(bounds.right, rect.left + (rect.bottom - bounds.top) * aspectRatio);
          newHeight = (newRight - rect.left) / aspectRatio;
        }
        if (bottomMoves && rect.top + newHeight > bounds.bottom) {
          newRight =
              Math.min(
                  newRight,
                  Math.min(bounds.right, rect.left + (bounds.bottom - rect.top) * aspectRatio));
        }
      }
    }
    rect.right = newRight;
  }
  private void imageAdjustTop(
      RectF rect,
      float top,
      RectF bounds,
      float snapMargin,
      float aspectRatio,
      boolean leftMoves,
      boolean rightMoves) {

    float newTop = top;

    if (newTop < 0) {
      newTop /= 1.05f;
      mTouchOffset.y -= newTop / 1.1f;
    }

    if (newTop < bounds.top) {
      mTouchOffset.y -= (newTop - bounds.top) / 2f;
    }

    if (newTop - bounds.top < snapMargin) {
      newTop = bounds.top;
    }

    if (rect.bottom - newTop < mMinCropHeight) {
      newTop = rect.bottom - mMinCropHeight;
    }

    if (rect.bottom - newTop > mMaxCropHeight) {
      newTop = rect.bottom - mMaxCropHeight;
    }

    if (newTop - bounds.top < snapMargin) {
      newTop = bounds.top;
    }

    if (aspectRatio > 0) {
      float newWidth = (rect.bottom - newTop) * aspectRatio;

      if (newWidth < mMinCropWidth) {
        newTop = Math.max(bounds.top, rect.bottom - (mMinCropWidth / aspectRatio));
        newWidth = (rect.bottom - newTop) * aspectRatio;
      }

      if (newWidth > mMaxCropWidth) {
        newTop = Math.max(bounds.top, rect.bottom - (mMaxCropWidth / aspectRatio));
        newWidth = (rect.bottom - newTop) * aspectRatio;
      }

      if (leftMoves && rightMoves) {
        newTop = Math.max(newTop, Math.max(bounds.top, rect.bottom - bounds.width() / aspectRatio));
      } else {
        if (leftMoves && rect.right - newWidth < bounds.left) {
          newTop = Math.max(bounds.top, rect.bottom - (rect.right - bounds.left) / aspectRatio);
          newWidth = (rect.bottom - newTop) * aspectRatio;
        }

        if (rightMoves && rect.left + newWidth > bounds.right) {
          newTop =
              Math.max(
                  newTop,
                  Math.max(bounds.top, rect.bottom - (bounds.right - rect.left) / aspectRatio));
        }
      }
    }

    rect.top = newTop;
  }
  private void imageAdjustBottom(
      RectF rect,
      float bottom,
      RectF bounds,
      int viewHeight,
      float snapMargin,
      float aspectRatio,
      boolean leftMoves,
      boolean rightMoves) {

    float newBottom = bottom;

    if (newBottom > viewHeight) {
      newBottom = viewHeight + (newBottom - viewHeight) / 1.05f;
      mTouchOffset.y -= (newBottom - viewHeight) / 1.1f;
    }

    if (newBottom > bounds.bottom) {
      mTouchOffset.y -= (newBottom - bounds.bottom) / 2f;
    }

    if (bounds.bottom - newBottom < snapMargin) {
      newBottom = bounds.bottom;
    }

    if (newBottom - rect.top < mMinCropHeight) {
      newBottom = rect.top + mMinCropHeight;
    }

    if (newBottom - rect.top > mMaxCropHeight) {
      newBottom = rect.top + mMaxCropHeight;
    }

    if (bounds.bottom - newBottom < snapMargin) {
      newBottom = bounds.bottom;
    }

    if (aspectRatio > 0) {
      float newWidth = (newBottom - rect.top) * aspectRatio;

      if (newWidth < mMinCropWidth) {
        newBottom = Math.min(bounds.bottom, rect.top + mMinCropWidth / aspectRatio);
        newWidth = (newBottom - rect.top) * aspectRatio;
      }

      if (newWidth > mMaxCropWidth) {
        newBottom = Math.min(bounds.bottom, rect.top + mMaxCropWidth / aspectRatio);
        newWidth = (newBottom - rect.top) * aspectRatio;
      }

      if (leftMoves && rightMoves) {
        newBottom =
            Math.min(newBottom, Math.min(bounds.bottom, rect.top + bounds.width() / aspectRatio));
      } else {
        if (leftMoves && rect.right - newWidth < bounds.left) {
          newBottom = Math.min(bounds.bottom, rect.top + (rect.right - bounds.left) / aspectRatio);
          newWidth = (newBottom - rect.top) * aspectRatio;
        }

        if (rightMoves && rect.left + newWidth > bounds.right) {
          newBottom =
              Math.min(
                  newBottom,
                  Math.min(bounds.bottom, rect.top + (bounds.right - rect.left) / aspectRatio));
        }
      }
    }

    rect.bottom = newBottom;
  }
  private void imageAdjustLeftByAspectRatio(RectF rect, float aspectRatio) {
    rect.left = rect.right - rect.height() * aspectRatio;
  }
  private void imageAdjustTopByAspectRatio(RectF rect, float aspectRatio) {
    rect.top = rect.bottom - rect.width() / aspectRatio;
  }
  private void imageAdjustRightByAspectRatio(RectF rect, float aspectRatio) {
    rect.right = rect.left + rect.height() * aspectRatio;
  }
  private void imageAdjustBottomByAspectRatio(RectF rect, float aspectRatio) {
    rect.bottom = rect.top + rect.width() / aspectRatio;
  }
  private void imageAdjustLeftRightByAspectRatio(RectF rect, RectF bounds, float aspectRatio) {
    rect.inset((rect.width() - rect.height() * aspectRatio) / 2, 0);
    if (rect.left < bounds.left) {
      rect.offset(bounds.left - rect.left, 0);
    }
    if (rect.right > bounds.right) {
      rect.offset(bounds.right - rect.right, 0);
    }
  }
  private void imageAdjustTopBottomByAspectRatio(RectF rect, RectF bounds, float aspectRatio) {
    rect.inset(0, (rect.height() - rect.width() / aspectRatio) / 2);
    if (rect.top < bounds.top) {
      rect.offset(0, bounds.top - rect.top);
    }
    if (rect.bottom > bounds.bottom) {
      rect.offset(0, bounds.bottom - rect.bottom);
    }
  }
  private static float imageCalculateAspectRatio(float left, float top, float right, float bottom) {
    return (right - left) / (bottom - top);
  }
  public enum Type {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    LEFT,
    TOP,
    RIGHT,
    BOTTOM,
    CENTER
  }
}
