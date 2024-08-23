package com.example.datacollectorx.view;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.datacollectorx.R;

import java.util.List;

public class LiveSensorDataActivity extends AppCompatActivity implements SensorEventListener, LocationListener {

    private TextView textViewWifiRssi, textViewWifiBssid, textViewAccelerometer, textViewMagnetometer, textViewGyroscope, textViewGps;
    private SensorManager sensorManager;
    private Sensor accelerometer, magnetometer, gyroscope;
    private LocationManager locationManager;
    private WifiManager wifiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_sensor_data);

        // Bind UI elements
        textViewWifiRssi = findViewById(R.id.textViewWifiRssi);
        textViewWifiBssid = findViewById(R.id.textViewWifiBssid);
        textViewAccelerometer = findViewById(R.id.textViewAccelerometer);
        textViewMagnetometer = findViewById(R.id.textViewMagnetometer);
        textViewGyroscope = findViewById(R.id.textViewGyroscope);
        textViewGps = findViewById(R.id.textViewGps);
        Button buttonGoBack = findViewById(R.id.buttonGoBack);

        // Set click listener for the Go Back button
        buttonGoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Go back to the previous activity
            }
        });

        // Request necessary permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Request the permissions
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    1);
        } else {
            // Permissions are already granted, proceed with setting up the sensors and Wi-Fi info
            initializeSensorsAndLocation();
        }
    }

    private void initializeSensorsAndLocation() {
        // Initialize sensor manager and sensors
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        // Register sensor listeners
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);

        // Initialize location manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);
        }

        // Initialize Wi-Fi manager and update Wi-Fi info
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        updateWifiInfo();
    }

    private void updateWifiInfo() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            String ssid = wifiInfo.getSSID();
            if (ssid.equals("<unknown ssid>")) {
                ssid = "SSID Unavailable";
            }
            textViewWifiRssi.setText("Wi-Fi RSSI: " + wifiInfo.getRssi() + " dBm");
            textViewWifiBssid.setText("Connected to SSID: " + ssid + ", BSSID: " + wifiInfo.getBSSID());


            List<ScanResult> scanResults = wifiManager.getScanResults();
            StringBuilder scanResultsStringBuilder = new StringBuilder();
            for (ScanResult scanResult : scanResults) {
                scanResultsStringBuilder.append("SSID: ").append(scanResult.SSID)
                        .append(", BSSID: ").append(scanResult.BSSID)
                        .append(", RSSI: ").append(scanResult.level).append(" dBm")
                        .append(", Frequency: ").append(scanResult.frequency).append(" MHz")
                        .append(", Capabilities: ").append(scanResult.capabilities)
                        .append("\n");
            }

        } else {
            textViewWifiRssi.setText("Wi-Fi RSSI: Permission Denied");
            textViewWifiBssid.setText("Wi-Fi BSSID: Permission Denied");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                initializeSensorsAndLocation();
            } else {
                // Permission denied
                textViewWifiRssi.setText("Wi-Fi RSSI: Permission Denied");
                textViewGps.setText("GPS: Permission Denied");
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            textViewAccelerometer.setText("Accelerometer: X=" + event.values[0] + ", Y=" + event.values[1] + ", Z=" + event.values[2]);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            textViewMagnetometer.setText("Magnetometer: X=" + event.values[0] + ", Y=" + event.values[1] + ", Z=" + event.values[2]);
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            textViewGyroscope.setText("Gyroscope: X=" + event.values[0] + ", Y=" + event.values[1] + ", Z=" + event.values[2]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            textViewGps.setText("GPS: Lat=" + location.getLatitude() + ", Lon=" + location.getLongitude());
        } else {
            textViewGps.setText("GPS: No signal");
        }
    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);
        }
        updateWifiInfo();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        locationManager.removeUpdates(this);
    }
}
