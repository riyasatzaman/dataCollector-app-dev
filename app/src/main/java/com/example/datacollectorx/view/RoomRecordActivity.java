package com.example.datacollectorx.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.example.datacollectorx.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomRecordActivity extends BaseActivity implements SensorEventListener, LocationListener {

    private ProgressBar circularProgressBar;
    private Button buttonHoldToRecord;
    private TextView textViewRoomLabel;

    private FirebaseFirestore db;
    private String buildingCode;
    private String room;
    private int squareIndex;
    private float xMeters, yMeters;

    private int progressStatus = 0;
    private Handler handler = new Handler();
    private boolean isHolding = false;
    
    // Separate thread-safe lists for sensors and wifi
    private List<Map<String, Object>> sensorDataList = Collections.synchronizedList(new ArrayList<>());
    private List<Map<String, Object>> wifiDataList = Collections.synchronizedList(new ArrayList<>());

    private SensorManager sensorManager;
    private LocationManager locationManager;
    private WifiManager wifiManager;

    private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Collect fresh results whenever the hardware finishes a scan
            collectWifiData();
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_record);

        textViewRoomLabel = findViewById(R.id.textViewRoomLabel);
        buttonHoldToRecord = findViewById(R.id.buttonHoldToRecord);
        circularProgressBar = findViewById(R.id.circularProgressBar);

        db = FirebaseFirestore.getInstance();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        initializeSensorsAndLocation();

        buildingCode = getIntent().getStringExtra("building_code");
        room = getIntent().getStringExtra("room");
        squareIndex = getIntent().getIntExtra("square_index", -1);
        xMeters = getIntent().getFloatExtra("x_m", -1f);
        yMeters = getIntent().getFloatExtra("y_m", -1f);

        textViewRoomLabel.setText(String.format("Location: Square %d\n(%.2fm, %.2fm)", squareIndex, xMeters, yMeters));

        buttonHoldToRecord.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isHolding = true;
                    progressStatus = 0;
                    sensorDataList.clear();
                    wifiDataList.clear();
                    
                    // Invalidate Cache by triggering a fresh scan immediately
                    if (wifiManager != null) {
                        wifiManager.startScan();
                    }
                    
                    startRecording();
                    return true;
                case MotionEvent.ACTION_UP:
                    isHolding = false;
                    if (progressStatus < 100) {
                        progressStatus = 0;
                        circularProgressBar.setProgress(0);
                        Toast.makeText(this, "Released early", Toast.LENGTH_SHORT).show();
                    } else {
                        recordSensorData();
                    }
                    return true;
            }
            return false;
        });

        findViewById(R.id.buttonGoBack_room_record).setOnClickListener(v -> finish());
    }

    private void startRecording() {
        new Thread(() -> {
            while (isHolding && progressStatus < 100) {
                progressStatus += 1; 
                handler.post(() -> {
                    circularProgressBar.setProgress(progressStatus);
                    // Periodically poll Wi-Fi to catch results from any background scans
                    if (progressStatus % 20 == 0) collectWifiData();
                });
                try { Thread.sleep(50); } catch (InterruptedException e) { break; }
            }
        }).start();
    }

    private void collectWifiData() {
        if (wifiManager == null) return;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        List<ScanResult> scanResults = wifiManager.getScanResults();
        if (scanResults == null) return;

        synchronized (wifiDataList) {
            for (ScanResult res : scanResults) {
                // Check if we already have this specific measurement (BSSID + Timestamp)
                boolean exists = false;
                for (Map<String, Object> item : wifiDataList) {
                    if (res.BSSID.equals(item.get("BSSID")) && Long.valueOf(res.timestamp).equals(item.get("hw_timestamp"))) {
                        exists = true;
                        break;
                    }
                }
                
                if (!exists) {
                    Map<String, Object> wifiMap = new HashMap<>();
                    wifiMap.put("SSID", res.SSID);
                    wifiMap.put("BSSID", res.BSSID);
                    wifiMap.put("RSSI", res.level);
                    wifiMap.put("frequency", res.frequency);
                    wifiMap.put("hw_timestamp", res.timestamp); // Hardware timestamp for precision
                    wifiMap.put("collected_at", System.currentTimeMillis());
                    wifiDataList.add(wifiMap);
                }
            }
        }
    }

    private void recordSensorData() {
        // Final sweep for any late-arriving scan results
        collectWifiData(); 

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", userId);
        payload.put("building_code", buildingCode);
        payload.put("square_index", squareIndex);
        payload.put("x_m", xMeters);
        payload.put("y_m", yMeters);
        payload.put("timestamp", System.currentTimeMillis());
        
        // Use separate fields for sensors and wifi
        synchronized (sensorDataList) {
            payload.put("sensorData", new ArrayList<>(sensorDataList));
        }
        synchronized (wifiDataList) {
            payload.put("wifiData", new ArrayList<>(wifiDataList));
        }

        db.collection("sensorData_new").add(payload)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Upload Success!", Toast.LENGTH_SHORT).show();
                    recordScannedRoom(buildingCode, room);
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Upload Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void recordScannedRoom(String buildingCode, String room) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DocumentReference userDoc = db.collection("users").document(userId);
        userDoc.get().addOnSuccessListener(snap -> {
            if (snap.exists()) {
                Map<String, Map<String, Boolean>> scanned = (Map<String, Map<String, Boolean>>) snap.get("scannedRooms");
                if (scanned == null) scanned = new HashMap<>();
                if (!scanned.containsKey(buildingCode)) scanned.put(buildingCode, new HashMap<>());
                scanned.get(buildingCode).put(room, true);
                userDoc.update("scannedRooms", scanned);
            }
        });
    }

    private void initializeSensorsAndLocation() {
        int d = SensorManager.SENSOR_DELAY_NORMAL;
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), d);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), d);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), d);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(wifiScanReceiver);
        sensorManager.unregisterListener(this);
    }

    @Override public void onSensorChanged(SensorEvent event) {
        if (!isHolding) return;
        Map<String, Object> map = new HashMap<>();
        map.put("timestamp", System.currentTimeMillis());
        map.put("type", event.sensor.getType() == Sensor.TYPE_ACCELEROMETER ? "accel" : 
                       (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD ? "mag" : "gyro"));
        map.put("x", event.values[0]);
        map.put("y", event.values[1]);
        map.put("z", event.values[2]);
        sensorDataList.add(map);
    }

    @Override public void onLocationChanged(Location loc) {
        if (!isHolding || loc == null) return;
        Map<String, Object> map = new HashMap<>();
        map.put("type", "gps");
        map.put("lat", loc.getLatitude());
        map.put("lng", loc.getLongitude());
        map.put("timestamp", System.currentTimeMillis());
        sensorDataList.add(map);
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
