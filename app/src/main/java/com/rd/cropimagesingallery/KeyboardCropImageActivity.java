
package com.rd.cropimagesingallery;
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.rd.cropimagesingallery.databinding.CropImageActivityBinding;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class KeyboardCropImageActivity extends AppCompatActivity {
    CropImageActivityBinding binding;

    @Override
    @SuppressLint("NewApi")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = CropImageActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnLeft.setOnClickListener(v -> {
            binding.cropImageView.imageRotateImage(-90);
        });
        binding.btnRight.setOnClickListener(v -> {
            binding.cropImageView.imageRotateImage(90);
        });
        binding.btnSave.setOnClickListener(v -> {
            Bitmap croppedBitmap = binding.cropImageView.imageGetCroppedImage();
            if (croppedBitmap != null) {
                Bitmap fixedRatioBitmap = Bitmap.createScaledBitmap(croppedBitmap, 640, 400, true);
                File file = new File(getCacheDir(), "abc.webp");
                try {
                    FileOutputStream outputStream = new FileOutputStream(file);
                    fixedRatioBitmap.compress(Bitmap.CompressFormat.WEBP, 75, outputStream);
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        String uri = getIntent().getStringExtra("img");
        binding.cropImageView.imageSetImageUriAsync(Uri.parse(uri));
        binding.cropImageView.imageSetAspectRatio(8, 5);
        binding.cropImageView.imageSetFixedAspectRatio(true);
    }
}
