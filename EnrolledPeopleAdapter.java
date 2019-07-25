package com.example.ayonixandroidsdkdemo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Vector;

public class EnrolledPeopleAdapter extends RecyclerView.Adapter<EnrolledPeopleAdapter.MyViewHolder> {
    private MainActivity context;
    private HashMap<byte[], EnrolledInfo> enrolledPeople;
    private RecyclerView mugshotView;
    protected int checkedPosition = -1;
    private final String TAG = "enrolledPeopleAdapter";
    private int index;
    private Vector<byte[]> afidList;

    public EnrolledPeopleAdapter(HashMap<byte[], EnrolledInfo> myDataset, Context context) {
        enrolledPeople = myDataset;
        this.context = (MainActivity)context;
        afidList = new Vector<>(myDataset.keySet());
    }

    public void setFacesToEnroll(HashMap<byte[], EnrolledInfo> enrolled) {
        this.enrolledPeople = enrolled;
        this.afidList = new Vector<>(enrolled.keySet());
    }

    @NonNull
    @Override
    public EnrolledPeopleAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
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
        afidList = new Vector<>(enrolledPeople.keySet());
        if(afidList.size() > index) {
            //viewHolder.multiMugshotView.setAdapter(mugshotRecyclerViewAdapter);
            EnrolledInfo info = enrolledPeople.get(afidList.get(index));
            if(info != null) {
                /*mugshotRecyclerViewAdapter.setImagesToShow(info.getMugshots());
                mugshotRecyclerViewAdapter.notifyDataSetChanged();*/
                viewHolder.bind(info);
                Log.d(TAG, "onBindViewHolder: got it");
            }
        }

    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
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
        private TextView info;
        private RecyclerView multiMugshotView;

        public MyViewHolder(View v) {
            super(v);
            this.multiMugshotView = v.findViewById(R.id.multiImages);
            multiMugshotView.setLayoutManager(new LinearLayoutManager(v.getContext(), LinearLayoutManager.HORIZONTAL, false));

            mugshot = v.findViewById(R.id.mugshot);
            check = v.findViewById(R.id.checkbox);
            info = v.findViewById(R.id.content);
            ViewGroup.LayoutParams params = check.getLayoutParams();
            params.width = 80;
            params.height = 80;
            check.setLayoutParams(params);
        }

        void bind(final EnrolledInfo enrolledInfo ) { //} EnrolledInfo info) {
            Log.d(TAG, "binding..");
            if(null != enrolledInfo) {

                // toggle check mark
                if(!context.getEnroll()) {
                    if (checkedPosition == -1) {
                        check.setVisibility(View.GONE);
                    } else {
                        if (checkedPosition == getAdapterPosition()) {
                            check.setVisibility(View.VISIBLE);
                        } else {
                            check.setVisibility(View.GONE);
                        }
                    }
                }

                /*mugshotRecyclerViewAdapter.setImagesToShow(info.getMugshots());
                mugshotRecyclerViewAdapter.notifyDataSetChanged();*/
                Bitmap bm = BitmapFactory.decodeFile(enrolledInfo.getMugshots().get(0).getAbsolutePath());
                bm = MainActivity.scaleBitmap(bm, 350, true);
                mugshot.setImageBitmap(bm);
                mugshot.setVisibility(View.VISIBLE);

                // print persons info
                String information = (enrolledInfo.getName() == null ?
                                    "N/A": enrolledInfo.getName()) + "\n" +
                                     enrolledInfo.getAge()+ "y" + "\n" +
                                     enrolledInfo.getGender();
                info.setText(information);

                // allows toggling of check mark
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!context.getEnroll()) { // && v.getVisibility() == View.VISIBLE) {
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

    public void resetSelection(){
        notifyItemChanged(checkedPosition);
        checkedPosition = -1;
    }
}
