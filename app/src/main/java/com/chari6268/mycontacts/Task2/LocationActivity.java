package com.chari6268.mycontacts.Task2;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.chari6268.mycontacts.R;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LocationActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int REQUEST_CHECK_SETTINGS = 1002;
    private static final long UPDATE_INTERVAL = 10000; // 10 seconds
    private static final long FASTEST_INTERVAL = 5000; // 5 seconds

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private DatabaseReference databaseReference;
    private Button locateButton, saveButton;
    private TextView locationTextView;
    private Location currentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);
        initialization();
        saveButton.setEnabled(false);
        locateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkLocationSettingsAndProceed();
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveLocationToFirebase();
            }
        });
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Toast.makeText(LocationActivity.this, "Could not get location", Toast.LENGTH_SHORT).show();
                    return;
                }
                Location location = locationResult.getLastLocation();
                currentLocation = location;
                String locationText = "Latitude: " + location.getLatitude() +
                        "\nLongitude: " + location.getLongitude();
                locationTextView.setText(locationText);
                saveButton.setEnabled(true);
                fusedLocationClient.removeLocationUpdates(locationCallback);
            }
        };
    }

    private void initialization() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference("locations");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationTextView = findViewById(R.id.location_text);
        locateButton = findViewById(R.id.locate_button);
        saveButton = findViewById(R.id.save_button);
    }

    private void checkLocationSettingsAndProceed() {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        client.checkLocationSettings(builder.build())
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        requestLocationPermissionAndGetLocation();
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                try {
                                    ResolvableApiException resolvable = (ResolvableApiException) e;
                                    resolvable.startResolutionForResult(LocationActivity.this, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sendEx) {
                                    Toast.makeText(LocationActivity.this, "Error opening location settings dialog", Toast.LENGTH_SHORT).show();
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                Toast.makeText(LocationActivity.this, "Unable to turn on location. Please enable location in settings.", Toast.LENGTH_LONG).show();
                                break;
                        }
                    }
                });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                requestLocationPermissionAndGetLocation();
            } else {
                Toast.makeText(this, "Location services must be enabled to use this feature", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void requestLocationPermissionAndGetLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getLocation();
        }
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Toast.makeText(this, "Getting location...", Toast.LENGTH_SHORT).show();

        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private void saveLocationToFirebase() {
        if (currentLocation == null) {
            Toast.makeText(this, "No location available to save", Toast.LENGTH_SHORT).show();
            return;
        }

        String locationId = databaseReference.push().getKey();

        Map<String, Object> locationData = new HashMap<>();
        locationData.put("latitude", currentLocation.getLatitude());
        locationData.put("longitude", currentLocation.getLongitude());
        locationData.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
        locationData.put("accuracy", currentLocation.getAccuracy());

        databaseReference.child(locationId).setValue(locationData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(LocationActivity.this, "Location saved successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(LocationActivity.this, "Failed to save location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}