package com.cht.smsforward3;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for displaying SMS messages with verification codes
 */
public class SmsAdapter extends RecyclerView.Adapter<SmsAdapter.SmsViewHolder> {
    
    private List<SmsMessage> smsMessages;
    private Context context;
    
    public SmsAdapter(Context context) {
        this.context = context;
        this.smsMessages = new ArrayList<>();
    }
    
    @NonNull
    @Override
    public SmsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sms_message, parent, false);
        return new SmsViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull SmsViewHolder holder, int position) {
        SmsMessage smsMessage = smsMessages.get(position);
        holder.bind(smsMessage);
    }
    
    @Override
    public int getItemCount() {
        return smsMessages.size();
    }
    
    /**
     * Add a new SMS message to the list
     */
    public void addSmsMessage(SmsMessage smsMessage) {
        smsMessages.add(0, smsMessage); // Add to top of list
        notifyItemInserted(0);
    }
    
    /**
     * Clear all SMS messages
     */
    public void clearMessages() {
        int size = smsMessages.size();
        smsMessages.clear();
        notifyItemRangeRemoved(0, size);
    }
    
    /**
     * Get all SMS messages
     */
    public List<SmsMessage> getMessages() {
        return new ArrayList<>(smsMessages);
    }
    
    /**
     * ViewHolder for SMS message items
     */
    class SmsViewHolder extends RecyclerView.ViewHolder {
        
        private TextView senderText;
        private TextView timestampText;
        private TextView contentText;
        private LinearLayout verificationCodesLayout;
        private TextView verificationCodesText;
        private Button copyCodeButton;
        private TextView packageText;
        
        public SmsViewHolder(@NonNull View itemView) {
            super(itemView);
            
            senderText = itemView.findViewById(R.id.senderText);
            timestampText = itemView.findViewById(R.id.timestampText);
            contentText = itemView.findViewById(R.id.contentText);
            verificationCodesLayout = itemView.findViewById(R.id.verificationCodesLayout);
            verificationCodesText = itemView.findViewById(R.id.verificationCodesText);
            copyCodeButton = itemView.findViewById(R.id.copyCodeButton);
            packageText = itemView.findViewById(R.id.packageText);
        }
        
        public void bind(SmsMessage smsMessage) {
            // Set sender and timestamp
            senderText.setText(smsMessage.getSender());
            timestampText.setText(smsMessage.getFormattedTimestamp());
            
            // Set content with highlighting
            contentText.setText(smsMessage.getHighlightedContent());
            
            // Set package info (for debugging)
            packageText.setText(smsMessage.getPackageName());
            
            // Handle verification codes
            if (smsMessage.hasVerificationCodes()) {
                verificationCodesLayout.setVisibility(View.VISIBLE);
                
                // Display all verification codes
                String codesText = String.join(", ", smsMessage.getVerificationCodes());
                verificationCodesText.setText(codesText);
                
                // Set up copy button
                copyCodeButton.setOnClickListener(v -> {
                    String primaryCode = smsMessage.getPrimaryVerificationCode();
                    if (primaryCode != null) {
                        copyToClipboard(primaryCode);
                        Toast.makeText(context, "Verification code copied: " + primaryCode, 
                                     Toast.LENGTH_SHORT).show();
                    }
                });
                
                // Update button text with primary code
                String primaryCode = smsMessage.getPrimaryVerificationCode();
                if (primaryCode != null) {
                    copyCodeButton.setText("Copy " + primaryCode);
                } else {
                    copyCodeButton.setText("Copy Code");
                }
                
            } else {
                verificationCodesLayout.setVisibility(View.GONE);
            }
        }
        
        private void copyToClipboard(String text) {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Verification Code", text);
            clipboard.setPrimaryClip(clip);
        }
    }
}
