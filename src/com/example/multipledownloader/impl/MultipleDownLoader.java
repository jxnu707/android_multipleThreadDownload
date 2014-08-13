package com.example.multipledownloader.impl;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import android.content.Context;
import android.util.Log;

import com.example.multipledownloader.db.FileService;

public class MultipleDownLoader {

	private static final String TAG="MultipleDownLoader";
	
	private Context context;
	private FileService fileService;
	private int downloadSize=0;//已下载的总长度
	private int fileSize;//源文件长度
	private MultipleDownLoadThread[] threads;
	private File saveFile;//本地最终保存文件
	private Map<Integer,Integer> data=new ConcurrentHashMap<Integer, Integer>();
	private int blcok;//每条线程要下载的长度
	private String downloadUrl;
	
	public int getThreadSize(){
		return threads.length;
	}
	
	public int getFileSize(){
		return this.fileSize;
	}
	//计算下载总长度
	protected synchronized void append(int size){
		downloadSize+=size;
	}
	//更新线程下载最后下载位置
	protected void update(int threadid,int pos){
		this.data.put(threadid, pos);
	}
	//保存记录文件（支持断点续传）
	protected synchronized void saveLogFile(){
		this.fileService.update(this.downloadUrl, this.data);
	}
	/*
	 * 构建下载器
	 * @para downloadUrl 下载路径
	 * @para fileSaveDir 文件保存目录
	 * @para threadNum 下载的线程数
	 */
	public MultipleDownLoader(Context context,String downloadUrl,File fileSaveDir,int threadNum) {
		// TODO Auto-generated constructor stub
		this.context=context;
		this.downloadUrl=downloadUrl;
		this.fileService=new FileService(this.context);
		try {
			URL url=new URL(downloadUrl);
			if(!fileSaveDir.isDirectory())  fileSaveDir.mkdirs();
			this.threads=new MultipleDownLoadThread[threadNum];
		
			HttpURLConnection conn=(HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(5*1000);
			/*------------------下面的Http头参数设置还不懂----------*/
			conn.setRequestProperty("Accept", "image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
			conn.setRequestProperty("Accept-Language", "zh-CN");
			conn.setRequestProperty("Referer", downloadUrl); 
			conn.setRequestProperty("Charset", "UTF-8");
			conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");
			conn.setRequestProperty("Connection", "Keep-Alive");
			/*----------------------------------------------------------*/
			conn.connect();
			
			printResponseHeader(conn);
			//链接成功
			if(conn.getResponseCode()==200	){
				//通过HttpUrlConnection获取文件总长度
				this.fileSize=conn.getContentLength();
				System.out.println(this.fileSize);
				if(this.fileSize<=0) throw new RuntimeException("Unknow file size ");
				String filename=getFileName(conn);
				System.out.println(filename);
				//新建本地存储文件
				this.saveFile=new File(fileSaveDir,filename);
				//获取数据库下载的记录
				Map<Integer,Integer> logData=this.fileService.getEachDownloadedLength(downloadUrl);
				if(logData.size()>0){//判断是否是断点续传
					for(Map.Entry<Integer, Integer> entry : logData.entrySet()){
						this.data.put(entry.getKey()	, entry.getValue());
					}
				}
				//将待下载文件分块
				this.blcok=(this.fileSize%this.threads.length)==0? this.fileSize/this.threads.length : this.fileSize/this.threads.length+1;
				//这个if成立 表示之前已下载部分 现在是断点续传 所以先设置之前下载的总长度
				if(this.data.size()==this.threads.length){
					for(int i=0;i<threads.length;i++)
						this.downloadSize+=data.get(i+1);//线程编号从1开始
				}
				print("已下载长度"+this.downloadSize);
			}else{
				throw new RuntimeException("server no response");
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException("cannot connect this url");
		}
	}

	/*
	 * 通过链接获取文件名
	 */
	private String getFileName(HttpURLConnection conn) {
		String fileName=null;
		fileName=this.downloadUrl.substring(this.downloadUrl.lastIndexOf('/')+1);
		System.out.println("fileName in getFileName :"+fileName);
		/*-----------------不懂的----------------*/
//		if(fileName==null || "".equals(fileName.trim())){
//			for(int i=0;;i++){
//				String mine=conn.getHeaderField(i);
//				if(mine==null) break;
//				if("content-disposition".equals(conn.getHeaderFieldKey(i).toLowerCase())){
//					Matcher m=Pattern.compile(".*filename=(.*)").matcher(mine.toLowerCase());
//					if(m.find()) return m.group(1);
//				}
//			}
//			fileName=UUID.randomUUID()+".temp";//默认一个文件名
//	/*----------------------------------------------*/
//		}
		return fileName;
	}

	/**
	 *  开始下载文件
	 * @param listener 监听下载数量的变化,如果不需要了解实时下载的数量,可以设置为null
	 * @return 已下载文件大小
	 * @throws Exception
	 */
	public int download(MultipleDownLoadProgressListener listener) throws Exception{
		
		try {
			RandomAccessFile randOut=new RandomAccessFile(saveFile, "rw");
			//设置本地文件的大小
			if(this.fileSize>0) randOut.setLength(this.fileSize);
			URL url=new URL(this.downloadUrl);
			//判断当前数据库是否clear
			if(this.data.size()!=this.threads.length){
				//清空数据
				data.clear();
				for(int i=0;i<this.threads.length;i++){
					data.put(i+1, 0);
				}
			}
			
			for(int i=0;i<this.threads.length;i++){
				int downlength=this.data.get(i+1);
				if(downlength<this.blcok&&this.downloadSize<this.fileSize){
					//说明该线程下载未完成，继续/开始下载
					this.threads[i]=new MultipleDownLoadThread(this,url,this.saveFile,this.blcok,this.data.get(i+1),i+1);
				    this.threads[i].setPriority(10);
				    this.threads[i].start();
				}else{
					//该线程下载完成了
					this.threads[i]=null;
				}
			}
			//保存下载前SQLite中的记录数据
			this.fileService.save(this.downloadUrl, this.data);
		    boolean notFinish=true;
			while(notFinish){//循环判断所有下载是否完成，直至所有下载完成跳出
				Thread.sleep(900);
				notFinish=false;//假设完成了
				for(int i=0;i<this.threads.length;i++){
					if(this.threads[i]!=null&&!this.threads[i].isFinish()){
						//假设不成立 下载未完成
						notFinish=true;
						if(this.downloadSize==-1){
							//下载失败 重新下载
							this.threads[i]=new MultipleDownLoadThread(this,url,this.saveFile,this.blcok,this.data.get(i+1),i+1);
							threads[i].setPriority(10);
							threads[i].start();
						}
					}
				}
				if(listener!=null){//下载进度监听
					listener.onDownLoadSize(this.downloadSize);
				}
				
			}
			this.fileService.delete(this.downloadUrl);//下载完成后 删除SQLite中的下载记录
		} catch (Exception e) {
			// TODO: handle exception
			print(e.toString());
			throw new Exception("download fail");
		}
		return this.downloadSize;
	}
	
	/*
	 *获得Http响应头字段
	 *@param http
	 *@return map
	 */
	private static Map<String,String> getResponseHeader(HttpURLConnection conn) {
		Map<String,String> header=new LinkedHashMap<String,String>();//LinkedHashMap迭代输出时顺序与存入时保持一致
		for(int i=0;;i++){
			String content=conn.getHeaderField(i);
			if(content==null) break;
			header.put(conn.getHeaderFieldKey(i), content);
		}
		return header;
	}

	/*
	 * 打印Http头字段
	 * @param http
	 */
	public static void printResponseHeader(HttpURLConnection http){
		Map<String,String> header=new LinkedHashMap<String,String>();
		for(Map.Entry<String, String> entry:header.entrySet()){
			String key=entry.getKey()!=null? entry.getKey()+": ":"";
			print(key+entry.getValue());
		}
	}
	
	/*
	 * 打印日志
	 */
	private static void print(String log){
		Log.i(TAG, log);
	}
}
