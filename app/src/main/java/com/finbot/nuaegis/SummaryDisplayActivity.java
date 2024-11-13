package com.finbot.nuaegis;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

public class SummaryDisplayActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary_display);

        // Get the data passed from the intent
        String summaryText = getIntent().getStringExtra("summaryText");
        String summaryTitle = getIntent().getStringExtra("summaryTitle");

        // Set the summary text and title
        TextView summaryTextView = findViewById(R.id.summaryTextView);
        TextView summaryTitleTextView = findViewById(R.id.summaryTitleTextView);

        summaryTitleTextView.setText(summaryTitle);
        summaryTextView.setText(summaryText);
    }
}