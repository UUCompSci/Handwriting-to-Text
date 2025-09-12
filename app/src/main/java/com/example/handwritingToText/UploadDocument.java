package com.example.handwritingToText;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;

public class UploadDocument extends AppCompatActivity {
    String TxtPath = null;

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

    private void checkHiddenDirectory(){
        //code adapted from code on Medium
        //https://medium.com/@WanderingNutBlog/building-a-text-recognition-app-on-android-with-tesseract-ocr-e1e623307366

        String path = getExternalFilesDir(null).getAbsolutePath() + "/PhoneApp";
        Log.d("path: ", path);
        File newDirectory = new File(path);
        if(!newDirectory.exists()) {
            newDirectory.mkdir();
            Log.d("newDirectory", "newDirectory created");
        }
    }
    public void createDoc(String text) {
        checkHiddenDirectory();

        String dataPath = getExternalFilesDir(null).getAbsolutePath() + "/PhoneApp";
        File txtFile = new File(dataPath + "/temp.txt"); // replace eng with any files you downloaded

        if (txtFile.exists()){
            txtFile.delete();
        }

        try{
            FileOutputStream outputStream = new FileOutputStream(txtFile);
            OutputStreamWriter outputWriter = new OutputStreamWriter(outputStream);
            outputWriter.write(text);
            outputWriter.close();
            outputStream.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        TxtPath = String.valueOf(txtFile);
    }

    public void tesseractResults() {
        Intent changeScreen = new Intent(this, TesseractResults.class);

        startActivity(changeScreen);
    }

    ActivityResultLauncher<String> saveResults = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("text/*"), o -> {

                Log.d("filePath", "createDoc: filepath = " + o);

                try {
                    InputStream inputStream = new FileInputStream(TxtPath);
                    FileOutputStream outputStream = (FileOutputStream) getContentResolver().openOutputStream(o);
                    byte[] buffer = new byte[1024];
                    int length = inputStream.read(buffer);
                    while (length > 0) {
                        outputStream.write(buffer, 0, length);
                        length = inputStream.read(buffer);
                    }
                    inputStream.close();
                    outputStream.close();
                    Log.d("saveResults", "data file copied to device");
                } catch (Exception e) {
                    Log.d("ERROR", "Error copying file to save file ", e);
                }

                tesseractResults();
            });

    public void useTesseract(Uri src) {
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



        Log.d("src path", "useTesseract: src" + src);
        try {
            // Load the image (file path, Bitmap, Pix...)
            // (can be called multiple times during Tesseract lifetime)

            //test image setup:
            tess.setImage(BitmapFactory.decodeStream(getContentResolver().openInputStream(src)));

            // Start the recognition (if not done for this image yet) and retrieve the result
            // (can be called multiple times during Tesseract lifetime)
            String text = tess.getUTF8Text();

            Log.d("Tesseract", text);

            createDoc(text);

            saveResults.launch("tessOutput.txt");
        }catch (Exception e) {
            Log.d("ERROR", "useTesseract try-catch failed:" + e);
        }

        // Release the native Tesseract instance when you don't want to use it anymore
        // After this call, no method can be called on this TessBaseAPI instance
        tess.recycle();
    }

    ActivityResultLauncher<PickVisualMediaRequest> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    Log.d("PhotoPicker", "Selected URI: " + uri);

                    useTesseract(uri);
                } else {
                    Log.d("PhotoPicker", "No media selected");
                }
            });

    public void getImage(View view) {
        pickImageLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }
}