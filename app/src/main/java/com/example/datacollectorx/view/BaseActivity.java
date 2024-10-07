package com.example.datacollectorx.view;

import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkWiFiAndLocationStatus();
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
}
