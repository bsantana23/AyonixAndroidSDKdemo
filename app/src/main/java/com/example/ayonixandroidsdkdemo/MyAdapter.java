package com.example.ayonixandroidsdkdemo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Vector;

import ayonix.AyonixException;
import ayonix.AyonixFace;
import ayonix.AyonixFaceID;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
    private Context context;
    private Vector<AyonixFace> facesToEnroll;
    private Vector<Bitmap> bitmapsToShow;
    private HashMap<byte[], EnrolledInfo> masterList;
    private AyonixFaceID engine;
    protected int checkedPosition = -1;
    protected boolean confirmButtonOff = true;
    private final String TAG = "myAdapter";
    protected byte[] matchAfid;

    public MyAdapter(Vector<AyonixFace> myDataset, HashMap<byte[], EnrolledInfo> master,
                     AyonixFaceID engine, Context context, Vector<Bitmap> bm) {
        this.facesToEnroll = myDataset;
        this.masterList = master;
        this.engine = engine;
        this.context = context;
        bitmapsToShow = bm;
        confirmButtonOff = true;
    }

    public void setFacesToEnroll(Vector<AyonixFace> facesToEnroll, int length, Vector<Bitmap> bm) {
        this.facesToEnroll = new Vector<>(length);
        this.facesToEnroll = facesToEnroll;
        this.bitmapsToShow = bm;
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
        viewHolder.bind(facesToEnroll.get(index), bitmapsToShow.get(index));
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
        private ImageView mugshot;
        private ImageView check;
        private volatile boolean matched = false;

        public MyViewHolder(View v) {
            super(v);
            faceFeatures = v.findViewById(R.id.content);
            mugshot = v.findViewById(R.id.mugshot);
            check = v.findViewById(R.id.checkbox);
            ViewGroup.LayoutParams params = check.getLayoutParams();
            params.width = 80;
            params.height = 80;
            check.setLayoutParams(params);
        }

        void bind(final AyonixFace face, Bitmap bitmap) {
            Log.d(TAG, "binding..");
            if(null != face) {

                // always print face features
                /*Bitmap bm = MainActivity.bitmapToImage(face);
                bm = MainActivity.scaleDown(bm, 450, true);
                mugshot.setImageBitmap(bm);
                mugshot.setVisibility(View.VISIBLE);*/

                bitmap = MainActivity.scaleDown(bitmap, 450, true);
                mugshot.setImageBitmap(bitmap);
                mugshot.setVisibility(View.VISIBLE);

                faceFeatures.setText(null);

                // check for any matches
                Vector<byte[]> afids = new Vector<>(masterList.keySet());
                float[] scores = new float[afids.size()];
                try {
                    byte[] afid = engine.CreateAfid(face);
                    engine.MatchAfids(afid, afids, scores);
                    for(int j = 0; j < scores.length; j++) {
                        if(scores[j]*100 >= MainActivity.getMatchMin()){
                            matched = true;
                            matchAfid = afids.get(j);
                            break;
                        }
                    }
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

                if(matched){
                    faceFeatures.append("Already enrolled. \n");
                    EnrolledInfo info = masterList.get(matchAfid);
                    String print = (
                            "Matched with: \n"
                                    + "             " + info.getName() + "\n"
                                    + "             " + info.getAge() +"y   " + info.getGender()
                    );
                    faceFeatures.append(print);
                    faceFeatures.append("\n\nWould you like to add a new face?\n");
                }
                else{
                    float smile = face.expression.smile*100;
                    String info = (
                            "       " + (face.gender > 0 ? "Female" : "Male") + "\n" +
                                    "       " + (int)face.age + "y\n"  +
                                    "       " + (smile > 0.1 ? "smiling" : "") + "\n" + //face.expression.smile < -0.9 ? "frowning" : "neutral") + "\n" +
                                    "       " + Math.round(face.quality*100) + "%"
                    );
                    faceFeatures.append(info);
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
                        else{

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

                //if (confirmButtonOff) {
                    confirmButtonOff = false;
                    Intent toggleEnrollButton = new Intent("toggleEnroll");
                    toggleEnrollButton.setAction("toggleEnroll");
                    boolean sent = LocalBroadcastManager.getInstance(context).sendBroadcast(toggleEnrollButton);
                    Log.d(TAG, "toggle enroll button intent sent " + sent);
                //}
            }
        }
    }

    public AyonixFace getSelected() {
        if(checkedPosition != -1) {
            return facesToEnroll.get(checkedPosition);
        }
        return null;
    }

    public Bitmap getSelectedBitmap(){
        if(checkedPosition != -1) {
            return bitmapsToShow.get(checkedPosition);
        }
        return null;
    }

    public byte[] getMatchAfid(){
        return matchAfid;
    }
}
