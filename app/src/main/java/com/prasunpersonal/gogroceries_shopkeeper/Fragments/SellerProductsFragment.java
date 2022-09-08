package com.prasunpersonal.gogroceries_shopkeeper.Fragments;

import static com.prasunpersonal.gogroceries_shopkeeper.App.MY_SHOP;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.prasunpersonal.gogroceries_shopkeeper.Adaptrers.AdapterProduct;
import com.prasunpersonal.gogroceries_shopkeeper.Models.ModelProduct;
import com.prasunpersonal.gogroceries_shopkeeper.databinding.FragmentSellerProductsBinding;

import java.util.ArrayList;
import java.util.Objects;

public class SellerProductsFragment extends Fragment {
    Context context;
    FragmentSellerProductsBinding binding;
    FirebaseFirestore db;
    private ArrayList<ModelProduct> productList;

    public SellerProductsFragment() {}


    @SuppressLint("NotifyDataSetChanged")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSellerProductsBinding.inflate(inflater, container, false);
        context = binding.getRoot().getContext();
        productList = new ArrayList<>();
        db = FirebaseFirestore.getInstance();

        binding.searchProducts.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ((AdapterProduct) Objects.requireNonNull(binding.productsRv.getAdapter())).getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        binding.productsRv.setLayoutManager(new LinearLayoutManager(context));
        binding.productsRv.setAdapter(new AdapterProduct(context, productList, (product, position) -> {

        }));

        db.collection("Shops").document(MY_SHOP.getUid()).collection("Products").addSnapshotListener((value, error) -> {
            if (error != null) {
                Toast.makeText(context, error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            if (value != null) {
                productList.clear();
                for (DocumentSnapshot dc : value) {
                    productList.add(dc.toObject(ModelProduct.class));
                }
                ((AdapterProduct) Objects.requireNonNull(binding.productsRv.getAdapter())).notifyChange();
            }
        });
        return binding.getRoot();
    }
}