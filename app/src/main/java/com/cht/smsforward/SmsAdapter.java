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
        private TextView forwardStatusIndicator;
        private TextView packageText;
        
        public SmsViewHolder(@NonNull View itemView) {
            super(itemView);
            
            senderText = itemView.findViewById(R.id.senderText);
            timestampText = itemView.findViewById(R.id.timestampText);
            contentText = itemView.findViewById(R.id.contentText);
            verificationCodesLayout = itemView.findViewById(R.id.verificationCodesLayout);
            verificationCodesText = itemView.findViewById(R.id.verificationCodesText);
            forwardStatusIndicator = itemView.findViewById(R.id.forwardStatusIndicator);
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

                // Update unified forwarding status indicator
                updateForwardStatusIndicator(smsMessage);
                
            } else {
                verificationCodesLayout.setVisibility(View.GONE);
            }
        }
        
        /**
         * Update unified forwarding status indicator based on SMS message forward status
         */
        private void updateForwardStatusIndicator(SmsMessage smsMessage) {
            ForwardStatus status = smsMessage.getForwardStatus();

            if (status == null || status == ForwardStatus.DISABLED) {
                forwardStatusIndicator.setVisibility(View.GONE);
                return;
            }

            forwardStatusIndicator.setVisibility(View.VISIBLE);

            switch (status) {
                case NOT_SENT:
                    forwardStatusIndicator.setText(context.getString(R.string.forward_status_not_sent));
                    forwardStatusIndicator.setTextColor(context.getResources().getColor(android.R.color.white));
                    forwardStatusIndicator.setBackgroundColor(context.getResources().getColor(android.R.color.darker_gray));
                    break;
                case SENDING:
                    forwardStatusIndicator.setText(context.getString(R.string.forward_status_sending));
                    forwardStatusIndicator.setTextColor(context.getResources().getColor(android.R.color.white));
                    forwardStatusIndicator.setBackgroundColor(context.getResources().getColor(android.R.color.holo_orange_dark));
                    break;
                case SUCCESS:
                    forwardStatusIndicator.setText(context.getString(R.string.forward_status_success));
                    forwardStatusIndicator.setTextColor(context.getResources().getColor(android.R.color.white));
                    forwardStatusIndicator.setBackgroundColor(context.getResources().getColor(android.R.color.holo_green_dark));
                    break;
                case FAILED:
                    forwardStatusIndicator.setText(context.getString(R.string.forward_status_failed));
                    forwardStatusIndicator.setTextColor(context.getResources().getColor(android.R.color.white));
                    forwardStatusIndicator.setBackgroundColor(context.getResources().getColor(android.R.color.holo_red_dark));
                    break;
                default:
                    forwardStatusIndicator.setVisibility(View.GONE);
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
