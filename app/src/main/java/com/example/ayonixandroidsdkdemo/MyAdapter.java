package com.example.ayonixandroidsdkdemo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import ayonix.AyonixException;
import ayonix.AyonixFace;
import ayonix.AyonixFaceID;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
    private Context context;
    private Vector<AyonixFace> facesToEnroll;
    private HashMap<byte[],  ArrayList<File>> masterList;
    private AyonixFaceID engine;
    protected int checkedPosition = -1;
    protected boolean confirmButtonOff = true;
    private final String TAG = "myAdapter";
    protected byte[] matchAfid;

    public MyAdapter(Vector<AyonixFace> myDataset, HashMap<byte[], ArrayList<File>> master, AyonixFaceID engine, Context context) {
        this.facesToEnroll = myDataset;
        this.masterList = master;
        this.engine = engine;
        this.context = context;
        confirmButtonOff = true;
    }

    public void setFacesToEnroll(Vector<AyonixFace> facesToEnroll, int length) {
        this.facesToEnroll = new Vector<>(length);
        this.facesToEnroll = facesToEnroll;
        for(int i = 0; i < facesToEnroll.size(); i++){
            System.out.println("Face " + (i+1) + " " + facesToEnroll.get(i) + "\n");
        }
    }

    @NonNull
    @Override
    public MyAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
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
        viewHolder.bind(facesToEnroll.get(index));
    }

    @Override
    public int getItemCount() {
        return facesToEnroll.size();
    }

    /**
     * Class that references the views for each data item.
     */
    class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView faceFeatures;
        private TextView count;
        private ImageView mugshot;
        private ImageView check;
        private boolean matched = false;

        public MyViewHolder(View v) {
            super(v);
            faceFeatures = v.findViewById(R.id.content);
            mugshot = v.findViewById(R.id.mugshot);
            count = v.findViewById(R.id.item_number);
            check = v.findViewById(R.id.checkbox);
            ViewGroup.LayoutParams params = check.getLayoutParams();
            params.width = 80;
            params.height = 80;
            check.setLayoutParams(params);
        }

        void bind(final AyonixFace face) {
            Log.d(TAG, "binding..");
            if(null != face) {

                // check for any matches
                final Vector<byte[]> afids = new Vector<>(masterList.keySet());
                float[] scores = new float[afids.size()];
                try {
                    byte[] afid = engine.CreateAfid(face);
                    engine.MatchAfids(afid, afids, scores);
                } catch (AyonixException e) {
                    e.printStackTrace();
                }

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

                // always print face features
                Bitmap bm = MainActivity.bitmapToImage(face);
                mugshot.setImageBitmap(bm);
                mugshot.setVisibility(View.VISIBLE);
                float smile = face.expression.smile*100;
                String info = (
                        "       "+(face.gender > 0 ? "female" : "male") + "\n" +
                        "       "+(int)face.age + "y\n"  +
                        "       "+(smile > 0.7 ? "smiling": smile < 0.7 ? "frowning": "neutral")+ "\n" +
                        "       mouth open: " + face.expression.mouthOpen + "\n" +
                        "       quality: " + face.quality*100 + "\n");
                faceFeatures.setText(info);
                for(int j = 0; j < scores.length; j++) {
                    if(scores[j]*100 >= MainActivity.MIN_MATCH){
                        faceFeatures.append("Face already enrolled in system. Would you like to add a new face?");
                        matched = true;
                        matchAfid = afids.get(j);
                        break;
                    }
                }

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
                        if(matched){
                            Intent toggleConfirm_Cancel = new Intent("toggleConfirm_Cancel");
                            toggleConfirm_Cancel.setAction("toggleConfirm_Cancel");
                            boolean sent = LocalBroadcastManager.getInstance(context).sendBroadcast(toggleConfirm_Cancel);
                            Log.d(TAG, "toggle confirm/cancel buttons intent sent " + sent);
                            matched = false;
                        }
                    }
                });

                if (confirmButtonOff) {
                    confirmButtonOff = false;
                    Intent toggleEnrollButton = new Intent("toggleEnroll");
                    toggleEnrollButton.setAction("toggleEnroll");
                    boolean sent = LocalBroadcastManager.getInstance(context).sendBroadcast(toggleEnrollButton);
                    Log.d(TAG, "toggle enroll button intent sent " + sent);
                }
            }
        }
    }

    public AyonixFace getSelected() {
        if(checkedPosition != -1) {
            return facesToEnroll.get(checkedPosition);
        }
        return null;
    }

    public byte[] getMatchAfid(){
        return matchAfid;
    }
}
