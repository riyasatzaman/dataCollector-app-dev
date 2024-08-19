package com.example.datacollectorx.repository;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class AuthRepository {

    private FirebaseAuth mAuth;

    public AuthRepository() {
        mAuth = FirebaseAuth.getInstance();
    }

    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    public void signIn(String email, String password, OnCompleteListener<AuthResult> callback) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(callback);
    }

    public void signUp(String email, String password, OnCompleteListener<AuthResult> callback) {
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(callback);
    }

    public void signOut() {
        mAuth.signOut();
    }
}
