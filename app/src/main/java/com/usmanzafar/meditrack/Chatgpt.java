package com.usmanzafar.meditrack;
import com.usmanzafar.meditrack.BuildConfig;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Chatgpt extends AppCompatActivity {

    public static class Message {
        public static final String SENT_BY_ME = "me";
        public static final String SENT_BY_BOT = "bot";

        private String message;
        private String sentBy;

        public Message(String message, String sentBy) {
            this.message = message;
            this.sentBy = sentBy;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getSentBy() {
            return sentBy;
        }

        public void setSentBy(String sentBy) {
            this.sentBy = sentBy;
        }
    }

    RecyclerView recyclerView;
    EditText messageditText;
    ImageButton send;
    public List<Message> messageList;
    MessageAdapter messageAdapter;
    TextView welcomeText;
    final String OPEN_AI_API_KEY1="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";
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");


    final String OPEN_AI_API_KEY=BuildConfig.OPENAI_API_KEY;


    OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatgpt);

        messageditText = findViewById(R.id.message_edit_text);
        recyclerView = findViewById(R.id.recycler_view);
        send = findViewById(R.id.send_button);
        welcomeText = findViewById(R.id.welcome_text);

        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setAdapter(messageAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        recyclerView.setLayoutManager(llm);

        send.setOnClickListener(v -> {
            String question = messageditText.getText().toString().trim();

            if (!question.isEmpty()) {
                addToChat(question, Message.SENT_BY_ME);
                messageAdapter.notifyItemInserted(messageList.size() - 1);
                Toast.makeText(this, "Message sent: " + question, Toast.LENGTH_LONG).show();
                messageditText.setText("");
                callAPI(question);
                welcomeText.setVisibility(View.GONE);
            } else {
                Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addToChat(String message, String sentBy) {
        runOnUiThread(() -> {
            messageList.add(new Message(message, sentBy));
            messageAdapter.notifyDataSetChanged();
            recyclerView.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
        });
    }

    void addResponse(String response) {
        addToChat(response, Message.SENT_BY_BOT);
    }

    private void callAPI(String question) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model", "gpt-3.5-turbo");

            JSONArray messages = new JSONArray();

            // SYSTEM MESSAGE — Add a system instruction to control GPT behavior
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", "Reply shortly, directly, and begin the conversation with something like this is your personal Doctor MediTrack and remember you are here to medically assist the paitent in this MedicalApp so keep your responses like a Doctor.");
            messages.put(systemMessage);

            // USER MESSAGE
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", question);
            messages.put(userMessage);

            jsonBody.put("messages", messages);

            jsonBody.put("max_tokens", 4000);
            jsonBody.put("temperature", 0.7);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer "+ OPEN_AI_API_KEY)
                .post(body )
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                addResponse("Failed to load due to " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        JSONArray jsonArray = jsonObject.getJSONArray("choices");
                        String result = jsonArray.getJSONObject(0).getJSONObject("message").getString("content");
                        addResponse(result.trim());
                    } catch (JSONException e) {
                        e.printStackTrace();
                        addResponse("Failed to parse response");
                    }
                } else {
                    addResponse("Failed to load due to " + response.body().string());
                }
            }
        });
    }

}
