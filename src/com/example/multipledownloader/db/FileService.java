package com.example.multipledownloader.db;
import java.util.HashMap;
import java.util.Map;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
/*
 * ���߳������ж����ݿ�Ĳ���
 */

public class FileService {

	private DBOpenHelper dbhelper=null; 
	public FileService(Context context) {
		dbhelper=new DBOpenHelper(context);
		// TODO Auto-generated constructor stub
	}
	/*
	 * @para path���������ļ�������
	 * ��ȡ���̵߳ĵ�ǰ�����س���
	 */
	public Map<Integer,Integer> getEachDownloadedLength(String path){
		
		HashMap<Integer,Integer> map=new HashMap<Integer,Integer>();
		SQLiteDatabase db= dbhelper.getReadableDatabase();
		//�����String[]������ǰ��sql����У��Ĳ�������
		Cursor cursor=db.rawQuery("select threadid,downlength from MultipleDownLoad where downloadpath=?", new String[]{path});
		while(cursor.moveToNext()){
			map.put(cursor.getInt(0),cursor.getInt(1));
		}
		
		cursor.close();
		//dbhelper.close(); �ر��������ݿ������
		db.close();
		return map;
		
	}
	
	/*
	 * ����ÿ���߳������صĳ���
	 * @para path:�����ļ�����
	 * @para Map�����ص��߳�id�����³���
	 */
	public void save(String path,Map<Integer,Integer> map){
		
		SQLiteDatabase db=dbhelper.getWritableDatabase();
		
		/*
		 * һ������
		 * ������������д����߳����³��ȣ�
		 * ���tryΪ����ı�׼�����ʽ
		 */
		db.beginTransaction();//��ʼ����
		   try {
		     //�������
			   for(Map.Entry<Integer,Integer> entry:map.entrySet()){
				   db.execSQL("insert into MultipleDownLoad(downloadpath,threadid,downlength) values(?,?,?)",
						   new Object[]{path,entry.getKey(),entry.getValue()});
			   }
			   
		     db.setTransactionSuccessful();//���������־�ɹ�
		   } finally {
		     db.endTransaction();//�������
		   }
		   db.close();
	}
	
	/*
	 * ʵʱ����ÿ���̵߳����³���
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
	 * ������ɺ� ɾ����Ӧ�����ؼ�¼
	 */
	public void delete(String path){
		
		SQLiteDatabase db=dbhelper.getWritableDatabase();
		db.execSQL("delete from MultipleDownLoad where path=?", new Object[]{path});
		db.close();	
		
	}

}