package com.example.datacollectorx.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.datacollectorx.repository.AuthRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuthViewModel extends ViewModel {
    private AuthRepository authRepository;
    private MutableLiveData<FirebaseUser> userLiveData;
    private MutableLiveData<String> authErrorLiveData;
    private MutableLiveData<Map<String, List<String>>> scannedRoomsLiveData;
    private FirebaseFirestore db;

    public AuthViewModel() {
        authRepository = new AuthRepository();
        userLiveData = new MutableLiveData<>();
        authErrorLiveData = new MutableLiveData<>();
        scannedRoomsLiveData = new MutableLiveData<>(new HashMap<>());  // Initialize empty map
        db = FirebaseFirestore.getInstance();
    }

    public LiveData<FirebaseUser> getUserLiveData() {
        return userLiveData;
    }

    public LiveData<String> getAuthErrorLiveData() {
        return authErrorLiveData;
    }

    public LiveData<Map<String, List<String>>> getScannedRoomsLiveData() {
        return scannedRoomsLiveData;
    }

    // Sign in the user and fetch scanned rooms
    public void signIn(String email, String password) {
        authRepository.signIn(email, password, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = authRepository.getCurrentUser();
                userLiveData.setValue(user);
                authErrorLiveData.setValue(null);
                fetchUserScannedRooms(user);  // Fetch scanned rooms on login
            } else {
                handleFirebaseAuthException(task.getException());
            }
        });
    }

    // Sign up the user, initialize Firestore data, and fetch scanned rooms
    public void signUp(String email, String password) {
        authRepository.signUp(email, password, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = authRepository.getCurrentUser();
                userLiveData.setValue(user);
                authErrorLiveData.setValue(null);
                initializeUserInFirestore(user);
                FirebaseMessaging.getInstance().getToken().addOnCompleteListener(fcmTask -> {
                    if (!fcmTask.isSuccessful()) {
                        Log.w("FCM", "Fetching FCM registration token failed", fcmTask.getException());
                        return;
                    }

                    // Get the FCM token and store it in Firestore
                    String fcmToken = fcmTask.getResult();
                    storeFcmToken(fcmToken);  // Store the token using a method that updates the Firestore user document
                });

                // Initialize new user data in Firestore
            } else {
                handleFirebaseAuthException(task.getException());
            }
        });
    }

    // Fetch scanned rooms from Firestore
    public void fetchUserScannedRooms(FirebaseUser user) {
        String uid = user.getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, List<String>> scannedRooms = (Map<String, List<String>>) documentSnapshot.get("scannedRooms");
                        if (scannedRooms == null) {
                            scannedRooms = new HashMap<>();  // If null, initialize empty map
                        }
                        scannedRoomsLiveData.setValue(scannedRooms);  // Set LiveData
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error fetching user data: " + e.getMessage());
                });
    }

    // Add a scanned room to Firestore
    public void addScannedRoom(FirebaseUser user, String buildingCode, String roomCode) {
        String uid = user.getUid();

        db.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Map<String, List<String>> scannedRooms = (Map<String, List<String>>) documentSnapshot.get("scannedRooms");
                if (scannedRooms == null) {
                    scannedRooms = new HashMap<>();
                }

                // Update the list for the specific building
                List<String> roomsInBuilding = scannedRooms.get(buildingCode);
                if (roomsInBuilding != null && !roomsInBuilding.contains(roomCode)) {
                    roomsInBuilding.add(roomCode);
                } else {
                    // If no rooms scanned yet for this building, create a new list
                    roomsInBuilding = new ArrayList<>();
                    roomsInBuilding.add(roomCode);
                    scannedRooms.put(buildingCode, roomsInBuilding);
                }

                // Update Firestore with the new scanned rooms
                db.collection("users").document(uid)
                        .update("scannedRooms", scannedRooms)
                        .addOnSuccessListener(aVoid -> Log.d("Firestore", "Room added successfully."))
                        .addOnFailureListener(e -> Log.e("Firestore", "Error adding room: " + e.getMessage()));
            }
        });
    }



    public void storeFcmToken(String fcmToken) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // Prepare the FCM token update
            Map<String, Object> updates = new HashMap<>();
            updates.put("fcmToken", fcmToken);

            // Update the Firestore document with the FCM token
            db.collection("users").document(uid)
                    .update(updates)  // Use update to modify only the fcmToken field
                    .addOnSuccessListener(aVoid -> Log.d("Firestore", "FCM token updated successfully"))
                    .addOnFailureListener(e -> Log.e("Firestore", "Error updating FCM token: " + e.getMessage()));
        }
    }



    private void initializeUserInFirestore(FirebaseUser firebaseUser) {
        String uid = firebaseUser.getUid();

        // Initialize the recordedBuildings map with building codes set to false
        Map<String, Boolean> recordedBuildings = new HashMap<>();
        recordedBuildings.put("ATH", false);
        recordedBuildings.put("ASH", false);
        recordedBuildings.put("CAB", false);
        recordedBuildings.put("SAB", false);
        recordedBuildings.put("PBH", false);
        recordedBuildings.put("CSC", false);
        recordedBuildings.put("SUB", false);
        recordedBuildings.put("CCIS", false);

        // Other user data to be initialized
        Map<String, Object> userData = new HashMap<>();
        userData.put("scannedRooms", new HashMap<String, List<String>>());  // Initialize empty map for scanned rooms
        userData.put("hasAgreedToTerms", false);  // Default value for terms
        userData.put("earnings", 0.0);  // Initialize earnings
        userData.put("roomsScanned", 0);  // Initialize rooms scanned count
        userData.put("recordedBuildings", recordedBuildings);  // Initialize recorded buildings

        // Store user data in Firestore
        db.collection("users").document(uid)
                .set(userData)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "User data initialized successfully"))
                .addOnFailureListener(e -> Log.e("Firestore", "Failed to initialize user data: " + e.getMessage()));
    }


    private void handleFirebaseAuthException(Exception exception) {
        if (exception instanceof FirebaseAuthException) {
            String errorCode = ((FirebaseAuthException) exception).getErrorCode();
            switch (errorCode) {
                case "ERROR_INVALID_EMAIL":
                    authErrorLiveData.setValue("The email address is badly formatted.");
                    break;
                case "ERROR_WRONG_PASSWORD":
                    authErrorLiveData.setValue("The password is incorrect. Please try again.");
                    break;
                case "ERROR_USER_NOT_FOUND":
                    authErrorLiveData.setValue("There is no user corresponding to this email.");
                    break;
                case "ERROR_EMAIL_ALREADY_IN_USE":
                    authErrorLiveData.setValue("This email address is already in use.");
                    break;
                case "ERROR_WEAK_PASSWORD":
                    authErrorLiveData.setValue("The password is too weak.");
                    break;
                default:
                    authErrorLiveData.setValue("Authentication failed. Please try again.");
                    break;
            }
        } else {
            authErrorLiveData.setValue("An unexpected error occurred.");
        }
    }

    public void signOut() {
        authRepository.signOut();
        userLiveData.setValue(null);
    }
}
