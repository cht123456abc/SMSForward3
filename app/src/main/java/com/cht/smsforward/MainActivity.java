package com.cht.smsforward;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
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
    private static final String SMS_STATUS_UPDATE_ACTION = "com.cht.smsforward.SMS_STATUS_UPDATE";

    private TextView notificationStatusText;
    private TextView emailStatusText;
    private TextView serverChanStatusText;
    private Button permissionButton;
    private Button emailConfigButton;
    private Button serverChanConfigButton;
    private RecyclerView smsRecyclerView;
    private TextView emptyStateText;
    private SmsAdapter smsAdapter;
    private SmsBroadcastReceiver smsBroadcastReceiver;
    private SmsDataManager smsDataManager;
    private EmailSettingsManager emailSettingsManager;
    private ServerChanSettingsManager serverChanSettingsManager;
    private Handler uiRefreshHandler;
    private Runnable uiRefreshRunnable;

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

        // Initialize Server酱 settings manager
        serverChanSettingsManager = new ServerChanSettingsManager(this);

        // Initialize UI components
        initializeUI();

        // Load saved SMS messages
        loadSavedMessages();

        // Set up SMS broadcast receiver
        setupSmsBroadcastReceiver();

        // Check notification access permission and email forwarding status
        checkAndUpdateAllStatus();

        // Set up periodic UI refresh to catch any missed status updates
        setupPeriodicUIRefresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "MainActivity onResume - reloading data and checking status");

        // Re-check permissions and status when returning from Settings
        checkAndUpdateAllStatus();

        // Force reload cache to get latest data from background processing
        smsDataManager.forceReloadCache();

        // Reload messages from storage to sync with any background processing
        loadSavedMessages();

        // Start periodic UI refresh when app is in foreground
        startPeriodicUIRefresh();

        Log.d(TAG, "MainActivity onResume completed - UI should be up to date");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MainActivity onDestroy - cleaning up resources");

        // Unregister broadcast receiver to avoid memory leaks
        if (smsBroadcastReceiver != null) {
            try {
                LocalBroadcastManager.getInstance(this).unregisterReceiver(smsBroadcastReceiver);
                Log.d(TAG, "Broadcast receiver unregistered successfully");
            } catch (IllegalArgumentException e) {
                // Receiver was not registered
                Log.d(TAG, "Broadcast receiver was not registered");
            }
        }

        // Stop periodic UI refresh
        stopPeriodicUIRefresh();

        // 清理SmsDataManager资源
        if (smsDataManager != null) {
            smsDataManager.cleanup();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "MainActivity onPause - keeping broadcast receiver active for background updates");
        // Keep broadcast receiver registered to receive status updates even when app is in background
        // This ensures real-time status updates work regardless of app state

        // Stop periodic UI refresh when app goes to background
        stopPeriodicUIRefresh();
    }

    /**
     * Initialize UI components
     */
    private void initializeUI() {
        // Initialize UI components
        Log.e(TAG, "Finding UI components");
        notificationStatusText = findViewById(R.id.notificationStatusText);
        emailStatusText = findViewById(R.id.emailStatusText);
        serverChanStatusText = findViewById(R.id.serverChanStatusText);
        permissionButton = findViewById(R.id.permissionButton);
        emailConfigButton = findViewById(R.id.emailConfigButton);
        serverChanConfigButton = findViewById(R.id.serverChanConfigButton);
        smsRecyclerView = findViewById(R.id.smsRecyclerView);
        emptyStateText = findViewById(R.id.emptyStateText);

        Log.e(TAG, "UI components found - emailConfigButton: " + (emailConfigButton != null) +
              ", serverChanConfigButton: " + (serverChanConfigButton != null) +
              ", notificationStatusText: " + (notificationStatusText != null) +
              ", emailStatusText: " + (emailStatusText != null) +
              ", serverChanStatusText: " + (serverChanStatusText != null));

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

        serverChanConfigButton.setOnClickListener(v -> {
            Log.e(TAG, "Server酱 config button clicked");
            openServerChanConfiguration();
        });

        Log.e(TAG, "UI components initialized successfully");
        System.out.println("UI components initialized successfully");
    }

    /**
     * Set up broadcast receiver for SMS notifications
     */
    private void setupSmsBroadcastReceiver() {
        smsBroadcastReceiver = new SmsBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(SMS_RECEIVED_ACTION);
        filter.addAction(SMS_STATUS_UPDATE_ACTION);

        // Register with LocalBroadcastManager only (simplified approach)
        LocalBroadcastManager.getInstance(this).registerReceiver(smsBroadcastReceiver, filter);

        Log.e(TAG, "SMS broadcast receiver registered (LocalBroadcastManager only)");
    }

    /**
     * Set up periodic UI refresh to catch any missed status updates
     */
    private void setupPeriodicUIRefresh() {
        uiRefreshHandler = new Handler(getMainLooper());
        uiRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Periodic UI refresh - checking for status updates");

                // Force reload cache and refresh UI
                smsDataManager.forceReloadCache();
                loadSavedMessages();

                // Schedule next refresh in 10 seconds
                uiRefreshHandler.postDelayed(this, 10000);
            }
        };
    }

    /**
     * Start periodic UI refresh (when app is in foreground)
     */
    private void startPeriodicUIRefresh() {
        if (uiRefreshHandler != null && uiRefreshRunnable != null) {
            // Remove any existing callbacks first
            uiRefreshHandler.removeCallbacks(uiRefreshRunnable);
            // Start periodic refresh every 10 seconds
            uiRefreshHandler.postDelayed(uiRefreshRunnable, 10000);
            Log.d(TAG, "Started periodic UI refresh");
        }
    }

    /**
     * Stop periodic UI refresh (when app goes to background)
     */
    private void stopPeriodicUIRefresh() {
        if (uiRefreshHandler != null && uiRefreshRunnable != null) {
            uiRefreshHandler.removeCallbacks(uiRefreshRunnable);
            Log.d(TAG, "Stopped periodic UI refresh");
        }
    }

    /**
     * Check and update all status displays (notification access, email forwarding, and Server酱 forwarding)
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

        // Check Server酱 forwarding status
        ServerChanConfig serverChanConfig = serverChanSettingsManager.loadServerChanConfig();
        boolean serverChanEnabled = serverChanConfig.isEnabled();
        boolean serverChanValid = serverChanConfig.isValid();
        Log.d(TAG, "Server酱 forwarding enabled: " + serverChanEnabled + ", valid: " + serverChanValid);

        // Update UI for all statuses
        updateNotificationStatus(notificationEnabled);
        updateEmailForwardingStatus(emailEnabled, emailValid);
        updateServerChanForwardingStatus(serverChanEnabled, serverChanValid);

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
     * Update Server酱 forwarding status display
     */
    private void updateServerChanForwardingStatus(boolean enabled, boolean valid) {
        if (enabled && valid) {
            serverChanStatusText.setText(getString(R.string.serverchan_forwarding_enabled));
            serverChanStatusText.setTextColor(getResources().getColor(R.color.success_green));
            Log.d(TAG, "Server酱 forwarding status: enabled and ready");
        } else {
            serverChanStatusText.setText(getString(R.string.serverchan_forwarding_disabled));
            serverChanStatusText.setTextColor(getResources().getColor(R.color.error_red));
            Log.d(TAG, "Server酱 forwarding status: disabled or invalid");
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
        Log.d(TAG, "Handling new SMS notification");

        // 强制重新加载缓存以确保获取最新数据
        smsDataManager.forceReloadCache();

        // Simply reload messages from storage to sync UI
        // The actual processing and storage is handled by SmsNotificationListener
        loadSavedMessages();

        // Show toast for verification codes if available
        String primaryCode = intent.getStringExtra("primary_verification_code");
        ArrayList<String> verificationCodes = intent.getStringArrayListExtra("verification_codes");
        List<String> codes = verificationCodes != null ? verificationCodes : new ArrayList<>();

        Log.d(TAG, "SMS notification details - Primary code: " + primaryCode + ", Codes count: " + codes.size());

        if (primaryCode != null) {
            showToast(getString(R.string.toast_new_verification_code, primaryCode));
        } else if (!codes.isEmpty()) {
            showToast(getString(R.string.toast_new_sms_with_codes));
        } else {
            showToast(getString(R.string.toast_new_sms));
        }

        Log.d(TAG, "UI updated for new SMS notification - Current adapter count: " + smsAdapter.getItemCount());
    }



    /**
     * Load saved SMS messages from persistent storage
     */
    private void loadSavedMessages() {
        List<SmsMessage> savedMessages = smsDataManager.loadSmsMessages();

        // Use the more efficient update method that properly notifies the adapter
        smsAdapter.updateAllMessages(savedMessages);
        updateEmptyState();
        Log.d(TAG, "Loaded " + savedMessages.size() + " saved SMS messages");
    }

    /**
     * Open email configuration activity
     */
    private void openEmailConfiguration() {
        Intent intent = new Intent(this, EmailConfigActivity.class);
        startActivity(intent);
    }

    /**
     * Open Server酱 configuration activity
     */
    private void openServerChanConfiguration() {
        Intent intent = new Intent(this, ServerChanConfigActivity.class);
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

            if (SMS_RECEIVED_ACTION.equals(intent.getAction())) {
                Log.e(TAG, "SMS broadcast received - updating UI");
                handleNewSmsNotification(intent);
            } else if (SMS_STATUS_UPDATE_ACTION.equals(intent.getAction())) {
                Log.e(TAG, "SMS status update received - refreshing UI");
                handleSmsStatusUpdate(intent);
            } else {
                Log.e(TAG, "Broadcast action mismatch - ignoring: " + intent.getAction());
            }
        }
    }

    /**
     * Handle SMS status update notification
     */
    private void handleSmsStatusUpdate(Intent intent) {
        Log.d(TAG, "Handling SMS status update");

        // Extract message details from intent
        String sender = intent.getStringExtra("sender");
        String content = intent.getStringExtra("content");
        long timestamp = intent.getLongExtra("timestamp", 0);
        String forwardStatus = intent.getStringExtra("forward_status");
        String forwardError = intent.getStringExtra("forward_error");

        Log.d(TAG, "Status update for message from " + sender + " at " + timestamp + " - Status: " + forwardStatus);

        // 强制重新加载缓存以确保获取最新状态
        smsDataManager.forceReloadCache();

        // Try to find and update the specific message in the adapter first (more efficient)
        List<SmsMessage> currentMessages = smsDataManager.loadSmsMessages();
        SmsMessage updatedMessage = null;

        for (SmsMessage message : currentMessages) {
            if (message.getTimestamp() == timestamp &&
                message.getSender().equals(sender) &&
                message.getContent().equals(content)) {
                updatedMessage = message;
                break;
            }
        }

        if (updatedMessage != null) {
            // Try to update the specific item in the adapter
            boolean updated = smsAdapter.updateSmsMessage(updatedMessage);
            if (!updated) {
                // If specific update failed, reload all messages
                Log.d(TAG, "Specific message update failed, reloading all messages");
                loadSavedMessages();
            } else {
                Log.d(TAG, "Successfully updated specific message in adapter");
            }
        } else {
            // If message not found, reload all messages
            Log.d(TAG, "Updated message not found, reloading all messages");
            loadSavedMessages();
        }

        // Show a brief status update toast if there's an error (but not for disabled services)
        if (forwardError != null && !forwardError.isEmpty() && !"disabled".equals(forwardError)) {
            showToast("转发失败: " + forwardError);
        }
    }


}