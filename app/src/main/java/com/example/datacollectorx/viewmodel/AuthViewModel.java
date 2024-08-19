package com.example.datacollectorx.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.datacollectorx.repository.AuthRepository;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;

public class AuthViewModel extends ViewModel {
    private AuthRepository authRepository;
    private MutableLiveData<FirebaseUser> userLiveData;
    private MutableLiveData<String> authErrorLiveData;  // Change to String to store error messages

    public AuthViewModel() {
        authRepository = new AuthRepository();
        userLiveData = new MutableLiveData<>();
        authErrorLiveData = new MutableLiveData<>();
    }

    public LiveData<FirebaseUser> getUserLiveData() {
        return userLiveData;
    }

    public LiveData<String> getAuthErrorLiveData() {  // Return String for error messages
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
                userLiveData.setValue(authRepository.getCurrentUser());
                authErrorLiveData.setValue(null);  // Clear error messages on success
            } else {
                handleFirebaseAuthException(task.getException());
            }
        });
    }

    private void handleFirebaseAuthException(Exception exception) {
        if (exception instanceof FirebaseAuthException) {
            String errorCode = ((FirebaseAuthException) exception).getErrorCode();
            Log.d("AuthError", "Error Code: " + errorCode);
            Log.d("AuthError", "Error Message: " + exception.getMessage());

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
                    authErrorLiveData.setValue("Authentication failed: " + "Wrong email or password.");
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
