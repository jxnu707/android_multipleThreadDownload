package com.example.multipledownloader.db;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.content.res.AssetManager;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class DBOpenHelper extends SQLiteOpenHelper {
	
	public static final String DBNAME="MutipleDownLoad.db";
	public static final int VERSION=1;
	
	private Context context;
	
	public DBOpenHelper(Context context){
		
		super(context, DBNAME, null, VERSION); 
		this.context=context;
		System.out.println("------DBOpenHelper Construction");
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		executeSQLScript(db,"create.sql");
//		db.execSQL("CREATE TABLE IF NOT EXISTS SmartFileDownlog (id integer primary key autoincrement, downpath varchar(100), threadid INTEGER, downlength INTEGER)");
		System.out.println("------DBOpenHelper onCreate");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		executeSQLScript(db, "upgrade.sql");
		onCreate(db);
	}

	private boolean executeSQLScript(SQLiteDatabase db,String sqlScriptName){
		
		InputStream input = null;
		ByteArrayOutputStream output=new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int lenth;

		AssetManager assetManager=context.getAssets();
		try {
			input=assetManager.open(sqlScriptName);
			while((lenth=input.read(buffer))!=-1){
				output.write(buffer, 0, lenth);
			}
			input.close();
			output.close();
			
			String[] scripts =output.toString().split(";");
			for(int i=0;i<scripts.length;i++){
				//去除开头后结尾的空格字符
				String script=scripts[i].trim();
				if(script.length()>0){
					db.execSQL(script+";");
					return true;
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
		
	} 
	
}
