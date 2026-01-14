package com.example.datacollectorx.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.example.datacollectorx.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashSet;
import java.util.Set;

public class FloorSelectionActivity extends BaseActivity {

    private FloorPlanGridView floorPlanGridView;
    private TextView textViewSelectedInfo;
    private Button buttonConfirmSelection;

    private int selectedIndex = -1;
    private float selectedXm = -1f;
    private float selectedYm = -1f;
    private String buildingCode;

    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onResume() {
        super.onResume();
        loadRecordedSquares();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_floor_selection);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        buildingCode = getIntent().getStringExtra("building_code");

        floorPlanGridView = findViewById(R.id.floorPlanGridView);
        textViewSelectedInfo = findViewById(R.id.textViewSelectedInfo);
        buttonConfirmSelection = findViewById(R.id.buttonConfirmSelection);

        floorPlanGridView.setOnSquareSelectedListener((index, xm, ym) -> {
            selectedIndex = index;
            selectedXm = xm;
            selectedYm = ym;
            textViewSelectedInfo.setText(String.format("Selected Square: %d (%.2fm, %.2fm)", index, xm, ym));
            buttonConfirmSelection.setEnabled(true);
        });

        buttonConfirmSelection.setOnClickListener(v -> {
            Intent intent = new Intent(FloorSelectionActivity.this, RoomRecordActivity.class);
            intent.putExtra("building_code", buildingCode);
            intent.putExtra("square_index", selectedIndex);
            intent.putExtra("x_m", selectedXm);
            intent.putExtra("y_m", selectedYm);
            intent.putExtra("room", "Square_" + selectedIndex);
            startActivity(intent);
        });
    }

    private void loadRecordedSquares() {
        // Fetch squares already recorded in the global collection for this user and building
        db.collection("sensorData_new")
                .whereEqualTo("user_id", userId)
                .whereEqualTo("building_code", buildingCode)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Set<Integer> recorded = new HashSet<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Long index = doc.getLong("square_index");
                        if (index != null) {
                            recorded.add(index.intValue());
                        }
                    }
                    floorPlanGridView.setRecordedSquares(recorded);
                    
                    // Reset selection if the previously selected square was just recorded
                    if (recorded.contains(selectedIndex)) {
                        selectedIndex = -1;
                        buttonConfirmSelection.setEnabled(false);
                        textViewSelectedInfo.setText("Selected: None");
                    }
                });
    }
}
