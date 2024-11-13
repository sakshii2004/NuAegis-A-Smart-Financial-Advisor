package com.finbot.nuaegis;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void openChat(View view) {
        //Toast.makeText(this, "Chat Button Clicked", Toast.LENGTH_SHORT).show();
        // Example of starting a new activity (if you have a ChatActivity):
        Intent intent = new Intent(this, ChatActivity2.class);
        startActivity(intent);
    }

    public void openSummary(View view) {
        //Toast.makeText(this, "Summary Button Clicked", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, SummaryActivity.class);
        startActivity(intent);
    }

}