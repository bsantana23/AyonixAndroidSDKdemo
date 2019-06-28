package com.example.ayonixandroidsdkdemo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

public class EnrolledPeopleAdapter extends RecyclerView.Adapter<EnrolledPeopleAdapter.MyViewHolder> {
    private Context context;
    private HashMap<byte[], EnrolledInfo> enrolledPeople;
    private RecyclerView mugshotView;
    protected int checkedPosition = -1;
    private final String TAG = "enrolledPeopleAdapter";
    private int index;
    private Vector<byte[]> afidList;
    private MugshotRecyclerViewAdapter mugshotRecyclerViewAdapter;

    public EnrolledPeopleAdapter(HashMap<byte[], EnrolledInfo> myDataset, Context context) {
        enrolledPeople = myDataset;
        this.context = context;
        afidList = new Vector<>(myDataset.keySet());
        mugshotRecyclerViewAdapter = new MugshotRecyclerViewAdapter(new ArrayList<File>(), context);
    }

    public void setFacesToEnroll(HashMap<byte[], EnrolledInfo> enrolled) {
        this.enrolledPeople = enrolled;
    }

    @NonNull
    @Override
    public EnrolledPeopleAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        android.view.View v =  LayoutInflater.from(context).inflate(R.layout.fragment_multifacelist, viewGroup, false);
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
        if(afidList.size() > index)
            viewHolder.bind(enrolledPeople.get(afidList.get(index)));
    }

    @Override
    public int getItemCount() {
        return enrolledPeople.size();
    }

    /**
     * Class that references the views for each data item.
     */
    class MyViewHolder extends RecyclerView.ViewHolder {
        private ImageView mugshot;
        private ImageView check;

        public MyViewHolder(View v) {
            super(v);
            mugshot = v.findViewById(R.id.multimugshot);
            check = v.findViewById(R.id.checkbox);
            ViewGroup.LayoutParams params = check.getLayoutParams();
            params.width = 80;
            params.height = 80;
            check.setLayoutParams(params);
        }

        void bind(final EnrolledInfo info) {
            Log.d(TAG, "binding..");
            if(null != info) {

                // toggle check mark
                if (checkedPosition == -1) {
                    check.setVisibility(View.GONE);
                } else {
                    if (checkedPosition == getAdapterPosition()) {
                        check.setVisibility(View.VISIBLE);
                    } else {
                        check.setVisibility(View.GONE);
                    }
                }

                mugshotRecyclerViewAdapter.setImagesToShow(info.getMugshots());
                mugshotRecyclerViewAdapter.notifyDataSetChanged();

                // allows toggling of check mark
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        check.setVisibility(View.VISIBLE);
                        v.setBackgroundColor(Color.parseColor("#B4F8C8"));
                        if (checkedPosition != getAdapterPosition()) {
                            notifyItemChanged(checkedPosition);
                            checkedPosition = getAdapterPosition();
                        }
                        Intent remove = new Intent("removeSelected");
                        remove.setAction("removeSelected");
                        boolean sent = LocalBroadcastManager.getInstance(context).sendBroadcast(remove);
                        Log.d(TAG, "toggle confirm/cancel buttons intent sent " + sent);
                    }
                });
            }
        }
    }

    public byte[] getSelected() {
        if(checkedPosition != -1) {
            return afidList.get(checkedPosition);
        }
        return null;
    }
}
