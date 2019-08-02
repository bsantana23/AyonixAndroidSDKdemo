package com.example.ayonixandroidsdkdemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.annotation.NonNull;
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

import ayonix.AyonixException;
import ayonix.AyonixFace;
import ayonix.AyonixFaceID;


public class MatchedPeopleAdapter extends RecyclerView.Adapter<MatchedPeopleAdapter.MyViewHolder>{

    private MainActivity context;
    private final String TAG = "matchedPeopleAdapter";
    private HashMap<byte[], EnrolledInfo> matchedPeople;
    private HashMap<byte[], EnrolledInfo> masterList;
    private Vector<Bitmap> bitmaps;
    private Vector<byte[]> afidKeySet;
    private AyonixFaceID engine = null;

    public MatchedPeopleAdapter(HashMap<byte[], EnrolledInfo> myDataset, Context context) {
        matchedPeople = myDataset;
        this.context = (MainActivity)context;
    }

    public void setMatchList(HashMap<byte[], EnrolledInfo> list) {
        matchedPeople = list;
    }
    public void setMasterList(HashMap<byte[], EnrolledInfo> list) { masterList = list; }
    public void setEngine(AyonixFaceID e){ engine = e; }
    public AyonixFaceID getEngine(){ return engine; }

    @NonNull
    @Override
    public MatchedPeopleAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        android.view.View v =  LayoutInflater.from(context).inflate(R.layout.recycle_view_item, viewGroup, false);
        return new MatchedPeopleAdapter.MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MatchedPeopleAdapter.MyViewHolder viewHolder, int index) {
        // alternate row colors
        if(index%2 == 1)
            viewHolder.itemView.setBackgroundColor(Color.parseColor("#6AB8EE"));
        else
            viewHolder.itemView.setBackgroundColor(Color.parseColor("#A8D9F8"));
        afidKeySet = new Vector<>(matchedPeople.keySet());
        if(index < afidKeySet.size()) {
            byte[] key = afidKeySet.elementAt(afidKeySet.size()-(index+1));
            if(matchedPeople.containsKey(key))
                viewHolder.bind(matchedPeople.get(key));
            else
                return;
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
        return matchedPeople.size();
    }

    /**
     * Class that references the views for each data item.
     */
    class MyViewHolder extends RecyclerView.ViewHolder {
        private ImageView mugshot;
        private ImageView enrolledMugshot;
        private TextView info;
        private RecyclerView multiMugshotView;

        public MyViewHolder(View v) {
            super(v);
            this.multiMugshotView = v.findViewById(R.id.multiImages);
            multiMugshotView.setLayoutManager(new LinearLayoutManager(v.getContext(), LinearLayoutManager.HORIZONTAL, false));

            mugshot = v.findViewById(R.id.mugshot);
            enrolledMugshot = v.findViewById(R.id.matchimage);
            info = v.findViewById(R.id.content);
        }

        void bind(final EnrolledInfo enrolledInfo) { //} EnrolledInfo info) {
            Log.d(TAG, "binding..");
            if(null != enrolledInfo) {

                //Bitmap matchFace = MainActivity.scaleBitmap(enrolledInfo.getMugshot(), 350, true);
                Bitmap matchFace = BitmapFactory.decodeFile(enrolledInfo.getMugshot().getAbsolutePath());
                mugshot.setImageBitmap(matchFace);
                mugshot.setVisibility(View.VISIBLE);


                // print persons info
                info.setText(null);




                String information = enrolledInfo.getEnrolled() ?
                            ("Matched on "  + enrolledInfo.getTimestamp() + "\n" +
                            "Matched with " + (enrolledInfo.getName() == null ? "N/A" : enrolledInfo.getName()) + "\n" +
                            "             " + enrolledInfo.getAge() + "y" + "\n" +
                            "             " + enrolledInfo.getGender()) +"\n" +
                            "             " + enrolledInfo.getCurrHighestQuality()*100+"%":
                        enrolledInfo.getMatched() ?
                            ("Matched on " + enrolledInfo.getTimestamp() + "\n" +
                            " Not enrolled in system! Updated match. \n " +
                            "                "  + (enrolledInfo.getName() == null ? "N/A" : enrolledInfo.getName()) + "\n" +
                            "                "  + enrolledInfo.getAge() + "y" + "\n" +
                            "                "  + enrolledInfo.getGender() + "\n" +
                            "                "  + enrolledInfo.getCurrHighestQuality()*100+"%") :
                            ("Matched on " + enrolledInfo.getTimestamp() + "\n" +
                            "                "  + (enrolledInfo.getName() == null ? "N/A" : enrolledInfo.getName()) + "\n" +
                            "                "  + enrolledInfo.getAge() + "y" + "\n" +
                            "                "  + enrolledInfo.getGender() + "\n" +
                            "                "  + enrolledInfo.getCurrHighestQuality()*100+"%")
                        ;
                info.append(information);

                Log.d(TAG, "bind: enrolled = "+enrolledInfo.getEnrolled() + ", matched = "+enrolledInfo.getMatched());

                if(enrolledInfo.getEnrolled()){
                    Bitmap bm;
                    if(!enrolledInfo.getMugshots().isEmpty()){
                        bm = BitmapFactory.decodeFile(enrolledInfo.getMugshots().get(0).getAbsolutePath());
                        enrolledMugshot.setImageBitmap(bm);
                    } else if(enrolledInfo.getMugshotMatched() != null){
                        bm = BitmapFactory.decodeFile(enrolledInfo.getMugshotMatched().getAbsolutePath());
                        enrolledMugshot.setImageBitmap(bm);
                    } else{
                        enrolledMugshot.setImageResource(R.mipmap.baseline_sentiment_dissatisfied_black_48);
                    }
                    //bm = MainActivity.scaleBitmap(bm, 350, true);
                } else if (enrolledInfo.getMatched() && !enrolledInfo.getEnrolled() ) {
                    if(null == enrolledInfo.getMugshotMatched())
                        enrolledMugshot.setImageResource(R.mipmap.baseline_sentiment_dissatisfied_black_48);
                    else {
                        //Bitmap bm = MainActivity.scaleBitmap(enrolledInfo.getMugshotMatched(), 350, true);
                        Bitmap bm = BitmapFactory.decodeFile(enrolledInfo.getMugshotMatched().getAbsolutePath());
                        enrolledMugshot.setImageBitmap(bm);
                        // or set the mugshot not mugshotmatched
                    }
                }else {
                    enrolledMugshot.setImageResource(R.mipmap.baseline_sentiment_dissatisfied_black_48);
                }
                enrolledMugshot.setVisibility(View.VISIBLE);

            }
        }
    }
}
