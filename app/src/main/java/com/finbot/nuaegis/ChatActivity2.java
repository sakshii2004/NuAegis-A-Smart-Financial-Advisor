package com.finbot.nuaegis;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ChatActivity2 extends AppCompatActivity {

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    private TextInputEditText chatPromptInput;
    private Timer typingTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat2); // Set the correct layout file

        // Initialize RecyclerView
        chatRecyclerView = findViewById(R.id.chatRecyclerView); // Ensure this matches the ID in activity_chat2.xml

        // Initialize TextInputEditText
        chatPromptInput = findViewById(R.id.chatPrompt); // Make sure this ID matches the one in activity_chat2.xml

        // Set up layout manager and adapter
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize message list and adapter
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);
        chatRecyclerView.setAdapter(chatAdapter);
    }

    public void sendQuery(View view) {
        String query = chatPromptInput.getText().toString();

        if (query.isEmpty()) {
            Toast.makeText(this, "Please enter a query", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add the user's message to the chat and clear input field
        chatMessages.add(new ChatMessage(query, ChatMessage.SENDER_USER));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1); // Scroll to the latest message
        chatPromptInput.setText("");

        // Show typing indicator
        ChatMessage typingIndicator = new ChatMessage("...", ChatMessage.SENDER_BOT);
        chatMessages.add(typingIndicator);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);

        // Start typing animation with Timer
        typingTimer = new Timer();
        typingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    // Cycle between ".", "..", "..."
                    if (typingIndicator.getMessage().equals("...")) {
                        typingIndicator.setMessage(".");
                    } else if (typingIndicator.getMessage().equals(".")) {
                        typingIndicator.setMessage("..");
                    } else {
                        typingIndicator.setMessage("...");
                    }
                    chatAdapter.notifyItemChanged(chatMessages.size() - 1);
                });
            }
        }, 0, 500); // Update every 500ms

        // Send query to API in a new thread to avoid blocking the UI
        new Thread(() -> {
            try {
                // Prepare the URL and connection
                String apiUrl = "https://fba4-223-233-83-191.ngrok-free.app/search"; // Replace with your Flask API URL
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);

                // Create JSON object to send
                JSONObject jsonParam = new JSONObject();
                jsonParam.put("query", query);

                // Write JSON payload
                conn.getOutputStream().write(jsonParam.toString().getBytes("UTF-8"));

                // Read the response
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }

                in.close();
                conn.disconnect();

                // Update the UI on the main thread
                runOnUiThread(() -> {

                    // Cancel the typing animation and remove the typing indicator
                    typingTimer.cancel();
                    chatMessages.remove(typingIndicator);
                    chatAdapter.notifyItemRemoved(chatMessages.size());

                    try {
                        // Parse the JSON response and add it to chat
                        JSONObject responseJson = new JSONObject(response.toString());
                        String reply = responseJson.optString("response", "No reply found");
                        chatMessages.add(new ChatMessage(reply, ChatMessage.SENDER_BOT));
                        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    chatMessages.add(new ChatMessage("Failed to connect to API", ChatMessage.SENDER_BOT));
                    chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                    chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
                });
                e.printStackTrace();
            }
        }).start();
    }
}
