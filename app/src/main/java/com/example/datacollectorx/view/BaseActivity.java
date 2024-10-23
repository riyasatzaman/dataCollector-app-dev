package com.example.datacollectorx.view;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class BaseActivity extends AppCompatActivity {
    private static final int REQUEST_NOTIFICATION_PERMISSION = 100;  // Define the constant here

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkWiFiAndLocationStatus();
        requestNotificationPermissionIfNeeded();

    }

    // Method to check if Wi-Fi and location are enabled
    private void checkWiFiAndLocationStatus() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        boolean isWifiEnabled = wifiManager.isWifiEnabled();
        boolean isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!isWifiEnabled || !isLocationEnabled) {
            showEnableServicesModal(isWifiEnabled, isLocationEnabled);
        }
    }

    // Show modal to prompt the user to enable services
    private void showEnableServicesModal(boolean isWifiEnabled, boolean isLocationEnabled) {
        String message = "";

        if (!isWifiEnabled && !isLocationEnabled) {
            message = "Please enable Wi-Fi and Location to continue.";
        } else if (!isWifiEnabled) {
            message = "Please enable Wi-Fi to continue.";
        } else if (!isLocationEnabled) {
            message = "Please enable Location to continue.";
        }

        new AlertDialog.Builder(this)
                .setTitle("Services Disabled")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Enable", (dialog, which) -> {
                    if (!isWifiEnabled) {
                        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                    }
                    if (!isLocationEnabled) {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("Exit App", (dialog, which) -> {
                    finishAffinity(); // Close the app if services are not enabled
                })
                .show();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                // Request notification permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted for notifications
                // You can handle this case as needed
            } else {
                // Permission denied
                // You can show a message to the user or handle this accordingly
                new AlertDialog.Builder(this)
                        .setTitle("Notification Permission Required")
                        .setMessage("Please allow notification permissions to receive important updates.")
                        .setPositiveButton("Grant Permission", (dialog, which) -> {
                            requestNotificationPermissionIfNeeded();
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            dialog.dismiss();
                        })
                        .show();
            }
        }
    }

}
