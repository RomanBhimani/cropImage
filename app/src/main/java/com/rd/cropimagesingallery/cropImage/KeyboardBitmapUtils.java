package com.rd.cropimagesingallery.cropImage;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
final class KeyboardBitmapUtils {

  static final Rect EMPTY_RECT = new Rect();
  static final RectF EMPTY_RECT_F = new RectF();
  static final RectF RECT = new RectF();
  static final float[] POINTS = new float[6];
  static final float[] POINTS2 = new float[6];
  private static int mMaxTextureSize;
  static Pair<String, WeakReference<Bitmap>> mStateBitmap;
  static imageRotateBitmapResult imageRotateBitmapByExif(Bitmap bitmap, Context context, Uri uri) {
    ExifInterface ei = null;
    try {
      InputStream is = context.getContentResolver().openInputStream(uri);
      if (is != null) {
        ei = new ExifInterface(is);
        is.close();
      }
    } catch (Exception ignored) {
    }
    return ei != null ? imageRotateBitmapByExif(bitmap, ei) : new imageRotateBitmapResult(bitmap, 0);
  }
  static imageRotateBitmapResult imageRotateBitmapByExif(Bitmap bitmap, ExifInterface exif) {
    int degrees;
    int orientation =
        exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
    switch (orientation) {
      case ExifInterface.ORIENTATION_ROTATE_90:
        degrees = 90;
        break;
      case ExifInterface.ORIENTATION_ROTATE_180:
        degrees = 180;
        break;
      case ExifInterface.ORIENTATION_ROTATE_270:
        degrees = 270;
        break;
      default:
        degrees = 0;
        break;
    }
    return new imageRotateBitmapResult(bitmap, degrees);
  }
  static imageBitmapSampled imageDecodeSampledBitmap(Context context, Uri uri, int reqWidth, int reqHeight) {

    try {
      ContentResolver resolver = context.getContentResolver();

      BitmapFactory.Options options = imageDecodeImageForOption(resolver, uri);

      if(options.outWidth  == -1 && options.outHeight == -1)
        throw new RuntimeException("File is not a picture");

      options.inSampleSize =
          Math.max(
              imageCalculateInSampleSizeByReqestedSize(
                  options.outWidth, options.outHeight, reqWidth, reqHeight),
              imageCalculateInSampleSizeByMaxTextureSize(options.outWidth, options.outHeight));

      Bitmap bitmap = imageDecodeImage(resolver, uri, options);

      return new imageBitmapSampled(bitmap, options.inSampleSize);

    } catch (Exception e) {
      throw new RuntimeException(
          "Failed to load sampled bitmap: " + uri + "\r\n" + e.getMessage(), e);
    }
  }
  static imageBitmapSampled imageCropBitmapObjectHandleOOM(
      Bitmap bitmap,
      float[] points,
      int degreesRotated,
      boolean fixAspectRatio,
      int aspectRatioX,
      int aspectRatioY,
      boolean flipHorizontally,
      boolean flipVertically) {
    int scale = 1;
    while (true) {
      try {
        Bitmap cropBitmap =
            imageCropBitmapObjectWithScale(
                bitmap,
                points,
                degreesRotated,
                fixAspectRatio,
                aspectRatioX,
                aspectRatioY,
                1 / (float) scale,
                flipHorizontally,
                flipVertically);
        return new imageBitmapSampled(cropBitmap, scale);
      } catch (OutOfMemoryError e) {
        scale *= 2;
        if (scale > 8) {
          throw e;
        }
      }
    }
  }
  private static Bitmap imageCropBitmapObjectWithScale(
      Bitmap bitmap,
      float[] points,
      int degreesRotated,
      boolean fixAspectRatio,
      int aspectRatioX,
      int aspectRatioY,
      float scale,
      boolean flipHorizontally,
      boolean flipVertically) {

    Rect rect =
        imageGetRectFromPoints(
            points,
            bitmap.getWidth(),
            bitmap.getHeight(),
            fixAspectRatio,
            aspectRatioX,
            aspectRatioY);

    Matrix matrix = new Matrix();
    matrix.setRotate(degreesRotated, bitmap.getWidth() / 2, bitmap.getHeight() / 2);
    matrix.postScale(flipHorizontally ? -scale : scale, flipVertically ? -scale : scale);
    Bitmap result =
        Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height(), matrix, true);
    if (result == bitmap) {
      result = bitmap.copy(bitmap.getConfig(), false);
    }
    if (degreesRotated % 90 != 0) {
      result =
          imageCropForRotatedImage(
              result, points, rect, degreesRotated, fixAspectRatio, aspectRatioX, aspectRatioY);
    }

    return result;
  }
  static imageBitmapSampled imageCropBitmap(
      Context context,
      Uri loadedImageUri,
      float[] points,
      int degreesRotated,
      int orgWidth,
      int orgHeight,
      boolean fixAspectRatio,
      int aspectRatioX,
      int aspectRatioY,
      int reqWidth,
      int reqHeight,
      boolean flipHorizontally,
      boolean flipVertically) {
    int sampleMulti = 1;
    while (true) {
      try {
        return imageCropBitmap(
            context,
            loadedImageUri,
            points,
            degreesRotated,
            orgWidth,
            orgHeight,
            fixAspectRatio,
            aspectRatioX,
            aspectRatioY,
            reqWidth,
            reqHeight,
            flipHorizontally,
            flipVertically,
            sampleMulti);
      } catch (OutOfMemoryError e) {
        sampleMulti *= 2;
        if (sampleMulti > 16) {
          throw new RuntimeException(
              "Failed to handle OOM by sampling ("
                  + sampleMulti
                  + "): "
                  + loadedImageUri
                  + "\r\n"
                  + e.getMessage(),
              e);
        }
      }
    }
  }
  static float imageGetRectLeft(float[] points) {
    return Math.min(Math.min(Math.min(points[0], points[2]), points[4]), points[6]);
  }
  static float imageGetRectTop(float[] points) {
    return Math.min(Math.min(Math.min(points[1], points[3]), points[5]), points[7]);
  }
  static float imageGetRectRight(float[] points) {
    return Math.max(Math.max(Math.max(points[0], points[2]), points[4]), points[6]);
  }
  static float imageGetRectBottom(float[] points) {
    return Math.max(Math.max(Math.max(points[1], points[3]), points[5]), points[7]);
  }
  static float imageGetRectWidth(float[] points) {
    return imageGetRectRight(points) - imageGetRectLeft(points);
  }

  static float imageGetRectHeight(float[] points) {
    return imageGetRectBottom(points) - imageGetRectTop(points);
  }

  static float imageGetRectCenterX(float[] points) {
    return (imageGetRectRight(points) + imageGetRectLeft(points)) / 2f;
  }

  static float imageGetRectCenterY(float[] points) {
    return (imageGetRectBottom(points) + imageGetRectTop(points)) / 2f;
  }
  static Rect imageGetRectFromPoints(
      float[] points,
      int imageWidth,
      int imageHeight,
      boolean fixAspectRatio,
      int aspectRatioX,
      int aspectRatioY) {
    int left = Math.round(Math.max(0, imageGetRectLeft(points)));
    int top = Math.round(Math.max(0, imageGetRectTop(points)));
    int right = Math.round(Math.min(imageWidth, imageGetRectRight(points)));
    int bottom = Math.round(Math.min(imageHeight, imageGetRectBottom(points)));

    Rect rect = new Rect(left, top, right, bottom);
    if (fixAspectRatio) {
      imageFixRectForAspectRatio(rect, aspectRatioX, aspectRatioY);
    }

    return rect;
  }
  private static void imageFixRectForAspectRatio(Rect rect, int aspectRatioX, int aspectRatioY) {
    if (aspectRatioX == aspectRatioY && rect.width() != rect.height()) {
      if (rect.height() > rect.width()) {
        rect.bottom -= rect.height() - rect.width();
      } else {
        rect.right -= rect.width() - rect.height();
      }
    }
  }
  static Uri imageWriteTempStateStoreBitmap(Context context, Bitmap bitmap, Uri uri) {
    try {
      boolean needSave = true;
      if (uri == null) {
        uri =
            Uri.fromFile(
                File.createTempFile("aic_state_store_temp", ".jpg", context.getCacheDir()));
      } else if (new File(uri.getPath()).exists()) {
        needSave = false;
      }
      if (needSave) {
        imageWriteBitmapToUri(context, bitmap, uri, Bitmap.CompressFormat.JPEG, 95);
      }
      return uri;
    } catch (Exception e) {
      Log.w("AIC", "Failed to write bitmap to temp file for image-cropper save instance state", e);
      return null;
    }
  }
  static void imageWriteBitmapToUri(
      Context context,
      Bitmap bitmap,
      Uri uri,
      Bitmap.CompressFormat compressFormat,
      int compressQuality)
      throws FileNotFoundException {
    OutputStream outputStream = null;
    try {
      outputStream = context.getContentResolver().openOutputStream(uri);
      bitmap.compress(compressFormat, compressQuality, outputStream);
    } finally {
      imageCloseSafe(outputStream);
    }
  }
  static Bitmap imageResizeBitmap(
      Bitmap bitmap, int reqWidth, int reqHeight, KeyboardCropImageView.imageRequestSizeOptions options) {
    try {
      if (reqWidth > 0
          && reqHeight > 0
          && (options == KeyboardCropImageView.imageRequestSizeOptions.RESIZE_FIT
              || options == KeyboardCropImageView.imageRequestSizeOptions.RESIZE_INSIDE
              || options == KeyboardCropImageView.imageRequestSizeOptions.RESIZE_EXACT)) {

        Bitmap resized = null;
        if (options == KeyboardCropImageView.imageRequestSizeOptions.RESIZE_EXACT) {
          resized = Bitmap.createScaledBitmap(bitmap, reqWidth, reqHeight, false);
        } else {
          int width = bitmap.getWidth();
          int height = bitmap.getHeight();
          float scale = Math.max(width / (float) reqWidth, height / (float) reqHeight);
          if (scale > 1 || options == KeyboardCropImageView.imageRequestSizeOptions.RESIZE_FIT) {
            resized =
                Bitmap.createScaledBitmap(
                    bitmap, (int) (width / scale), (int) (height / scale), false);
          }
        }
        if (resized != null) {
          if (resized != bitmap) {
            bitmap.recycle();
          }
          return resized;
        }
      }
    } catch (Exception e) {
      Log.w("AIC", "Failed to resize cropped image, return bitmap before resize", e);
    }
    return bitmap;
  }
  private static imageBitmapSampled imageCropBitmap(
      Context context,
      Uri loadedImageUri,
      float[] points,
      int degreesRotated,
      int orgWidth,
      int orgHeight,
      boolean fixAspectRatio,
      int aspectRatioX,
      int aspectRatioY,
      int reqWidth,
      int reqHeight,
      boolean flipHorizontally,
      boolean flipVertically,
      int sampleMulti) {

    Rect rect =
        imageGetRectFromPoints(points, orgWidth, orgHeight, fixAspectRatio, aspectRatioX, aspectRatioY);

    int width = reqWidth > 0 ? reqWidth : rect.width();
    int height = reqHeight > 0 ? reqHeight : rect.height();

    Bitmap result = null;
    int sampleSize = 1;
    try {
      imageBitmapSampled bitmapSampled =
          imageDecodeSampledBitmapRegion(context, loadedImageUri, rect, width, height, sampleMulti);
      result = bitmapSampled.bitmap;
      sampleSize = bitmapSampled.sampleSize;
    } catch (Exception ignored) {
    }

    if (result != null) {
      try {
        result = imageRotateAndFlipBitmapInt(result, degreesRotated, flipHorizontally, flipVertically);
        if (degreesRotated % 90 != 0) {
          result =
              imageCropForRotatedImage(
                  result, points, rect, degreesRotated, fixAspectRatio, aspectRatioX, aspectRatioY);
        }
      } catch (OutOfMemoryError e) {
        if (result != null) {
          result.recycle();
        }
        throw e;
      }
      return new imageBitmapSampled(result, sampleSize);
    } else {
      return imageCropBitmap(
          context,
          loadedImageUri,
          points,
          degreesRotated,
          fixAspectRatio,
          aspectRatioX,
          aspectRatioY,
          sampleMulti,
          rect,
          width,
          height,
          flipHorizontally,
          flipVertically);
    }
  }
  private static imageBitmapSampled imageCropBitmap(
      Context context,
      Uri loadedImageUri,
      float[] points,
      int degreesRotated,
      boolean fixAspectRatio,
      int aspectRatioX,
      int aspectRatioY,
      int sampleMulti,
      Rect rect,
      int width,
      int height,
      boolean flipHorizontally,
      boolean flipVertically) {
    Bitmap result = null;
    int sampleSize;
    try {
      BitmapFactory.Options options = new BitmapFactory.Options();
      options.inSampleSize =
          sampleSize =
              sampleMulti
                  * imageCalculateInSampleSizeByReqestedSize(rect.width(), rect.height(), width, height);

      Bitmap fullBitmap = imageDecodeImage(context.getContentResolver(), loadedImageUri, options);
      if (fullBitmap != null) {
        try {
          float[] points2 = new float[points.length];
          System.arraycopy(points, 0, points2, 0, points.length);
          for (int i = 0; i < points2.length; i++) {
            points2[i] = points2[i] / options.inSampleSize;
          }

          result =
              imageCropBitmapObjectWithScale(
                  fullBitmap,
                  points2,
                  degreesRotated,
                  fixAspectRatio,
                  aspectRatioX,
                  aspectRatioY,
                  1,
                  flipHorizontally,
                  flipVertically);
        } finally {
          if (result != fullBitmap) {
            fullBitmap.recycle();
          }
        }
      }
    } catch (OutOfMemoryError e) {
      if (result != null) {
        result.recycle();
      }
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(
          "Failed to load sampled bitmap: " + loadedImageUri + "\r\n" + e.getMessage(), e);
    }
    return new imageBitmapSampled(result, sampleSize);
  }
  private static BitmapFactory.Options imageDecodeImageForOption(ContentResolver resolver, Uri uri)
      throws FileNotFoundException {
    InputStream stream = null;
    try {
      stream = resolver.openInputStream(uri);
      BitmapFactory.Options options = new BitmapFactory.Options();
      options.inJustDecodeBounds = true;
      BitmapFactory.decodeStream(stream, EMPTY_RECT, options);
      options.inJustDecodeBounds = false;
      return options;
    } finally {
      imageCloseSafe(stream);
    }
  }
  private static Bitmap imageDecodeImage(
      ContentResolver resolver, Uri uri, BitmapFactory.Options options)
      throws FileNotFoundException {
    do {
      InputStream stream = null;
      try {
        stream = resolver.openInputStream(uri);
        return BitmapFactory.decodeStream(stream, EMPTY_RECT, options);
      } catch (OutOfMemoryError e) {
        options.inSampleSize *= 2;
      } finally {
        imageCloseSafe(stream);
      }
    } while (options.inSampleSize <= 512);
    throw new RuntimeException("Failed to decode image: " + uri);
  }
  private static imageBitmapSampled imageDecodeSampledBitmapRegion(
      Context context, Uri uri, Rect rect, int reqWidth, int reqHeight, int sampleMulti) {
    InputStream stream = null;
    BitmapRegionDecoder decoder = null;
    try {
      BitmapFactory.Options options = new BitmapFactory.Options();
      options.inSampleSize =
          sampleMulti
              * imageCalculateInSampleSizeByReqestedSize(
                  rect.width(), rect.height(), reqWidth, reqHeight);

      stream = context.getContentResolver().openInputStream(uri);
      decoder = BitmapRegionDecoder.newInstance(stream, false);
      do {
        try {
          return new imageBitmapSampled(decoder.decodeRegion(rect, options), options.inSampleSize);
        } catch (OutOfMemoryError e) {
          options.inSampleSize *= 2;
        }
      } while (options.inSampleSize <= 512);
    } catch (Exception e) {
      throw new RuntimeException(
          "Failed to load sampled bitmap: " + uri + "\r\n" + e.getMessage(), e);
    } finally {
      imageCloseSafe(stream);
      if (decoder != null) {
        decoder.recycle();
      }
    }
    return new imageBitmapSampled(null, 1);
  }
  private static Bitmap imageCropForRotatedImage(
      Bitmap bitmap,
      float[] points,
      Rect rect,
      int degreesRotated,
      boolean fixAspectRatio,
      int aspectRatioX,
      int aspectRatioY) {
    if (degreesRotated % 90 != 0) {

      int adjLeft = 0, adjTop = 0, width = 0, height = 0;
      double rads = Math.toRadians(degreesRotated);
      int compareTo =
          degreesRotated < 90 || (degreesRotated > 180 && degreesRotated < 270)
              ? rect.left
              : rect.right;
      for (int i = 0; i < points.length; i += 2) {
        if (points[i] >= compareTo - 1 && points[i] <= compareTo + 1) {
          adjLeft = (int) Math.abs(Math.sin(rads) * (rect.bottom - points[i + 1]));
          adjTop = (int) Math.abs(Math.cos(rads) * (points[i + 1] - rect.top));
          width = (int) Math.abs((points[i + 1] - rect.top) / Math.sin(rads));
          height = (int) Math.abs((rect.bottom - points[i + 1]) / Math.cos(rads));
          break;
        }
      }

      rect.set(adjLeft, adjTop, adjLeft + width, adjTop + height);
      if (fixAspectRatio) {
        imageFixRectForAspectRatio(rect, aspectRatioX, aspectRatioY);
      }

      Bitmap bitmapTmp = bitmap;
      bitmap = Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height());
      if (bitmapTmp != bitmap) {
        bitmapTmp.recycle();
      }
    }
    return bitmap;
  }
  private static int imageCalculateInSampleSizeByReqestedSize(
      int width, int height, int reqWidth, int reqHeight) {
    int inSampleSize = 1;
    if (height > reqHeight || width > reqWidth) {
      while ((height / 2 / inSampleSize) > reqHeight && (width / 2 / inSampleSize) > reqWidth) {
        inSampleSize *= 2;
      }
    }
    return inSampleSize;
  }
  private static int imageCalculateInSampleSizeByMaxTextureSize(int width, int height) {
    int inSampleSize = 1;
    if (mMaxTextureSize == 0) {
      mMaxTextureSize = imageGetMaxTextureSize();
    }
    if (mMaxTextureSize > 0) {
      while ((height / inSampleSize) > mMaxTextureSize
          || (width / inSampleSize) > mMaxTextureSize) {
        inSampleSize *= 2;
      }
    }
    return inSampleSize;
  }
  private static Bitmap imageRotateAndFlipBitmapInt(
      Bitmap bitmap, int degrees, boolean flipHorizontally, boolean flipVertically) {
    if (degrees > 0 || flipHorizontally || flipVertically) {
      Matrix matrix = new Matrix();
      matrix.setRotate(degrees);
      matrix.postScale(flipHorizontally ? -1 : 1, flipVertically ? -1 : 1);
      Bitmap newBitmap =
          Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
      if (newBitmap != bitmap) {
        bitmap.recycle();
      }
      return newBitmap;
    } else {
      return bitmap;
    }
  }
  private static int imageGetMaxTextureSize() {
    final int IMAGE_MAX_BITMAP_DIMENSION = 2048;

    try {
      EGL10 egl = (EGL10) EGLContext.getEGL();
      EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

      int[] version = new int[2];
      egl.eglInitialize(display, version);

      int[] totalConfigurations = new int[1];
      egl.eglGetConfigs(display, null, 0, totalConfigurations);

      EGLConfig[] configurationsList = new EGLConfig[totalConfigurations[0]];
      egl.eglGetConfigs(display, configurationsList, totalConfigurations[0], totalConfigurations);

      int[] textureSize = new int[1];
      int maximumTextureSize = 0;

      for (int i = 0; i < totalConfigurations[0]; i++) {
        egl.eglGetConfigAttrib(
            display, configurationsList[i], EGL10.EGL_MAX_PBUFFER_WIDTH, textureSize);

        if (maximumTextureSize < textureSize[0]) {
          maximumTextureSize = textureSize[0];
        }
      }

      egl.eglTerminate(display);

      return Math.max(maximumTextureSize, IMAGE_MAX_BITMAP_DIMENSION);
    } catch (Exception e) {
      return IMAGE_MAX_BITMAP_DIMENSION;
    }
  }
  private static void imageCloseSafe(Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (IOException ignored) {
      }
    }
  }
  static final class imageBitmapSampled {
    public final Bitmap bitmap;
    final int sampleSize;

    imageBitmapSampled(Bitmap bitmap, int sampleSize) {
      this.bitmap = bitmap;
      this.sampleSize = sampleSize;
    }
  }
  static final class imageRotateBitmapResult {
    public final Bitmap bitmap;
    final int degrees;
    imageRotateBitmapResult(Bitmap bitmap, int degrees) {
      this.bitmap = bitmap;
      this.degrees = degrees;
    }
  }
}
