package com.example.datacollectorx.util;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datacollectorx.R;

import java.util.List;
import java.util.Map;

public class BuildingAdapter extends RecyclerView.Adapter<BuildingAdapter.ViewHolder> {

    private List<Integer> buildingImages;
    private List<String> buildingLabels;
    private List<String> buildingCodes;
    private Map<String, List<String>> roomDataMap;
    private Map<String, Map<String, Boolean>> scannedRoomsMap;
    private Map<String, Long> buildingsMap;
    private OnItemClickListener listener;

    // Constants for CAB Grid (calculated from 120ft x 190ft at 3m squares)
    private static final int CAB_TOTAL_SQUARES = 260; 

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
        this.buildingsMap = buildingsMap;
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
        holder.imageViewBuilding.setImageResource(buildingImages.get(position));
        holder.textViewBuildingLabel.setText(buildingLabels.get(position));

        String buildingCode = buildingCodes.get(position);

        // Determine Total Squares based on Building
        int totalTarget;
        if ("CAB".equals(buildingCode)) {
            totalTarget = CAB_TOTAL_SQUARES;
        } else {
            // Fallback to old room list size for other buildings if they haven't been converted to grid yet
            List<String> rooms = roomDataMap.get(buildingCode);
            totalTarget = (rooms != null) ? rooms.size() : 0;
        }

        // Calculate recorded squares/rooms
        int recordedCount = 0;
        if (scannedRoomsMap.containsKey(buildingCode)) {
            Map<String, Boolean> scanned = scannedRoomsMap.get(buildingCode);
            recordedCount = (int) scanned.values().stream().filter(v -> v).count();
        }

        // Calculate progress percentage
        int progress = totalTarget > 0 ? (recordedCount * 100) / totalTarget : 0;

        holder.buildingProgressBar.setProgress(progress);
        holder.textViewPercentage.setText(progress + "%");

        // Handle disabled state for buildings reaching limit
        if (buildingsMap.containsKey(buildingCode)) {
            Object countObj = buildingsMap.get(buildingCode);
            long buildingCount = 0;
            if (countObj instanceof Long) buildingCount = (Long) countObj;
            else if (countObj instanceof String) {
                try { buildingCount = Long.parseLong((String) countObj); } catch (Exception e) {}
            }

            if (buildingCount >= 10) {
                holder.itemView.setAlpha(0.5f);
                holder.itemView.setEnabled(false);
            } else {
                holder.itemView.setAlpha(1.0f);
                holder.itemView.setEnabled(true);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null && holder.itemView.isEnabled()) {
                listener.onItemClick(position);
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
}
