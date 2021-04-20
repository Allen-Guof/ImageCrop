package com.example.imagepicker;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.navigation.ui.AppBarConfiguration;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.imagepicker.databinding.ActivityMainBinding;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private String TAG = MainActivity.class.getSimpleName();

    private static final int RES_IMAGE = 101;
    private static final int RES_CROP = 102;

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private Uri imageUri;
    private String imgPath;
    private Uri outputFileUri;
    private File mPickFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        binding.btnPickImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(getPickImageIntent(), RES_IMAGE);
            }
        });
        binding.btnCropImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                CropImage.activity(imageUri)
//                        .start(MainActivity.this);
//
//                if (true) {
//                    return;
//                }
                outputFileUri = getFileUri("Image_crop.jpg");
                Uri pickUri = FileProvider.getUriForFile(MainActivity.this, "com.example.imagepicker.fileProvider", mPickFile);
                cropImage(MainActivity.this, pickUri, outputFileUri,
                        RES_CROP, 1, 1, 300, 300);
            }
        });
        //申请权限部分
        String write=Manifest.permission.WRITE_EXTERNAL_STORAGE;
        String read= Manifest.permission.READ_EXTERNAL_STORAGE;

        final String[] WriteReadPermission = new String[] {write, read};

        int checkWrite= ContextCompat.checkSelfPermission(MainActivity.this,write);
        int checkRead= ContextCompat.checkSelfPermission(MainActivity.this,read);
        int ok=PackageManager.PERMISSION_GRANTED;

        if (checkWrite!= ok && checkRead!=ok){
            //申请权限，读和写都申请一下，不然容易出问题
            ActivityCompat.requestPermissions(MainActivity.this,WriteReadPermission,1);
        }
    }

    /**
     * 调用系统裁减功能，裁减某张指定的图片，并输出到指定的位置
     *
     * @param activity
     * @param originalFileUri 原始图片位置
     * @param outputFileUri   裁减后图片的输出位置，两个地址最好不一样。如果一样的话，有的手机上面无法保存裁减的结果
     * @return
     */
    public static void cropImage(Activity activity, Uri originalFileUri, Uri outputFileUri, int requestCode, int aspectX, int aspectY, int outputX,
                                 int outputY) {
        if (originalFileUri == null) {
            return;
        }
        final Intent intent = new Intent("com.android.camera.action.CROP");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.setDataAndType(originalFileUri, "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", aspectX);
        intent.putExtra("aspectY", aspectY);
        intent.putExtra("outputX", outputX);
        intent.putExtra("outputY", outputY);
        intent.putExtra("scale", true);
        intent.putExtra("scaleUpIfNeeded", true); // 部分机型没有设置该参数截图会有黑边
        intent.putExtra("return-data", false);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        // 不启用人脸识别
        intent.putExtra("noFaceDetection", false);
        Intent chooser = Intent.createChooser(intent, "Share File");

        List<ResolveInfo> resInfoList = activity.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            activity.grantUriPermission(packageName, outputFileUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        activity.startActivityForResult(chooser, requestCode);
    }

    private final Intent getPickImageIntent() {
        // pick iamge
        Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        //take a photo
        Intent takePhotoIntent = new Intent("android.media.action.IMAGE_CAPTURE");
        takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, setImageUri("Image_Tmp.jpg"));
        return takePhotoIntent;
    }

    private final Uri setImageUri(String imgName) {
//        File folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        File folder = new File(String.valueOf(this.getExternalFilesDir(Environment.DIRECTORY_DCIM)));
        folder.mkdirs();
        File file = new File(folder, imgName);
        if (file.exists()) {
            file.delete();
        }
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.imageUri = FileProvider.getUriForFile((Context) this, "com.example.imagepicker.fileProvider", file);
        this.imgPath = file.getAbsolutePath();
        return imageUri;
    }

    private final Uri getFileUri(String imgName) {
//        File folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        File folder = new File(String.valueOf(this.getExternalFilesDir(Environment.DIRECTORY_DCIM)));
        folder.mkdirs();
        File file = new File(folder, imgName);
        if (file.exists()) {
            file.delete();
        }
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return FileProvider.getUriForFile((Context) this, "com.example.imagepicker.fileProvider", file);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            Log.e(TAG, "onActivityResult: false");
            return;
        }
        switch (requestCode) {
            case RES_IMAGE:
                try {
                    handleIntent(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case RES_CROP:
                try {
                    handleCropIntent(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    private void handleIntent(Intent data) throws IOException {
        if (data != null && data.getData() != null) {
            imageUri = data.getData();
        }
//                File file = new File(imageUri.);

        String path = imageUri.getPath();
        Log.i(TAG, "onActivityResult image path: " + path);
        mPickFile = getFile(MainActivity.this, imageUri);
        Glide.with(MainActivity.this)
                .asBitmap()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .load(mPickFile)
                .into(binding.image);
//        binding.image.setImageURI(imageUri);
    }

    private void handleCropIntent(Intent data) throws IOException {
        Glide.with(MainActivity.this)
                .asBitmap()
                .load(getFile(MainActivity.this, outputFileUri))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(binding.imageCrop);
//        binding.image.setImageURI(imageUri);
    }

    public static File getFile(Context context, Uri uri) throws IOException {
//        File folder = new File(String.valueOf(context.getExternalFilesDir(Environment.DIRECTORY_DCIM)));
//        folder.mkdirs();
//        File destinationFilename = new File(folder, queryName(context, uri));
        File destinationFilename = new File(context.getFilesDir().getPath() + File.separatorChar + queryName(context, uri));
        try (InputStream ins = context.getContentResolver().openInputStream(uri)) {
            createFileFromStream(ins, destinationFilename);
        } catch (Exception ex) {
            Log.e("Save File", ex.getMessage());
            ex.printStackTrace();
        }
        return destinationFilename;
    }

    public static void createFileFromStream(InputStream ins, File destination) {
        try (OutputStream os = new FileOutputStream(destination)) {
            byte[] buffer = new byte[4096];
            int length;
            while ((length = ins.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            os.flush();
        } catch (Exception ex) {
            Log.e("Save File", ex.getMessage());
            ex.printStackTrace();
        }
    }


    private static String queryName(Context context, Uri uri) {
        Cursor returnCursor =
                context.getContentResolver().query(uri, null, null, null, null);
        assert returnCursor != null;
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        returnCursor.moveToFirst();
        String name = returnCursor.getString(nameIndex);
        returnCursor.close();
        return name;
    }
}