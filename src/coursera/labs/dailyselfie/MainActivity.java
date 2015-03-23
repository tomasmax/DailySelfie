package coursera.labs.dailyselfie;

import java.io.File;
import java.io.IOException;

import android.app.AlarmManager;
import android.app.ListActivity;
import android.app.LoaderManager.LoaderCallbacks;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import coursera.labs.dailyselfie.provider.SelfieContract;

public class MainActivity extends ListActivity implements LoaderCallbacks<Cursor> {

	private static final String TAG = "SelfieActivity";
	private static final int    REQ_SNAP_PHOTO = 0;
	private static final long   INITIAL_DELAY = 2*60*1000;
	private static final long   REPEAT_DELAY = 2*60*1000;	
	private static final String ALARM_KEY = "alarms";
	private static final String SELFIE_KEY = "selfiePath";
	
	private SelfieImageAdapter  mAdapter;
	private String              mImagePath;
	private PendingIntent       mAlarmOperation;
	private SharedPreferences   mSharedPreferences;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			mImagePath = savedInstanceState.getString(SELFIE_KEY);
			Log.d(TAG,"restored selfieImagePath");
		}
		mAdapter = new SelfieImageAdapter(this);
		
		//View Initialization
		getListView().setAdapter(mAdapter);
		getLoaderManager().initLoader(0, null, this);
		mSharedPreferences = getSharedPreferences("selfie", Context.MODE_PRIVATE);
		
		setAlarm(null,false);
	
		
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Log.d(TAG,"click on item at position "+position);
		SelfieImage selfie = (SelfieImage)mAdapter.getItem(position);
		Log.d(TAG, "fetched item "+selfie.getName());
		Intent intent = new Intent(this,DisplayImageActivity.class);
		intent.putExtra(DisplayImageActivity.EXTRA_NAME,selfie.getName());
		intent.putExtra(DisplayImageActivity.EXTRA_PATH,selfie.getPath());
		Log.i(TAG,"opening fullscreen activity");
		startActivity(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);		
		MenuItem item = menu.findItem(R.id.action_alarm);
		setAlarm(item,false);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_picture) {
			Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		    if (cameraIntent.resolveActivity(getPackageManager()) != null) {
		        File imageFile = null;
		        try {
		        	Log.i(TAG,"creating temp file");
		            imageFile = BitmapUtil.createImageFile();
		            mImagePath = imageFile.getAbsolutePath();
		            Log.d(TAG,"temp file stored at : "+mImagePath);
		        } catch (IOException e) {
		            
		           	Log.w(TAG,"unable to create image file",e);
		        }
		        if (imageFile != null) {
		        	Log.i(TAG,"starting camera intent to take selfie");
		            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,Uri.fromFile(imageFile));
		            startActivityForResult(cameraIntent, REQ_SNAP_PHOTO);
		        }
		    }
			return true;
		}
		if (id == R.id.action_alarm) {
			Log.d(TAG,"click on toggle alarm");
			setAlarm(item,true);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (REQ_SNAP_PHOTO == requestCode) {
			if (resultCode == RESULT_CANCELED){
				Log.i(TAG,"user canceled, deleting file...");
				new File(mImagePath).delete();
			}
			if (resultCode == RESULT_OK) {
				Log.i(TAG,"processing selfie");
				SelfieImage selfie = new SelfieImage();
				selfie.setName(new File(mImagePath).getName());
				selfie.setPath(mImagePath);
				
				Log.i(TAG,"creating thumb bitmap");
				Bitmap fullSized = BitmapUtil.getBitmapFromFile(mImagePath);
				Float aspectRatio = ((float)fullSized.getHeight())/(float)fullSized.getWidth();
				Bitmap thumb = Bitmap.createScaledBitmap(
						fullSized,
						120, 
						(int)(120*aspectRatio), 
						false);
				String thumbPath = BitmapUtil.getThumbPath(mImagePath);
		        selfie.setThumbPath(thumbPath);
		        BitmapUtil.storeBitmapToFile(thumb, thumbPath);
		        
		        Log.i(TAG,"recycling resources");
		        fullSized.recycle();
		        thumb.recycle();
				
				mImagePath = null;
				
				Log.i(TAG,"adding selfie to adapter");
				mAdapter.addSelfie(selfie);
			}
		}
		
	}
	
	/**
	 * Triggers the alarm if needed.
	 * Also set the correct label for the item if provided. 
	 * Also toggle the alarm setting if requested
	 * @param item the menu item to edit the label
	 * @param toggle if the alarm parameter needs to be toggled
	 */
	protected void setAlarm(MenuItem item,boolean toggle) {
		//Setting the alarm
		if (mAlarmOperation == null) {
			Log.d(TAG,"initiating alarm operation");
			mAlarmOperation = PendingIntent.getBroadcast(
				getApplicationContext(), 
				0, 
				new Intent(getApplicationContext(),AlarmReceiver.class), 
				0);
		}
		
		boolean alarmEnabled = mSharedPreferences.getBoolean(ALARM_KEY, true);
		if (toggle) {
			Log.d(TAG,"requesting alarm toggle");
			alarmEnabled = !alarmEnabled;
			mSharedPreferences.edit().putBoolean(ALARM_KEY, alarmEnabled).commit();
		}
		
		AlarmManager alarm = (AlarmManager) getSystemService(Service.ALARM_SERVICE);
		if (alarmEnabled) {
			Log.i(TAG,"programming alarm");
			alarm.setRepeating(
					AlarmManager.ELAPSED_REALTIME_WAKEUP, 
					SystemClock.elapsedRealtime()+INITIAL_DELAY, 
					REPEAT_DELAY, mAlarmOperation);
		} else {
			Log.i(TAG,"alarm disabled, canceling");
			alarm.cancel(mAlarmOperation);
		}
		
		if (item != null) {
			if (alarmEnabled)
				item.setTitle(R.string.action_disable_alarm);
			else
				item.setTitle(R.string.action_enable_alarm);
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Log.d(TAG,"configuration is changing, saving instance state");
		outState.putString(SELFIE_KEY, mImagePath);
	};
	

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		CursorLoader loader = new CursorLoader(this, SelfieContract.SELFIE_URI, null, null,null,null);
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor newCursor) {
		mAdapter.swapCursor(newCursor);
		
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
		
	}
}

