package com.example.multipledownloader.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

import android.util.Log;

public class MultipleDownLoadThread extends Thread {

	private static final String TAG="MultipleDownLoadThread";
	
	private File saveFile;
	private URL downLoadUrl;
	private int block;
	private int threadID=-1;
	private int downLength;
	private boolean isFinish=false;
	private MultipleDownLoader downLoader;
	
	
	public MultipleDownLoadThread(MultipleDownLoader multipleDownLoader,
			URL url, File saveFile, int blcok, Integer downLength, int threadID) {
		this.downLoader=multipleDownLoader;
		this.block=blcok;
		this.saveFile=saveFile;
		this.threadID=threadID;
		this.downLength=downLength;
		this.downLoadUrl=url;
	}
	
	@Override
	public void run(){
		if(this.downLength<this.block){//该线程还没下完
			try {
				//----------------不懂----------------------------
				HttpURLConnection http=(HttpURLConnection) downLoadUrl.openConnection();
				http.setConnectTimeout(5 * 1000);
				http.setRequestMethod("GET");
				http.setRequestProperty("Accept", "image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
				http.setRequestProperty("Accept-Language", "zh-CN");
				http.setRequestProperty("Charset", "UTF-8");
				http.setRequestProperty("Referer", downLoadUrl.toString());
				//------------------------------------------------------------
				//确定开始位置
				int startPos=this.block*(this.threadID-1)+this.downLength;
				//结束位置
				int endPos=this.block*this.threadID-1;
				//下载的范围
				http.setRequestProperty("Range", "bytes="+startPos+"-"+endPos);
				
				http.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");
				http.setRequestProperty("Connection", "Keep-Alive");
				
				InputStream input=http.getInputStream();
				byte[] buffer=new byte[1024];
				int offset;
				print("Thread "+this.threadID+" start download from position "+startPos);
				RandomAccessFile threadFile=new RandomAccessFile(this.saveFile, "rwd");
				//找到起点
				threadFile.seek(startPos);
				while((offset=input.read(buffer, 0, 1024))!=-1){//每次从流中0位置开始 最多到1024 读出来存在buffer
					threadFile.write(buffer, 0, offset);//每次从buffer的0位置开始 到offset位置的内容写入文件
					this.downLength+=offset;
					this.downLoader.update(threadID, downLength);
					this.downLoader.saveLogFile();
					this.downLoader.append(offset);
				}
				threadFile.close();
				input.close();
				print("Thread "+this.threadID+" download finished");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				this.downLength=-1;
				print("Thread "+this.threadID+" : "+e);
			}
			
		}
	}
	
	private static void print(String log){
		Log.i(TAG,log);
	}
	
	/*
	 * 该线程下载是否完成
	 */
	public boolean isFinish(){
		return this.isFinish;
	}
	/*
	 * 该线程已下载的长度
	 */
	public long getDownLength(){
		return this.downLength;
	}
}
