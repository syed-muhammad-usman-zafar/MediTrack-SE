package com.usmanzafar.meditrack;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

// Define Pharmacy class separately
class Pharmacy {
    @SerializedName("tags")
    private Tags tags;

    static class Tags {
        @SerializedName("name")
        private String name;
        @SerializedName("addr:street")
        private String street;
        @SerializedName("addr:city")
        private String city;
        @SerializedName("dispensing")
        private String dispensing;
    }

    public String getName() {
        return (tags != null && tags.name != null) ? tags.name : "Unknown Pharmacy";
    }

    public String getStreet() {
        return (tags != null && tags.street != null) ? tags.street : "Unknown Street";
    }

    public String getCity() {
        return (tags != null && tags.city != null) ? tags.city : "Unknown City";
    }

    public String getDispensing() {
        return (tags != null && tags.dispensing != null) ? tags.dispensing : "Unknown";
    }
}

// Updated PharmacyResponse class
class PharmacyResponse {
    @SerializedName("elements")
    private List<Pharmacy> pharmaciesList;

    public List<Pharmacy> getPharmaciesList() {
        return pharmaciesList;
    }
}

interface PharmacyApiService {
    @GET("interpreter")
    Call<PharmacyResponse> getPharmacies(@Query("data") String query);
}

public class PharmaciesActivity extends AppCompatActivity {
    private static final String BASE_URL = "https://overpass-api.de/api/";
    private static final String HARDCODED_QUERY = "[out:json];node[\"amenity\"=\"pharmacy\"](around:5000,31.483223207853392,74.3630766306419);out;";

    private LinearLayout pharmacyContainer;
    private Button getFindPharmaciesButton;
    private PharmacyApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pharmacy);

        pharmacyContainer = findViewById(R.id.pharmacyContainer);
        getFindPharmaciesButton = findViewById(R.id.fetch_pharmacies);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(PharmacyApiService.class);

        getFindPharmaciesButton.setOnClickListener(v -> fetchPharmacies());

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.black));
    }

    private void fetchPharmacies() {
        apiService.getPharmacies(HARDCODED_QUERY).enqueue(new Callback<PharmacyResponse>() {
            @Override
            public void onResponse(Call<PharmacyResponse> call, Response<PharmacyResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    displayPharmacies(response.body().getPharmaciesList());
                } else {
                    Toast.makeText(PharmaciesActivity.this, "Failed to get pharmacies", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PharmacyResponse> call, Throwable t) {
                Toast.makeText(PharmaciesActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayPharmacies(List<Pharmacy> pharmacies) {
        pharmacyContainer.removeAllViews();

        for (Pharmacy pharmacy : pharmacies) {
            TextView textView = new TextView(this);
            textView.setText("📍 Name: " + pharmacy.getName() +
                    "\n📌 Street: " + pharmacy.getStreet() +
                    "\n🏙 City: " + pharmacy.getCity() +
                    "\n💊 Dispensing: " + pharmacy.getDispensing() + "\n");

            textView.setPadding(30, 30, 30, 30);
            textView.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            textView.setTextSize(20);
            textView.setTypeface(null, Typeface.BOLD);
            textView.setLineSpacing(1.5f, 1.5f);

            GradientDrawable background = new GradientDrawable();
            background.setColor(ContextCompat.getColor(this, R.color.primary_light));
            background.setCornerRadius(30);

            textView.setBackground(background);
            textView.setElevation(8);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(20, 20, 20, 20);
            textView.setLayoutParams(params);

            pharmacyContainer.addView(textView);
        }
    }
}
