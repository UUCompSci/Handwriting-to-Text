package com.example.handwritingToText;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class UploadDocument extends AppCompatActivity {

    String photoPath = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_upload_document);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //openDirectory();
        //getFolderPath.launch();


    }

    private void testPhotoDirectory(){
        //code adapted from setupTessdataDirectory method

        String path = getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/PhoneApp";
        Log.d("path: ", path);
        File tempPhotos = new File(path);
        if(!tempPhotos.exists()) {
            tempPhotos.mkdir();
            Log.d("Photos", "tempPhotos folder created");
        }

        photoPath = path;
    }


    public static void copyFile(File src, File dst) throws IOException{
        try {
            InputStream inputStream = new FileInputStream(src);
            FileOutputStream outputStream = new FileOutputStream(dst);
            byte[] buffer = new byte[1024];
            int length = inputStream.read(buffer);
            while (length > 0) {
                outputStream.write(buffer, 0, length);
                length = inputStream.read(buffer);
            }
            inputStream.close();
            outputStream.close();
            Log.d("CopyFile", "file copied to new folder");
        } catch (Exception e) {
            Log.d("ERROR", "Error copying : ", e);
        }
    }

    private void copyPhoto(File src){
        //source = the path from the original photo
        //code adapted from copyTessdataFile()

        String dataPath = photoPath;
        File tempPhoto = new File(dataPath + "/testPhoto.jpg");

        try {
            copyFile(src, tempPhoto);
            Log.d("copy", "successfully copied image");;
        } catch (IOException e) {
            Log.d("ERROR", "copy failed" + e);
        }
    }

    private void setupTessdataDirectory(){
        //code adapted from code on Medium
        //https://medium.com/@WanderingNutBlog/building-a-text-recognition-app-on-android-with-tesseract-ocr-e1e623307366

        String path = getExternalFilesDir(null).getAbsolutePath() + "/tessdata";
        Log.d("path: ", path);
        File tessDataDirectory = new File(path);
        if(!tessDataDirectory.exists()) {
            tessDataDirectory.mkdir();
            Log.d("Tesseract", "Tessdata folder created");
        }
    }

    private void copyTraineddataFile(){
        //code adapted from code on Medium
        //https://medium.com/@WanderingNutBlog/building-a-text-recognition-app-on-android-with-tesseract-ocr-e1e623307366

        String dataPath = getExternalFilesDir(null).getAbsolutePath() + "/tessdata";
        File tessDataFile = new File(dataPath + "/eng.traineddata"); // replace eng with any files you downloaded
        if (!tessDataFile.exists()) {
            try {
                InputStream inputStream = getAssets().open("tesseract/eng.traineddata");
                FileOutputStream outputStream = new FileOutputStream(tessDataFile);
                byte[] buffer = new byte[1024];
                int length = inputStream.read(buffer);
                while (length > 0) {
                    outputStream.write(buffer, 0, length);
                    length = inputStream.read(buffer);
                }
                inputStream.close();
                outputStream.close();
                Log.d("Tesseract", "Eng.traineddata copied to device");
            } catch (Exception e) {
                Log.d("ERROR", "Error copying .traineddata: ", e);
            }
        }
    }

    private void testTesseractDirectory(){
        setupTessdataDirectory();
        copyTraineddataFile();
    }

    /*
 Request code for creating a PDF document.
    private void createFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/txt");
        intent.putExtra(Intent.EXTRA_TITLE, "invoice.txt");

        startActivityForResult(intent, CREATE_FILE);
    }
*/

/*
    ActivityResultLauncher<Uri> getFolderPath = registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uri) {
                    dataPath = uri;
                }
            });
    */


/*
    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == 2
                && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the document or directory that
            // the user selected.
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                // Perform operations on the document using its URI.
            }
        }
    }
*/

    public void openDirectory() {
        // Choose a directory using the system's file picker.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

        startActivityForResult(intent, 2);
    }

    public void useTesseract() {
        testTesseractDirectory();

        //code mostly taken from tesseract4android readme sample code on github

        // Create TessBaseAPI instance (this internally creates the native Tesseract instance)
        TessBaseAPI tess = new TessBaseAPI();

        // NOTE: TessBaseAPI is not thread-safe. If you want to process multiple images in parallel,
        // create separate instance of TessBaseAPI for each thread.

        // Given path must contain subdirectory `tesseract` where are `*.traineddata` language files
        // The path must be directly readable by the app

        try{
            //String dataPath = "project/app/src/main/assets/tesseract";
            String dataPath = getExternalFilesDir(null).getAbsolutePath();
            Log.d("useTesseract:datapath", dataPath);

            // Initialize API for specified language
            // (can be called multiple times during Tesseract lifetime)
            if (!tess.init(dataPath, "eng")) {
                // could be multiple languages, like "eng+deu+fra"
                // Error initializing Tesseract (wrong/inaccessible data path or not existing language file(s))
                // Release the native Tesseract instance
                tess.recycle();
                return;
            }
        }catch(Exception e){
            Log.d("ERROR", "useTesseract: " + e);
        }

        // Load the image (file path, Bitmap, Pix...)
        // (can be called multiple times during Tesseract lifetime)
        tess.setImage(BitmapFactory.decodeResource(getResources(), R.drawable.test_image));

        // Start the recognition (if not done for this image yet) and retrieve the result
        // (can be called multiple times during Tesseract lifetime)
        String text = tess.getUTF8Text();
        Log.d("Tesseract", text);


        // Release the native Tesseract instance when you don't want to use it anymore
        // After this call, no method can be called on this TessBaseAPI instance
        tess.recycle();
    }

    ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uri) {
                    int flag = Intent.FLAG_GRANT_READ_URI_PERMISSION;

                    Context context = getApplicationContext();
                    context.contentResolver.takePersistableUriPermission(uri, flag);

                    //File src = new File(String.valueOf(uri));
                    //copyPhoto(src);

                    Log.d("TAG", "onActivityResult: " + uri.toString());

                    //useTesseract();
                }
            });

    public void getImage(View view) {
        testPhotoDirectory();

        pickImageLauncher.launch("image/*");
    }

    public void tesseractResults(View view) {
        Intent changeScreen = new Intent(this, TesseractResults.class);

        startActivity(changeScreen);
    }
}