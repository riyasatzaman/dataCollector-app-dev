package com.example.datacollectorx.util;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datacollectorx.R;

import java.util.List;
import java.util.Map;

public class BuildingAdapter extends RecyclerView.Adapter<BuildingAdapter.ViewHolder> {

    private List<Integer> buildingImages;
    private List<String> buildingLabels;
    private List<String> buildingCodes;
    private Map<String, List<String>> roomDataMap;  // Rooms loaded from JSON
    private Map<String, Map<String, Boolean>> scannedRoomsMap;  // Scanned rooms from Firestore
    private Map<String, Long> buildingsMap;  // Map from admin collection (building -> user count)
    private OnItemClickListener listener;
    private static final int MAX_USERS_PER_BUILDING = 10;  // Maximum number of users per building

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public BuildingAdapter(List<Integer> buildingImages, List<String> buildingLabels, List<String> buildingCodes,
                           Map<String, List<String>> roomDataMap, Map<String, Map<String, Boolean>> scannedRoomsMap,
                           Map<String, Long> buildingsMap, OnItemClickListener listener) {
        this.buildingImages = buildingImages;
        this.buildingLabels = buildingLabels;
        this.buildingCodes = buildingCodes;
        this.roomDataMap = roomDataMap;
        this.scannedRoomsMap = scannedRoomsMap;
        this.buildingsMap = buildingsMap;  // Data from admin (building -> user count)
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.grid_item_building, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Set building image and label
        holder.imageViewBuilding.setImageResource(buildingImages.get(position));
        holder.textViewBuildingLabel.setText(buildingLabels.get(position));

        String buildingCode = buildingCodes.get(position);

        // Calculate total rooms from JSON
        int totalRooms = getTotalRoomsForBuilding(buildingCode);

        // Fetch scanned rooms and calculate progress
        int scannedRooms = getScannedRoomsForBuilding(buildingCode);

        // Calculate progress percentage
        int progress = totalRooms > 0 ? (scannedRooms * 100) / totalRooms : 0;

        // Set progress bar and text
        holder.buildingProgressBar.setProgress(progress);
        holder.textViewPercentage.setText(progress + "%");

        // Fetch user count for the building
        if (buildingsMap.containsKey(buildingCode)) {
            Object buildingCountObject = buildingsMap.get(buildingCode);

            // Handle the case where the object is a string and needs to be converted to a Long
            long buildingCount;
            if (buildingCountObject instanceof String) {
                try {
                    buildingCount = Long.parseLong((String) buildingCountObject);
                } catch (NumberFormatException e) {
                    buildingCount = 0; // Handle parsing failure gracefully
                }
            } else if (buildingCountObject instanceof Long) {
                buildingCount = (Long) buildingCountObject;
            } else {
                buildingCount = 0; // Default to 0 if type is unexpected
            }

            // Check if the building limit is reached
            if (buildingCount >= 10) {
                // Set the view to disabled or gray out the building selection


                holder.itemView.setAlpha(0.5f);  // Make the item look disabled visually
            } else {
                // Set the view to enabled
                holder.itemView.setEnabled(true);
                holder.itemView.setAlpha(1.0f);  // Reset opacity
            }
        }

        // Set onClick listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null && holder.itemView.isEnabled()) {
                listener.onItemClick(position);  // Pass building index
            }
        });
    }


    @Override
    public int getItemCount() {
        return buildingImages.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewBuilding;
        TextView textViewBuildingLabel;
        ProgressBar buildingProgressBar;
        TextView textViewPercentage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewBuilding = itemView.findViewById(R.id.buildingImage);
            textViewBuildingLabel = itemView.findViewById(R.id.buildingLabel);
            buildingProgressBar = itemView.findViewById(R.id.buildingProgressBar);
            textViewPercentage = itemView.findViewById(R.id.buildingPercentage);
        }
    }

    // Get total rooms from JSON data for a specific building
    private int getTotalRoomsForBuilding(String buildingCode) {
        List<String> roomsForBuilding = roomDataMap.get(buildingCode);
        return roomsForBuilding != null ? roomsForBuilding.size() : 0;
    }

    // Get scanned rooms from Firestore data for a specific building
    private int getScannedRoomsForBuilding(String buildingCode) {
        if (scannedRoomsMap.containsKey(buildingCode)) {
            Map<String, Boolean> buildingScannedRooms = scannedRoomsMap.get(buildingCode);
            return (int) buildingScannedRooms.values().stream().filter(scanned -> scanned).count();
        }
        return 0;
    }
}
