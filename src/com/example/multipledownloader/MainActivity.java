package com.example.multipledownloader;

import java.io.File;

import com.example.multipledownloader.db.DBOpenHelper;
import com.example.multipledownloader.impl.MultipleDownLoadProgressListener;
import com.example.multipledownloader.impl.MultipleDownLoader;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	private ProgressBar downLoadbar;
	private TextView resultView;
	private EditText pathText;
//	private SQLiteDatabase db;
	private final int threadNum=3;
	private Handler handler=new Handler(){

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
			switch(msg.what){
			case 1:
				//计算进度条当前百分比
					int size=msg.getData().getInt("size");
					downLoadbar.setProgress(size);
					float result=(float)downLoadbar.getProgress()/(float)downLoadbar.getMax();
					int p=(int) (result*100);
					resultView.setTag(p+"%");
					//如果所有下载完了
					if(downLoadbar.getProgress()==downLoadbar.getMax())
						Toast.makeText(MainActivity.this, R.string.success, 1).show();
					break;
			case -1:
				Toast.makeText(MainActivity.this, R.string.error, 1).show();
					break;
			}
		}
		
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		DBOpenHelper dbOpenHelper=new DBOpenHelper(this.getApplicationContext());
//		db=dbOpenHelper.getWritableDatabase();
		Button button=(Button) findViewById(R.id.button1);
		pathText=(EditText) findViewById(R.id.editText1);
		downLoadbar=(ProgressBar) findViewById(R.id.progressBar1);
		resultView=(TextView) findViewById(R.id.textView1);
		
		button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				String path=pathText.getText().toString();
				//判断SD卡是否存在
				if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
					File dir=Environment.getExternalStorageDirectory();
					System.out.println(dir.toString());
					downLoad(path,dir);
				}
				else{
					Toast.makeText(MainActivity.this, R.string.sdcarderror, 1).show();
				}
			}
		});
	}

	private void downLoad(final String path, final File dir) {
			new Thread(new Runnable(){

				@Override
				public void run() {
					MultipleDownLoader loader=new MultipleDownLoader(MainActivity.this, path, dir, threadNum);
					//设置进度条
					int length=loader.getFileSize();
					downLoadbar.setMax(length);
					try {
						loader.download(new MultipleDownLoadProgressListener() {
							
							@Override
							public void onDownLoadSize(int size) {
								//实时获得文件下载的长度
								Message msg=handler.obtainMessage();
								msg.what=1;
								msg.getData().putInt("size", size);
								
								handler.sendMessage(msg);
							}
						});
					} catch (Exception e) {
						e.printStackTrace();
						Message msg=handler.obtainMessage();
						msg.what=-1;
						msg.getData().putString("error", "下载失败");
						
						handler.sendMessage(msg);
					}
				}
				
			}).start();
	}


}
