package com.example.datacollectorx.view;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.datacollectorx.R;
import com.example.datacollectorx.viewmodel.AuthViewModel;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AuthActivity extends BaseActivity {

    private AuthViewModel authViewModel;
    private EditText editTextEmail, editTextPassword;
    private Button buttonRegister, buttonLogin;
    private FirebaseFirestore db;
    private SensorManager sensorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        // Initialize ViewModel and Firestore
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        db = FirebaseFirestore.getInstance();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Bind UI elements
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonRegister = findViewById(R.id.buttonRegister);
        buttonLogin = findViewById(R.id.buttonLogin);

        // Set up observers
        authViewModel.getUserLiveData().observe(this, new Observer<FirebaseUser>() {
            @Override
            public void onChanged(FirebaseUser firebaseUser) {
                if (firebaseUser != null) {
                    Toast.makeText(AuthActivity.this, "Welcome, " + firebaseUser.getEmail(), Toast.LENGTH_SHORT).show();
                    checkLocationServices(firebaseUser);
                }
            }
        });

        authViewModel.getAuthErrorLiveData().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String errorMessage) {
                if (errorMessage != null) {
                    Toast.makeText(AuthActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Set up button click listeners
        buttonRegister.setOnClickListener(view -> signUpUser());
        buttonLogin.setOnClickListener(view -> loginUser());
    }

    private void signUpUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // Regex to ensure email is a @ualberta.ca email
        String ualbertaEmailRegex = "^[A-Za-z0-9._%+-]+@ualberta\\.ca$";

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email and Password cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate if the email is a valid @ualberta.ca email
        if (!email.matches(ualbertaEmailRegex)) {
            Toast.makeText(this, "Please use a valid @ualberta.ca email address", Toast.LENGTH_SHORT).show();
            return;
        }

        // Proceed with the signup process
        authViewModel.signUp(email, password);
    }


    private void loginUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email and Password cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        authViewModel.signIn(email, password);
    }

    private void checkLocationServices(FirebaseUser firebaseUser) {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!isLocationEnabled) {
            showLocationServicesDialog(firebaseUser);
        } else {
            checkTermsAgreement(firebaseUser);
        }
    }

    private void showLocationServicesDialog(FirebaseUser firebaseUser) {
        new AlertDialog.Builder(this)
                .setTitle("Location Services Required")
                .setMessage("This app requires location services to be enabled. Please enable location services.")
                .setPositiveButton("Enable", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Redirect the user to the location settings
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Show toast and close the app
                        Toast.makeText(AuthActivity.this, "App cannot function without location services.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void checkTermsAgreement(FirebaseUser firebaseUser) {
        String uid = firebaseUser.getUid();
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean hasAgreedToTerms = documentSnapshot.getBoolean("hasAgreedToTerms");
                        if (hasAgreedToTerms == null || !hasAgreedToTerms) {
                            showTermsAndConditionsDialog(firebaseUser);
                        } else {
                            checkDeviceCompatibility();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle failure to retrieve user data
                    Toast.makeText(AuthActivity.this, "Error checking terms agreement: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showTermsAndConditionsDialog(FirebaseUser firebaseUser) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Terms and Conditions");

        // Set up the terms text with a scrollable view
        final TextView termsTextView = new TextView(this);
        termsTextView.setText("title of the study: \tSensor Data Collection for Campus Map Building\n" +
                "\n" +
                "Principal Investigator(s) (Supervisor(s)):\tMo Adel Abdelghany\n" +
                "\t\t\t\t\t\tMSc. Student \n" +
                "\t\t\t\t\t\tDepartment of Computing Science\n" +
                "\t\t\t\t\t\tUniversity of Alberta\n" +
                "\t\t\t\t\t\tEdmonton, AB\n" +
                "\t\t\t\t\t\tmoadel.abdelghany@ualberta.ca\n" +
                "\n" +
                "Dr. Eleni Stroulia\n" +
                "\t\t\t\t\t\tProfessor \n" +
                "\t\t\t\t\t\tDepartment of Computing Science\n" +
                "\t\t\t\t\t\tUniversity of Alberta\n" +
                "\t\t\t\t\t\tEdmonton, AB\n" +
                "\t\t\t\t\t\tstroulia@ualberta.ca\n" +
                "\n" +
                "Invitation to Participate: You are invited to participate in this research data collection on Sensor Data Collection for Campus Map Building.\n" +
                "\n" +
                "Purpose of the Study: The purpose of this study is to collect and integrate data from multiple sensors—such as Received Signal Strength Indicator  (RSSI) from Wi-Fi, GPS, gyroscope, accelerometer, magnetometer, and rotation vector—a highly accurate deep-learning AI indoor localizer can be developed. And test our hypothesis that by applying either temporal machine learning methods or converting this multimodal data into heatmaps and using Convolutional Neural Networks (CNNs), we can achieve precise and reliable indoor localization. \n" +
                "\n" +
                "Participation: The participant must download and use an in-house developed mobile application for which we send a download link. The application will guide the participant to walk through specific buildings around the University of Alberta campus (The Main Quad). When a participant opens the application, the application will ask for participation consent in the first login, and will guide the user to walk inside a specific building. Once inside, the participant must click on a button to save a log of the current device’s sensors. This collected data will be sent to our secure server. To verify a participant’s data accuracy, the participant will be asked to take a picture of the room label through the application. Each participant will be assigned one building to complete in order to fully participate in the study. Full participation will take approximately 20 minutes to an hour to complete, depending on the building assigned. Assignments of the buildings will be done as first come first serve, the application will ask the user to select the building, from a list of incomplete buildings. Each building requires ten participants, depending on the availability. Participants can withdraw ANY TIME from the study by clicking on the withdraw button as seen on documentation 5.0 and simply uninstalling the application.\n" +
                " \n" +
                "Benefits: There are no benefits to participating in this study. \n" +
                "\n" +
                "Risks: There are no risks associated with participation in this study.\n" +
                "\n" +
                "Confidentiality and Anonymity: The information that you will share will remain strictly confidential and will be used solely for the purpose of this research. The only people who will have access to the research data are the principal investigator (Mo Adel Abdelghany) and the study supervisor (Eleni Stroulia). Submitted survey responses are anonymous; no personal information will be linked to your survey responses. Anonymity is not guaranteed as you are required to register for the survey using a valid email address.\n" +
                "\n" +
                "\n" +
                "Data Storage:  Data collected by the application will be encrypted and stored on a password-protected computer in the Department of Computing Science at the University of Alberta.\n" +
                "\n" +
                "Compensation: Participants will receive a gift card for their involvement in this research. Each participant will be paid according to the specific building they are assigned to, with amounts ranging from $15 to $40 per building. Participants working on the study will receive different amounts based on the following table:\n" +
                "\n" +
                "\n" +
                "Athabasca Hall\t20$\t for 30 Minutes\n" +
                "Computing Science Center\t15$ \t for 20 Minutes\n" +
                "Assiniboia Hall\t20$\t for 30 Minutes\n" +
                "Pembina Hall\t20$ \t for 30 Minutes\n" +
                "South Academic Building (SAB)\t30$\t for 40 Minutes\n" +
                "Student Union Building (SUB)\t20$ \tfor 30 Minutes\n" +
                "Central Academic Building (CAB)\t20$\t for 30 Minutes\n" +
                "CCIS\t40$\t for one hour\n" + "all times are approximate\n" +
                "\n" +
                "Voluntary Participation: You are under no obligation to participate. And if you do, an amount of money will be recorded to your account and is shown in the application for every room scanned, this amount is calculated based on the number of rooms divided over each building’s full price as shown in the table, Should you choose to withdraw midway through the data collection process simply close and uninstall the application and no further responses for the survey will be collected. Given the real time nature of the survey, once you have submitted a data log, it will no longer be possible to withdraw that record from the study. Participants may withdraw from the survey at any time, however, data submitted up to this point will be used.\n" +
                "The implication of a participant's withdrawal is that they will no longer collect data and they will only receive a partial reward for the data they have contributed to date.\n" +
                "\n" +
                "Information about the Study Results: The purpose of this study is purely for data collection.\n" +
                "\n" +
                "Contact Information:  If you have any questions or require more information about the study itself, you may contact the researcher (or his supervisor) at the emails mentioned herein.  \n" +
                "\n" +
                "The plan for this study has been reviewed by a Research Ethics Board at the University of Alberta. If you have any questions regarding your rights as a research participant or how the research is being conducted, you may contact the Research Ethics Office at 780-492-2615.\n" +
                "\n" +
                "Please keep this form for your records.\n" +
                "\n" +

                "UofA ethics board approval no. Pro00143170 "+
                "Checking “I agree” button on this form means you’re a 18 years old or above, and your consent to it. ");
        termsTextView.setPadding(16, 16, 16, 16);
        termsTextView.setMovementMethod(new ScrollingMovementMethod()); // Allows scrolling

        // Wrap the TextView in a ScrollView
        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(termsTextView);

        // Set up the checkbox
        final CheckBox checkBox = new CheckBox(this);
        checkBox.setText("I agree");

        // Layout for the dialog
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);
        layout.addView(scrollView); // Add scrollable terms
        layout.addView(checkBox);   // Add checkbox for agreement

        builder.setView(layout);

        // Set up the buttons
        builder.setPositiveButton("Agree", (dialog, which) -> {
            if (checkBox.isChecked()) {
                // Update Firestore to indicate agreement
                db.collection("users").document(firebaseUser.getUid())
                        .update("hasAgreedToTerms", true)
                        .addOnSuccessListener(aVoid -> {
                            checkDeviceCompatibility(); // Proceed to the next step
                        })
                        .addOnFailureListener(e -> {
                            // Handle failure to update user data
                            Toast.makeText(AuthActivity.this, "Error saving agreement: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else {
                Toast.makeText(this, "You must agree to the terms to use this app.", Toast.LENGTH_SHORT).show();
                showTermsAndConditionsDialog(firebaseUser); // Show the dialog again if not agreed
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            // Exit the app if the user does not agree
            finish();
        });

        // Make sure the dialog can't be dismissed without a decision
        builder.setCancelable(false);
        builder.show();
    }


    private void checkDeviceCompatibility() {
        // Check if the device has the required sensors
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        Sensor gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        if (accelerometer == null || magnetometer == null || gyroscope == null) {
            showIncompatibilityDialog();
        } else {
            navigateToMainActivity();
        }
    }

    private void showIncompatibilityDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Device Not Compatible")
                .setMessage("Your device is not compatible with the requirements of the study, so you cannot participate. Thank you for your interest!")
                .setPositiveButton("OK", (dialog, which) -> {
                    finish(); // Close the app if the device is not compatible
                })
                .setCancelable(false)
                .show();
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(AuthActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
