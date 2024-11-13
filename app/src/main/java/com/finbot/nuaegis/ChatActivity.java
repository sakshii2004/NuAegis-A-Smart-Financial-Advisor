package com.finbot.nuaegis;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ChatActivity extends AppCompatActivity {

    private TextView chatReplyText;
    private TextInputEditText chatPromptInput;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatReplyText = findViewById(R.id.chatReply);
        chatPromptInput = findViewById(R.id.chatPrompt);
    }

    public void sendQuery(View view) {

        String query = chatPromptInput.getText().toString();

        if (query.isEmpty()) {
            Toast.makeText(this, "Please enter a query", Toast.LENGTH_SHORT).show();
            return;
        }

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
                    try {
                        // Parse the JSON response and set it to chatReplyText
                        JSONObject responseJson = new JSONObject(response.toString());
                        String reply = responseJson.optString("response", "No reply found");

                        chatReplyText.setText(reply);
                    } catch (Exception e) {
                        chatReplyText.setText("Failed to parse response");
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    chatReplyText.setText("Failed to connect to API");
                    e.printStackTrace();
                });
            }
        }).start();
    }
}
