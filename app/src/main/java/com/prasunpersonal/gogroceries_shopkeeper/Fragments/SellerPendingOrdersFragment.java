package com.prasunpersonal.gogroceries_shopkeeper.Fragments;

import static com.prasunpersonal.gogroceries_shopkeeper.App.MY_SHOP;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.prasunpersonal.gogroceries_shopkeeper.Activities.SellerOrderDetailsActivity;
import com.prasunpersonal.gogroceries_shopkeeper.Adaptrers.AdapterOrder;
import com.prasunpersonal.gogroceries_shopkeeper.Models.ModelOrder;
import com.prasunpersonal.gogroceries_shopkeeper.databinding.FragmentSellerPendingOrdersBinding;

import java.util.ArrayList;
import java.util.Objects;

public class SellerPendingOrdersFragment extends Fragment {
    FragmentSellerPendingOrdersBinding binding;
    ArrayList<ModelOrder> orders;
    Context context;

    public SellerPendingOrdersFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSellerPendingOrdersBinding.inflate(inflater, container, false);
        context = binding.getRoot().getContext();

        orders = new ArrayList<>();
        binding.sellerPendingOrders.setLayoutManager(new LinearLayoutManager(context));
        binding.sellerPendingOrders.setAdapter(new AdapterOrder(context, orders, (order, position) -> startActivity(new Intent(context, SellerOrderDetailsActivity.class).putExtra("ORDER_ID", order.getOrderId()).putExtra("CUSTOMER_ID", order.getCustomerId()).putExtra("SHOP_ID", order.getShopId()))));

        loadOrders();
        return binding.getRoot();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadOrders() {
        FirebaseFirestore.getInstance().collection("Orders").whereEqualTo("shopId", MY_SHOP.getUid()).orderBy("orderStatus").orderBy("orderTime", Query.Direction.DESCENDING).addSnapshotListener((value, error) -> {
            if (error != null) {
                Toast.makeText(context, error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            if (value != null) {
                orders.clear();
                for (QueryDocumentSnapshot doc : value) {
                    ModelOrder order = doc.toObject(ModelOrder.class);
                    if (order.getOrderStatus() == ModelOrder.DELIVERED) break;
                    orders.add(order);
                }
                Objects.requireNonNull(binding.sellerPendingOrders.getAdapter()).notifyDataSetChanged();
                Toast.makeText(context, "Order list updated just now", Toast.LENGTH_SHORT).show();
            }
        });
    }
}