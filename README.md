# dataCollectorX: Indoor Localization Data Collection Framework

dataCollectorX is an Android-based framework designed for high-precision indoor signal mapping. It allows researchers to collect synchronized multi-modal sensor data (Wi-Fi, Accelerometer, Gyroscope, Magnetometer, and GPS) mapped to specific spatial coordinates on a floor plan.

## Screenshots

<p align="center">
  <img src="https://github.com/user-attachments/assets/3bc897ba-e3ef-4f11-af88-3a25f0200888" width="250" alt="Map Selection" />
  <img src="https://github.com/user-attachments/assets/a60b529b-96da-4ee4-b01e-4b5795bbd725" width="250" alt="Data Recording" />
  <img src="https://github.com/user-attachments/assets/6dcbda6e-9f01-46d9-8f68-6008c540b0d1" width="250" alt="Building Selection" />
</p>

## Features

- **Grid-Based Spatial Sampling**: Divides floor plans into customizable squares (default 3m x 3m) to ensure uniform data distribution.
- **Dynamic Coordinate Mapping**: Uses matrix transformations to map screen touches to real-world meter coordinates (x, y), maintaining accuracy through pan and zoom operations.
- **Cache-Invalidated Wi-Fi Scanning**: Triggers fresh hardware scans upon recording to bypass system-level Wi-Fi result caching, ensuring real-time signal fingerprints.
- **Synchronized Data Logging**: Captures high-frequency inertial sensors alongside Wi-Fi scans during a controlled 5-second window.
- **Visual Progress Tracking**: Automatically highlights completed areas on the map to prevent redundant data collection.

## Customization Guide for Researchers

This framework is designed to be adaptable to different buildings and research requirements.

### 1. Backend Configuration (Firestore)
The framework requires a Firebase project. You must configure your own backend:
1. Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com/).
2. Enable **Firestore Database** and **Firebase Authentication**.
3. Download your `google-services.json` and place it in the `app/` directory of the project.
4. **Security Rules**: Ensure your Firestore rules allow authenticated users to write to the `sensorData_new` and `users` collections.

### 2. Adding a Custom Floor Plan
1. Prepare your floor plan as a PNG image.
2. Place the file in `app/src/main/res/drawable/` and name it `floor_plan.png`.
3. The application will automatically detect and load this image as the interactive map.

### 3. Updating Map Metadata
To ensure the coordinate system matches your physical building, update the constants in `FloorPlanGridView.java`:
- `widthFeet` and `heightFeet`: The real-world dimensions of the area shown in the PNG.
- `squareSizeMeters`: The desired size of each grid cell (e.g., 1.5m, 3.0m).
- `imageWidthPx` and `imageHeightPx`: The pixel dimensions of your PNG file.

### 4. UI Customization
Researchers are free to remove or modify the building and room labels in the XML layouts to suit their specific environment. The framework is designed to be building-agnostic.

## Data Structure

Data is exported to Firebase Firestore in the following schema:
- `user_id`: Unique identifier of the collector.
- `building_code`: Identifier for the current building.
- `square_index`: The ID of the grid cell (1 to N).
- `x_m` / `y_m`: Real-world center coordinates of the square in meters.
- `sensorData`: Array of inertial sensor readings (accel, mag, gyro, gps).
- `wifiData`: Array of detected Wi-Fi access points.

## Project Origins
This application was developed at the University of Alberta for crowd sourcing with incentives to participants to facilitate indoor localization research.

## Prerequisites
- Android Studio Flamingo or newer.
- Location and Wi-Fi permissions must be granted on the device.

## License
This project is intended for research and educational purposes. Future researchers are encouraged to modify the grid logic and signal processing methods to suit specific indoor positioning algorithms.
