package com.prasunpersonal.gogroceries_shopkeeper.Fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.prasunpersonal.gogroceries_shopkeeper.Adaptrers.AdapterFragment;
import com.prasunpersonal.gogroceries_shopkeeper.Models.ModelOrder;
import com.prasunpersonal.gogroceries_shopkeeper.databinding.FragmentSellerOrdersBinding;

import java.util.ArrayList;

public class SellerOrdersFragment extends Fragment {
    FragmentSellerOrdersBinding binding;
    Context context;
    ArrayList<ModelOrder> orders;

    public SellerOrdersFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSellerOrdersBinding.inflate(inflater, container, false);
        context = binding.getRoot().getContext();

        ArrayList<Fragment> fragments = new ArrayList<>();
        fragments.add(new SellerPendingOrdersFragment());
        fragments.add(new SellerDeliveredOrdersFragment());

        binding.sellerOrdersViewpager.setAdapter(new AdapterFragment(getChildFragmentManager(), getLifecycle(), fragments));
        binding.sellerOrdersViewpager.setOffscreenPageLimit(fragments.size());
        binding.sellerOrdersViewpager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                binding.sellerOrdersMainTab.selectTab(binding.sellerOrdersMainTab.getTabAt(position));
            }
        });
        binding.sellerOrdersMainTab.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                binding.sellerOrdersViewpager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        return binding.getRoot();
    }
}