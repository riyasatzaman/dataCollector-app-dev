package com.example.datacollectorx.view;

import android.Manifest;
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
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.example.datacollectorx.R;

import java.util.List;

public class LiveSensorDataActivity extends BaseActivity implements SensorEventListener, LocationListener {

    private TextView textViewWifiRssi, textViewWifiBssid, textViewAccelerometer, textViewMagnetometer, textViewGyroscope, textViewGps, textViewWifiScanResults;
    private SensorManager sensorManager;
    private Sensor accelerometer, magnetometer, gyroscope;
    private LocationManager locationManager;
    private WifiManager wifiManager;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private Handler scanHandler = new Handler();
    private Runnable scanRunnable = new Runnable() {
        @Override
        public void run() {
            if (wifiManager != null && wifiManager.isWifiEnabled()) {
                wifiManager.startScan();
            }
            // Request a fresh scan every 10 seconds (respecting Android throttle)
            scanHandler.postDelayed(this, 10000);
        }
    };

    private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateWifiInfo();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_sensor_data);

        textViewWifiRssi = findViewById(R.id.textViewWifiRssi);
        textViewWifiBssid = findViewById(R.id.textViewWifiBssid);
        textViewWifiScanResults = findViewById(R.id.textViewWifiScanResults);
        textViewAccelerometer = findViewById(R.id.textViewAccelerometer);
        textViewMagnetometer = findViewById(R.id.textViewMagnetometer);
        textViewGyroscope = findViewById(R.id.textViewGyroscope);
        textViewGps = findViewById(R.id.textViewGps);
        Button buttonGoBack = findViewById(R.id.buttonGoBack);

        buttonGoBack.setOnClickListener(v -> finish());

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if (checkLocationPermission()) {
            initializeSensorsAndLocation();
        }
    }

    private boolean checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    private void initializeSensorsAndLocation() {
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_UI);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);
        }

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        updateWifiInfo();
    }

    private void updateWifiInfo() {
        if (wifiManager == null || !wifiManager.isWifiEnabled()) {
            textViewWifiRssi.setText("Wi-Fi is disabled.");
            textViewWifiBssid.setText("Wi-Fi is disabled.");
            textViewWifiScanResults.setText("");
            return;
        }

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String ssid = wifiInfo.getSSID();
        if (ssid.equals("<unknown ssid>")) ssid = "SSID Unavailable";
        
        textViewWifiRssi.setText("Wi-Fi RSSI: " + wifiInfo.getRssi() + " dBm");
        textViewWifiBssid.setText("Connected to SSID: " + ssid + ", BSSID: " + wifiInfo.getBSSID());

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            List<ScanResult> scanResults = wifiManager.getScanResults();
            StringBuilder sb = new StringBuilder();
            sb.append("Nearby Access Points (").append(scanResults.size()).append("):\n");
            for (ScanResult res : scanResults) {
                sb.append("- ").append(res.SSID).append(" (").append(res.level).append("dBm)\n");
            }
            textViewWifiScanResults.setText(sb.toString());
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            textViewAccelerometer.setText(String.format("Accelerometer: X=%.2f, Y=%.2f, Z=%.2f", event.values[0], event.values[1], event.values[2]));
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            textViewMagnetometer.setText(String.format("Magnetometer: X=%.2f, Y=%.2f, Z=%.2f", event.values[0], event.values[1], event.values[2]));
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            textViewGyroscope.setText(String.format("Gyroscope: X=%.2f, Y=%.2f, Z=%.2f", event.values[0], event.values[1], event.values[2]));
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            textViewGps.setText(String.format("GPS: Lat=%.6f, Lon=%.6f", location.getLatitude(), location.getLongitude()));
        } else {
            textViewGps.setText("GPS: No signal");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        scanHandler.post(scanRunnable);
        if (checkLocationPermission()) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(wifiScanReceiver);
        scanHandler.removeCallbacks(scanRunnable);
        sensorManager.unregisterListener(this);
        locationManager.removeUpdates(this);
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    @Override public void onProviderEnabled(String provider) {}
    @Override public void onProviderDisabled(String provider) {}
}
