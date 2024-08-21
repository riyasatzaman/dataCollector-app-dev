package com.example.datacollectorx.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.datacollectorx.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserStatsFragment extends Fragment {

    private TextView textViewRoomsScanned;
    private TextView textViewEarnings;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_stats, container, false);

        // Initialize UI elements
        textViewRoomsScanned = view.findViewById(R.id.textViewRoomsScanned);
        textViewEarnings = view.findViewById(R.id.textViewEarnings);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Load user stats
        loadUserStats();

        return view;
    }

    private void loadUserStats() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            db.collection("users").document(uid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Long roomsScanned = documentSnapshot.getLong("roomsScanned");
                            Double earnings = documentSnapshot.getDouble("earnings");

                            textViewRoomsScanned.setText("Rooms Scanned: " + roomsScanned);
                            textViewEarnings.setText("Earnings: $" + earnings);
                        }
                    })
                    .addOnFailureListener(e -> {
                        textViewRoomsScanned.setText("Failed to load data");
                        textViewEarnings.setText("Failed to load data");
                    });
        }
    }
}
