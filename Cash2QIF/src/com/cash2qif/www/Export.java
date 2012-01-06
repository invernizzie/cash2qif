package com.cash2qif.www;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

public class Export extends Activity {
    private DbAdapter mDbHelper = new DbAdapter(this);
    private EditText mFileName;
	static final int ID_DATEPICKER = 0;
	static final int ID_EXPORT_SINCE_LAST = 0;
	static final int ID_EXPORT_EARLIEST = 1;
	static final int ID_EXPORT_SELECT_DATE = 2;
	private int mYear, mMonth, mDay;
	private Button mDatePickerText;
    private RadioButton mSelectDate;
    private RadioButton mEarliest;
    private RadioButton mSinceLast;
    private int picked = ID_EXPORT_SINCE_LAST;
    private CheckBox sendEmail;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.export);

        OnClickListener radioListener = new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				// hide soft keyboard
				((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
					.hideSoftInputFromWindow(mFileName.getWindowToken(), 0);
				RadioButton rb = (RadioButton) arg0;
				switch(rb.getId()) {
				case R.id.since_last_export:
					picked = ID_EXPORT_SINCE_LAST;
					break;
				case R.id.earliest:
					picked = ID_EXPORT_EARLIEST;
					break;
				case R.id.select_date:
					picked = ID_EXPORT_SELECT_DATE;
					final Calendar cal = Calendar.getInstance();
					mYear = cal.get(Calendar.YEAR);
					mMonth = cal.get(Calendar.MONTH);
					mDay = cal.get(Calendar.DAY_OF_MONTH);
					showDialog(ID_DATEPICKER);
					break;
				}
			}
		};
        
        mSelectDate = (RadioButton)findViewById(R.id.select_date);
        mSelectDate.setOnClickListener(radioListener);
	    mDatePickerText = (Button) findViewById(R.id.select_date);

	    mFileName = (EditText) findViewById(R.id.fileName);
		DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getApplicationContext());
		String text = dateFormat.format(new Date());
		text = text.replaceAll("/", "-");
		String fileName = text + ".qif";
        mFileName.setText(fileName);
        sendEmail = (CheckBox) findViewById(R.id.email);
        mSinceLast = (RadioButton) findViewById(R.id.since_last_export);
        mSinceLast.setOnClickListener(radioListener);
        mEarliest = (RadioButton) findViewById(R.id.earliest);
        mEarliest.setOnClickListener(radioListener);
        Button confirmButton = (Button) findViewById(R.id.confirm);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
            	Long time = null;
            	if (picked == ID_EXPORT_SELECT_DATE)
            		time = new GregorianCalendar(mYear, mMonth, mDay).getTimeInMillis();
            	if (picked == ID_EXPORT_SINCE_LAST) {
            		SharedPreferences settings = getSharedPreferences(Settings.SETTINGS_NAME, 0);
            		time = settings.getLong(Settings.LAST_EXPORT_DATE, 0);
            	}
            	if (sendEmail.isChecked())
            		email(mFileName.getText().toString(), time);
            	else
            		export(mFileName.getText().toString(), time);
                setResult(RESULT_OK);
                finish();
            }
        });
    }

	/**
     * Exports to an Excel readable .QIF file on the SD card all entries after
     * given dateTime.
	 * @param fileName
	 * @param dateTime
	 * @return
	 */
	protected String export(String fileName, Long dateTime) {
		mDbHelper.open();
		Cursor cursor;
		if (dateTime != null)
			cursor = mDbHelper.fetchStarting(dateTime);
		else 
			cursor = mDbHelper.fetchAll();
        String result = export(fileName, cursor);
        mDbHelper.close();
        return result;
	}

	/**
     * Save a .QIF file to SD card and email a copy of it.
	 * @param fileName
	 * @param dateTime
	 * @return
	 */
	protected void email(String fileName, Long dateTime) {
		mDbHelper.open();
    	Cursor cursor;
		if (dateTime != null)
			cursor = mDbHelper.fetchStarting(dateTime);
		else 
			cursor = mDbHelper.fetchAll();
        export(fileName, cursor);
        email(fileName);
        mDbHelper.close();
	}

	/**
	 * Send an email with the .QIF file as an attachment.
	 * @param fileName
	 */
	private void email(String fileName) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
        	File root = Environment.getExternalStorageDirectory();
        	SharedPreferences settings = getSharedPreferences(Settings.SETTINGS_NAME, 0);
        	String[] address = {settings.getString(Settings.EMAIL_ADDRESS, "")};
        	Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
        	emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, address);
        	emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, fileName);
        	emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "See attached file.");
        	emailIntent.setType("text/plain");
        	StringBuilder builder = new StringBuilder();
        	builder.append("file://");
        	builder.append(root);
        	builder.append(Utils.DIRECTORY);
        	builder.append(fileName);
        	emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(builder.toString()));
        	startActivity(emailIntent);
        }
	}

	/**
	 * Export the results in cursor to fileName.
	 * @param fileName
	 * @param cursor
	 * @return
	 */
	private String export(String fileName, Cursor cursor) {
		startManagingCursor(cursor);
    	StringBuilder builder = new StringBuilder();
        if (cursor.getCount() > 0) {
        	cursor.moveToFirst();
    		builder.append(Main.QIF_HEADER);
    		builder.append("\n");
        	boolean last = false;
        	String amount;
        	while (!last) {
        		amount = cursor.getString(cursor.getColumnIndexOrThrow(DbAdapter.KEY_AMOUNT));
        		builder.append(Main.QIF_DATE);
        		String dateString = cursor.getString(cursor.getColumnIndexOrThrow(DbAdapter.KEY_DATE));
        		Long dateTime = Utils.parseDateString(dateString);
				String text = Utils.formatDateTime(dateTime);
				builder.append(text);
        		builder.append("\n");
        		builder.append(Main.QIF_PAYEE);
        		builder.append(cursor.getString(cursor.getColumnIndexOrThrow(DbAdapter.KEY_PAYEE)));
        		builder.append("\n");
        		builder.append(Main.QIF_AMOUNT);
        		Utils.invertSign(builder, amount);
        		builder.append("\n");
        		builder.append(Main.QIF_CATEGORY);
        		builder.append(cursor.getString(cursor.getColumnIndexOrThrow(DbAdapter.KEY_CATEGORY)));
        		String tag = cursor.getString(cursor.getColumnIndexOrThrow(DbAdapter.KEY_TAG));
        		if (tag != null) {
        			builder.append(Main.TAG_DIVIDER);
        			builder.append(tag);
        		}
        		builder.append("\n");
        		builder.append(Main.QIF_MEMO);
        		builder.append(cursor.getString(cursor.getColumnIndexOrThrow(DbAdapter.KEY_MEMO)));
        		builder.append("\n");
        		builder.append(Main.QIF_DIVIDER);
        		builder.append("\n");
        		last = cursor.isLast();
        		cursor.moveToNext();
        	}
        }
        String result = builder.toString();
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
        	File root = Environment.getExternalStorageDirectory();
			try {
				File dir = new File(root + Utils.DIRECTORY);
				dir.mkdirs();
				File file = new File(root + Utils.DIRECTORY, fileName);
				file.createNewFile();
				if (dir.exists() && file.exists())
					System.out.println("file exists");
				BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
	            out.write(result.getBytes("ISO-8859-1"));
	            out.flush();
	            out.close();
	    		SharedPreferences.Editor editor = getSharedPreferences(Settings.SETTINGS_NAME, 0).edit();
	    		editor.putLong(Settings.LAST_EXPORT_DATE, new Date().getTime());
	    		editor.commit();
	            Toast.makeText(this, file + " created.", Toast.LENGTH_LONG).show();
			} catch (IOException e) {
				Log.e("Export", "Could not write file " + e.getMessage());
			}
        }
		return result;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id) {
		case ID_DATEPICKER:
			DatePickerDialog dialog = 
				new DatePickerDialog(this,
						myDateSetListener,
						mYear, mMonth, mDay);
				dialog.setButton3(getString(R.string.today), todayListener);
				return dialog; 
		default:
			return null;
		}
	}
	
	/**
	 * When a Date is set, update member fields and the display.
	 */
	private DatePickerDialog.OnDateSetListener myDateSetListener
	= new DatePickerDialog.OnDateSetListener() {
		@Override
		public void onDateSet(DatePicker view, int year, 
				int monthOfYear, int dayOfMonth) {
            mYear = year;
            mMonth = monthOfYear;
            mDay = dayOfMonth;
			updateDate();
		} 
	};
	
	/**
	 * OnClickListener for the Today button in the date picker.
	 */
	private DialogInterface.OnClickListener todayListener = new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface intf, int which) {
			setToday();
			updateDate();
		}
	};
	
	/**
	 * Set member fields to the values for Today.
	 */
	private void setToday() {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(System.currentTimeMillis());
		mYear = cal.get(Calendar.YEAR);
		mMonth = cal.get(Calendar.MONTH);
		mDay = cal.get(Calendar.DAY_OF_MONTH);
	}

	/**
	 * Get selected date and update the text on the screen.
	 */
	protected void updateDate() {
		GregorianCalendar cal = new GregorianCalendar(mYear, mMonth, mDay);
		Date date = new Date();
		date.setTime(cal.getTimeInMillis());
		DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getApplicationContext());
		String text = dateFormat.format(date);
		mDatePickerText.setText(text);
	}
}