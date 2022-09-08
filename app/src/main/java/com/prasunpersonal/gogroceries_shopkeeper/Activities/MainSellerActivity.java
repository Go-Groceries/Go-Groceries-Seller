package com.prasunpersonal.gogroceries_shopkeeper.Activities;

import static com.prasunpersonal.gogroceries_shopkeeper.App.MY_SHOP;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.prasunpersonal.gogroceries_shopkeeper.Adaptrers.AdapterFragment;
import com.prasunpersonal.gogroceries_shopkeeper.Fragments.SellerOrdersFragment;
import com.prasunpersonal.gogroceries_shopkeeper.Fragments.SellerProductsFragment;
import com.prasunpersonal.gogroceries_shopkeeper.Models.ModelProduct;
import com.prasunpersonal.gogroceries_shopkeeper.R;
import com.prasunpersonal.gogroceries_shopkeeper.databinding.ActivityMainSellerBinding;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

public class MainSellerActivity extends AppCompatActivity {
    ActivityMainSellerBinding binding;

    ActivityResultLauncher<String> launcher = registerForActivityResult(new ActivityResultContracts.GetContent(), result -> {
        if (result != null) deletePreviousProductsAndUploadNewProducts(result);
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainSellerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setTitle(MY_SHOP.getShopName());
        binding.toolbar.setSubtitle(MY_SHOP.getCity());
        setSupportActionBar(binding.toolbar);


        ArrayList<Fragment> fragments = new ArrayList<>();
        fragments.add(new SellerProductsFragment());
        fragments.add(new SellerOrdersFragment());

        binding.sellerMainViewpager.setAdapter(new AdapterFragment(getSupportFragmentManager(), getLifecycle(), fragments));
        binding.sellerMainViewpager.setOffscreenPageLimit(fragments.size());

        binding.sellerMainTab.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                binding.sellerMainViewpager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        binding.sellerMainViewpager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                binding.sellerMainTab.selectTab(binding.sellerMainTab.getTabAt(position));
            }
        });
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_seller_menu, menu);
        Glide.with(this).load(MY_SHOP.getDp()).placeholder(R.drawable.ic_person).into((ImageView) menu.findItem(R.id.profile).getActionView().findViewById(R.id.profileMenuBtnPhoto));
        menu.findItem(R.id.profile).getActionView().findViewById(R.id.profileMenuBtnPhoto).setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.uploadFile) {
            launcher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        invalidateOptionsMenu();
    }

    private void deletePreviousProductsAndUploadNewProducts(Uri uri) {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setMessage("Please wait...");
        dialog.show();

        FirebaseFirestore.getInstance().collection("Shops").document(MY_SHOP.getUid()).collection("Products").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                dialog.setMessage("Removing all old products.");
                dialog.setProgress(0);
                int count = 0;

                for (QueryDocumentSnapshot doc : task.getResult()) {
                    doc.getReference().delete();
                    dialog.setProgress(count*100/task.getResult().size());
                    count++;
                }

                try {
                    XSSFWorkbook workbook = new XSSFWorkbook(getContentResolver().openInputStream(uri));
                    XSSFSheet sheet = workbook.getSheetAt(0);

                    dialog.setMessage("Adding all new products.");
                    dialog.setProgress(0);

                    for (int i = 1; i <= sheet.getPhysicalNumberOfRows(); i++) {
                        if (sheet.getRow(i) != null && sheet.getRow(i).getCell(0).getRawValue() != null && sheet.getRow(i).getCell(1).getRawValue() != null && sheet.getRow(i).getCell(2).getRawValue() != null && sheet.getRow(i).getCell(3).getRawValue() != null && sheet.getRow(i).getCell(4).getRawValue() != null && sheet.getRow(i).getCell(5).getRawValue() != null && sheet.getRow(i).getCell(6).getRawValue() != null) {
                            String  productId = String.format(Locale.getDefault(), "%s_product_%d", MY_SHOP.getUid(), i);
                            ModelProduct product = new ModelProduct(productId, sheet.getRow(i).getCell(0).getStringCellValue(), Integer.parseInt(sheet.getRow(i).getCell(1).getRawValue()), Double.parseDouble(sheet.getRow(i).getCell(2).getRawValue()), Double.parseDouble(sheet.getRow(i).getCell(3).getRawValue()), Double.parseDouble(sheet.getRow(i).getCell(4).getRawValue()), sheet.getRow(i).getCell(5).getStringCellValue(), sheet.getRow(i).getCell(6).getStringCellValue());
                            FirebaseFirestore.getInstance().collection("Shops").document(MY_SHOP.getUid()).collection("Products").document(productId).set(product);
                        }
                        dialog.setProgress(i*100/sheet.getPhysicalNumberOfRows());
                    }
                    workbook.close();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    dialog.dismiss();
                }
            } else {
                Toast.makeText(this, Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}