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
public class EmailConfigActivity extends BaseConfigActivity<EmailConfig, EmailSender> {

    private EditText senderEmailEditText;
    private EditText senderPasswordEditText;
    private EditText recipientEmailEditText;
    
    // Abstract method implementations

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_email_config;
    }

    @Override
    protected int getEnabledSwitchId() {
        return R.id.emailEnabledSwitch;
    }

    @Override
    protected int getTestButtonId() {
        return R.id.testEmailButton;
    }

    @Override
    protected EmailSender createMessageSender() {
        return new EmailSender(this);
    }

    @Override
    protected EmailConfig loadConfig() {
        return settingsManager.loadEmailConfig();
    }

    @Override
    protected boolean saveConfig(EmailConfig config) {
        return settingsManager.saveEmailConfig(config);
    }

    @Override
    protected boolean setEnabled(boolean enabled) {
        return settingsManager.setEmailEnabled(enabled);
    }
    
    @Override
    protected void initializeSpecificUI() {
        senderEmailEditText = findViewById(R.id.senderEmailEditText);
        senderPasswordEditText = findViewById(R.id.senderPasswordEditText);
        recipientEmailEditText = findViewById(R.id.recipientEmailEditText);
    }
    
    @Override
    protected void populateForm(EmailConfig config) {
        if (!TextUtils.isEmpty(config.getSenderEmail())) {
            senderEmailEditText.setText(config.getSenderEmail());
        }

        if (!TextUtils.isEmpty(config.getSenderPassword())) {
            senderPasswordEditText.setText(config.getSenderPassword());
        }

        if (!TextUtils.isEmpty(config.getRecipientEmail())) {
            recipientEmailEditText.setText(config.getRecipientEmail());
        }
    }

    @Override
    protected EmailConfig getConfigFromForm() {
        String senderEmail = senderEmailEditText.getText().toString().trim();
        String senderPassword = senderPasswordEditText.getText().toString().trim();
        String recipientEmail = recipientEmailEditText.getText().toString().trim();
        boolean enabled = enabledSwitch.isChecked();

        return new EmailConfig(senderEmail, senderPassword, recipientEmail, enabled);
    }
    
    // Message implementations

    @Override
    protected String getEnabledMessage() {
        return "邮件转发已开启";
    }

    @Override
    protected String getDisabledMessage() {
        return "邮件转发已关闭";
    }

    @Override
    protected String getInvalidConfigMessage() {
        return getString(R.string.toast_fill_email_fields);
    }

    @Override
    protected String getTestingMessage() {
        return getString(R.string.toast_testing_email);
    }

    @Override
    protected String getTestSuccessMessage() {
        return getString(R.string.toast_test_email_success);
    }

    @Override
    protected String getSaveSuccessMessage() {
        return getString(R.string.toast_config_saved);
    }

    @Override
    protected String getSaveFailureMessage() {
        return getString(R.string.toast_config_save_failed);
    }

    @Override
    protected String getErrorDialogTitle() {
        return "Email Send Failed";
    }

    // Email-specific network diagnostics functionality

    private void testEmailConfiguration() {
        // Delegate to base class but add network diagnostics on failure
        super.testConfiguration();
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


}
