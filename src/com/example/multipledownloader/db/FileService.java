package com.example.multipledownloader.db;
import java.util.HashMap;
import java.util.Map;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
/*
 * 各线程下载中对数据库的操作
 */

public class FileService {

	private DBOpenHelper dbhelper=null; 
	public FileService(Context context) {
		dbhelper=new DBOpenHelper(context);
		// TODO Auto-generated constructor stub
	}
	/*
	 * @para path：所下载文件的链接
	 * 获取各线程的当前已下载长度
	 */
	public Map<Integer,Integer> getEachDownloadedLength(String path){
		
		HashMap<Integer,Integer> map=new HashMap<Integer,Integer>();
		SQLiteDatabase db= dbhelper.getReadableDatabase();
		//后面的String[]参数是前面sql语句中？的参数数组
		Cursor cursor=db.rawQuery("select threadid,downlength from MultipleDownLoad where downloadpath=?", new String[]{path});
		while(cursor.moveToNext()){
			map.put(cursor.getInt(0),cursor.getInt(1));
		}
		
		cursor.close();
		//dbhelper.close(); 关闭所有数据库的连接
		db.close();
		return map;
		
	}
	
	/*
	 * 保存每条线程已下载的长度
	 * @para path:下载文件链接
	 * @para Map：下载的线程id和已下长度
	 */
	public void save(String path,Map<Integer,Integer> map){
		
		SQLiteDatabase db=dbhelper.getWritableDatabase();
		
		/*
		 * 一个事务
		 * 批量处理（批量写入各线程已下长度）
		 * 这个try为事务的标准代码格式
		 */
		db.beginTransaction();//开始事务
		   try {
		     //处理过程
			   for(Map.Entry<Integer,Integer> entry:map.entrySet()){
				   db.execSQL("insert into MultipleDownLoad(downloadpath,threadid,downlength) values(?,?,?)",
						   new Object[]{path,entry.getKey(),entry.getValue()});
			   }
			   
		     db.setTransactionSuccessful();//设置事务标志成功
		   } finally {
		     db.endTransaction();//事务结束
		   }
		   db.close();
	}
	
	/*
	 * 实时更新每条线程的已下长度
	 */
	public void update(String path,Map<Integer,Integer> map){
		
		SQLiteDatabase db=dbhelper.getWritableDatabase();
		
		db.beginTransaction();
		try {
			for(Map.Entry<Integer, Integer> entry:map.entrySet()){
				db.execSQL("update MultipleDownLoad set downlength=? where downloadpath=? and threadid=?",
						new Object[]{entry.getValue(),path,entry.getKey()});
			}
			db.setTransactionSuccessful();
		}finally{
			db.endTransaction();
		}
		db.close();
	}
	
	/*
	 * 下载完成后 删除对应的下载记录
	 */
	public void delete(String path){
		
		SQLiteDatabase db=dbhelper.getWritableDatabase();
		db.execSQL("delete from MultipleDownLoad where path=?", new Object[]{path});
		db.close();	
		
	}

}