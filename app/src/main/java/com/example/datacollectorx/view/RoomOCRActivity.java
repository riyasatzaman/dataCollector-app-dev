package com.example.datacollectorx.view;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.datacollectorx.R;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

public class RoomOCRActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 2;
    private String roomName;
    private ImageView imageView;
    private Bitmap imageBitmap;
    private String buildingCode;  // Declare the building code
    private Button goBack;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_ocr);
        goBack = findViewById(R.id.buttonGoBack_room_ocr);


        goBack.setOnClickListener(v -> {
            finish();
        });

        imageView = findViewById(R.id.imageViewRoomLabel);
        Button captureButton = findViewById(R.id.buttonCapture);
        Button validateButton = findViewById(R.id.buttonValidate);

        roomName = getIntent().getStringExtra("room");
        buildingCode = getIntent().getStringExtra("building_code");

        // Capture image from camera
        captureButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                // Request camera permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        REQUEST_CAMERA_PERMISSION);
            } else {
                // Permission already granted, start camera
                dispatchTakePictureIntent();
            }
        });

        // Validate OCR result with the room name
        validateButton.setOnClickListener(v -> {
            if (imageBitmap != null) {
                runTextRecognition(imageBitmap);
            } else {
                Toast.makeText(RoomOCRActivity.this, "Please take a picture first!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Launch camera to capture an image
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            imageBitmap = (Bitmap) extras.get("data");
            imageView.setImageBitmap(imageBitmap);  // Display the captured image
        }
    }

    // Run OCR on the captured image
    private void runTextRecognition(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(this::processTextRecognitionResult)
                .addOnFailureListener(e -> {
                    Toast.makeText(RoomOCRActivity.this, "Failed to recognize text", Toast.LENGTH_SHORT).show();
                });
    }

    // Process the result of text recognition
    private void processTextRecognitionResult(Text result) {
        StringBuilder recognizedText = new StringBuilder();
        for (Text.TextBlock block : result.getTextBlocks()) {
            recognizedText.append(block.getText()).append("\n");
        }

        String recognizedString = recognizedText.toString().toLowerCase().trim();
        if (recognizedString.contains(roomName.toLowerCase().trim())) {
            Intent intent = new Intent(RoomOCRActivity.this, RoomRecordActivity.class);
            intent.putExtra("building_code", buildingCode);  // Pass the building code
            intent.putExtra("room", roomName);  // Pass the room name
            Log.d("RoomOCRActivity", "Building code: " + buildingCode);
            startActivity(intent);

        } else {
            Toast.makeText(this, "Room label does not match!", Toast.LENGTH_SHORT).show();
        }
    }

    // Handle the result of the permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, launch the camera
                dispatchTakePictureIntent();
            } else {
                // Permission denied, show a message
                Toast.makeText(this, "Camera permission is required to take a picture.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
