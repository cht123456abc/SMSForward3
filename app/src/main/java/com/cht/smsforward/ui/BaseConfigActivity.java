package com.cht.smsforward.ui;

import com.cht.smsforward.R;
import com.cht.smsforward.config.ForwardingConfig;
import com.cht.smsforward.config.UnifiedSettingsManager;
import com.cht.smsforward.sender.MessageSender;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Base activity for configuration screens (Email, ServerChan, etc.)
 * Provides common functionality for form handling, validation, and testing
 */
public abstract class BaseConfigActivity<T extends ForwardingConfig, S extends MessageSender<T>> extends AppCompatActivity {
    
    protected String TAG;
    protected SwitchCompat enabledSwitch;
    protected View configFormLayout;
    protected View actionButtonsCard;
    protected Button testButton;
    protected Button saveConfigButton;
    protected Button backButton;
    
    protected UnifiedSettingsManager settingsManager;
    protected S messageSender;
    protected boolean isLoadingConfiguration = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        TAG = getClass().getSimpleName();
        
        EdgeToEdge.enable(this);
        setContentView(getLayoutResourceId());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        // Initialize managers
        settingsManager = new UnifiedSettingsManager(this);
        messageSender = createMessageSender();
        
        // Initialize UI components
        initializeCommonUI();
        initializeSpecificUI();
        
        // Load existing configuration
        loadConfiguration();
        
        // Set up event listeners
        setupEventListeners();
    }
    
    /**
     * Initialize common UI components
     */
    private void initializeCommonUI() {
        enabledSwitch = findViewById(getEnabledSwitchId());
        configFormLayout = findViewById(R.id.configFormLayout);
        actionButtonsCard = findViewById(R.id.actionButtonsCard);
        testButton = findViewById(getTestButtonId());
        saveConfigButton = findViewById(R.id.saveConfigButton);
        backButton = findViewById(R.id.backButton);
    }
    
    /**
     * Load configuration and populate form
     */
    private void loadConfiguration() {
        isLoadingConfiguration = true;
        
        T config = loadConfig();
        Log.d(TAG, "Loading configuration: enabled=" + config.isEnabled());
        
        enabledSwitch.setChecked(config.isEnabled());
        populateForm(config);
        updateFormVisibility();
        
        isLoadingConfiguration = false;
    }
    
    /**
     * Set up common event listeners
     */
    private void setupEventListeners() {
        enabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d(TAG, "Enabled switch changed to: " + isChecked);
            updateFormVisibility();
            saveToggleState(isChecked);
        });
        
        testButton.setOnClickListener(v -> testConfiguration());
        saveConfigButton.setOnClickListener(v -> saveConfiguration());
        backButton.setOnClickListener(v -> finish());
    }
    
    /**
     * Update form visibility based on enabled switch
     */
    private void updateFormVisibility() {
        boolean isEnabled = enabledSwitch.isChecked();
        configFormLayout.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
        actionButtonsCard.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
        Log.d(TAG, "Form visibility updated - enabled: " + isEnabled);
    }
    
    /**
     * Save toggle state immediately when user changes switch
     */
    private void saveToggleState(boolean enabled) {
        if (isLoadingConfiguration) {
            Log.d(TAG, "Skipping toggle state save during configuration loading");
            return;
        }
        
        Log.d(TAG, "Saving toggle state immediately: " + enabled);
        
        boolean success = setEnabled(enabled);
        
        if (success) {
            Log.d(TAG, "Toggle state saved successfully: " + enabled);
            String message = enabled ? getEnabledMessage() : getDisabledMessage();
            showToast(message);
        } else {
            Log.e(TAG, "Failed to save toggle state: " + enabled);
            showToast("保存状态失败");
        }
    }
    
    /**
     * Test configuration
     */
    protected void testConfiguration() {
        T config = getConfigFromForm();
        
        if (!config.isValid()) {
            showToast(getInvalidConfigMessage());
            return;
        }
        
        showToast(getTestingMessage());
        testButton.setEnabled(false);
        
        messageSender.sendTestMessage(config, new MessageSender.SendCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    testButton.setEnabled(true);
                    showToast(getTestSuccessMessage());
                    Log.d(TAG, "Test message sent successfully");
                });
            }
            
            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    testButton.setEnabled(true);
                    showDetailedErrorDialog(error);
                    Log.e(TAG, "Test message failed: " + error);
                });
            }
        });
    }
    
    /**
     * Save configuration
     */
    private void saveConfiguration() {
        T config;
        
        if (enabledSwitch.isChecked()) {
            config = getConfigFromForm();
            if (!config.isValid()) {
                showToast(getInvalidConfigMessage());
                return;
            }
        } else {
            config = getConfigFromForm();
            config.setEnabled(false);
        }
        
        boolean success = saveConfig(config);
        
        if (success) {
            showToast(getSaveSuccessMessage());
            Log.d(TAG, "Configuration saved with enabled state: " + config.isEnabled());
        } else {
            showToast(getSaveFailureMessage());
            Log.e(TAG, "Failed to save configuration");
        }
    }
    
    /**
     * Show detailed error dialog
     */
    private void showDetailedErrorDialog(String error) {
        new AlertDialog.Builder(this)
                .setTitle(getErrorDialogTitle())
                .setMessage(error)
                .setPositiveButton("OK", null)
                .show();
    }
    
    /**
     * Show toast message
     */
    protected void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    // Abstract methods that subclasses must implement
    
    protected abstract int getLayoutResourceId();
    protected abstract int getEnabledSwitchId();
    protected abstract int getTestButtonId();
    protected abstract S createMessageSender();
    protected abstract T loadConfig();
    protected abstract boolean saveConfig(T config);
    protected abstract boolean setEnabled(boolean enabled);
    protected abstract T getConfigFromForm();
    protected abstract void populateForm(T config);
    protected abstract void initializeSpecificUI();
    
    // Abstract methods for messages
    protected abstract String getEnabledMessage();
    protected abstract String getDisabledMessage();
    protected abstract String getInvalidConfigMessage();
    protected abstract String getTestingMessage();
    protected abstract String getTestSuccessMessage();
    protected abstract String getSaveSuccessMessage();
    protected abstract String getSaveFailureMessage();
    protected abstract String getErrorDialogTitle();
}
