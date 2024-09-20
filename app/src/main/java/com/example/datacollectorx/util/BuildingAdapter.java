package com.example.datacollectorx.util;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.datacollectorx.R;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;
import java.util.Map;

public class BuildingAdapter extends RecyclerView.Adapter<BuildingAdapter.ViewHolder> {

    private List<Integer> buildingImages;
    private List<String> buildingLabels;
    private List<String> buildingCodes;  // e.g., "ATH", "CAB", etc.
    private Map<String, List<String>> roomDataMap;  // Rooms loaded from JSON (assets)
    private Map<String, Map<String, Boolean>> scannedRoomsMap;  // Scanned rooms from Firestore
    private OnItemClickListener listener;
    private FirebaseFirestore db;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public BuildingAdapter(List<Integer> buildingImages, List<String> buildingLabels, List<String> buildingCodes, Map<String, List<String>> roomDataMap, Map<String, Map<String, Boolean>> scannedRoomsMap, OnItemClickListener listener) {
        this.buildingImages = buildingImages;
        this.buildingLabels = buildingLabels;
        this.buildingCodes = buildingCodes;
        this.roomDataMap = roomDataMap;  // Room data loaded from JSON
        this.scannedRoomsMap = scannedRoomsMap;  // Scanned room data from Firestore
        this.listener = listener;
        this.db = FirebaseFirestore.getInstance();  // Initialize Firestore
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

        // Change progress bar color based on the progress value


        // Set onClick listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
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
