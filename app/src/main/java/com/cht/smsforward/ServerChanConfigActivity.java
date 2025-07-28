package com.cht.smsforward;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Activity for configuring Server酱 settings
 */
public class ServerChanConfigActivity extends AppCompatActivity {
    private static final String TAG = "ServerChanConfigActivity";
    
    private SwitchCompat serverChanEnabledSwitch;
    private View configFormLayout;
    private View actionButtonsCard;
    private EditText sendKeyEditText;
    private Button testMessageButton;
    private Button saveConfigButton;
    private Button backButton;
    
    private ServerChanSettingsManager settingsManager;
    private ServerChanSender serverChanSender;
    private boolean isLoadingConfiguration = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_serverchan_config);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        // Initialize managers
        settingsManager = new ServerChanSettingsManager(this);
        serverChanSender = new ServerChanSender(this);
        
        // Initialize UI components
        initializeUI();
        
        // Load existing configuration
        loadServerChanConfiguration();
        
        // Set up event listeners
        setupEventListeners();
    }
    
    /**
     * Initialize UI components
     */
    private void initializeUI() {
        serverChanEnabledSwitch = findViewById(R.id.serverChanEnabledSwitch);
        configFormLayout = findViewById(R.id.configFormLayout);
        actionButtonsCard = findViewById(R.id.actionButtonsCard);
        sendKeyEditText = findViewById(R.id.sendKeyEditText);
        testMessageButton = findViewById(R.id.testMessageButton);
        saveConfigButton = findViewById(R.id.saveConfigButton);
        backButton = findViewById(R.id.backButton);
    }
    
    /**
     * Load existing Server酱 configuration
     */
    private void loadServerChanConfiguration() {
        isLoadingConfiguration = true;

        ServerChanConfig config = settingsManager.loadServerChanConfig();
        Log.d(TAG, "Loading Server酱 configuration: enabled=" + config.isEnabled());

        serverChanEnabledSwitch.setChecked(config.isEnabled());

        if (!TextUtils.isEmpty(config.getSendKey())) {
            sendKeyEditText.setText(config.getSendKey());
        }

        updateFormVisibility();

        isLoadingConfiguration = false;
    }
    
    /**
     * Set up event listeners for UI components
     */
    private void setupEventListeners() {
        // Server酱 enabled switch listener
        serverChanEnabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d(TAG, "Server酱 enabled switch changed to: " + isChecked);
            updateFormVisibility();
            // 立即保存开关状态
            saveToggleState(isChecked);
        });

        // Test message button
        testMessageButton.setOnClickListener(v -> testServerChanConfiguration());

        // Save configuration button
        saveConfigButton.setOnClickListener(v -> saveServerChanConfiguration());

        // Back button
        backButton.setOnClickListener(v -> finish());
    }
    
    /**
     * Update form visibility based on enabled switch
     */
    private void updateFormVisibility() {
        boolean isEnabled = serverChanEnabledSwitch.isChecked();

        configFormLayout.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
        actionButtonsCard.setVisibility(isEnabled ? View.VISIBLE : View.GONE);

        Log.d(TAG, "Form visibility updated - enabled: " + isEnabled);
    }

    /**
     * 立即保存开关状态（当用户手动切换开关时）
     */
    private void saveToggleState(boolean enabled) {
        // 如果正在加载配置，不要触发保存操作
        if (isLoadingConfiguration) {
            Log.d(TAG, "Skipping toggle state save during configuration loading");
            return;
        }

        Log.d(TAG, "Saving toggle state immediately: " + enabled);

        // 使用ServerChanSettingsManager的setServerChanEnabled方法直接保存开关状态
        boolean success = settingsManager.setServerChanEnabled(enabled);

        if (success) {
            Log.d(TAG, "Toggle state saved successfully: " + enabled);
            // 显示简短的状态提示
            String message = enabled ? "Server酱转发已开启" : "Server酱转发已关闭";
            showToast(message);
        } else {
            Log.e(TAG, "Failed to save toggle state: " + enabled);
            showToast("保存状态失败");
        }
    }
    
    /**
     * Get Server酱 configuration from form inputs
     */
    private ServerChanConfig getServerChanConfigFromForm() {
        String sendKey = sendKeyEditText.getText().toString().trim();
        boolean enabled = serverChanEnabledSwitch.isChecked();
        
        return new ServerChanConfig(sendKey, enabled);
    }
    
    /**
     * Test Server酱 configuration
     */
    private void testServerChanConfiguration() {
        ServerChanConfig config = getServerChanConfigFromForm();
        
        if (!config.isValid()) {
            showToast(getString(R.string.toast_fill_serverchan_fields));
            return;
        }

        showToast(getString(R.string.toast_testing_serverchan));
        testMessageButton.setEnabled(false);
        
        serverChanSender.sendTestMessage(config, new ServerChanSender.ServerChanSendCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    testMessageButton.setEnabled(true);
                    showToast(getString(R.string.toast_test_serverchan_success));
                    Log.d(TAG, "Test message sent successfully");
                });
            }
            
            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    testMessageButton.setEnabled(true);
                    showDetailedErrorDialog(error);
                    Log.e(TAG, "Test message failed: " + error);
                });
            }
        });
    }
    
    /**
     * Save Server酱 configuration
     */
    private void saveServerChanConfiguration() {
        ServerChanConfig config;

        if (serverChanEnabledSwitch.isChecked()) {
            // When enabled, get config from form and validate
            config = getServerChanConfigFromForm();

            if (!config.isValid()) {
                showToast(getString(R.string.toast_fill_serverchan_fields));
                return;
            }
        } else {
            // When disabled, preserve existing form data but set enabled = false
            config = getServerChanConfigFromForm();
            config.setEnabled(false);
        }

        boolean success = settingsManager.saveServerChanConfig(config);

        if (success) {
            showToast(getString(R.string.toast_serverchan_config_saved));
            Log.d(TAG, "Server酱 configuration saved with enabled state: " + config.isEnabled());
            // 不再自动返回页面，只显示成功提示
        } else {
            showToast(getString(R.string.toast_serverchan_config_save_failed));
            Log.e(TAG, "Failed to save Server酱 configuration");
        }
    }
    
    /**
     * Show detailed error dialog
     */
    private void showDetailedErrorDialog(String error) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.serverchan_send_failed))
                .setMessage(error)
                .setPositiveButton("OK", null)
                .show();
    }
    
    /**
     * Show toast message
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
