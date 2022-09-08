package com.prasunpersonal.gogroceries_shopkeeper.Activities;

import static com.prasunpersonal.gogroceries_shopkeeper.App.MY_SHOP;

import android.Manifest;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.prasunpersonal.gogroceries_shopkeeper.Models.ModelShop;
import com.prasunpersonal.gogroceries_shopkeeper.R;
import com.prasunpersonal.gogroceries_shopkeeper.databinding.ActivityMainBinding;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> checkAndRequestPermission());
    private AlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        checkAndRequestPermission();
    }

    private void checkAndRequestPermission() {
        Dexter.withContext(this).withPermission(Manifest.permission.ACCESS_FINE_LOCATION).withListener(new PermissionListener() {
            @Override
            public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                getShop();
            }

            @Override
            public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                if (permissionDeniedResponse.isPermanentlyDenied()) {
                    showSettingsDialog();
                } else {
                    checkAndRequestPermission();
                }
            }

            @Override
            public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                permissionToken.continuePermissionRequest();
            }
        }).onSameThread().check();
    }

    private void getShop() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            FirebaseFirestore.getInstance().collection("Shops").document(Objects.requireNonNull(FirebaseAuth.getInstance().getUid())).get().addOnCompleteListener(task1 -> {
                if (task1.isSuccessful()) {
                    MY_SHOP = task1.getResult().toObject(ModelShop.class);
                    assert MY_SHOP != null;
                    startActivity(new Intent(this, MainSellerActivity.class));
                    finishAffinity();
                } else {
                    Toast.makeText(this, Objects.requireNonNull(task1.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            new Handler().postDelayed(() -> {
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finishAffinity();
            }, 2000);
        }
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.ic_location);
        builder.setTitle("Location Permission");
        builder.setMessage("This app needs location permission. You can enable it in app settings.");
        builder.setCancelable(false);
        builder.setPositiveButton("Settings", (dialog, which) -> {
            dialog.cancel();
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            launcher.launch(intent);
        });
        builder.setNegativeButton("Exit", (dialog, which) -> {
            dialog.cancel();
            finish();
        });

        alertDialog = builder.create();
        alertDialog.setOnShowListener(dialog -> alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.LTGRAY));
        alertDialog.show();
    }
}