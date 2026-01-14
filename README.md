# dataCollectorX: Indoor Localization Data Collection Framework

dataCollectorX is an Android-based framework designed for high-precision indoor signal mapping. It allows researchers to collect synchronized multi-modal sensor data (Wi-Fi, Accelerometer, Gyroscope, Magnetometer, and GPS) mapped to specific spatial coordinates on a floor plan.

## Features

- **Grid-Based Spatial Sampling**: Dividies floor plans into customizable squares (default 3m x 3m) to ensure uniform data distribution.
- **Dynamic Coordinate Mapping**: Uses matrix transformations to map screen touches to real-world meter coordinates (x, y), maintaining accuracy through pan and zoom operations.
- **Cache-Invalidated Wi-Fi Scanning**: Triggers fresh hardware scans upon recording to bypass system-level Wi-Fi result caching, ensuring real-time signal fingerprints.
- **Synchronized Data Logging**: Captures high-frequency inertial sensors alongside Wi-Fi scans during a controlled 5-second window.
- **Visual Progress Tracking**: Automatically highlights completed areas on the map to prevent redundant data collection.

## Customization Guide for Researchers

This framework is designed to be adaptable to different buildings and research requirements.

### 1. Adding a Custom Floor Plan
1. Prepare your floor plan as a PNG image.
2. Place the file in `app/src/main/res/drawable/` and name it `floor_plan.png`.
3. The application will automatically detect and load this image as the interactive map.

### 2. Updating Map Metadata
To ensure the coordinate system matches your physical building, update the constants in `FloorPlanGridView.java`:

- `widthFeet` and `heightFeet`: The real-world dimensions of the area shown in the PNG.
- `squareSizeMeters`: The desired size of each grid cell (e.g., 1.5m, 3.0m).
- `imageWidthPx` and `imageHeightPx`: The pixel dimensions of your PNG file.

The coordinate system uses the top-left corner as (0,0) and calculates the center of each square for the exported coordinates.

### 3. Changing Collection Parameters
In `RoomRecordActivity.java`, you can modify:
- **Recording Duration**: The default is 5 seconds. Adjust the `progressStatus` increment logic in `startRecording()` to change the duration.
- **Data Target**: Currently configured to upload to the `sensorData_new` Firestore collection. You can change the collection name in the `recordSensorData()` method.

## Data Structure

Data is exported to Firebase Firestore in the following schema:

- `user_id`: Unique identifier of the collector.
- `building_code`: Identifier for the current building.
- `square_index`: The ID of the grid cell (1 to N).
- `x_m` / `y_m`: Real-world center coordinates of the square in meters.
- `sensorData`: Array of inertial sensor readings (accel, mag, gyro, gps).
- `wifiData`: Array of detected Wi-Fi access points containing SSID, BSSID, RSSI, Frequency, and Hardware Timestamps.

## Prerequisites

- Android Studio Flamingo or newer.
- A Firebase project with Firestore and Authentication enabled.
- `google-services.json` placed in the `app/` directory.
- Location and Wi-Fi permissions must be granted on the device.

## License

This project is intended for research and educational purposes. Future researchers are encouraged to modify the grid logic and signal processing methods to suit specific indoor positioning algorithms.
