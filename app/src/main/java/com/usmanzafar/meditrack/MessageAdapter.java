package com.usmanzafar.meditrack;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Chatgpt.Message> messageList;

    // Constructor to accept the message list
    public MessageAdapter(List<Chatgpt.Message> messageList) {
        this.messageList = messageList;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for each message item
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        // Get the current message
        Chatgpt.Message message = messageList.get(position);

        // Check who sent the message and display accordingly
        if (message.getSentBy().equals(Chatgpt.Message.SENT_BY_ME)) {

            holder.leftChatView.setVisibility(View.GONE); // Hide left view for "me"
            holder.rightChatView.setVisibility(View.VISIBLE); // Show right view for "me"
            holder.rightTextView.setText(message.getMessage());


        } else {
            holder.leftChatView.setVisibility(View.VISIBLE); // Show left view for "bot"
            holder.rightChatView.setVisibility(View.GONE); // Hide right view for "bot"
            holder.leftTextView.setText(message.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size(); // Return the size of the message list
    }

    // ViewHolder to hold the views for each message
    public static class MessageViewHolder extends RecyclerView.ViewHolder {

        LinearLayout leftChatView, rightChatView;
        TextView leftTextView, rightTextView;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize the views
            leftChatView = itemView.findViewById(R.id.left_chat_view);
            rightChatView = itemView.findViewById(R.id.right_chat_view);
            leftTextView = itemView.findViewById(R.id.left_chat_text_view);
            rightTextView = itemView.findViewById(R.id.right_chat_text_view);
        }
    }
}
