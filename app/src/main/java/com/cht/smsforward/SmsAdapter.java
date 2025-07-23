package com.cht.smsforward;

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
     * Add a new SMS message to the list in the correct position (sorted by timestamp)
     */
    public void addSmsMessage(SmsMessage smsMessage) {
        // Find the correct insertion position (sorted by timestamp descending)
        int insertIndex = 0;
        for (int i = 0; i < smsMessages.size(); i++) {
            if (smsMessage.getTimestamp() > smsMessages.get(i).getTimestamp()) {
                insertIndex = i;
                break;
            }
            insertIndex = i + 1;
        }

        smsMessages.add(insertIndex, smsMessage);
        notifyItemInserted(insertIndex);
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
        private TextView emailStatusIndicator;
        private TextView packageText;
        
        public SmsViewHolder(@NonNull View itemView) {
            super(itemView);
            
            senderText = itemView.findViewById(R.id.senderText);
            timestampText = itemView.findViewById(R.id.timestampText);
            contentText = itemView.findViewById(R.id.contentText);
            verificationCodesLayout = itemView.findViewById(R.id.verificationCodesLayout);
            verificationCodesText = itemView.findViewById(R.id.verificationCodesText);
            emailStatusIndicator = itemView.findViewById(R.id.emailStatusIndicator);
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

                // Set up click to copy functionality
                verificationCodesText.setOnClickListener(v -> {
                    String primaryCode = smsMessage.getPrimaryVerificationCode();
                    if (primaryCode != null) {
                        copyToClipboard(primaryCode);
                        Toast.makeText(context, context.getString(R.string.toast_code_copied, primaryCode),
                                     Toast.LENGTH_SHORT).show();
                    }
                });

                // Update email status indicator
                updateEmailStatusIndicator(smsMessage);
                
            } else {
                verificationCodesLayout.setVisibility(View.GONE);
            }
        }
        
        /**
         * Update email status indicator based on SMS message email forward status
         */
        private void updateEmailStatusIndicator(SmsMessage smsMessage) {
            EmailForwardStatus status = smsMessage.getEmailForwardStatus();

            if (status == null || status == EmailForwardStatus.DISABLED) {
                emailStatusIndicator.setVisibility(View.GONE);
                return;
            }

            emailStatusIndicator.setVisibility(View.VISIBLE);

            switch (status) {
                case NOT_SENT:
                    emailStatusIndicator.setText("未转发");
                    emailStatusIndicator.setTextColor(context.getResources().getColor(android.R.color.white));
                    emailStatusIndicator.setBackgroundColor(context.getResources().getColor(android.R.color.darker_gray));
                    break;
                case SENDING:
                    emailStatusIndicator.setText("转发中");
                    emailStatusIndicator.setTextColor(context.getResources().getColor(android.R.color.white));
                    emailStatusIndicator.setBackgroundColor(context.getResources().getColor(android.R.color.holo_orange_dark));
                    break;
                case SUCCESS:
                    emailStatusIndicator.setText("已转发");
                    emailStatusIndicator.setTextColor(context.getResources().getColor(android.R.color.white));
                    emailStatusIndicator.setBackgroundColor(context.getResources().getColor(android.R.color.holo_green_dark));
                    break;
                case FAILED:
                    emailStatusIndicator.setText("转发失败");
                    emailStatusIndicator.setTextColor(context.getResources().getColor(android.R.color.white));
                    emailStatusIndicator.setBackgroundColor(context.getResources().getColor(android.R.color.holo_red_dark));
                    break;
                default:
                    emailStatusIndicator.setVisibility(View.GONE);
                    break;
            }
        }

        private void copyToClipboard(String text) {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(context.getString(R.string.copy_code), text);
            clipboard.setPrimaryClip(clip);
        }
    }
}
