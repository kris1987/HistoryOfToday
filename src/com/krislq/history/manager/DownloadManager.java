package com.krislq.history.manager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.widget.ImageView;

import com.krislq.history.Constants;
import com.krislq.history.util.HistoryUtil;
import com.krislq.history.util.ImageUtil;
import com.krislq.history.util.L;
import com.krislq.history.util.ThreadPoolUtil;
/**
 * 
 * @author <a href="mailto:kris1987@qq.com">Kris.lee</a>
 * @website www.krislq.com
 * @date Nov 20, 2012
 * @version 1.0.0
 *
 */
public class DownloadManager {
	/**
	 * max download image thread.
	 */
	public static final int MAX_THREAD_LENGTH = 5;
	
	private static final int 	STATUS_NORMAL	=0;
	private static final int 	STATUS_PAUSE	=1;
	private static final int 	STATUS_STOP		=2;
	private int status = STATUS_NORMAL;
	
	private Context context;
	private Handler handler ;
	/**
	 * downloading or will be downloaded url map .
	 */
	private Map<String,Set<ImageView>> 	downloadMaps = null;
	/**
	 * downloadind thread map
	 */
	private Map<String,Thread>			threadMap = null;
	/**
	 * pending url list
	 */
	private List<String>				peddingList = null;
	
	private Map<Object,SoftReference<Bitmap>> bitmapCache = null;

	public DownloadManager(Context context, Handler handler)
	{
		downloadMaps = new HashMap<String, Set<ImageView>>();
		threadMap = new HashMap<String,Thread>();
		peddingList = new ArrayList<String>();
		bitmapCache = new HashMap<Object, SoftReference<Bitmap>>();
		
		this.context =context;
		if(handler ==null)
		{
			throw new IllegalArgumentException("handler is null");
		}
		this.handler = handler;
	}
	/**
	 * stop to start a new  download thread. but not pause or interrupt�?he downloading thread.
	 */
	public void pause()
	{
		status = STATUS_PAUSE;
	}
	/**
	 * resume the download manager.<br>
	 * if there are pending urls , will begin new threads to download. or accept to be added the new url .
	 */
	public void resume()
	{
		status = STATUS_NORMAL;
		//if the current thread number is less than the max thread length,
		//so need to check pending list, if there are any pending urls,so need start new thread to download them
		L.i("DownloadManager#current thread size:"+threadMap.size());
		if(threadMap.size() < MAX_THREAD_LENGTH)
		{ 
			int threadCount = MAX_THREAD_LENGTH - threadMap.size();
			for(int i=0;i<threadCount;i++)
			{
				//if there are free thread ,so need to get one of the pending url.
				synchronized (peddingList) {
					if(peddingList.size() >0)
					{
						String purl = peddingList.get(0);
						DownLoadThread thread = new DownLoadThread(purl);
						thread.start();
						synchronized(threadMap)
						{
							threadMap.put(purl, thread);
						}
						//if begin a new thread to download the pending url, will remove the url from the pending list
						peddingList.remove(0);
					}
				}
			}
		}
	}
	
	public void destory()
	{
		status = STATUS_STOP;
		if(threadMap!=null)
		{
			Iterator<String> iterator =  threadMap.keySet().iterator();
			while (iterator.hasNext()) {
				String threadName = iterator.next();
				Thread thread = threadMap.get(threadName);
				if(thread!=null)
				{
					try
					{
						thread.interrupt();
					}catch(Exception e)
					{
						
					}
				}
			}
			threadMap.clear();
//			threadMap = null;
		}
		if(downloadMaps!=null)
		{
			downloadMaps.clear();
//			downloadMaps = null;
		}
		if(peddingList!=null)
		{
			peddingList.clear();
//			peddingList = null;
		}
		if(bitmapCache!=null)
		{
			//recycle the bitmap
			Iterator<Object> iterator = bitmapCache.keySet().iterator();
			while (iterator.hasNext()) {
				Object key = iterator.next();
				SoftReference<Bitmap> srBitmap = bitmapCache.get(key);
				if(srBitmap!=null)
				{
					Bitmap bitmap = srBitmap.get();
					if(bitmap!=null)
					{
						if(!bitmap.isRecycled())
						{
							bitmap.recycle();
						}
					}
				}
			}
			bitmapCache.clear();
//			bitmapCache = null;
		}
	}
	/**
	 * if the url is downloaded , need to remove from the download map and size map .
	 * @param url
	 */
	public void removeUrl(String url)
	{
		synchronized (downloadMaps) {
			downloadMaps.remove(url);
		}
	}
	
	public Context getContext() {
		return context;
	}
	public void setContext(Context context) {
		this.context = context;
	}
	public Handler getHandler() {
		return handler;
	}
	public void setHandler(Handler handler) {
		this.handler = handler;
	}
	/**
	 * return the total url size, maybe this url is downloading or pending .
	 * @return
	 */
	public int getUrlSize()
	{
		return downloadMaps.size();
	}
	/**
	 * return the current thread size
	 * @return
	 */
	public int getThreadSize()
	{
		return threadMap.size();
	}
	/**
	 * return the pending url size
	 * @return
	 */
	public int getPendingSize()
	{
		return peddingList.size();
	}
	/**
	 * add a url to the manager
	 * @param url			need to download url
	 * @param imageView		the imageview need to be paint after the url have downloaded
	 * @param defaultRes	default image resource
	 * @param type		the type of the url,just used for the current url.
	 */
	public synchronized void add(final String url,final ImageView imageView,final int defaultRes)
	{
		ThreadPoolUtil.execute(new Runnable() {
			@Override
			public void run() {
			//if the url is empty, so set the default resource
			if(HistoryUtil.isEmpty(url))
			{
				setDefaultImage(imageView, defaultRes);
				return;
			}
			//if the image view is empty, return directly .no download value
			if(imageView == null)
			{
				return;
			}
			//search in the cache, and search the local cache file, at the last to download from web
			if(containsCache(url))
			{
				Bitmap bitmap = getCahceBitmap(url);
				if(bitmap!=null)
				{
					L.i("DownloadManager#have the cache image:"+url);
					setImage(imageView, bitmap);
					return;
				}
			}
	
			File cacheFile = new File(Constants.CACHE_DIR + File.separator+HistoryUtil.toCharString(url));
			//if exist in the local cache file,get it directly
			if (cacheFile.exists()) {
				L.v("DownloadManager#local image exist");
				//decode the file 
				decodeFromFile(imageView, cacheFile.getAbsolutePath(), url);
			} else {
				//begin download, need to set the default image resource .
				setDefaultImage(imageView, defaultRes);
	
				//if this url is existed in the download urls.just save the image view, and wait.
				//will no the start a new thread.
				if(downloadMaps.containsKey(url))
				{
					L.v("DownloadManager#exist download ,only wait:"+imageView.toString());
					Set<ImageView> set = downloadMaps.get(url);
					if(set == null)
					{
						set = new HashSet<ImageView>();
					}
					synchronized(set)
					{
						//if the imageview have existed in the set, the set will remove the�?xtra
						set.add(imageView);
					}
				}
				else//if the url is a new url . will add to the download map.and start a new thread if there is a free thread.
				{
					L.v("DownloadManager#start a thread to download image");
					Set<ImageView> set = new HashSet<ImageView>();
					set.add(imageView);
					synchronized (downloadMaps) {
						downloadMaps.put(url, set);
					}
					addThread(url);
				}
	
			}

		
			}
		});
		
	}
	
	private void setDefaultImage(ImageView imageView, int defaultRes)
	{
		if(defaultRes >0)
		{
			if(containsCache(defaultRes) )
			{
				Bitmap bitmap = getCahceBitmap(defaultRes);
				if(bitmap!=null)
				{
					setImage(imageView, bitmap);
				}
				else
				{
					decodeFromResource(imageView, defaultRes);
				}
			}
			else
			{
				//encode the bitmap
				decodeFromResource(imageView, defaultRes);
			}
		}
	}
	
	/**
	 * add a thread. <br>
	 * if the download thread size is less than MAX_THREAD_LENGTH and the manager is normal(not pause, or destroy),<br>
	 * will start a new thread to download the url;
	 * @param url
	 */
	private  void addThread(String url)
	{
		if(threadMap.size() < MAX_THREAD_LENGTH && status == STATUS_NORMAL)
		{
			L.i("DownloadManager#downloading thread is less than MAX_THREAD:"+url);
			DownLoadThread thread = new DownLoadThread(url);
			thread.start();
			synchronized(threadMap)
			{
				threadMap.put(url, thread);
			}
		}
		else
		{
			synchronized(peddingList)
			{
				L.i("DownloadManager#Have enough download thread.only pending");
				peddingList.add(url);
			}	
		}
	}
	/**
	 * when one of the url have downloaded.will remove thread thread, and remove the url from the cache.<br>
	 * then start a new thread if the manager status is normal
	 * @param url
	 */
	private void removeThread(String url) {
		L.e("removeThread:"+url);
		threadMap.remove(url);
		removeUrl(url);
		synchronized(peddingList) {
			if(peddingList.size() >0 && status == STATUS_NORMAL) {
				L.v("DownloadManager#pending size:"+peddingList.size());
				String pUrl = peddingList.get(0);
				DownLoadThread thread = new DownLoadThread(pUrl);
				thread.start();
				synchronized(threadMap)
				{
					threadMap.put(pUrl, thread);
				}
				//remove the url from the pending list
				peddingList.remove(0);
			}
			else {
				L.v("DownloadManager#no pending list");
			}
		}
	}
	/**
	 * decode the bitmap from the resource and put it to the cache
	 * @param imageView
	 * @param resource
	 */
	private void decodeFromResource(final ImageView imageView,final int resource) {
		ThreadPoolUtil.execute(new Runnable() {
			
			@Override
			public void run() {
				final Bitmap bitmap =  BitmapFactory.decodeResource(context.getResources(), resource);
				addCacheBitmap(resource, bitmap);
				setImage(imageView, bitmap);
			}
		});
	}
	private void setImage(final ImageView imageView,final Bitmap bitmap) {
		if(handler!=null) {
			handler.post(new Runnable() {
				@Override
				public void run() {
					imageView.setImageBitmap(bitmap);
					imageView.requestLayout();
				}
			});
		}
	}
	private void decodeFromFile(final ImageView imageView,final String path,final String url)
	{
		ThreadPoolUtil.execute(new Runnable() {
			@Override
			public void run() {
				final Bitmap bitmap = BitmapFactory.decodeFile(path);
				addCacheBitmap(url, bitmap);
				if(handler!=null)
				{
					setImage(imageView, bitmap);
				}
			}
		});
	}
	/**
	 * check whether contained the url in the cache or not
	 * @param key
	 * @return
	 */
	public boolean containsCache(Object key)
	{
		return bitmapCache.containsKey(key);
	}
	/**
	 * get the cache bitmap from the bitmap cache map
	 * @param key
	 * @return
	 */
	public Bitmap getCahceBitmap(Object key)
	{
		SoftReference<Bitmap> srBitmap = bitmapCache.get(key);
		if(srBitmap!=null)
		{
			Bitmap bitmap = srBitmap.get();
			if(bitmap!=null)
			{
				return bitmap;
			}
		}
		return null;
	}
	/**
	 * add a cache bitmap to the cache map
	 * @param key
	 * @param bitmap
	 */
	public void addCacheBitmap(Object key,Bitmap bitmap)
	{
		//if not contains this key , so add to the hashmap
		if(!bitmapCache.containsKey(key))
		{
			SoftReference<Bitmap> srBitmap = new SoftReference<Bitmap>(bitmap);
			bitmapCache.put(key, srBitmap);
		}
	}
	
	class DownLoadThread extends Thread
	{
		private String url;
		public DownLoadThread(String url)
		{
			super(url);
			this.url = url;
			
		}
		public void run()
		{

			HttpEntity entity = null;
			InputStream is = null;
			try {
				HttpClient client = new DefaultHttpClient();
				L.i("DownloadManager#"+url);
				HttpGet get = new HttpGet(url);
				HttpResponse response = client.execute(get);
				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					entity = response.getEntity();
					is = entity.getContent();
					Bitmap bitmap = BitmapFactory.decodeStream(is);
					if(bitmap!=null)
					{
						L.i("DownloadManager#success#"+url);
						//add to the bitmap cache  map
						addCacheBitmap(url, bitmap);
						
						//get the sounded corner bitmap
						File cacheFile = new File(Constants.CACHE_DIR + File.separator+HistoryUtil.toCharString(url));
						ImageUtil.writeBitmap2File(bitmap, cacheFile);
						
						//notify the imageview
						notifyView(url, bitmap);
						return;
					}
					L.i("DownloadManager#failture#"+url);
				}
				else
				{
					L.e("DownloadManager#error Code:"+response.getStatusLine().getStatusCode());
				}
			} catch (ClientProtocolException e) {
				L.e("DownloadManager#ClientProtocolException url:"+url, e);
			} catch (Exception e) {
				L.e("DownloadManager#exception url:"+url, e);
			} finally {
				//下载完成了后，就把当前的线程移除�?
				removeThread(url);
				if (entity != null) {
					try {
						entity.consumeContent();
					} catch (IOException e) {
					}
				}
				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
					}
				}
			}
		}
		private void notifyView(final String url,final Bitmap bitmap) {
			Set<ImageView> set = downloadMaps.get(url);
			if(set == null) {
				L.e("download Set<ImageView> is null");
				return;
			}
			Iterator<ImageView> iterator =  set.iterator();
			while (iterator.hasNext()) {
				L.e("while Notify image View:"+url);
				final ImageView view = iterator.next();
				setImage(view, bitmap);
//				view.post(new Runnable() {
//					@Override
//					public void run() {
//						L.e("Notify image View:"+url);
//						view.setImageBitmap(bitmap);
//					}
//				});
			}
		}
	}
	class ImageSize 
	{
		int width;
		int height;
		public ImageSize(int width,int height)
		{
			this.width = width;
			this.height = height;
		}
	}
}
