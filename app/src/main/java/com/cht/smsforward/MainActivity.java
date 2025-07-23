package com.cht.smsforward;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String SMS_RECEIVED_ACTION = "com.cht.smsforward.SMS_RECEIVED";

    private TextView statusText;
    private Button permissionButton;
    private Button emailConfigButton;
    private RecyclerView smsRecyclerView;
    private TextView emptyStateText;
    private SmsAdapter smsAdapter;
    private SmsBroadcastReceiver smsBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "=== MAIN ACTIVITY ONCREATE STARTED ===");
        System.out.println("=== MAIN ACTIVITY ONCREATE STARTED ===");

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI components
        initializeUI();

        // Set up SMS broadcast receiver
        setupSmsBroadcastReceiver();

        // Check notification access permission
        checkNotificationAccess();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-check permission when returning from Settings
        checkNotificationAccess();

        // Re-register broadcast receiver
        if (smsBroadcastReceiver != null) {
            IntentFilter filter = new IntentFilter(SMS_RECEIVED_ACTION);

            // Register with LocalBroadcastManager
            LocalBroadcastManager.getInstance(this).registerReceiver(smsBroadcastReceiver, filter);

            // Also register regular receiver as fallback
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(smsBroadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(smsBroadcastReceiver, filter);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister broadcast receiver to avoid memory leaks
        if (smsBroadcastReceiver != null) {
            try {
                // Unregister from LocalBroadcastManager
                LocalBroadcastManager.getInstance(this).unregisterReceiver(smsBroadcastReceiver);

                // Unregister regular receiver
                unregisterReceiver(smsBroadcastReceiver);
            } catch (IllegalArgumentException e) {
                // Receiver was not registered
                Log.d(TAG, "Broadcast receiver was not registered");
            }
        }
    }

    /**
     * Initialize UI components
     */
    private void initializeUI() {
        // Initialize UI components
        Log.e(TAG, "Finding UI components");
        statusText = findViewById(R.id.statusText);
        permissionButton = findViewById(R.id.permissionButton);
        emailConfigButton = findViewById(R.id.emailConfigButton);
        smsRecyclerView = findViewById(R.id.smsRecyclerView);
        emptyStateText = findViewById(R.id.emptyStateText);

        Log.e(TAG, "UI components found - emailConfigButton: " + (emailConfigButton != null));

        // Set up RecyclerView
        smsAdapter = new SmsAdapter(this);
        smsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        smsRecyclerView.setAdapter(smsAdapter);

        // Set up button click listeners
        Log.e(TAG, "Setting up button click listeners");

        permissionButton.setOnClickListener(v -> {
            Log.e(TAG, "Permission button clicked");
            openNotificationSettings();
        });

        emailConfigButton.setOnClickListener(v -> {
            Log.e(TAG, "Email config button clicked");
            openEmailConfiguration();
        });

        Log.e(TAG, "UI components initialized successfully");
        System.out.println("UI components initialized successfully");
    }

    /**
     * Set up broadcast receiver for SMS notifications
     */
    private void setupSmsBroadcastReceiver() {
        smsBroadcastReceiver = new SmsBroadcastReceiver();
        IntentFilter filter = new IntentFilter(SMS_RECEIVED_ACTION);

        // Register with LocalBroadcastManager (more reliable for internal broadcasts)
        LocalBroadcastManager.getInstance(this).registerReceiver(smsBroadcastReceiver, filter);

        // Also register regular receiver as fallback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(smsBroadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(smsBroadcastReceiver, filter);
        }

        Log.e(TAG, "SMS broadcast receiver registered (local + regular)");
    }

    /**
     * Check if notification access permission is granted
     */
    private void checkNotificationAccess() {
        boolean isEnabled = isNotificationServiceEnabled();
        Log.d(TAG, "=== CHECKING NOTIFICATION ACCESS ===");
        Log.d(TAG, "Notification access enabled: " + isEnabled);

        if (isEnabled) {
            Log.d(TAG, "Notification access is granted - service should be running");
            updatePermissionStatus(true);
        } else {
            Log.d(TAG, "Notification access is NOT granted - service will not work");
            updatePermissionStatus(false);
        }
    }

    /**
     * Check if our NotificationListenerService is enabled
     */
    private boolean isNotificationServiceEnabled() {
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(),
                "enabled_notification_listeners");

        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (String name : names) {
                final ComponentName cn = ComponentName.unflattenFromString(name);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Update UI based on permission status
     */
    private void updatePermissionStatus(boolean hasPermission) {
        if (hasPermission) {
            statusText.setText(getString(R.string.notification_access_enabled));
            statusText.setTextColor(getResources().getColor(R.color.success_green));
            permissionButton.setVisibility(View.GONE);
            Log.d(TAG, "Ready to receive SMS notifications");
        } else {
            statusText.setText(getString(R.string.notification_access_required));
            statusText.setTextColor(getResources().getColor(R.color.error_red));
            permissionButton.setVisibility(View.VISIBLE);
            showToast("Please enable notification access for SMS forwarding");
        }

        // Update empty state visibility
        updateEmptyState();
    }

    /**
     * Update empty state visibility based on message count and permission status
     */
    private void updateEmptyState() {
        boolean hasMessages = smsAdapter.getItemCount() > 0;
        boolean hasPermission = isNotificationServiceEnabled();

        if (hasMessages) {
            emptyStateText.setVisibility(View.GONE);
            smsRecyclerView.setVisibility(View.VISIBLE);
        } else {
            emptyStateText.setVisibility(View.VISIBLE);
            smsRecyclerView.setVisibility(View.GONE);

            if (hasPermission) {
                emptyStateText.setText(getString(R.string.empty_state_message));
            } else {
                emptyStateText.setText("Notification access is required to receive SMS messages.\n\nPlease enable notification access in Settings.");
            }
        }
    }

    /**
     * Open Android Settings for notification access
     */
    private void openNotificationSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open notification settings", e);
            showToast("Please manually enable notification access in Settings");
        }
    }

    /**
     * Handle received SMS data from NotificationListenerService
     */
    private void handleReceivedSms(Intent intent) {
        String content = intent.getStringExtra("sms_content");
        String sender = intent.getStringExtra("sender");
        String packageName = intent.getStringExtra("package_name");
        long timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis());

        // Get verification codes
        ArrayList<String> verificationCodes = intent.getStringArrayListExtra("verification_codes");
        String primaryCode = intent.getStringExtra("primary_verification_code");

        if (content != null && sender != null) {
            // Create SMS message object
            List<String> codes = verificationCodes != null ? verificationCodes : new ArrayList<>();
            SmsMessage smsMessage = new SmsMessage(content, sender, packageName, timestamp, codes, primaryCode);

            // Add to adapter and update UI
            smsAdapter.addSmsMessage(smsMessage);
            updateEmptyState();

            // Show toast for verification codes
            if (primaryCode != null) {
                showToast("New verification code: " + primaryCode);
            } else if (!codes.isEmpty()) {
                showToast("New SMS with verification codes received");
            } else {
                showToast("New SMS received");
            }

            Log.d(TAG, "SMS added to UI - Sender: " + sender + ", Codes: " + codes.size());
        }
    }



    /**
     * Open email configuration activity
     */
    private void openEmailConfiguration() {
        Intent intent = new Intent(this, EmailConfigActivity.class);
        startActivity(intent);
    }

    /**
     * Show toast message
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Broadcast receiver for SMS notifications from NotificationListenerService
     */
    private class SmsBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(TAG, "=== BROADCAST RECEIVED ===");
            Log.e(TAG, "Intent action: " + intent.getAction());
            Log.e(TAG, "Expected action: " + SMS_RECEIVED_ACTION);
            System.out.println("=== BROADCAST RECEIVED ===");

            if (SMS_RECEIVED_ACTION.equals(intent.getAction())) {
                Log.e(TAG, "SMS broadcast received - processing");
                handleReceivedSms(intent);
            } else {
                Log.e(TAG, "Broadcast action mismatch - ignoring");
            }
        }
    }
}