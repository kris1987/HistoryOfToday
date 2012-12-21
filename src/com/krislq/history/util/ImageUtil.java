package com.krislq.history.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;

/**
 * 
 * @author <a href="mailto:kris1987@qq.com">Kris.lee</a>
 * @website www.krislq.com
 * @date Nov 20, 2012
 * @version 1.0.0
 *
 */
public class ImageUtil {
	public static Bitmap getRightShadowBitmap(Bitmap bitmap,int shadow,int bgColor)
	{
		if(bitmap==null)
			return null;
		int width = bitmap.getWidth()+shadow;
		int height = bitmap.getHeight();
		Bitmap output = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		Canvas canvas = new Canvas(output);
		
		Paint paint = new Paint();
		Rect rect = new Rect(0, 0, width-shadow, height);
		RectF rectF = new RectF(rect);

		paint.setAntiAlias(true);
		paint.setColor(bgColor);
		
		canvas.drawColor(bgColor);
		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		// �趨��Ӱ(���, X ��λ��, Y ��λ��, ��Ӱ��ɫ)  
		paint.setShadowLayer(shadow, 0, 0, Color.BLACK);
		
		canvas.drawRoundRect(rectF, 0, 0, paint);
		canvas.drawBitmap(bitmap, rect, rect, paint);

		return output;
	}
	
	public static Bitmap getShadowBitmap(Bitmap bitmap,int shadow,int bgColor)
	{
		if(bitmap==null)
			return null;
		int width = bitmap.getWidth()+shadow*2;
		int height = bitmap.getHeight()+shadow*2;
		int dx = shadow/2;
		int dy = 2*shadow/3;
		Bitmap output = Bitmap.createBitmap(width+5, height+5, Config.ARGB_8888);
		Canvas canvas = new Canvas(output);
		
		Paint paint = new Paint();
		Rect rect = new Rect(shadow-dx, shadow-dy, width-shadow-dx, height-shadow-dy);
		RectF rectF = new RectF(rect);

		paint.setAntiAlias(true);
		paint.setColor(bgColor);
		
		canvas.drawColor(bgColor);
		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		// �趨��Ӱ(���, X ��λ��, Y ��λ��, ��Ӱ��ɫ)  
		paint.setShadowLayer(shadow, dx, dy, Color.argb(0xff, 0xdd, 0xdd, 0xdd));
		
		canvas.drawRoundRect(rectF, 0, 0, paint);
		canvas.drawBitmap(bitmap, shadow-dx, shadow-dy, paint);

		return output;
	}
	/**
	 * get the round corner of image
	 * 
	 * @param bitmap
	 * @return
	 */
	public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, float roundPx) {
		if (bitmap == null) {
			return null;
		}
		Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(output);

		final int color = 0xff424242;
		final Paint paint = new Paint();
		final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
		final RectF rectF = new RectF(rect);

		paint.setAntiAlias(true);
		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(color);
		canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(bitmap, rect, rect, paint);

		return output;
	}

	/***
	 * scaling the image
	 * 
	 * @param bitMap
	 *            source image resources
	 * 
	 * @param newWidth
	 *            new width of the image
	 * 
	 * @param newHeight
	 *            new height of the image
	 * 
	 * @return return the image after scaling .if the input bitmap is null,return null;
	 */

	public static Bitmap zoomImage(Bitmap bitMap, int newWidth, int newHeight) {

		// File file = new File("");
		// Bitmap b = B
		if (bitMap == null) {
			return null;
		}
		// ��ȡ���ͼƬ�Ŀ�͸�
		int width = bitMap.getWidth();
		int height = bitMap.getHeight();
		// ��������ͼƬ�õ�matrix����
		Matrix matrix = new Matrix();
		// ���������ʣ��³ߴ��ԭʼ�ߴ�
		float scaleWidth = ((float) newWidth) / width;
		float scaleHeight = ((float) newHeight) / height;
		// �³ߴ����ԭʼ�ߴ�������
		if (scaleWidth > 1.0f || scaleHeight > 1.0f) {
			return bitMap;
		}
		// ����ͼƬ����
		matrix.postScale(scaleWidth, scaleHeight);
		Bitmap bitmap = Bitmap.createBitmap(bitMap, 0, 0, width, height, matrix, true);
		return bitmap;

	}

	public static Bitmap decodeImage(Context context,Uri uri) throws FileNotFoundException {
		BitmapFactory.Options op = new BitmapFactory.Options();
		// op.inSampleSize = 8;
		op.inJustDecodeBounds = true;
		// Bitmap pic = BitmapFactory.decodeFile(imageFilePath,
		// op);//������������Ժ�op�е�outWidth��outHeight����ֵ��
		// ����ʹ����MediaStore�洢���������URI��ȡ����������ʽ
		Bitmap bitmap = BitmapFactory.decodeStream(context  
                .getContentResolver().openInputStream(uri),  
                null, op);
		int screenWidth = HistoryUtil.getSceenWidth(context);
		int screenHeight = HistoryUtil.getSceenHeight(context);
		// ���������ʣ��³ߴ��ԭʼ�ߴ�
		op.inSampleSize = computeSampleSize(op, -1, screenWidth*screenHeight);
		op.inJustDecodeBounds = false; //ע�����һ��Ҫ����Ϊfalse����Ϊ�������ǽ�������Ϊtrue����ȡͼƬ�ߴ���
		bitmap = BitmapFactory.decodeStream(context  
                .getContentResolver().openInputStream(uri),  
                null, op);
		return bitmap;
	}
	
	public static void writeBitmap2File(Bitmap bitmap,File file)
	{
		if(bitmap==null || file ==null) return;
		FileOutputStream fos =null;
		try {
			File parent = file.getParentFile();
			if(parent!=null && !parent.exists())
			{
				HistoryUtil.initExternalDir(false);
			}
			//save to the local file
			fos = new FileOutputStream(file);
			bitmap.compress(CompressFormat.PNG, 100, fos);
			fos.flush();
		} catch (Exception e) {
			L.e("write the bitmap to file exception", e);
		}
		finally
		{
			if(fos!=null)
			{
				try {
					fos.close();
				} catch (Exception e) {
					
				}
			}
		}
	}
	public static int computeSampleSize(BitmapFactory.Options options,
	        int minSideLength, int maxNumOfPixels) {
	    int initialSize = computeInitialSampleSize(options, minSideLength,
	            maxNumOfPixels);
	    int roundedSize;
	    if (initialSize <= 8) {
	        roundedSize = 1;
	        while (roundedSize < initialSize) {
	            roundedSize <<= 1;
	        }
	    } else {
	        roundedSize = (initialSize + 7) / 8 * 8;
	    }
	 
	    return roundedSize;
	}
	 
	private static int computeInitialSampleSize(BitmapFactory.Options options,
	        int minSideLength, int maxNumOfPixels) {
	    double w = options.outWidth;
	    double h = options.outHeight;
	 
	    int lowerBound = (maxNumOfPixels == -1) ? 1 :
	            (int) Math.ceil(Math.sqrt(w * h / maxNumOfPixels));
	    int upperBound = (minSideLength == -1) ? 128 :
	            (int) Math.min(Math.floor(w / minSideLength),
	            Math.floor(h / minSideLength));
	    if (upperBound < lowerBound) {
	        // return the larger one when there is no overlapping zone.
	        return lowerBound;
	    }
	 
	    if ((maxNumOfPixels == -1) &&
	            (minSideLength == -1)) {
	        return 1;
	    } else if (minSideLength == -1) {
	        return lowerBound;
	    } else {
	        return upperBound;
	    }
	}   
}
