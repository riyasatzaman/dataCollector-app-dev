package com.example.datacollectorx.util;

import android.widget.Filter;
import android.widget.Filterable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.datacollectorx.R;

import java.util.ArrayList;
import java.util.List;

public class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.ViewHolder> implements Filterable {

    private List<String> roomList;           // Original list of rooms
    private List<String> filteredRoomList;   // Filtered list of rooms
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(String room);
    }

    public RoomAdapter(List<String> roomList, OnItemClickListener listener) {
        this.roomList = roomList;
        this.filteredRoomList = new ArrayList<>(roomList);  // Initialize with the full list
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_room, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String room = filteredRoomList.get(position);
        holder.textViewRoom.setText(room);
        holder.itemView.setOnClickListener(v -> listener.onItemClick(room));
    }

    @Override
    public int getItemCount() {
        return filteredRoomList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewRoom;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewRoom = itemView.findViewById(R.id.textViewRoom);
        }
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                List<String> filteredList = new ArrayList<>();
                if (charSequence == null || charSequence.length() == 0) {
                    filteredList.addAll(roomList);  // Show all rooms when search is empty
                } else {
                    String filterPattern = charSequence.toString().toLowerCase().trim();
                    for (String room : roomList) {
                        if (room.toLowerCase().contains(filterPattern)) {
                            filteredList.add(room);
                        }
                    }
                }

                FilterResults results = new FilterResults();
                results.values = filteredList;
                return results;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                filteredRoomList.clear();
                filteredRoomList.addAll((List) filterResults.values);
                notifyDataSetChanged();  // Refresh the list
            }
        };
    }
}
