package com.cht.smsforward;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

    private TextView notificationStatusText;
    private TextView emailStatusText;
    private Button permissionButton;
    private Button emailConfigButton;
    private RecyclerView smsRecyclerView;
    private TextView emptyStateText;
    private SmsAdapter smsAdapter;
    private SmsBroadcastReceiver smsBroadcastReceiver;
    private SmsDataManager smsDataManager;
    private EmailSettingsManager emailSettingsManager;

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

        // Initialize data manager
        smsDataManager = new SmsDataManager(this);

        // Initialize email settings manager
        emailSettingsManager = new EmailSettingsManager(this);

        // Initialize UI components
        initializeUI();

        // Load saved SMS messages
        loadSavedMessages();

        // Set up SMS broadcast receiver
        setupSmsBroadcastReceiver();

        // Check notification access permission and email forwarding status
        checkAndUpdateAllStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-check permissions and status when returning from Settings
        checkAndUpdateAllStatus();

        // Re-register broadcast receiver (LocalBroadcastManager only)
        if (smsBroadcastReceiver != null) {
            IntentFilter filter = new IntentFilter(SMS_RECEIVED_ACTION);
            LocalBroadcastManager.getInstance(this).registerReceiver(smsBroadcastReceiver, filter);
        }

        // Reload messages from storage to sync with any background processing
        loadSavedMessages();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister broadcast receiver to avoid memory leaks
        if (smsBroadcastReceiver != null) {
            try {
                LocalBroadcastManager.getInstance(this).unregisterReceiver(smsBroadcastReceiver);
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
        notificationStatusText = findViewById(R.id.notificationStatusText);
        emailStatusText = findViewById(R.id.emailStatusText);
        permissionButton = findViewById(R.id.permissionButton);
        emailConfigButton = findViewById(R.id.emailConfigButton);
        smsRecyclerView = findViewById(R.id.smsRecyclerView);
        emptyStateText = findViewById(R.id.emptyStateText);

        Log.e(TAG, "UI components found - emailConfigButton: " + (emailConfigButton != null) +
              ", notificationStatusText: " + (notificationStatusText != null) +
              ", emailStatusText: " + (emailStatusText != null));

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

        // Register with LocalBroadcastManager only (simplified approach)
        LocalBroadcastManager.getInstance(this).registerReceiver(smsBroadcastReceiver, filter);

        Log.e(TAG, "SMS broadcast receiver registered (LocalBroadcastManager only)");
    }

    /**
     * Check and update all status displays (notification access and email forwarding)
     */
    private void checkAndUpdateAllStatus() {
        Log.d(TAG, "=== CHECKING ALL STATUS ===");

        // Check notification access
        boolean notificationEnabled = isNotificationServiceEnabled();
        Log.d(TAG, "Notification access enabled: " + notificationEnabled);

        // Check email forwarding status
        EmailConfig emailConfig = emailSettingsManager.loadEmailConfig();
        boolean emailEnabled = emailConfig.isEnabled();
        boolean emailValid = emailConfig.isValid();
        Log.d(TAG, "Email forwarding enabled: " + emailEnabled + ", valid: " + emailValid);

        // Update UI for both statuses
        updateNotificationStatus(notificationEnabled);
        updateEmailForwardingStatus(emailEnabled, emailValid);

        // Update empty state and permission button visibility
        updateEmptyState();
        updatePermissionButtonVisibility(notificationEnabled);
    }

    /**
     * Update notification access status display
     */
    private void updateNotificationStatus(boolean hasPermission) {
        if (hasPermission) {
            notificationStatusText.setText(getString(R.string.notification_access_enabled));
            notificationStatusText.setTextColor(getResources().getColor(R.color.success_green));
            Log.d(TAG, "Notification access status: enabled");
        } else {
            notificationStatusText.setText(getString(R.string.notification_access_required));
            notificationStatusText.setTextColor(getResources().getColor(R.color.error_red));
            Log.d(TAG, "Notification access status: required");
        }
    }

    /**
     * Update email forwarding status display
     */
    private void updateEmailForwardingStatus(boolean enabled, boolean valid) {
        if (enabled && valid) {
            emailStatusText.setText(getString(R.string.email_forwarding_enabled));
            emailStatusText.setTextColor(getResources().getColor(R.color.success_green));
            Log.d(TAG, "Email forwarding status: enabled and ready");
        } else {
            emailStatusText.setText(getString(R.string.email_forwarding_disabled));
            emailStatusText.setTextColor(getResources().getColor(R.color.error_red));
            Log.d(TAG, "Email forwarding status: disabled or invalid");
        }
    }

    /**
     * Update permission button visibility
     */
    private void updatePermissionButtonVisibility(boolean hasNotificationPermission) {
        if (hasNotificationPermission) {
            permissionButton.setVisibility(View.GONE);
        } else {
            permissionButton.setVisibility(View.VISIBLE);
            showToast(getString(R.string.toast_notification_access_required));
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
                emptyStateText.setText(getString(R.string.notification_access_description));
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
     * Handle UI notification of new SMS (reload from storage)
     */
    private void handleNewSmsNotification(Intent intent) {
        // Simply reload messages from storage to sync UI
        // The actual processing and storage is handled by SmsNotificationListener
        loadSavedMessages();

        // Show toast for verification codes if available
        String primaryCode = intent.getStringExtra("primary_verification_code");
        ArrayList<String> verificationCodes = intent.getStringArrayListExtra("verification_codes");
        List<String> codes = verificationCodes != null ? verificationCodes : new ArrayList<>();

        if (primaryCode != null) {
            showToast(getString(R.string.toast_new_verification_code, primaryCode));
        } else if (!codes.isEmpty()) {
            showToast(getString(R.string.toast_new_sms_with_codes));
        } else {
            showToast(getString(R.string.toast_new_sms));
        }

        Log.d(TAG, "UI updated for new SMS notification");
    }



    /**
     * Load saved SMS messages from persistent storage
     */
    private void loadSavedMessages() {
        List<SmsMessage> savedMessages = smsDataManager.loadSmsMessages();

        // Sort messages by timestamp in descending order (newest first)
        savedMessages.sort((msg1, msg2) -> Long.compare(msg2.getTimestamp(), msg1.getTimestamp()));

        // Clear existing messages and add sorted messages
        smsAdapter.clearMessages();
        for (SmsMessage message : savedMessages) {
            smsAdapter.addSmsMessage(message);
        }
        updateEmptyState();
        Log.d(TAG, "Loaded " + savedMessages.size() + " saved SMS messages (sorted by timestamp)");
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

            if (SMS_RECEIVED_ACTION.equals(intent.getAction())) {
                Log.e(TAG, "SMS broadcast received - updating UI");
                handleNewSmsNotification(intent);
            } else {
                Log.e(TAG, "Broadcast action mismatch - ignoring");
            }
        }
    }


}