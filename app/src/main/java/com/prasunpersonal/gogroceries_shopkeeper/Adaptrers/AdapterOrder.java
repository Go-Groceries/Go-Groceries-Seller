package com.prasunpersonal.gogroceries_shopkeeper.Adaptrers;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.prasunpersonal.gogroceries_shopkeeper.Models.ModelCustomer;
import com.prasunpersonal.gogroceries_shopkeeper.Models.ModelOrder;
import com.prasunpersonal.gogroceries_shopkeeper.R;
import com.prasunpersonal.gogroceries_shopkeeper.databinding.RowOrderBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class AdapterOrder extends RecyclerView.Adapter<AdapterOrder.HolderOrder> {

    private final Context context;
    private final ArrayList<ModelOrder> orderList;
    private final setOnClickListener listener;

    public AdapterOrder(Context context, ArrayList<ModelOrder> orderList, setOnClickListener listener) {
        this.context = context;
        this.orderList = orderList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public HolderOrder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new HolderOrder(RowOrderBinding.inflate(LayoutInflater.from(context), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull HolderOrder holder, int position) {
        ModelOrder order = orderList.get(position);

        FirebaseFirestore.getInstance().collection("Customers").document(order.getCustomerId()).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult() != null && task.getResult().exists()) {
                    ModelCustomer customer = task.getResult().toObject(ModelCustomer.class);
                    assert customer != null;
                    Glide.with(context).load(customer.getDp()).placeholder(R.drawable.ic_person).into(holder.binding.productImg);
                    holder.binding.orderTitle.setText(String.format(Locale.getDefault(), "%d product(s) ordered by %s", order.getCartItems().size(), customer.getName()));
                }
            } else {
                Toast.makeText(context, Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        holder.binding.orderTime.setText(new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(new Date(order.getOrderTime())));
        MaterialCardView[] points = {holder.binding.orderPlacedPoint, holder.binding.orderPackedPoint, holder.binding.outForDeliveryPoint, holder.binding.moneyReceivedPoint, holder.binding.deliveredPoint};
        View[] lines = {holder.binding.connectionLine0, holder.binding.connectionLine1, holder.binding.connectionLine2, holder.binding.connectionLine3};
        for (int i=0; i < points.length; i++){
            if (i < order.getOrderStatus()){
                points[i].setCardBackgroundColor(Color.GREEN);
                if (i < lines.length) lines[i].setBackgroundColor(Color.GREEN);
            } else if (i == order.getOrderStatus()) {
                points[i].setCardBackgroundColor(Color.GREEN);
                if (i < lines.length) lines[i].setBackgroundColor(Color.LTGRAY);
            } else {
                points[i].setCardBackgroundColor(Color.LTGRAY);
                if (i < lines.length) lines[i].setBackgroundColor(Color.LTGRAY);
            }
        }

        holder.itemView.setOnClickListener(v -> listener.OnClickListener(order, position));
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public interface setOnClickListener{
        void OnClickListener(ModelOrder order, int position);
    }

    static class HolderOrder extends RecyclerView.ViewHolder{
        RowOrderBinding binding;

        public HolderOrder(@NonNull RowOrderBinding binding) {
            super(binding.getRoot());

            this.binding = binding;
        }
    }
}



