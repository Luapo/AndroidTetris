package com.example.tetris;

import static java.lang.Math.log;
import static java.lang.Math.max;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.renderscript.ScriptGroup;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TimeUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLEngineResult;

public class MainActivity extends AppCompatActivity {
    private static final int tableHeight=20;
    private static final int tableWidth= 10;
    private static final String TAG = MainActivity.class.getSimpleName();
    private int[][] status= new int[tableHeight][tableWidth];
    private final String[] colors=new String[]{"white","orange","blue","purple","yellow","red","green","indigo"};
    private final String[] shapes=new String[]{"null","l","j","t","o","z","s","i"};
    private GridView gameList;
    private int phoneHeight;
    private int phoneWidth;
    private View dialog=null;
    private int timeWait=1000;
    private Thread thread=null;
    private int nextStatus=0;
    private int nowPosition=5;
    private int nowStatus=0;
    private int nowRoat=0;
    private AlertDialog alertDialog=null;
    private double dicFactor=0.98;//test 0.95 normal 0.998
    private int minTime=500;//test 300 normal 500
    private  int score=0;
    private  int time=0;
    private Context context=this;
    private boolean runnning=false;
    private boolean finished=true;
    private final int[][] shapeStyle =new int[][]{
            {},
            {00,01,02,12},
            {00,01,02,-12},
            {00,10,11,20},
            {00,10,01,11},
            {00,10,11,21},
            {00,01,-11,10},
            {00,01,02,03}
    };
    private Random random=new Random();
    private GameAdapter adapter=new GameAdapter();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gameList=(GridView) findViewById(R.id.gameList);
        DisplayMetrics displayMetrics;
        displayMetrics=getResources().getDisplayMetrics();
        phoneHeight=displayMetrics.heightPixels;
        phoneWidth=displayMetrics.widthPixels;
        gameList.setAdapter(adapter);
        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean f=false;
                int flag=intent.getIntExtra("flag",-1);
                if (flag!=-1){
                    f=moveManager(2);time+=timeWait;
                    timeWait=max((int)(timeWait*dicFactor),minTime);
                    timeShow();
                }
            }
        },new IntentFilter("update_progress"));
        //test();
        resume();
        //over();
    }
    public int getStatus(int i){ return status[i/tableWidth][i%tableWidth];}
    public String getColors(int i){ return colors[getStatus(i)]; }
    public boolean checkBoard(int i){
        return checkBoard(i/tableWidth,i%tableWidth);
    }
    public boolean checkBoard(int h,int w){
        if(w<0||h<0||w>=tableWidth||h>=tableHeight)return false;
        return true;
    }
    public void timeShow(){
        int h=time/60000,w=(time/1000)%60;
        ((TextView)findViewById(R.id.timeShow)).setText(String.format("%02d:%02d",h,w));
    }
    public void scoreShow(){
        ((TextView)findViewById(R.id.scoreShow)).setText(String.format("%05d",score));
    }
    public void rowClear(int r){
        for(int h=r;h>=0;h--) {
            for (int w = 0; w < tableWidth; w++) {
                status[h][w] = (h-1>=0)?status[h-1][w]:0;
            }
        }
    }
    public void gameClearManager(){
        int t=0;
        for(int h=0;h<tableHeight;h++){
            int tp=0;
            for(int w=0;w<tableWidth;w++){
                if(status[h][w]==0)break;
                if(w==tableWidth-1){
                    t++;
                    rowClear(h);
                }
            }
        }
        if(t>0){
            score+=t*(t+1)/2*10;
            scoreShow();
        }
    }
    public boolean shapeManager(int op,int statu,int pos){//0删除 1填补
        if(statu==0){
            return true;
        }
        List<Integer> list =new ArrayList<Integer>(){};
        int h=pos/tableWidth,w=pos%tableWidth,ah,aw,newPos,bh,bw;
        boolean f=true;
        for(int i=0;f&&i<shapeStyle[statu].length;i++){
            int tp=shapeStyle[statu][i];
            aw=tp/10;
            if(tp<0)tp=-tp;
            ah=tp%10;
            for(int j=0;j<nowRoat;j++){
                bw=-ah;
                bh=aw;
                aw=bw;
                ah=bh;
            }
            ah+=h;aw+=w;
            newPos=ah*tableWidth+aw;
            if(!checkBoard(ah,aw)){f=false;break;}
            if(op==1&&status[ah][aw]!=0)f=false;
            list.add(newPos);
        }
        for (int i=0;f&&i<list.size();i++){
            newPos= list.get(i);
            if(op==0) status[newPos/tableWidth][newPos%tableWidth]=0;
            else status[newPos/tableWidth][newPos%tableWidth]=statu;
        }
        return f;
    }
    public void moveSpin(View view){
        if(!runnning)return;
        moveManager(4);
    }
    public void moveLeft(View view){
        if(!runnning)return;
        moveManager(1);
    }
    public void moveRight(View view){
        if(!runnning)return;
        moveManager(3);
    }
    public void moveDown(View view){
        if(!runnning)return;
        moveManager(2);
    }
    public void pause(View view){
        if(!finished) {
            if (runnning) {
                runnning = false;
                ((Button) view).setText("开始");
            } else {
                runnning = true;
                ((Button) view).setText("暂停");
            }
        }else resume();
    }
    public void over(){
        runnning=false;
        finished=true;
        ((Button)findViewById(R.id.pauseButton)).setText("重开");
        if(thread!=null)thread.interrupt();
        if(dialog==null){dialog=this.getLayoutInflater().inflate(R.layout.finish_dialog,null);}
        if(alertDialog==null){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("游戏结束");
            builder.setView(dialog);
            alertDialog =builder.create();
        }
        TextView scoreShowEx=(TextView)dialog.findViewById(R.id.scoreShowEx);
        scoreShowEx.setText(String.format("%5d",score));
        alertDialog.show();
    }
    public void getNewStatus(){
        nextStatus=random.nextInt(shapes.length-1)+1;
        int id=getResources().getIdentifier(shapes[nextStatus]+"_img","drawable",getPackageName());
        ((ImageView)findViewById(R.id.nextImage)).setImageDrawable(getDrawable(id));
    }
    public boolean moveManager(int op){//1 左移 2下移 3 右移
        boolean f=true;
        if(nowStatus==0){
            nowStatus=nextStatus;
            nowPosition=5;
            nowRoat=0;
            getNewStatus();
            f= shapeManager(1,nowStatus,nowPosition);
            if(f==false)over();
        }
        else{
            int h=nowPosition/tableWidth,w=nowPosition%tableWidth;
            shapeManager(0,nowStatus,nowPosition);
            if(op==1){
                if(checkBoard(h,w-1)){
                    f=shapeManager(1,nowStatus,nowPosition-1);
                    if(f)nowPosition=nowPosition-1;
                }else f=false;
            }
            if(op==2){
                if(checkBoard(h+1,w)){
                    f=shapeManager(1,nowStatus,nowPosition+tableWidth);
                    if(f)nowPosition=nowPosition+tableWidth;
                }else f=false;
            }
            if(op==3){
                if(checkBoard(h,w+1)){
                    f=shapeManager(1,nowStatus,nowPosition+1);
                    if(f)nowPosition=nowPosition+1;
                }else f=false;
            }
            if(op==4){
                nowRoat=(nowRoat+1)%4;
                if(checkBoard(h,w)){
                    f=shapeManager(1,nowStatus,nowPosition);
                }else f=false;
                if(!f)nowRoat=(nowRoat+3)%4;
            }
            if(!f)shapeManager(1,nowStatus,nowPosition);
            if(!f&&op==2){
                nowStatus=0;
                nowPosition=5;
                nowRoat=0;
                gameClearManager();
            }
        }
        if(f){
            adapter.notifyDataSetChanged();
        }
        return f;
    }
    public void resume(){
        timeWait=1000;
        runnning=true;
        finished=false;
        score=0;
        time=0;
        getNewStatus();
        timeShow();
        scoreShow();
        ((Button)findViewById(R.id.pauseButton)).setText("暂停");
        for(int i=0;i<tableHeight;i++){
            for(int j=0;j<tableWidth;j++){
                status[i][j]=0;
            }
        }
        adapter.notifyDataSetChanged();
        if(thread ==null) {
            Runnable runnable=new Runnable() {
                @Override
                public void run() {
                    while(!thread.isInterrupted()){
                        while(!thread.isInterrupted()&&runnning){
                            try {
                                Intent intent = new Intent();
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.putExtra("flag", 1);
                                intent.setAction("update_progress");
                                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                                TimeUnit.MILLISECONDS.sleep(timeWait);
                            }catch (InterruptedException e){
                                e.printStackTrace();
                            }
                        }
                    }
                }
            };
            thread = new Thread(runnable);
            thread.start();
        }
        runnning=true;
    }
    public void test(){
        timeWait=100000;
        for(int i=0;i<tableHeight;i++){
            for(int j=0;j<tableWidth;j++){
                status[i][j]= random.nextInt(shapes.length);
            }
        }
        adapter.notifyDataSetChanged();
    }
    public void uploadRank(){
        String name=((EditText)dialog.findViewById(R.id.nameInput)).getText().toString();
        if(name!=null&&!name.equals("")) {
            //Log.i(TAG, "uploadRank: "+name);
            DBConnectHelper openHelper = new DBConnectHelper(this);
            SQLiteDatabase writeDB = openHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("name",name);
            values.put("score",score);
            writeDB.insert(openHelper.tableName, null, values);
            writeDB.close();
        }
    }
    public void jumpRankListEx(View view){
        closeDialog(view);
        Intent intent=new Intent(this,RankListActivity.class);
        startActivity(intent);
    }
    public void closeDialog(View view){
        uploadRank();
        alertDialog.cancel();
    }
    class GameAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return tableWidth*tableHeight;
        }
        @Override
        public Object getItem(int i) {return status[i/tableWidth][i%tableWidth];}
        @Override
        public long getItemId(int i) {return i;}
        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            View itemView = View.inflate(MainActivity.this, R.layout.list_item, null);
            itemView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,phoneHeight/24));
            int id=getResources().getIdentifier(getColors(i)+"_solid","drawable",getPackageName());
            itemView.setBackground(getDrawable(id));
            /*
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    status[i/tableWidth][i%tableWidth]=(status[i/tableWidth][i%tableWidth]+1)%colors.length;
                    int id2=getResources().getIdentifier(getColors(i)+"_solid","drawable",getPackageName());
                    Log.i(TAG, "onClick: "+String.valueOf(i));
                    adapter.notifyDataSetChanged();
                }
            });
             */
            return itemView;
        }
    }
}