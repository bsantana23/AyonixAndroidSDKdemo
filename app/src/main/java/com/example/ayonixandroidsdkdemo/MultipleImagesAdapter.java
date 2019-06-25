package com.example.ayonixandroidsdkdemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MultipleImagesAdapter extends RecyclerView.Adapter<MultipleImagesAdapter.MyViewHolder> {
    private Context context;
    private ArrayList<File> allImages;
    protected int checkedPosition = -1;
    private final String TAG = "enrolledPeopleAdapter";

    public MultipleImagesAdapter(ArrayList<File> myDataset, Context context) {
        allImages = myDataset;
        this.context = context;
    }

    public void setImagesToShow(ArrayList<File> images) {
        allImages = new ArrayList<>();
        this.allImages = images;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MultipleImagesAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        android.view.View v =  LayoutInflater.from(context).inflate(R.layout.recycle_view_item, viewGroup, false);
        Log.d(TAG, "creating create view holder");
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder viewHolder, int index) {
        // alternate row colors
        if(index%2 == 1)
            viewHolder.itemView.setBackgroundColor(Color.parseColor("#6AB8EE"));
        else
            viewHolder.itemView.setBackgroundColor(Color.parseColor("#A8D9F8"));
        System.out.println("getting afid index " + index);
        viewHolder.bind(allImages.get(index));
    }

    @Override
    public int getItemCount() {
        return allImages.size();
    }

    /**
     * Class that references the views for each data item.
     */
    class MyViewHolder extends RecyclerView.ViewHolder {
        private ImageView mugshot;

        public MyViewHolder(View v) {
            super(v);
            mugshot = v.findViewById(R.id.mugshot);
        }

        void bind(final File jpegFile) {
            Log.d(TAG, "binding..");
            if(null != jpegFile) {
                Bitmap bm = BitmapFactory.decodeFile(jpegFile.getAbsolutePath());
                mugshot.setImageBitmap(bm);
                mugshot.setVisibility(View.VISIBLE);
            }
        }
    }

    public File getSelected() {
        if(checkedPosition != -1) {
            return allImages.get(checkedPosition);
        }
        return null;
    }
}
