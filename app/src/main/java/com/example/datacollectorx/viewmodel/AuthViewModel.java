package com.example.datacollectorx.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.datacollectorx.repository.AuthRepository;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AuthViewModel extends ViewModel {
    private AuthRepository authRepository;
    private MutableLiveData<FirebaseUser> userLiveData;
    private MutableLiveData<String> authErrorLiveData;
    private FirebaseFirestore db;

    public AuthViewModel() {
        authRepository = new AuthRepository();
        userLiveData = new MutableLiveData<>();
        authErrorLiveData = new MutableLiveData<>();
        db = FirebaseFirestore.getInstance();  // Initialize Firestore instance
    }

    public LiveData<FirebaseUser> getUserLiveData() {
        return userLiveData;
    }

    public LiveData<String> getAuthErrorLiveData() {
        return authErrorLiveData;
    }

    public void signIn(String email, String password) {
        authRepository.signIn(email, password, task -> {
            if (task.isSuccessful()) {
                userLiveData.setValue(authRepository.getCurrentUser());
                authErrorLiveData.setValue(null);  // Clear error messages on success
            } else {
                handleFirebaseAuthException(task.getException());
            }
        });
    }

    public void signUp(String email, String password) {
        authRepository.signUp(email, password, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = authRepository.getCurrentUser();
                userLiveData.setValue(user);
                authErrorLiveData.setValue(null);
                createUserInFirestore(user);  // Initialize user data in Firestore after registration
            } else {
                handleFirebaseAuthException(task.getException());
            }
        });
    }

    private void createUserInFirestore(FirebaseUser firebaseUser) {
        String uid = firebaseUser.getUid();

        Map<String, Object> userData = new HashMap<>();
        userData.put("roomsScanned", 0);
        userData.put("earnings", 0.0);

        db.collection("users").document(uid)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    // Log success
                    Log.d("Firestore", "User data successfully initialized for UID: " + uid);
                })
                .addOnFailureListener(e -> {
                    // Log the error
                    Log.e("Firestore", "Failed to initialize user data: " + e.getMessage());
                    authErrorLiveData.setValue("Failed to initialize user data: " + e.getMessage());
                });
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
                    authErrorLiveData.setValue("The password is too weak. Please use a stronger password.");
                    break;
                default:
                    authErrorLiveData.setValue("Authentication failed, Wrong email or password. Please try again.");
                    break;
            }
        } else {
            authErrorLiveData.setValue("An unexpected error occurred. Please try again.");
        }
    }

    public void signOut() {
        authRepository.signOut();
        userLiveData.setValue(null);
    }
}
