package com.rd.cropimagesingallery;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import com.rd.cropimagesingallery.databinding.ActivityMainBinding;
import com.rd.cropimagesingallery.permision.StoragePermission;
import java.io.File;
public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnPermision.setOnClickListener(v -> {
            cvStartActivityNext();
        });

        binding.btnCamera.setOnClickListener(v -> {
            dispatchTakePictureIntent();
        });
        binding.btnCrop.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), 200);
        });
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = new File(getCacheDir(),"abcd.jpeg");
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this, getPackageName()+".fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, 201);
            }
        }
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 200) {
           if (resultCode==RESULT_OK){
               Uri uri =data.getData();
               Intent intent=new Intent(this, KeyboardCropImageActivity.class);
               intent.putExtra("img",uri.toString());
               startActivity(intent);
           }
        }else if (requestCode == 201) {
            if (resultCode==RESULT_OK){
                File photoFile = new File(getCacheDir(),"abcd.jpeg");
                if (photoFile.exists()){
                    Uri fileUri = Uri.fromFile(photoFile);
                    String uriString = fileUri.toString();
                    Intent intent=new Intent(this, KeyboardCropImageActivity.class);
                    intent.putExtra("img",uriString);
                    startActivity(intent);
                }

            }
        }
    }
    public void cvStartActivityNext() {
        if (StoragePermission.PermissionGranted(this)) {

        } else {
            StoragePermission.RequestPermission(this);
        }
    }
}