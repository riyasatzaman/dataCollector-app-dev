package com.example.datacollectorx.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.example.datacollectorx.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomRecordActivity extends BaseActivity implements SensorEventListener, LocationListener {

    private ProgressBar circularProgressBar;
    private Button buttonHoldToRecord;
    private Button buttonNext;
    private Button buttonGoToSearch;
    private TextView textViewRoomLabel; // Displays the room label

    // Fields for guide mode (sequential room recording)
    private ArrayList<String> roomsList;
    private int currentRoomIndex = 0; // default to 0 if not provided

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String buildingCode;
    private String room;

    private int progressStatus = 0;
    private Handler handler = new Handler();
    private boolean isHolding = false;
    private boolean shouldRecord = false;
    private List<Map<String, Object>> sensorDataList = new ArrayList<>();

    private SensorManager sensorManager;
    private Sensor accelerometer, magnetometer, gyroscope;
    private LocationManager locationManager;
    private WifiManager wifiManager;

    // Define an interface to get fetched scanned rooms from Firestore
    public interface NextRoomCallback {
        void onCallback(List<String> fetchedScannedRooms);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_record);

        // Bind UI elements
        textViewRoomLabel = findViewById(R.id.textViewRoomLabel);
        buttonHoldToRecord = findViewById(R.id.buttonHoldToRecord);
        buttonNext = findViewById(R.id.buttonNext);
        buttonGoToSearch = findViewById(R.id.buttonGoBack_room_record);
        circularProgressBar = findViewById(R.id.circularProgressBar);

        // Initially, hide the Next button
        buttonNext.setVisibility(View.GONE);

        // Initialize Firebase, sensor manager, etc.
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        initializeSensorsAndLocation();

        // Retrieve intent extras (for guide mode, we pass the entire rooms list and current index)
        buildingCode = getIntent().getStringExtra("building_code");
        room = getIntent().getStringExtra("room");
        roomsList = getIntent().getStringArrayListExtra("rooms_list");
        currentRoomIndex = getIntent().getIntExtra("current_index", 0);

        // Set the initial room label
        textViewRoomLabel.setText("Recording: " + room);

        // Set up the long press recording action
        buttonHoldToRecord.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isHolding = true;
                    progressStatus = 0;
                    sensorDataList.clear();  // Clear data for a new recording
                    startRecording();
                    return true;
                case MotionEvent.ACTION_UP:
                    isHolding = false;
                    if (progressStatus < 100) {
                        resetRecording();
                    } else {
                        shouldRecord = true;
                        recordSensorData();
                        recordScannedRoom(buildingCode, room);
                        // Update UI for guide mode
                        onRecordingComplete();
                    }
                    return true;
            }
            return false;
        });

        // Next button: move to the next unscanned room based on Firestore data
        buttonNext.setOnClickListener(v -> {
            // Fetch scanned rooms from Firestore (the ones recorded so far)
            fetchScannedRoomsForRecord(new NextRoomCallback() {
                @Override
                public void onCallback(List<String> fetchedScannedRooms) {
                    boolean foundNext = false;
                    int nextIndex = currentRoomIndex;
                    while (nextIndex < roomsList.size() - 1) {
                        nextIndex++;
                        String candidateRoom = roomsList.get(nextIndex);
                        // If this candidate is not in the fetched scanned list, it's unscanned
                        if (!fetchedScannedRooms.contains(candidateRoom)) {
                            foundNext = true;
                            break;
                        }
                    }
                    if (foundNext) {
                        currentRoomIndex = nextIndex;
                        room = roomsList.get(currentRoomIndex);
                        resetUIForNextRoom();
                    } else {
                        Toast.makeText(RoomRecordActivity.this, "All unscanned rooms recorded", Toast.LENGTH_SHORT).show();
                        // Optionally finish or navigate back.
                    }
                }
            });
        });

        // "Go to Search" button: return to the search (RoomSelection) screen
        buttonGoToSearch.setOnClickListener(v -> finish());
    }

    // Called when recording is completed successfully
    private void onRecordingComplete() {
        textViewRoomLabel.setText("ROOM " + room + " scanned");
        textViewRoomLabel.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        buttonHoldToRecord.setEnabled(false);
        buttonNext.setVisibility(View.VISIBLE);
    }

    // Resets UI to record the next room
    private void resetUIForNextRoom() {
        progressStatus = 0;
        circularProgressBar.setProgress(progressStatus);
        shouldRecord = false;
        textViewRoomLabel.setText("Recording: " + room);
        textViewRoomLabel.setTextColor(Color.BLACK);
        buttonHoldToRecord.setEnabled(true);
        buttonNext.setVisibility(View.GONE);
        sensorDataList.clear();
    }

    // Resets recording if the hold is released early
    private void resetRecording() {
        progressStatus = 0;
        circularProgressBar.setProgress(0);
        shouldRecord = false;
        Toast.makeText(RoomRecordActivity.this, "Hold was released early. Data not recorded.", Toast.LENGTH_SHORT).show();
    }

    // Starts the 5-second recording process
    private void startRecording() {
        circularProgressBar.setProgress(0);
        new Thread(() -> {
            while (isHolding && progressStatus < 100) {
                progressStatus += 2; // 100 steps over 5 seconds (50ms per step)
                handler.post(() -> circularProgressBar.setProgress(progressStatus));
                if (progressStatus % 20 == 0) {
                    collectSensorData();
                    collectWifiData();
                }
                try {
                    Thread.sleep(25);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (progressStatus >= 100) {
                handler.post(() -> Toast.makeText(RoomRecordActivity.this, "Hold completed!", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // Collect sensor data (placeholder logic)
    private void collectSensorData() {
        Map<String, Object> sensorData = new HashMap<>();
        sensorData.put("timestamp", System.currentTimeMillis());
        sensorDataList.add(sensorData);
    }

    // Collect Wi-Fi data
    private void collectWifiData() {
        if (wifiManager != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            List<ScanResult> scanResults = wifiManager.getScanResults();
            for (ScanResult scanResult : scanResults) {
                Map<String, Object> wifiData = new HashMap<>();
                wifiData.put("SSID", scanResult.SSID);
                wifiData.put("BSSID", scanResult.BSSID);
                wifiData.put("RSSI", scanResult.level);
                wifiData.put("Frequency", scanResult.frequency);
                sensorDataList.add(wifiData);
            }
        }
    }

    // Record sensor and Wi-Fi data to Firestore
    private void recordSensorData() {
        if (shouldRecord) {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            CollectionReference sensorDataCollection = db.collection("sensorData").document(userId).collection("rooms");
            Map<String, Object> roomData = new HashMap<>();
            roomData.put("building_code", buildingCode);
            roomData.put("room", room);
            roomData.put("sensorData", sensorDataList);
            roomData.put("timestamp", System.currentTimeMillis());
            sensorDataCollection.add(roomData)
                    .addOnSuccessListener(documentReference -> Toast.makeText(RoomRecordActivity.this, "Sensor data recorded successfully!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(RoomRecordActivity.this, "Error recording data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    // Update Firestore with the scanned room and earnings
    private void recordScannedRoom(String buildingCode, String room) {
        if (shouldRecord) {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DocumentReference userDocRef = db.collection("users").document(userId);
            DocumentReference adminDocRef = db.collection("admin").document("cQMK4loeC3SVPdaBzKDi");

            userDocRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Map<String, Map<String, Boolean>> scannedRooms = (Map<String, Map<String, Boolean>>) documentSnapshot.get("scannedRooms");
                    Double currentEarnings = documentSnapshot.getDouble("earnings");
                    Long roomsScanned = documentSnapshot.getLong("roomsScanned");
                    Map<String, Boolean> recordedBuildings = (Map<String, Boolean>) documentSnapshot.get("recordedBuildings");

                    if (scannedRooms == null) {
                        scannedRooms = new HashMap<>();
                    }
                    if (recordedBuildings == null) {
                        recordedBuildings = new HashMap<>();
                    }
                    if (!scannedRooms.containsKey(buildingCode)) {
                        scannedRooms.put(buildingCode, new HashMap<>());
                    }
                    Map<String, Boolean> buildingRooms = scannedRooms.get(buildingCode);
                    buildingRooms.put(room, true);

                    double earningsPerRoom = calculateEarningsPerRoom(buildingCode);
                    double updatedEarnings = (currentEarnings != null ? currentEarnings : 0) + earningsPerRoom;
                    long updatedRoomsScanned = (roomsScanned != null ? roomsScanned : 0) + 1;

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("scannedRooms", scannedRooms);
                    updates.put("earnings", updatedEarnings);
                    updates.put("roomsScanned", updatedRoomsScanned);

                    if (!recordedBuildings.containsKey(buildingCode) || !recordedBuildings.get(buildingCode)) {
                        recordedBuildings.put(buildingCode, true);
                        updates.put("recordedBuildings", recordedBuildings);
                        adminDocRef.update("buildings." + buildingCode, FieldValue.increment(1));
                    }

                    userDocRef.update(updates)
                            .addOnSuccessListener(aVoid -> updateGlobalStats(earningsPerRoom))
                            .addOnFailureListener(e -> Toast.makeText(RoomRecordActivity.this, "Error recording room: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }).addOnFailureListener(e -> Toast.makeText(RoomRecordActivity.this, "Failed to fetch user data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    // Update global stats in Firestore
    private void updateGlobalStats(double earningsPerRoom) {
        DocumentReference adminDocRef = db.collection("admin").document("cQMK4loeC3SVPdaBzKDi");
        adminDocRef.update("totalMoney", FieldValue.increment(earningsPerRoom));
        adminDocRef.update("totalRooms", FieldValue.increment(1));
    }

    // Calculate earnings per room based on building code
    private double calculateEarningsPerRoom(String buildingCode) {
        double totalEarnings = 0;
        int totalRooms = 0;
        switch (buildingCode) {
            case "ATH":
                totalEarnings = 20;
                totalRooms = 248;
                break;
            case "CSC":
                totalEarnings = 150;
                totalRooms = 1128;
                break;
            case "ASH":
                totalEarnings = 20;
                totalRooms = 217;
                break;
            case "PBH":
                totalEarnings = 20;
                totalRooms = 219;
                break;
            case "SAB":
                totalEarnings = 30;
                totalRooms = 544;
                break;
            case "SUB":
                totalEarnings = 20;
                totalRooms = 694;
                break;
            case "CAB":
                totalEarnings = 20;
                totalRooms = 450;
                break;
            case "CCIS":
                totalEarnings = 40;
                totalRooms = 1562;
                break;
            default:
                break;
        }
        return totalRooms > 0 ? totalEarnings / totalRooms : 0;
    }

    // Fetch scanned rooms from Firestore for this building for use in guide mode next button.
    private void fetchScannedRoomsForRecord(NextRoomCallback callback) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DocumentReference userDocRef = db.collection("users").document(userId);
        userDocRef.get().addOnSuccessListener(documentSnapshot -> {
            List<String> fetchedScannedRooms = new ArrayList<>();
            if (documentSnapshot.exists()) {
                Map<String, Boolean> scannedRoomsMap = (Map<String, Boolean>) documentSnapshot.get("scannedRooms." + buildingCode);
                if (scannedRoomsMap != null) {
                    fetchedScannedRooms = new ArrayList<>(scannedRoomsMap.keySet());
                }
            }
            callback.onCallback(fetchedScannedRooms);
        }).addOnFailureListener(e -> {
            callback.onCallback(new ArrayList<>());
        });
    }

    private void initializeSensorsAndLocation() {
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            Map<String, Object> accelerometerData = new HashMap<>();
            accelerometerData.put("Accelerometer_X", event.values[0]);
            accelerometerData.put("Accelerometer_Y", event.values[1]);
            accelerometerData.put("Accelerometer_Z", event.values[2]);
            sensorDataList.add(accelerometerData);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            Map<String, Object> magnetometerData = new HashMap<>();
            magnetometerData.put("Magnetometer_X", event.values[0]);
            magnetometerData.put("Magnetometer_Y", event.values[1]);
            magnetometerData.put("Magnetometer_Z", event.values[2]);
            sensorDataList.add(magnetometerData);
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            Map<String, Object> gyroscopeData = new HashMap<>();
            gyroscopeData.put("Gyroscope_X", event.values[0]);
            gyroscopeData.put("Gyroscope_Y", event.values[1]);
            gyroscopeData.put("Gyroscope_Z", event.values[2]);
            sensorDataList.add(gyroscopeData);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            Map<String, Object> locationData = new HashMap<>();
            locationData.put("Latitude", location.getLatitude());
            locationData.put("Longitude", location.getLongitude());
            sensorDataList.add(locationData);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override
    public void onProviderEnabled(String provider) { }

    @Override
    public void onProviderDisabled(String provider) { }
}
