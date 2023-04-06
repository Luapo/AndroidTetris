package com.example.tetris;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DBConnectHelper extends SQLiteOpenHelper {
    public String tableName="rankList";
    public String dbName="missions.db";
    public DBConnectHelper(@Nullable Context context){
        super(context,"missions.db",null,1);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table "+tableName+"(id integer primary key autoincrement,name text,score text)");
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        //
    }
    public void Delete(SQLiteDatabase db,String table,String row, String value){
        db.execSQL("delete from "+table+" where "+row+" = "+value);
    }
}
