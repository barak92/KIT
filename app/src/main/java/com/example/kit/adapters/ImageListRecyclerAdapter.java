package com.example.kit.adapters;

import android.content.Context;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.kit.R;


import java.util.ArrayList;

public class ImageListRecyclerAdapter extends RecyclerView.Adapter<ImageListRecyclerAdapter.ViewHolder>{

    private ArrayList<Integer> mImages = new ArrayList<>();
    private ImageListRecyclerClickListener mImageListRecyclerClickListener;
    private Context mContext;

    public ImageListRecyclerAdapter(Context context, ArrayList<Integer> images, ImageListRecyclerClickListener imageListRecyclerClickListener) {
        mContext = context;
        mImages = images;
        mImageListRecyclerClickListener = imageListRecyclerClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_image_list_item, parent, false);
        final ViewHolder holder = new ViewHolder(view, mImageListRecyclerClickListener);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        RequestOptions requestOptions = new RequestOptions()
                .placeholder(R.drawable.cartman_cop)
                .error(R.drawable.cartman_cop);

        Glide.with(mContext)
                .setDefaultRequestOptions(requestOptions)
                .load(mImages.get(position))
                .into(((ViewHolder)holder).image);
    }

    @Override
    public int getItemCount() {
        return mImages.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements
            View.OnClickListener
    {
        ImageView image;
        ImageListRecyclerClickListener mClickListener;

        public ViewHolder(View itemView, ImageListRecyclerClickListener clickListener) {
            super(itemView);
            image = itemView.findViewById(R.id.image);
            mClickListener = clickListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            mClickListener.onImageSelected(getAdapterPosition());
        }
    }

    public interface ImageListRecyclerClickListener{
        void onImageSelected(int position);
    }
}
