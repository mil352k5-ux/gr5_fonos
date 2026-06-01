package com.example.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.model.Book;

import java.util.List;

public class BestSellerAdapter extends RecyclerView.Adapter<BestSellerAdapter.BestSellerViewHolder> {

    private List<Book> bestSellers;
    private OnBestSellerClickListener listener;

    public interface OnBestSellerClickListener {
        void onBestSellerClick(Book book);
    }

    public BestSellerAdapter(List<Book> bestSellers, OnBestSellerClickListener listener) {
        this.bestSellers = bestSellers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BestSellerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_best_seller, parent, false);
        return new BestSellerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BestSellerViewHolder holder, int position) {
        Book book = bestSellers.get(position);

        Glide.with(holder.itemView.getContext())
                .load(book.getCoverUrl())
                .placeholder(android.R.drawable.ic_menu_report_image)
                .into(holder.imgCover);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBestSellerClick(book);
            }
        });
    }

    @Override
    public int getItemCount() {
        return bestSellers.size();
    }

    public static class BestSellerViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCover;

        public BestSellerViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.imgBestSellerCover);
        }
    }
}
