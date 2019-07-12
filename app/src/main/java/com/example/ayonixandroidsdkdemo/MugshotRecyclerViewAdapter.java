package com.example.ayonixandroidsdkdemo;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;
import android.widget.ImageView;
import java.io.File;
import java.util.ArrayList;

import com.example.ayonixandroidsdkdemo.MultiFaceListFragment.OnListFragmentInteractionListener;
import com.example.ayonixandroidsdkdemo.dummy.DummyContent.DummyItem;

/**
 * {@link RecyclerView.Adapter} that can display a {@link DummyItem} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class MugshotRecyclerViewAdapter extends RecyclerView.Adapter<MugshotRecyclerViewAdapter.ViewHolder> {

    private ArrayList<File> mValues;
    private Context context;
    private final String TAG = "MyMultiFaceListAdapter";

    public MugshotRecyclerViewAdapter(ArrayList<File> info) {
        mValues = info;
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
        if(position%2 == 1)
            holder.itemView.setBackgroundColor(Color.parseColor("#6AB8EE"));
        else
            holder.itemView.setBackgroundColor(Color.parseColor("#A8D9F8"));
        holder.mItem = mValues.get(position);
        holder.bind(mValues.get(position));
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
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
                bm = MainActivity.scaleDown(bm, 350, true);
                mugshot.setImageBitmap(bm);
                mugshot.setVisibility(View.VISIBLE);
            }
        }
    }
}
