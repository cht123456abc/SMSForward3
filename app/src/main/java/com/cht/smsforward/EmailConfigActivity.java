package com.cht.smsforward;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.widget.SwitchCompat;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity for configuring email settings
 */
public class EmailConfigActivity extends AppCompatActivity {
    private static final String TAG = "EmailConfigActivity";
    
    private SwitchCompat emailEnabledSwitch;
    private View configFormLayout;
    private EditText senderEmailEditText;
    private EditText senderPasswordEditText;
    private EditText recipientEmailEditText;
    private Button testEmailButton;
    private Button saveConfigButton;
    private Button clearConfigButton;
    private Button backButton;
    
    private EmailSettingsManager settingsManager;
    private EmailSender emailSender;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_config);
        
        // Initialize managers
        settingsManager = new EmailSettingsManager(this);
        emailSender = new EmailSender(this);
        
        // Initialize UI components
        initializeUI();
        
        // Load existing configuration
        loadEmailConfiguration();
        
        // Set up event listeners
        setupEventListeners();
    }
    
    /**
     * Initialize UI components
     */
    private void initializeUI() {
        emailEnabledSwitch = findViewById(R.id.emailEnabledSwitch);
        configFormLayout = findViewById(R.id.configFormLayout);
        senderEmailEditText = findViewById(R.id.senderEmailEditText);
        senderPasswordEditText = findViewById(R.id.senderPasswordEditText);
        recipientEmailEditText = findViewById(R.id.recipientEmailEditText);
        testEmailButton = findViewById(R.id.testEmailButton);
        saveConfigButton = findViewById(R.id.saveConfigButton);
        clearConfigButton = findViewById(R.id.clearConfigButton);
        backButton = findViewById(R.id.backButton);
    }
    
    /**
     * Load existing email configuration
     */
    private void loadEmailConfiguration() {
        EmailConfig config = settingsManager.loadEmailConfig();
        
        emailEnabledSwitch.setChecked(config.isEnabled());
        
        if (!TextUtils.isEmpty(config.getSenderEmail())) {
            senderEmailEditText.setText(config.getSenderEmail());
        }
        
        if (!TextUtils.isEmpty(config.getSenderPassword())) {
            senderPasswordEditText.setText(config.getSenderPassword());
        }
        
        if (!TextUtils.isEmpty(config.getRecipientEmail())) {
            recipientEmailEditText.setText(config.getRecipientEmail());
        }
        
        updateFormVisibility();
    }
    
    /**
     * Set up event listeners
     */
    private void setupEventListeners() {
        emailEnabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateFormVisibility();
        });
        
        testEmailButton.setOnClickListener(v -> testEmailConfiguration());
        saveConfigButton.setOnClickListener(v -> saveEmailConfiguration());
        clearConfigButton.setOnClickListener(v -> clearEmailConfiguration());
        backButton.setOnClickListener(v -> finish());
    }
    
    /**
     * Update form visibility based on enabled switch
     */
    private void updateFormVisibility() {
        boolean enabled = emailEnabledSwitch.isChecked();
        configFormLayout.setVisibility(enabled ? View.VISIBLE : View.GONE);
        testEmailButton.setVisibility(enabled ? View.VISIBLE : View.GONE);
        saveConfigButton.setText(enabled ? getString(R.string.save_configuration) : getString(R.string.save_settings));
    }
    
    /**
     * Test email configuration
     */
    private void testEmailConfiguration() {
        EmailConfig config = getEmailConfigFromForm();
        
        if (!config.isValid()) {
            showToast(getString(R.string.toast_fill_email_fields));
            return;
        }

        showToast(getString(R.string.toast_testing_email));
        testEmailButton.setEnabled(false);
        
        emailSender.sendTestEmail(config, new EmailSender.EmailSendCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    testEmailButton.setEnabled(true);
                    showToast(getString(R.string.toast_test_email_success));
                    Log.d(TAG, "Test email sent successfully");
                });
            }
            
            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    testEmailButton.setEnabled(true);
                    showToast(getString(R.string.toast_test_email_failed, error));
                    Log.e(TAG, "Test email failed: " + error);
                });
            }
        });
    }
    
    /**
     * Save email configuration
     */
    private void saveEmailConfiguration() {
        EmailConfig config;
        
        if (emailEnabledSwitch.isChecked()) {
            config = getEmailConfigFromForm();
            
            if (!config.isValid()) {
                showToast(getString(R.string.toast_fill_email_fields));
                return;
            }
        } else {
            // If disabled, just save the enabled state
            config = settingsManager.loadEmailConfig();
            config.setEnabled(false);
        }
        
        boolean success = settingsManager.saveEmailConfig(config);
        
        if (success) {
            showToast(getString(R.string.toast_config_saved));
            Log.d(TAG, "Email configuration saved");
            finish(); // Return to main activity
        } else {
            showToast(getString(R.string.toast_config_save_failed));
            Log.e(TAG, "Failed to save email configuration");
        }
    }
    
    /**
     * Clear email configuration
     */
    private void clearEmailConfiguration() {
        boolean success = settingsManager.clearEmailConfig();
        
        if (success) {
            showToast(getString(R.string.toast_config_cleared));
            Log.d(TAG, "Email configuration cleared");

            // Reset form
            emailEnabledSwitch.setChecked(false);
            senderEmailEditText.setText("");
            senderPasswordEditText.setText("");
            recipientEmailEditText.setText("");
            updateFormVisibility();
        } else {
            showToast(getString(R.string.toast_config_clear_failed));
            Log.e(TAG, "Failed to clear email configuration");
        }
    }
    
    /**
     * Get email configuration from form inputs
     */
    private EmailConfig getEmailConfigFromForm() {
        String senderEmail = senderEmailEditText.getText().toString().trim();
        String senderPassword = senderPasswordEditText.getText().toString().trim();
        String recipientEmail = recipientEmailEditText.getText().toString().trim();
        boolean enabled = emailEnabledSwitch.isChecked();
        
        return new EmailConfig(senderEmail, senderPassword, recipientEmail, enabled);
    }
    
    /**
     * Show toast message
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
