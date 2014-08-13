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
	private int downloadSize=0;//�����ص��ܳ���
	private int fileSize;//Դ�ļ�����
	private MultipleDownLoadThread[] threads;
	private File saveFile;//�������ձ����ļ�
	private Map<Integer,Integer> data=new ConcurrentHashMap<Integer, Integer>();
	private int blcok;//ÿ���߳�Ҫ���صĳ���
	private String downloadUrl;
	
	public int getThreadSize(){
		return threads.length;
	}
	
	public int getFileSize(){
		return this.fileSize;
	}
	//���������ܳ���
	protected synchronized void append(int size){
		downloadSize+=size;
	}
	//�����߳������������λ��
	protected void update(int threadid,int pos){
		this.data.put(threadid, pos);
	}
	//�����¼�ļ���֧�ֶϵ�������
	protected synchronized void saveLogFile(){
		this.fileService.update(this.downloadUrl, this.data);
	}
	/*
	 * ����������
	 * @para downloadUrl ����·��
	 * @para fileSaveDir �ļ�����Ŀ¼
	 * @para threadNum ���ص��߳���
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
			/*------------------�����Httpͷ�������û�����----------*/
			conn.setRequestProperty("Accept", "image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
			conn.setRequestProperty("Accept-Language", "zh-CN");
			conn.setRequestProperty("Referer", downloadUrl); 
			conn.setRequestProperty("Charset", "UTF-8");
			conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");
			conn.setRequestProperty("Connection", "Keep-Alive");
			/*----------------------------------------------------------*/
			conn.connect();
			
			printResponseHeader(conn);
			//���ӳɹ�
			if(conn.getResponseCode()==200	){
				//ͨ��HttpUrlConnection��ȡ�ļ��ܳ���
				this.fileSize=conn.getContentLength();
				System.out.println(this.fileSize);
				if(this.fileSize<=0) throw new RuntimeException("Unknow file size ");
				String filename=getFileName(conn);
				System.out.println(filename);
				//�½����ش洢�ļ�
				this.saveFile=new File(fileSaveDir,filename);
				//��ȡ���ݿ����صļ�¼
				Map<Integer,Integer> logData=this.fileService.getEachDownloadedLength(downloadUrl);
				if(logData.size()>0){//�ж��Ƿ��Ƕϵ�����
					for(Map.Entry<Integer, Integer> entry : logData.entrySet()){
						this.data.put(entry.getKey()	, entry.getValue());
					}
				}
				//���������ļ��ֿ�
				this.blcok=(this.fileSize%this.threads.length)==0? this.fileSize/this.threads.length : this.fileSize/this.threads.length+1;
				//���if���� ��ʾ֮ǰ�����ز��� �����Ƕϵ����� ����������֮ǰ���ص��ܳ���
				if(this.data.size()==this.threads.length){
					for(int i=0;i<threads.length;i++)
						this.downloadSize+=data.get(i+1);//�̱߳�Ŵ�1��ʼ
				}
				print("�����س���"+this.downloadSize);
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
	 * ͨ�����ӻ�ȡ�ļ���
	 */
	private String getFileName(HttpURLConnection conn) {
		String fileName=null;
		fileName=this.downloadUrl.substring(this.downloadUrl.lastIndexOf('/')+1);
		System.out.println("fileName in getFileName :"+fileName);
		/*-----------------������----------------*/
//		if(fileName==null || "".equals(fileName.trim())){
//			for(int i=0;;i++){
//				String mine=conn.getHeaderField(i);
//				if(mine==null) break;
//				if("content-disposition".equals(conn.getHeaderFieldKey(i).toLowerCase())){
//					Matcher m=Pattern.compile(".*filename=(.*)").matcher(mine.toLowerCase());
//					if(m.find()) return m.group(1);
//				}
//			}
//			fileName=UUID.randomUUID()+".temp";//Ĭ��һ���ļ���
//	/*----------------------------------------------*/
//		}
		return fileName;
	}

	/**
	 *  ��ʼ�����ļ�
	 * @param listener �������������ı仯,�������Ҫ�˽�ʵʱ���ص�����,��������Ϊnull
	 * @return �������ļ���С
	 * @throws Exception
	 */
	public int download(MultipleDownLoadProgressListener listener) throws Exception{
		
		try {
			RandomAccessFile randOut=new RandomAccessFile(saveFile, "rw");
			//���ñ����ļ��Ĵ�С
			if(this.fileSize>0) randOut.setLength(this.fileSize);
			URL url=new URL(this.downloadUrl);
			//�жϵ�ǰ���ݿ��Ƿ�clear
			if(this.data.size()!=this.threads.length){
				//�������
				data.clear();
				for(int i=0;i<this.threads.length;i++){
					data.put(i+1, 0);
				}
			}
			
			for(int i=0;i<this.threads.length;i++){
				int downlength=this.data.get(i+1);
				if(downlength<this.blcok&&this.downloadSize<this.fileSize){
					//˵�����߳�����δ��ɣ�����/��ʼ����
					this.threads[i]=new MultipleDownLoadThread(this,url,this.saveFile,this.blcok,this.data.get(i+1),i+1);
				    this.threads[i].setPriority(10);
				    this.threads[i].start();
				}else{
					//���߳����������
					this.threads[i]=null;
				}
			}
			//��������ǰSQLite�еļ�¼����
			this.fileService.save(this.downloadUrl, this.data);
		    boolean notFinish=true;
			while(notFinish){//ѭ���ж����������Ƿ���ɣ�ֱ�����������������
				Thread.sleep(900);
				notFinish=false;//���������
				for(int i=0;i<this.threads.length;i++){
					if(this.threads[i]!=null&&!this.threads[i].isFinish()){
						//���費���� ����δ���
						notFinish=true;
						if(this.downloadSize==-1){
							//����ʧ�� ��������
							this.threads[i]=new MultipleDownLoadThread(this,url,this.saveFile,this.blcok,this.data.get(i+1),i+1);
							threads[i].setPriority(10);
							threads[i].start();
						}
					}
				}
				if(listener!=null){//���ؽ��ȼ���
					listener.onDownLoadSize(this.downloadSize);
				}
				
			}
			this.fileService.delete(this.downloadUrl);//������ɺ� ɾ��SQLite�е����ؼ�¼
		} catch (Exception e) {
			// TODO: handle exception
			print(e.toString());
			throw new Exception("download fail");
		}
		return this.downloadSize;
	}
	
	/*
	 *���Http��Ӧͷ�ֶ�
	 *@param http
	 *@return map
	 */
	private static Map<String,String> getResponseHeader(HttpURLConnection conn) {
		Map<String,String> header=new LinkedHashMap<String,String>();//LinkedHashMap�������ʱ˳�������ʱ����һ��
		for(int i=0;;i++){
			String content=conn.getHeaderField(i);
			if(content==null) break;
			header.put(conn.getHeaderFieldKey(i), content);
		}
		return header;
	}

	/*
	 * ��ӡHttpͷ�ֶ�
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
	 * ��ӡ��־
	 */
	private static void print(String log){
		Log.i(TAG, log);
	}
}
