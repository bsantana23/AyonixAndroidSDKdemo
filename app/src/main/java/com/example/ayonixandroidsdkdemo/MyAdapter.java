package com.example.ayonixandroidsdkdemo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.Image;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Vector;

import ayonix.AyonixFace;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
    private Context context;
    private Vector<AyonixFace> facesToEnroll;
    protected int checkedPosition = -1;
    protected boolean confirmButtonOff = true;
    private final String TAG = "myAdapter";

    public MyAdapter(Vector<AyonixFace> myDataset, Context context) {
        facesToEnroll = myDataset;
        this.context = context;
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
        Log.d(TAG, "creating create view holde");
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

                Bitmap bm = Bitmap.createBitmap(face.mugshot.width, face.mugshot.height, Bitmap.Config.RGB_565);
                // always print face features
                    // convert byte array to int array, then set pixels into  bitmap to create image

                int[] ret = new int[face.mugshot.data.length];
                for (int i = 0; i < face.mugshot.data.length; i++)
                {
                    ret[i] = face.mugshot.data[i]; //& 0xff; // Range 0 to 255, not -128 to 127
                }

                bm.setPixels(ret, 0, bm.getWidth(), 0, 0, face.mugshot.width, face.mugshot.height);

                mugshot.setImageBitmap(bm);
                mugshot.setVisibility(View.VISIBLE);
                String info = (
                        "       gender: " + (face.gender > 0 ? "female" : "male") + "\n" +
                        "       age: " + face.age + "y\n"  +
                        "       smile: " + face.expression.smile + "\n" +
                        "       mouth open: " + face.expression.mouthOpen + "\n" +
                        "       quality: " + face.quality + "\n");
                faceFeatures.setText(info);

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
                    }
                });

                if (confirmButtonOff) {
                    confirmButtonOff = false;
                    Intent toggleEnrollButton = new Intent("toggle");
                    toggleEnrollButton.setAction("toggle");
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
}
