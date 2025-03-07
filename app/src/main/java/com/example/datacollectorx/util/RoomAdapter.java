package com.example.datacollectorx.util;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datacollectorx.R;

import java.util.ArrayList;
import java.util.List;

public class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.RoomViewHolder> implements Filterable {

    private List<String> roomList;
    private List<String> filteredRoomList;
    private List<String> scannedRooms;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(String room);
    }

    public RoomAdapter(List<String> roomList, List<String> scannedRooms, OnItemClickListener listener) {
        this.roomList = roomList;
        this.filteredRoomList = new ArrayList<>(roomList);  // Initialize filtered list with all rooms
        this.scannedRooms = scannedRooms;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_room, parent, false);
        return new RoomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RoomViewHolder holder, int position) {
        String room = filteredRoomList.get(position);
        holder.bind(room, scannedRooms.contains(room));  // Check if the room is already scanned
    }

    @Override
    public int getItemCount() {
        return filteredRoomList.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<String> filteredResults = new ArrayList<>();
                if (constraint == null || constraint.length() == 0) {
                    filteredResults.addAll(roomList);  // No filter applied, show all rooms
                } else {
                    String query = constraint.toString().toLowerCase().trim();
                    for (String room : roomList) {
                        if (room.toLowerCase().contains(query)) {
                            filteredResults.add(room);
                        }
                    }
                }
                FilterResults results = new FilterResults();
                results.values = filteredResults;
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredRoomList.clear();
                filteredRoomList.addAll((List<String>) results.values);
                notifyDataSetChanged();
            }
        };
    }

    // New method to update the scanned rooms list and refresh the adapter
    public void updateScannedRooms(List<String> newScannedRooms) {
        scannedRooms.clear();
        scannedRooms.addAll(newScannedRooms);
        notifyDataSetChanged();
    }

    public class RoomViewHolder extends RecyclerView.ViewHolder {
        private TextView textViewRoom;

        public RoomViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewRoom = itemView.findViewById(R.id.textViewRoom);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(filteredRoomList.get(position));
                }
            });
        }

        public void bind(String room, boolean isScanned) {
            textViewRoom.setText(room);
            if (isScanned) {
                // Set background color to green if the room has been scanned
                textViewRoom.setBackgroundColor(Color.parseColor("#4CAF50"));
                textViewRoom.setTextColor(Color.WHITE);  // White text for better visibility
            } else {
                // Set background color to default
                textViewRoom.setBackgroundColor(Color.WHITE);
                textViewRoom.setTextColor(Color.BLACK);
            }
        }
    }
}
