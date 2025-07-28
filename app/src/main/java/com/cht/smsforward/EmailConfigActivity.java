package com.cht.smsforward;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.widget.SwitchCompat;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Activity for configuring email settings
 */
public class EmailConfigActivity extends AppCompatActivity {
    private static final String TAG = "EmailConfigActivity";
    
    private SwitchCompat emailEnabledSwitch;
    private View configFormLayout;
    private View actionButtonsCard;
    private EditText senderEmailEditText;
    private EditText senderPasswordEditText;
    private EditText recipientEmailEditText;
    private Button testEmailButton;
    private Button saveConfigButton;
    private Button backButton;
    
    private EmailSettingsManager settingsManager;
    private EmailSender emailSender;
    private boolean isLoadingConfiguration = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_email_config);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
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
        actionButtonsCard = findViewById(R.id.actionButtonsCard);
        senderEmailEditText = findViewById(R.id.senderEmailEditText);
        senderPasswordEditText = findViewById(R.id.senderPasswordEditText);
        recipientEmailEditText = findViewById(R.id.recipientEmailEditText);
        testEmailButton = findViewById(R.id.testEmailButton);
        saveConfigButton = findViewById(R.id.saveConfigButton);
        backButton = findViewById(R.id.backButton);
    }
    
    /**
     * Load existing email configuration
     */
    private void loadEmailConfiguration() {
        isLoadingConfiguration = true;

        EmailConfig config = settingsManager.loadEmailConfig();
        Log.d(TAG, "Loading email configuration: enabled=" + config.isEnabled());

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

        isLoadingConfiguration = false;
    }
    
    /**
     * Set up event listeners
     */
    private void setupEventListeners() {
        emailEnabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d(TAG, "Email enabled switch changed to: " + isChecked);
            updateFormVisibility();
            // 立即保存开关状态
            saveToggleState(isChecked);
        });

        testEmailButton.setOnClickListener(v -> testEmailConfiguration());
        saveConfigButton.setOnClickListener(v -> saveEmailConfiguration());
        backButton.setOnClickListener(v -> finish());
    }
    
    /**
     * Update form visibility based on enabled switch
     */
    private void updateFormVisibility() {
        boolean enabled = emailEnabledSwitch.isChecked();
        configFormLayout.setVisibility(enabled ? View.VISIBLE : View.GONE);
        // 匹配Server酱页面的行为：只有在启用时才显示操作按钮
        actionButtonsCard.setVisibility(enabled ? View.VISIBLE : View.GONE);
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

        // 使用EmailSettingsManager的setEmailEnabled方法直接保存开关状态
        boolean success = settingsManager.setEmailEnabled(enabled);

        if (success) {
            Log.d(TAG, "Toggle state saved successfully: " + enabled);
            // 显示简短的状态提示
            String message = enabled ? "邮件转发已开启" : "邮件转发已关闭";
            showToast(message);
        } else {
            Log.e(TAG, "Failed to save toggle state: " + enabled);
            showToast("保存状态失败");
        }
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
                    showDetailedErrorDialog(error);
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
            // When enabled, get config from form and validate
            config = getEmailConfigFromForm();

            if (!config.isValid()) {
                showToast(getString(R.string.toast_fill_email_fields));
                return;
            }
        } else {
            // When disabled, preserve existing form data but set enabled = false
            config = getEmailConfigFromForm();
            config.setEnabled(false);
        }

        boolean success = settingsManager.saveEmailConfig(config);

        if (success) {
            showToast(getString(R.string.toast_config_saved));
            Log.d(TAG, "Email configuration saved with enabled state: " + config.isEnabled());
            // 不再自动返回页面，只显示成功提示
        } else {
            showToast(getString(R.string.toast_config_save_failed));
            Log.e(TAG, "Failed to save email configuration");
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
     * Show detailed error dialog with network diagnostics option
     */
    private void showDetailedErrorDialog(String error) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("邮件发送失败");
        builder.setMessage(error);

        // Add network diagnostics button
        builder.setPositiveButton("网络诊断", (dialog, which) -> {
            runNetworkDiagnostics();
        });

        builder.setNegativeButton("确定", null);
        builder.show();
    }

    /**
     * Run network diagnostics
     */
    private void runNetworkDiagnostics() {
        // Show progress dialog
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setTitle("网络诊断")
                .setMessage("正在检测网络连接...")
                .setCancelable(false)
                .create();
        progressDialog.show();

        NetworkDiagnostics.runDiagnostics(this, result -> {
            runOnUiThread(() -> {
                progressDialog.dismiss();
                showDiagnosticsResult(result);
            });
        });
    }

    /**
     * Show network diagnostics result
     */
    private void showDiagnosticsResult(NetworkDiagnostics.DiagnosticsResult result) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("网络诊断结果");
        builder.setMessage(result.getSummary());
        builder.setPositiveButton("确定", null);

        // Add retry button if there might be a solution
        if (result.networkAvailable && (result.tlsPortOpen || result.sslPortOpen)) {
            builder.setNegativeButton("重试发送", (dialog, which) -> {
                testEmailConfiguration();
            });
        }

        builder.show();
    }

    /**
     * Show toast message
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
