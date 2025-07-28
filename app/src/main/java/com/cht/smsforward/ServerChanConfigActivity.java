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
public class ServerChanConfigActivity extends BaseConfigActivity<ServerChanConfig, ServerChanSender> {

    private EditText sendKeyEditText;
    
    // Abstract method implementations

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_serverchan_config;
    }

    @Override
    protected int getEnabledSwitchId() {
        return R.id.serverChanEnabledSwitch;
    }

    @Override
    protected int getTestButtonId() {
        return R.id.testMessageButton;
    }

    @Override
    protected ServerChanSender createMessageSender() {
        return new ServerChanSender(this);
    }

    @Override
    protected ServerChanConfig loadConfig() {
        return settingsManager.loadServerChanConfig();
    }

    @Override
    protected boolean saveConfig(ServerChanConfig config) {
        return settingsManager.saveServerChanConfig(config);
    }

    @Override
    protected boolean setEnabled(boolean enabled) {
        return settingsManager.setServerChanEnabled(enabled);
    }
    
    @Override
    protected void initializeSpecificUI() {
        sendKeyEditText = findViewById(R.id.sendKeyEditText);
    }
    
    @Override
    protected void populateForm(ServerChanConfig config) {
        if (!TextUtils.isEmpty(config.getSendKey())) {
            sendKeyEditText.setText(config.getSendKey());
        }
    }

    @Override
    protected ServerChanConfig getConfigFromForm() {
        String sendKey = sendKeyEditText.getText().toString().trim();
        boolean enabled = enabledSwitch.isChecked();

        return new ServerChanConfig(sendKey, enabled);
    }
    
    // Message implementations

    @Override
    protected String getEnabledMessage() {
        return "Server酱转发已开启";
    }

    @Override
    protected String getDisabledMessage() {
        return "Server酱转发已关闭";
    }

    @Override
    protected String getInvalidConfigMessage() {
        return getString(R.string.toast_fill_serverchan_fields);
    }

    @Override
    protected String getTestingMessage() {
        return getString(R.string.toast_testing_serverchan);
    }

    @Override
    protected String getTestSuccessMessage() {
        return getString(R.string.toast_test_serverchan_success);
    }

    @Override
    protected String getSaveSuccessMessage() {
        return getString(R.string.toast_serverchan_config_saved);
    }

    @Override
    protected String getSaveFailureMessage() {
        return getString(R.string.toast_serverchan_config_save_failed);
    }

    @Override
    protected String getErrorDialogTitle() {
        return getString(R.string.serverchan_send_failed);
    }
    

}
