package com.example.datacollectorx.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
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
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.datacollectorx.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomRecordActivity extends AppCompatActivity implements SensorEventListener, LocationListener {

    private ProgressBar circularProgressBar;
    private Button buttonHoldToRecord;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String buildingCode;
    private String room;
    private int progressStatus = 0;
    private Handler handler = new Handler();
    private boolean isHolding = false;
    private boolean shouldRecord = false;
    private boolean isRoomAlreadyScanned = false;

    private SensorManager sensorManager;
    private Sensor accelerometer, magnetometer, gyroscope;
    private LocationManager locationManager;
    private WifiManager wifiManager;

    private List<Map<String, Object>> sensorDataList = new ArrayList<>();  // List to hold sensor, location, and Wi-Fi data

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_record);

        // Initialize Firebase instances
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Get the room and building code from the intent
        buildingCode = getIntent().getStringExtra("building_code");
        room = getIntent().getStringExtra("room");

        circularProgressBar = findViewById(R.id.circularProgressBar);
        buttonHoldToRecord = findViewById(R.id.buttonHoldToRecord);

        // Initialize sensor and location services
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        initializeSensorsAndLocation();

        // Set the button on long press
        buttonHoldToRecord.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isHolding = true;
                    progressStatus = 0;
                    sensorDataList.clear();  // Clear the data for new recording
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
                    }
                    return true;
            }
            return false;
        });
    }

    private double calculateEarningsPerRoom(String buildingCode) {
        double totalEarnings = 0;
        int totalRooms = 0;

        switch (buildingCode) {
            case "ATH":  // Athabasca Hall
                totalEarnings = 20;
                totalRooms = 248;
                break;
            case "CSC":  // Computing Science Center
                totalEarnings = 15;
                totalRooms = 188;
                break;
            case "ASH":  // Assiniobia Hall
                totalEarnings = 20;
                totalRooms = 217;
                break;
            case "PBH":  // Pembina Hall
                totalEarnings = 20;
                totalRooms = 219;
                break;
            case "SAB":  // South Academic Building
                totalEarnings = 30;
                totalRooms = 544;
                break;
            case "SUB":  // Student Union Building
                totalEarnings = 20;
                totalRooms = 694;
                break;
            case "CAB":  // Central Academic Building
                totalEarnings = 20;
                totalRooms = 450;
                break;
            case "CCIS":  // CCIS
                totalEarnings = 40;
                totalRooms = 1562;
                break;
            default:
                break;
        }
        return totalEarnings;
    }

    private void recordScannedRoom(String buildingCode, String room) {
        if (shouldRecord) {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DocumentReference userDocRef = db.collection("users").document(userId);

            // Fetch user data from Firestore to update roomsScanned and earnings
            userDocRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // Get the scannedRooms map and earnings
                    Map<String, Map<String, Boolean>> scannedRooms = (Map<String, Map<String, Boolean>>) documentSnapshot.get("scannedRooms");
                    Double currentEarnings = documentSnapshot.getDouble("earnings");
                    Long roomsScanned = documentSnapshot.getLong("roomsScanned");

                    // Initialize scannedRooms if null
                    if (scannedRooms == null) {
                        scannedRooms = new HashMap<>();
                    }

                    // Check if the building exists in the map
                    if (!scannedRooms.containsKey(buildingCode)) {
                        scannedRooms.put(buildingCode, new HashMap<>());
                    }

                    // Update the specific room as scanned
                    Map<String, Boolean> buildingRooms = scannedRooms.get(buildingCode);
                    buildingRooms.put(room, true);

                    // Calculate earnings per room for the building
                    double earningsPerRoom = calculateEarningsPerRoom(buildingCode);

                    // Update the total earnings and roomsScanned count
                    double updatedEarnings = (currentEarnings != null ? currentEarnings : 0) + earningsPerRoom;
                    long updatedRoomsScanned = (roomsScanned != null ? roomsScanned : 0) + 1;

                    // Prepare the updates to send to Firestore
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("scannedRooms", scannedRooms);  // Update the scanned rooms
                    updates.put("earnings", updatedEarnings);  // Update the earnings
                    updates.put("roomsScanned", updatedRoomsScanned);  // Update the rooms scanned count

                    // Update Firestore document with the scanned room and earnings
                    userDocRef.update(updates)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(RoomRecordActivity.this, "Room recorded successfully!", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(RoomRecordActivity.this, "Error recording room: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(RoomRecordActivity.this, "Failed to fetch user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }


    private void startRecording() {
        // Reset progress bar
        circularProgressBar.setProgress(0);

        // Start the countdown for 5 seconds (100 steps of 50ms each)
        new Thread(() -> {
            while (isHolding && progressStatus < 100) {
                progressStatus += 2;  // Increase progress every 50 ms (5 seconds = 100 steps of 50ms)
                handler.post(() -> circularProgressBar.setProgress(progressStatus));

                // Collect sensor and Wi-Fi data
                if (progressStatus % 20 == 0) {  // Collect data 5 times during the hold
                    collectSensorData();
                    collectWifiData();
                }

                try {
                    Thread.sleep(50);  // Wait for 50 milliseconds
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (progressStatus == 100) {
                handler.post(() -> Toast.makeText(RoomRecordActivity.this, "Hold completed!", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }


    private void resetRecording() {
        progressStatus = 0;
        circularProgressBar.setProgress(0);
        shouldRecord = false;
        Toast.makeText(RoomRecordActivity.this, "Hold was released early. Data not recorded.", Toast.LENGTH_SHORT).show();
    }

    // Function to collect sensor data and add it to the list
    private void collectSensorData() {
        Map<String, Object> sensorData = new HashMap<>();
        // Add timestamp and placeholder for sensor values (to be updated in onSensorChanged)
        sensorData.put("timestamp", System.currentTimeMillis());
        sensorDataList.add(sensorData);
    }

    // Function to collect Wi-Fi scan results and add to the list
    private void collectWifiData() {
        if (wifiManager != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
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

    // Function to record sensor and Wi-Fi data to Firestore
    private void recordSensorData() {
        if (shouldRecord) {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DocumentReference userDocRef = db.collection("sensorData").document(userId);

            // Upload sensor data to Firestore
            Map<String, Object> data = new HashMap<>();
            data.put("building_code", buildingCode);
            data.put("room", room);
            data.put("sensorData", sensorDataList);  // Add the collected sensor and Wi-Fi data

            userDocRef.set(data)
                    .addOnSuccessListener(aVoid -> Toast.makeText(RoomRecordActivity.this, "Sensor data recorded successfully!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(RoomRecordActivity.this, "Error recording data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void initializeSensorsAndLocation() {
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        // Register sensor listeners
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);
        }
    }

    // Override sensor and location event listeners
    @Override
    public void onSensorChanged(SensorEvent event) {
        // Add sensor data collection logic here (X, Y, Z values for accelerometer, magnetometer, gyroscope)
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
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Implement if needed
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }
}
