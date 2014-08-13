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
		if(this.downLength<this.block){//���̻߳�û����
			try {
				//----------------����----------------------------
				HttpURLConnection http=(HttpURLConnection) downLoadUrl.openConnection();
				http.setConnectTimeout(5 * 1000);
				http.setRequestMethod("GET");
				http.setRequestProperty("Accept", "image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
				http.setRequestProperty("Accept-Language", "zh-CN");
				http.setRequestProperty("Charset", "UTF-8");
				http.setRequestProperty("Referer", downLoadUrl.toString());
				//------------------------------------------------------------
				//ȷ����ʼλ��
				int startPos=this.block*(this.threadID-1)+this.downLength;
				//����λ��
				int endPos=this.block*this.threadID-1;
				//���صķ�Χ
				http.setRequestProperty("Range", "bytes="+startPos+"-"+endPos);
				
				http.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");
				http.setRequestProperty("Connection", "Keep-Alive");
				
				InputStream input=http.getInputStream();
				byte[] buffer=new byte[1024];
				int offset;
				print("Thread "+this.threadID+" start download from position "+startPos);
				RandomAccessFile threadFile=new RandomAccessFile(this.saveFile, "rwd");
				//�ҵ����
				threadFile.seek(startPos);
				while((offset=input.read(buffer, 0, 1024))!=-1){//ÿ�δ�����0λ�ÿ�ʼ ��ൽ1024 ����������buffer
					threadFile.write(buffer, 0, offset);//ÿ�δ�buffer��0λ�ÿ�ʼ ��offsetλ�õ�����д���ļ�
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
	 * ���߳������Ƿ����
	 */
	public boolean isFinish(){
		return this.isFinish;
	}
	/*
	 * ���߳������صĳ���
	 */
	public long getDownLength(){
		return this.downLength;
	}
}
