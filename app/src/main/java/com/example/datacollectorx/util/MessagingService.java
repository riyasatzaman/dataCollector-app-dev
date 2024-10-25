package com.example.datacollectorx.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import com.example.datacollectorx.view.AuthActivity;
import com.example.datacollectorx.view.BuildingSelectionActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.example.datacollectorx.R;  // Replace with your package name

public class MessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            sendNotification(title, body);
        }

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            // Handle data payload here
            String title = remoteMessage.getData().get("title");
            String messageBody = remoteMessage.getData().get("message");
            sendNotification(title, messageBody);
        }
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        // You can log the token here or send it to your server to send messages to this token
        Log.d("FCM Token", token);
    }

    private void sendNotification(String title, String messageBody) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        String channelId = "default_channel_id";

        // Check if the user is logged in using FirebaseAuth
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        Intent intent;
        if (currentUser != null) {
            // User is logged in, redirect to BuildingSelectionActivity
            intent = new Intent(this, BuildingSelectionActivity.class);
        } else {
            // User is not logged in, redirect to AuthActivity
            intent = new Intent(this, AuthActivity.class);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);  // Ensure the activity is brought to the top of the stack
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        // Create a notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Default Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        // Create a custom small layout
        RemoteViews notificationLayout = new RemoteViews(getPackageName(), R.layout.notification_small);
        notificationLayout.setTextViewText(R.id.notification_title, title);

        // Create a custom large layout (for expanded notification)
        RemoteViews notificationLayoutExpanded = new RemoteViews(getPackageName(), R.layout.notification_large);
        notificationLayoutExpanded.setTextViewText(R.id.notification_title, title);
        notificationLayoutExpanded.setTextViewText(R.id.notification_body, messageBody);

        // Build the notification with custom views
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.app_logo)  // Your small icon
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())  // Use custom view style
                .setCustomContentView(notificationLayout)  // Small notification layout
                .setCustomBigContentView(notificationLayoutExpanded)  // Expanded notification layout
                .setAutoCancel(true)  // Automatically remove the notification when clicked
                .setContentIntent(pendingIntent)  // Redirect based on user login status
                .setPriority(NotificationCompat.PRIORITY_HIGH);  // High priority for visibility

        // Show the notification
        notificationManager.notify(0, notificationBuilder.build());
    }


}
