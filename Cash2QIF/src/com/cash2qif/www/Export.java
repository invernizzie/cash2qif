package com.cash2qif.www;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

public class Export extends Activity {
    private DbAdapter mDbHelper = new DbAdapter(this);
    private EditText mFileName;
	static final int ID_DATEPICKER = 0;
	private int mYear, mMonth, mDay;
	private Button mDatePickerText;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.export);

		Button datePickerButton = (Button)findViewById(R.id.datepickerbutton);
		datePickerButton.setOnClickListener(datePickerButtonOnClickListener);
	    mDatePickerText = (Button) findViewById(R.id.datepickerbutton);

	    mFileName = (EditText) findViewById(R.id.fileName);
		String fileName = Utils.dateFormatter.format(new Date()) + ".qif";
        mFileName.setText(fileName);
        Button confirmButton = (Button) findViewById(R.id.confirm);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
            	export(mFileName.getText().toString(), new GregorianCalendar(mYear, mMonth, mDay).getTimeInMillis());
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    /**
     * Exports each entry into an Excel readable .QIF file on the
     * SD card.
     * @param fileName
     * @return
     */
    protected String export(String fileName) {
		mDbHelper.open();
    	Cursor cursor = mDbHelper.fetchAll();
        String result = export(fileName, cursor);
        mDbHelper.close();
        return result;
	}

	/**
     * Exports to an Excel readable .QIF file on the SD card all entries after
     * given dateTime.
	 * @param fileName
	 * @param dateTime
	 * @return
	 */
	protected String export(String fileName, long dateTime) {
		mDbHelper.open();
    	Cursor cursor = mDbHelper.fetchStarting(dateTime);
        String result = export(fileName, cursor);
        mDbHelper.close();
        return result;
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
        try {
            File root = Environment.getExternalStorageDirectory();
            if (root.canWrite()){
    			File file = new File(root, fileName);
                FileWriter writer = new FileWriter(file);
                BufferedWriter out = new BufferedWriter(writer);
                out.write(result);
                out.close();
                Toast.makeText(this, file + " created.", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            Log.e("Export", "Could not write file " + e.getMessage());
        }
		return result;
	}

    /**
     * OnClickListener for the datePickerButton.
     */
    private Button.OnClickListener datePickerButtonOnClickListener
	= new Button.OnClickListener() {
		@Override
		public void onClick(View v) {
			final Calendar cal = Calendar.getInstance();
			mYear = cal.get(Calendar.YEAR);
			mMonth = cal.get(Calendar.MONTH);
			mDay = cal.get(Calendar.DAY_OF_MONTH);
			showDialog(ID_DATEPICKER);
		}
	};

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
		mDatePickerText.setText(Utils.dateFormatter.format(date));
	}
}