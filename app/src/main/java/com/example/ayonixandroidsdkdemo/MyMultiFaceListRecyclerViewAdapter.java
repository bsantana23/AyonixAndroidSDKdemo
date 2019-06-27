package com.example.ayonixandroidsdkdemo;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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

import com.example.ayonixandroidsdkdemo.MultiFaceListFragment.OnListFragmentInteractionListener;
import com.example.ayonixandroidsdkdemo.dummy.DummyContent.DummyItem;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link DummyItem} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class MyMultiFaceListRecyclerViewAdapter extends RecyclerView.Adapter<MyMultiFaceListRecyclerViewAdapter.ViewHolder> {

    private ArrayList<File> mValues;
    private final OnListFragmentInteractionListener mListener;
    private Context context;
    protected int checkedPosition = -1;
    private final String TAG = "MyMultiFaceListAdapter";

    public MyMultiFaceListRecyclerViewAdapter(ArrayList<File> items, OnListFragmentInteractionListener listener, Context c) {
        mValues = items;
        mListener = listener;
        context = c;
    }

    public void setImagesToShow(ArrayList<File> images) {
        mValues = new ArrayList<>();
        this.mValues = images;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_multifacelist, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    //mListener.onListFragmentInteraction(holder.mItem);
                }
            }
        });
        holder.bind(mValues.get(position));
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public File mItem;
        private ImageView mugshot;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mugshot = view.findViewById(R.id.multimugshot);
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
}
