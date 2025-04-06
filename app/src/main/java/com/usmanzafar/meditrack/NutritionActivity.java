package com.usmanzafar.meditrack;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.card.MaterialCardView;

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public class NutritionActivity extends AppCompatActivity {

    private EditText foodInput, foodWeight;
    private TextView nutritionResultText;
    private Button fetchNutritionButton;
    private MaterialCardView nutritionCard;
    private FusedLocationProviderClient fusedLocationClient;

    private static final String BASE_URL = "https://trackapi.nutritionix.com/v2/";
    private static final String APP_ID = "720d4a3a";
    private static final String API_KEY = "099aa6064e5bd3b0e9ae127e38504f25";

    private Retrofit retrofit;
    private NutritionApi nutritionApi;

    public interface NutritionApi {
        @Headers({
                "Content-Type: application/json",
                "x-app-id: " + APP_ID,
                "x-app-key: " + API_KEY
        })
        @POST("natural/nutrients")
        Call<NutritionResponse> getNutrition(@Body NutritionRequest request);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nutrition); // Fixed layout reference

        // Initialize UI components
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        foodInput = findViewById(R.id.food_input);
        foodWeight = findViewById(R.id.food_weight);
        fetchNutritionButton = findViewById(R.id.fetch_nutrition);
        nutritionResultText = findViewById(R.id.nutrition_result);
        nutritionCard = findViewById(R.id.nutrition_card);

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initializing Retrofit
        try {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            nutritionApi = retrofit.create(NutritionApi.class);
        } catch (Exception e) {
            Toast.makeText(this, "Initializing Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        // Set click listener for the button
        fetchNutritionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String foodItem = foodInput.getText().toString().trim();
                String weightText = foodWeight.getText().toString().trim();

                if (foodItem.isEmpty()) {
                    Toast.makeText(NutritionActivity.this, "Please enter a food item", Toast.LENGTH_LONG).show();
                    return;
                }

                double foodWeightValue = 100.0; // Default weight if empty
                if (!weightText.isEmpty()) {
                    try {
                        foodWeightValue = Double.parseDouble(weightText);
                    } catch (NumberFormatException e) {
                        Toast.makeText(NutritionActivity.this, "Invalid weight input, using default 100g", Toast.LENGTH_LONG).show();
                    }
                }

                fetchNutritionData(foodItem, foodWeightValue);
            }
        });

        // Handle toolbar back button
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // Initialize location services
        getCurrentLocation();
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.black));
    }

    private void fetchNutritionData(String foodItem, double foodWeight) {
        // Add weight to the query if provided
        String queryWithWeight = foodItem;
        if (foodWeight > 0) {
            queryWithWeight = foodWeight + "g " + foodItem;
        }

        NutritionRequest request = new NutritionRequest(queryWithWeight);

        nutritionApi.getNutrition(request).enqueue(new Callback<NutritionResponse>() {
            @Override
            public void onResponse(Call<NutritionResponse> call, Response<NutritionResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    NutritionResponse nutritionResponse = response.body();
                    List<NutritionResponse.FoodItem> foods = nutritionResponse.getFoods();

                    if (foods != null && !foods.isEmpty()) {
                        StringBuilder result = new StringBuilder();

                        for (NutritionResponse.FoodItem food : foods) {
                            result.append("🍽 Food: ").append(food.getFoodName()).append("\n\n")
                                    .append("🔥 Calories: ").append(food.getCalories()).append(" kcal\n")
                                    .append("💪 Protein: ").append(food.getProtein()).append(" g\n")
                                    .append("🌾 Carbohydrates: ").append(food.getCarbohydrates()).append(" g\n")
                                    .append("🍭 Sugars: ").append(food.getSugars()).append(" g\n\n");
                        }

                        // Make card visible and set text
                        nutritionCard.setVisibility(View.VISIBLE);
                        nutritionResultText.setText(result.toString());
                    } else {
                        nutritionCard.setVisibility(View.VISIBLE);
                        nutritionResultText.setText("⚠ No nutrition data found for this food item.");
                    }
                } else {
                    nutritionCard.setVisibility(View.VISIBLE);
                    String errorMessage = "⚠ Failed to fetch nutrition data. ";
                    if (response.errorBody() != null) {
                        errorMessage += "Status code: " + response.code();
                    }
                    nutritionResultText.setText(errorMessage);
                    Log.e("NutritionAPI", "Error response: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<NutritionResponse> call, Throwable t) {
                nutritionCard.setVisibility(View.VISIBLE);
                nutritionResultText.setText("Error: " + t.getMessage());
                Log.e("NutritionAPI", "Error fetching nutrition data", t);
            }
        });
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                // Store these values for potential future use
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                Log.d("Location", "Lat: " + latitude + ", Long: " + longitude);
            } else {
                Log.d("Location", "Unable to get location");
            }
        });
    }

    public static class NutritionRequest {
        private String query;

        public NutritionRequest(String query) {
            this.query = query;
        }

        public String getQuery() {
            return query;
        }
    }

    public static class NutritionResponse {
        private List<FoodItem> foods;

        public List<FoodItem> getFoods() {
            return foods;
        }

        public static class FoodItem {
            private String food_name;
            private double nf_calories;
            private double nf_protein;
            private double nf_total_carbohydrate;
            private double nf_sugars;

            public String getFoodName() {
                return food_name;
            }

            public double getCalories() {
                return nf_calories;
            }

            public double getProtein() {
                return nf_protein;
            }

            public double getCarbohydrates() {
                return nf_total_carbohydrate;
            }

            public double getSugars() {
                return nf_sugars;
            }
        }
    }
}