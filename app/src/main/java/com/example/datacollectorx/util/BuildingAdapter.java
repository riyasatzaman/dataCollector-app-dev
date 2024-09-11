package com.example.datacollectorx.util;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.datacollectorx.R;
import java.util.List;

public class BuildingAdapter extends RecyclerView.Adapter<BuildingAdapter.ViewHolder> {

    private List<Integer> buildingImages;
    private List<String> buildingLabels;  // New List for building labels
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public BuildingAdapter(List<Integer> buildingImages, List<String> buildingLabels, OnItemClickListener listener) {
        this.buildingImages = buildingImages;
        this.buildingLabels = buildingLabels;  // Initialize labels
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
        // Set the image
        holder.imageView.setImageResource(buildingImages.get(position));
        // Set the label
        holder.textViewLabel.setText(buildingLabels.get(position));

        holder.itemView.setOnClickListener(v -> listener.onItemClick(position));
    }

    @Override
    public int getItemCount() {
        return buildingImages.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView textViewLabel;  // Reference to the TextView for labels

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewBuilding);
            textViewLabel = itemView.findViewById(R.id.textViewBuildingLabel);  // Bind the TextView for labels
        }
    }
}
