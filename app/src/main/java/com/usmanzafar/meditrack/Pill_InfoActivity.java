package com.usmanzafar.meditrack;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class Pill_InfoActivity extends AppCompatActivity {

    private static final String BASE_URL = "https://api.fda.gov/";
    private Button searchButton;
    private EditText medNameInput;
    private LinearLayout medResultContainer;

    public interface ApiService {
        @GET("drug/label.json")
        Call<ApiResponse> searchMedicine(@Query("search") String medicine);
    }

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pill_info);

        searchButton = findViewById(R.id.fetch_med);
        medNameInput = findViewById(R.id.medname);
        medResultContainer = findViewById(R.id.med_result_container);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String medicine = medNameInput.getText().toString().trim();
                if (medicine.isEmpty()) {
                    Toast.makeText(Pill_InfoActivity.this, "Please enter a medicine name", Toast.LENGTH_SHORT).show();
                    return;
                }

                apiService.searchMedicine(medicine).enqueue(new Callback<ApiResponse>() {
                    @Override
                    public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                        medResultContainer.removeAllViews(); // Clear previous results

                        if (response.isSuccessful() && response.body() != null) {
                            List<DrugDetails> drugDetails = response.body().getResults();

                            if (drugDetails != null && !drugDetails.isEmpty()) {
                                DrugDetails drug = drugDetails.get(0);

                                addSection("🧪 Active Ingredient", drug.getActiveIngredient(), medResultContainer);
                                addSection("🎯 Purpose", drug.getPurpose(), medResultContainer);
                                addSection("📋 Indications and Usage", drug.getIndications_and_usage(), medResultContainer);
                                addSection("⚠ Warnings", drug.getWarnings(), medResultContainer);

                                if (medResultContainer.getChildCount() == 0) {
                                    addTextView("No detailed information available.");
                                }
                            } else {
                                Toast.makeText(Pill_InfoActivity.this, "No Data Found", Toast.LENGTH_SHORT).show();
                                addTextView("No Data Found");
                            }
                        } else {
                            Toast.makeText(Pill_InfoActivity.this, "API Error or No Results", Toast.LENGTH_SHORT).show();
                            addTextView("API Error or No Results");
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse> call, Throwable t) {
                        Toast.makeText(Pill_InfoActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e("API_ERROR", "Failure: ", t);
                        medResultContainer.removeAllViews();
                        addTextView("Network Error: " + t.getMessage());
                    }
                });
            }
        });
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Intent intent = null;
            if (id == R.id.nav_home) {
                intent = new Intent(Pill_InfoActivity.this, MainActivity.class);
            }
            else if (id == R.id.nav_calendar) {
                intent = new Intent(Pill_InfoActivity.this, CalendarActivity.class);
            }else if (id == R.id.nav_profile) {
                intent = new Intent(Pill_InfoActivity.this, UserProfileActivity.class);
            }
            if (intent != null) {
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); // <--- smoother transition
                startActivity(intent);
                return true;
            }

            return false;
        });

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.black));
    }

    // --- API Response Model Classes ---
    public static class ApiResponse {
        @SerializedName("results")
        private List<DrugDetails> results;

        public List<DrugDetails> getResults() {
            return results;
        }
    }

    public static class DrugDetails {
        @SerializedName("active_ingredient")
        private List<String> activeIngredient;

        @SerializedName("purpose")
        private List<String> purpose;

        @SerializedName("indications_and_usage")
        private List<String> indicationsAndUsage;

        @SerializedName("warnings")
        private List<String> warnings;

        public List<String> getActiveIngredient() {
            return activeIngredient != null ? activeIngredient : new ArrayList<>();
        }

        public List<String> getPurpose() {
            return purpose != null ? purpose : new ArrayList<>();
        }

        public List<String> getIndications_and_usage() {
            return indicationsAndUsage != null ? indicationsAndUsage : new ArrayList<>();
        }

        public List<String> getWarnings() {
            return warnings != null ? warnings : new ArrayList<>();
        }
    }

    // --- UI Helpers ---
    private void addSection(String title, List<String> items, LinearLayout container) {
        if (items == null || items.isEmpty()) return;

        TextView sectionTitle = new TextView(this);
        sectionTitle.setText(title);
        sectionTitle.setTextSize(18);
        sectionTitle.setTextColor(ContextCompat.getColor(this, R.color.primary_dark));
        sectionTitle.setPadding(0, 16, 0, 8);
        sectionTitle.setTypeface(null, Typeface.BOLD);
        container.addView(sectionTitle);

        for (String item : items) {
            TextView bulletItem = new TextView(this);
            bulletItem.setText("• " + item.trim());
            bulletItem.setTextSize(16);
            bulletItem.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            bulletItem.setPadding(8, 4, 8, 4);
            container.addView(bulletItem);
        }
    }

    private void addTextView(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(16);
        textView.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        textView.setPadding(8, 16, 8, 16);
        medResultContainer.addView(textView);
    }
}