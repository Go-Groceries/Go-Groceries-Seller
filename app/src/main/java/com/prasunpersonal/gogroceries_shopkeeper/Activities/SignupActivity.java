package com.prasunpersonal.gogroceries_shopkeeper.Activities;

import static com.prasunpersonal.gogroceries_shopkeeper.App.MY_SHOP;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.prasunpersonal.gogroceries_shopkeeper.Models.ModelShop;
import com.prasunpersonal.gogroceries_shopkeeper.databinding.ActivitySignupBinding;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class SignupActivity extends AppCompatActivity {
    ActivitySignupBinding binding;
    double latitude = 0, longitude = 0;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.signupToolbar.setNavigationOnClickListener(v -> finish());

        progressDialog = new ProgressDialog(this);
        progressDialog.setCanceledOnTouchOutside(false);

        binding.sSignup.setOnClickListener(v -> {
            if (binding.sName.getText().toString().trim().isEmpty()) {
                binding.sName.setError("Name is required!");
                binding.sName.requestFocus();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(binding.sEmail.getText().toString().trim()).matches()) {
                binding.sEmail.setError("Enter a valid Email address!");
                binding.sEmail.requestFocus();
                return;
            }
            if (!Patterns.PHONE.matcher(binding.sPhone.getText().toString().trim()).matches()) {
                binding.sPhone.setError("Enter a valid phone number!");
                binding.sPhone.requestFocus();
                return;
            }
            if (binding.sPass1.getText().toString().trim().length() < 6) {
                binding.sPass1.setError("Enter a valid password of 6 digits!");
                binding.sPass1.requestFocus();
                return;
            }
            if (!binding.sPass2.getText().toString().equals(binding.sPass1.getText().toString())) {
                binding.sPass2.setError("Passwords doesn't match!");
                binding.sPass2.requestFocus();
                return;
            }
            if (binding.shopAddress.getText().toString().trim().isEmpty()) {
                binding.shopAddress.setError("Shop address is required!");
                binding.shopAddress.requestFocus();
                return;
            }
            if (binding.sCity.getText().toString().trim().isEmpty()) {
                binding.sCity.setError("Town/City is required!");
                binding.sCity.requestFocus();
                return;
            }
            if (binding.sState.getText().toString().trim().isEmpty()) {
                binding.sState.setError("State is required!");
                binding.sState.requestFocus();
                return;
            }
            if (binding.sCountry.getText().toString().trim().isEmpty()) {
                binding.sCountry.setError("Country is required!");
                binding.sCountry.requestFocus();
                return;
            }
            if (binding.sPincode.getText().toString().trim().isEmpty()) {
                binding.sPincode.setError("Pincode is required!");
                binding.sPincode.requestFocus();
                return;
            }

            Geocoder geocoder;
            List<Address> addresses;
            geocoder = new Geocoder(this, Locale.getDefault());
            try {
                addresses = geocoder.getFromLocationName(String.format(Locale.getDefault(), "%s, %s, %s, %s, %s", binding.shopAddress.getText().toString().trim(), binding.sCity.getText().toString().trim(), binding.sState.getText().toString().trim(), binding.sCountry.getText().toString().trim(), binding.sPincode.getText().toString().trim()), 1);
                latitude = addresses.get(0).getLatitude();
                longitude = addresses.get(0).getLongitude();

                binding.sPincode.setText(addresses.get(0).getPostalCode());
                binding.sCountry.setText(addresses.get(0).getCountryName());
                binding.sState.setText(addresses.get(0).getAdminArea());
                binding.sCity.setText(addresses.get(0).getLocality());
                binding.shopAddress.setText(addresses.get(0).getAddressLine(0));
            } catch (Exception e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            if (latitude == 0.0 || longitude == 0.0) {
                Toast.makeText(this, "Address can't be found!", Toast.LENGTH_SHORT).show();
                return;
            }

            progressDialog.setMessage("Creating Account...");
            progressDialog.show();

            FirebaseAuth.getInstance().createUserWithEmailAndPassword(binding.sEmail.getText().toString().trim(), binding.sPass1.getText().toString().trim()).addOnSuccessListener(authResult -> {
                progressDialog.setMessage("Saving Account Info ...");

                MY_SHOP = new ModelShop(FirebaseAuth.getInstance().getUid(), binding.sShopName.getText().toString().trim(), binding.sName.getText().toString().trim(), binding.sPhone.getText().toString(), binding.sEmail.getText().toString().trim(), Double.parseDouble(binding.sdeliveryCharge.getText().toString().trim()), binding.sCountry.getText().toString().trim(), binding.sState.getText().toString().trim(), binding.sCity.getText().toString().trim(), binding.shopAddress.getText().toString().trim(), binding.sPincode.getText().toString().trim(), latitude, longitude, null, binding.sOpenTime.getText().toString().trim(), binding.sCloseTime.getText().toString().trim());
                FirebaseFirestore.getInstance().collection("Shops").document(MY_SHOP.getUid()).set(MY_SHOP).addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        startActivity(new Intent(this, MainSellerActivity.class));
                        progressDialog.dismiss();
                        finishAffinity();
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(this, Objects.requireNonNull(task1.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });

        binding.gpsBtn.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Detecting location. Please wait.", Toast.LENGTH_SHORT).show();
                ((LocationManager) this.getSystemService(Context.LOCATION_SERVICE)).requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, location -> {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    Geocoder geocoder;
                    List<Address> addresses;
                    geocoder = new Geocoder(this, Locale.getDefault());
                    try {
                        addresses = geocoder.getFromLocation(latitude, longitude, 1);
                        binding.sPincode.setText(addresses.get(0).getPostalCode());
                        binding.sCountry.setText(addresses.get(0).getCountryName());
                        binding.sState.setText(addresses.get(0).getAdminArea());
                        binding.sCity.setText(addresses.get(0).getLocality());
                        binding.shopAddress.setText(addresses.get(0).getAddressLine(0));
                    } catch (Exception e) {
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        binding.sOpenTime.setOnClickListener(v -> {
            int h=0,m=0;
            if (!binding.sOpenTime.getText().toString().isEmpty()) {
                try {
                    String str = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(Objects.requireNonNull(new SimpleDateFormat("hh:mm aa", Locale.getDefault()).parse(binding.sOpenTime.getText().toString())));
                    String[] arr = str.split(":");
                    h = Integer.parseInt(arr[0]);
                    m = Integer.parseInt(arr[1]);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            TimePickerDialog dialog = new TimePickerDialog(this, android.R.style.Theme_Holo_Light_Dialog_MinWidth, (view, hourOfDay, minute) -> {
                try {
                    binding.sOpenTime.setText(new SimpleDateFormat("hh:mm aa", Locale.getDefault()).format(Objects.requireNonNull(new SimpleDateFormat("HH:mm", Locale.getDefault()).parse(hourOfDay + ":" + minute))));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }, h, m, false);

            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.show();
        });

        binding.sCloseTime.setOnClickListener(v -> {
            int h=0,m=0;
            if (!binding.sOpenTime.getText().toString().isEmpty()) {
                try {
                    String str = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(Objects.requireNonNull(new SimpleDateFormat("hh:mm aa", Locale.getDefault()).parse(binding.sCloseTime.getText().toString())));
                    String[] arr = str.split(":");
                    h = Integer.parseInt(arr[0]);
                    m = Integer.parseInt(arr[1]);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            TimePickerDialog dialog = new TimePickerDialog(this, android.R.style.Theme_Holo_Light_Dialog_MinWidth, (view, hourOfDay, minute) -> {
                try {
                    binding.sCloseTime.setText(new SimpleDateFormat("hh:mm aa", Locale.getDefault()).format(Objects.requireNonNull(new SimpleDateFormat("HH:mm", Locale.getDefault()).parse(hourOfDay + ":" + minute))));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }, h, m, false);

            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.show();
        });
    }
}