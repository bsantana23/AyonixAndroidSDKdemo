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
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class EnrolledPeopleAdapter extends RecyclerView.Adapter<EnrolledPeopleAdapter.MyViewHolder> {
    private Context context;
    private HashMap<byte[], ArrayList<File>> enrolledPeople;
    protected int checkedPosition = -1;
    private final String TAG = "enrolledPeopleAdapter";
    private Set afidSet;
    private List<byte[]> afidList;

    public EnrolledPeopleAdapter(HashMap<byte[], ArrayList<File>> myDataset, Context context) {
        enrolledPeople = myDataset;
        this.context = context;
    }

    public void setFacesToEnroll(HashMap<byte[], ArrayList<File>> enrolled) {
        this.enrolledPeople = new HashMap<>();
        this.enrolledPeople = enrolled;
        if(!enrolled.isEmpty()){
            afidSet = enrolledPeople.keySet();
            afidList = new ArrayList<>(afidSet);
        }
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
        ArrayList<File> fileArrayList = new ArrayList<>();
        if(!afidList.isEmpty() && index < afidList.size()){
            byte[] afid = afidList.get(index);
            if(index < afidList.size())
                fileArrayList = enrolledPeople.get(afid);
        }
        viewHolder.bind(fileArrayList);
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
            mugshot = v.findViewById(R.id.mugshot);
            check = v.findViewById(R.id.checkbox);
            ViewGroup.LayoutParams params = check.getLayoutParams();
            params.width = 80;
            params.height = 80;
            check.setLayoutParams(params);
        }

        void bind(final ArrayList<File> jpegFiles) {
            Log.d(TAG, "binding..");
            if(null != jpegFiles) {

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

                if(null != jpegFiles) {
                    Bitmap bm = BitmapFactory.decodeFile(jpegFiles.get(0).getAbsolutePath());
                    mugshot.setImageBitmap(bm);
                    mugshot.setVisibility(View.VISIBLE);
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
