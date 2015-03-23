package coursera.labs.dailyselfie;

import coursera.labs.dailyselfie.provider.SelfieContract;
import android.database.Cursor;
import android.graphics.Bitmap;

public class SelfieImage {
	private int _id;
	private String name;
	private String path;
	private String thumbPath;
	private Bitmap bmp;
	
	public static SelfieImage fromCursor(Cursor cursor) {
		SelfieImage selfie = new SelfieImage();
		
		selfie.setId(cursor.getInt(cursor.getColumnIndex(SelfieContract.SELFIE_COLUMN_ID)));
		selfie.setPath(cursor.getString(cursor.getColumnIndex(SelfieContract.SELFIE_COLUMN_PATH)));
		selfie.setName(cursor.getString(cursor.getColumnIndex(SelfieContract.SELFIE_COLUMN_NAME)));
		return selfie;
	}
	
	public int getId() {
		return _id;
	}
	public void setId(int id) {
		this._id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public Bitmap getBmp() {
		return bmp;
	}
	public void setBmp(Bitmap bmp) {
		this.bmp = bmp;
	}

	public String getThumbPath() {
		return thumbPath;
	}

	public void setThumbPath(String thumbPath) {
		this.thumbPath = thumbPath;
	}
	
}

