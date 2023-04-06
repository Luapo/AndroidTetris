package com.example.tetris;

import androidx.appcompat.app.AppCompatActivity;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class RankListActivity extends AppCompatActivity {

    private int phoneHeight;
    private int phoneWidth;
    private ListAdapter adapter;
    private List<Rank> rankList=new ArrayList<Rank>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rank_list);
        DisplayMetrics displayMetrics;
        displayMetrics=getResources().getDisplayMetrics();
        phoneHeight=displayMetrics.heightPixels;
        phoneWidth=displayMetrics.widthPixels;
        adapter=new ListAdapter();
        ((ListView)findViewById(R.id.rankListShow)).setAdapter(adapter);
        getRankList();
        adapter.notifyDataSetChanged();
    }
    public void test(){
        for(int i=0;i<10;i++){
            Rank rank=new Rank();
            rank.setName("小红");
            rank.setScore(String.valueOf(i*10+5));
            rankList.add(rank);
        }
    }
    public void getRankList(){
        DBConnectHelper openHelper=new DBConnectHelper(this);
        SQLiteDatabase readDB=openHelper.getReadableDatabase();
        Cursor cursor =readDB.query(openHelper.tableName,new String[]{"id","name","score"},null,null,null,null,"score desc");
        rankList.clear();
        while(cursor.moveToNext()&&rankList.size()<10){
            LinearLayout lineLy1=new LinearLayout(this);
            TextView textView=new TextView(this);
            Rank rank=new Rank();
            rank.setId(cursor.getInt(0));
            rank.setName(cursor.getString(1));
            rank.setScore(cursor.getString(2));
            rankList.add(rank);
        }
        fillList();
        adapter.notifyDataSetChanged();
        readDB.close();
    }
    public void fillList(){
        while(rankList.size()<10){
            Rank rank=new Rank();
            rank.setName("---");
            rank.setScore("00000");
            rankList.add(rank);
        }
    }
    class ListAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return rankList.size();
        }
        @Override
        public Object getItem(int i) {return rankList.get(i);}
        @Override
        public long getItemId(int i) {return i;}
        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            View itemView = View.inflate(RankListActivity.this, R.layout.rank_list_item, null);
            itemView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,phoneHeight/14));
            ((TextView)itemView.findViewById(R.id.nameShow)).setText(String.format("%02d",i+1)+" . "+rankList.get(i).getName());
            ((TextView)itemView.findViewById(R.id.scoreShow)).setText(String.format("%05d",Integer.parseInt(rankList.get(i).getScore())));
            return itemView;
        }
    }
}