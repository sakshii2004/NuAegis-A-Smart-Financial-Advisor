package com.finbot.nuaegis;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.util.Arrays;
import java.util.List;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class SummaryActivity extends AppCompatActivity {

    private String previousCompany = "";
    private String previousYear = "";
    private String previousQuarter = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);

        // Company Drop Down
        MaterialAutoCompleteTextView companyDropDownList = findViewById(R.id.companyDropDown);
        List<String> companies = Arrays.asList("AMD", "AXP", "Accenture", "Adobe", "Airbus", "Allstate", "Alphabet", "Amadeus", "Amazon", "Apple", "BAM", "BBVA", "BKNG", "BMW", "BP", "Bank of America", "Branco", "Capgemini", "Cardinal Health", "Cisco", "Citi", "Compass Group", "Costco", "Deutsche Bank", "EDF", "Elevance Health", "Engie", "Ford", "GM", "IBM", "JPM", "Loreal", "Louis Vuitton", "Lululemon", "META", "Marriott", "Mastercard", "Microsoft", "Nike", "Nvidia", "Oracle", "PAYPAL", "SIE", "SalesForce", "Schneider Electric", "Shell", "UnitedHealth", "Volvo", "Walmart", "Walt Disney");
        ArrayAdapter<String> adapter3 = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, companies);
        companyDropDownList.setAdapter(adapter3);
        companyDropDownList.setThreshold(0);
        companyDropDownList.setOnClickListener(v -> companyDropDownList.showDropDown());

        // Year Drop Down
        MaterialAutoCompleteTextView yearDropDownList = findViewById(R.id.yearDropDown);
        List<String> years = Arrays.asList("2018", "2019", "2020", "2021", "2022", "2023", "2024");
        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, years);
        yearDropDownList.setAdapter(adapter2);
        yearDropDownList.setThreshold(0);
        yearDropDownList.setOnClickListener(v -> yearDropDownList.showDropDown());

        // Quarter Drop Down
        MaterialAutoCompleteTextView quarterDropDownField = findViewById(R.id.quaterDropDown);
        List<String> quarters = Arrays.asList("Q1", "Q2", "Q3", "Q4");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, quarters);
        quarterDropDownField.setAdapter(adapter);
        quarterDropDownField.setThreshold(0);
        quarterDropDownField.setOnClickListener(v -> quarterDropDownField.showDropDown());
    }

    public void getSummary(View view) {
        MaterialAutoCompleteTextView companyDropDownList = findViewById(R.id.companyDropDown);
        MaterialAutoCompleteTextView yearDropDownList = findViewById(R.id.yearDropDown);
        MaterialAutoCompleteTextView quarterDropDownField = findViewById(R.id.quaterDropDown);
        TextView tempTextBox = findViewById(R.id.tempTextBox);

        String selectedCompany = companyDropDownList.getText().toString();
        String selectedYear = yearDropDownList.getText().toString();
        String selectedQuarter = quarterDropDownField.getText().toString();

        if (selectedCompany.isEmpty() || selectedYear.isEmpty() || selectedQuarter.isEmpty()) {
            Toast.makeText(this, "Please fill all the fields.", Toast.LENGTH_SHORT).show();
        }

        else {
            String yearQuarter = selectedYear + "_" + selectedQuarter;
            sendRequestToAPI(selectedCompany, yearQuarter);
        }
    }

    public void getSentimentSummary(View view) {
        MaterialAutoCompleteTextView companyDropDownList = findViewById(R.id.companyDropDown);
        MaterialAutoCompleteTextView yearDropDownList = findViewById(R.id.yearDropDown);
        MaterialAutoCompleteTextView quarterDropDownField = findViewById(R.id.quaterDropDown);
        TextView tempTextBox = findViewById(R.id.tempTextBox);

        String selectedCompany = companyDropDownList.getText().toString();
        String selectedYear = yearDropDownList.getText().toString();
        String selectedQuarter = quarterDropDownField.getText().toString();

        if (selectedCompany.isEmpty() || selectedYear.isEmpty() || selectedQuarter.isEmpty()) {
            Toast.makeText(this, "Please fill all the fields.", Toast.LENGTH_SHORT).show();
        }

        else {
            String yearQuarter = selectedYear + "_" + selectedQuarter;
            sendSummaryRequestToAPI(selectedCompany, yearQuarter);
        }
    }

    public void sendSummaryRequestToAPI(String company, String yearQuarter) {
        // Define URLs for positive and negative summary endpoints
        String urlPos = "https://fba4-223-233-83-191.ngrok-free.app/summary_positive?company=" + company + "&year_quarter=" + yearQuarter;
        String urlNeg = "https://fba4-223-233-83-191.ngrok-free.app/summary_negative?company=" + company + "&year_quarter=" + yearQuarter;

        // Start a new thread for network operations
        new Thread(() -> {
            // Send GET requests to both URLs
            String responsePos = sendGetRequest(urlPos);
            String responseNeg = sendGetRequest(urlNeg);

            // Run on UI thread to update the UI components
            runOnUiThread(() -> {
                TextView responseText = findViewById(R.id.tempTextBox);
                TextView posSum = findViewById(R.id.posSumText);
                TextView negSum = findViewById(R.id.negSumText);
                TextView summaryTitleText = findViewById(R.id.summaryTitle);

                // Determine the appropriate message based on responses
                if (responsePos.equals("[]") && responseNeg.equals("[]")) {
                    summaryTitleText.setText("Sentiment Summary for " + company + " " + yearQuarter.substring(0, 4) + " " + yearQuarter.substring(5));
                    responseText.setText("No sentiment summaries found.");
                } else {
                    StringBuilder responseFinal = new StringBuilder();

                    if (responsePos != null && !responsePos.equals("[]")) {
                        String responsePosText = responsePos.substring(2, responsePos.length() - 2);
                        posSum.setText("Positive Highlights: " + responsePosText.toString());
                        posSum.setBackgroundColor(Color.parseColor("#e5ffe2"));
                        //responseFinal.append("Positive summary: ").append(responsePosText).append("\n\n");
                    } else {
                        posSum.setText("No positive highlights found");
                        posSum.setBackgroundColor(Color.parseColor("#e5ffe2"));
                        //responseFinal.append("No positive sentiment summaries found.\n\n");
                    }

                    if (responseNeg != null && !responseNeg.equals("[]")) {
                        String responseNegText = responseNeg.substring(2, responseNeg.length() - 2);
                        negSum.setText("Negative Highlights: " + responseNegText.toString());
                        negSum.setBackgroundColor(Color.parseColor("#ffe2e2"));
                        //responseFinal.append("Negative summary: ").append(responseNegText);
                    } else {
                        negSum.setText("No negative highlights found");
                        negSum.setBackgroundColor(Color.parseColor("#ffe2e2"));
                        //responseFinal.append("No negative sentiment summaries found.");
                    }

                    summaryTitleText.setText("Sentiment Summary for " + company + " " + yearQuarter.substring(0, 4) + " " + yearQuarter.substring(5));
                    responseText.setText(responseFinal.toString());
                }
            });
        }).start();
    }


    /*public void sendRequestToAPI(String company, String yearQuarter) {
        // Create the URL with query parameters for company and year_quarter
        String url = "https://fba4-223-233-83-191.ngrok-free.app/summary?company=" + company + "&year_quarter=" + yearQuarter;

        // Start a new thread for network operations
        new Thread(() -> {
            String response = sendGetRequest(url);
            runOnUiThread(() -> {
                // Display the response in a TextView or Toast
                TextView responseText = findViewById(R.id.tempTextBox);
                TextView summaryTitleText = findViewById(R.id.summaryTitle);
                if (response != null) {
                    String responseCleaned = response.replace("\\\"", "");
                    summaryTitleText.setText("Summary for " + company + " " + yearQuarter.substring(0, 4) + " " + yearQuarter.substring(5));
                    responseText.setText(responseCleaned);
                } else {
                    Toast.makeText(this, "Failed to connect to API", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }*/

    public void sendRequestToAPI(String company, String yearQuarter) {
        // Create the URL with query parameters for company and year_quarter
        String url = "https://fba4-223-233-83-191.ngrok-free.app/summary?company=" + company + "&year_quarter=" + yearQuarter;

        // Start a new thread for network operations
        new Thread(() -> {
            String response = sendGetRequest(url);
            runOnUiThread(() -> {
                if (response != null) {
                    String responseCleaned = response.replace("\\\"", "");

                    // Start new activity and pass summary data
                    Intent intent = new Intent(this, SummaryDisplayActivity.class);
                    intent.putExtra("summaryText", responseCleaned);
                    intent.putExtra("summaryTitle", "Summary for " + company + " " + yearQuarter.substring(0, 4) + " " + yearQuarter.substring(5));
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Failed to connect to API", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }


    private String sendGetRequest(String urlString) {
        StringBuilder result = new StringBuilder();
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            rd.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return result.toString();
    }
}

//we will get highest marks in wmad