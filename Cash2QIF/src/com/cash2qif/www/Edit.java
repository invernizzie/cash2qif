/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.cash2qif.www;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;

public class Edit extends Activity {
	static final int ID_DATEPICKER = 0;
	private int mYear, mMonth, mDay;
	private Button mDatePickerText;
    private EditText mPayeeText;
    private EditText mAmountText;
    private EditText mCategoryText;
    private EditText mMemoText;
    private Long mRowId;
    private DbAdapter mDbHelper = new DbAdapter(this);
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit);
		Button datePickerButton = (Button)findViewById(R.id.datepickerbutton);
		datePickerButton.setOnClickListener(datePickerButtonOnClickListener);
        
		SharedPreferences settings = getSharedPreferences(Settings.SETTINGS_NAME, 0);
		boolean defaultCategories = settings.getBoolean("default_categories", false);
		String[] categories = null;
		if (defaultCategories)
			categories = Constants.CATEGORIES; 
		
	    autoCompleteField(R.id.autocomplete_payee, R.layout.payees, DbAdapter.KEY_PAYEE);
	    autoCompleteField(R.id.autocomplete_category, R.layout.categories, DbAdapter.KEY_CATEGORY, categories);
	    autoCompleteField(R.id.autocomplete_memo, R.layout.memos, DbAdapter.KEY_MEMO);

	    mDatePickerText = (Button) findViewById(R.id.datepickerbutton);
        mPayeeText = (EditText) findViewById(R.id.autocomplete_payee);
        mAmountText = (EditText) findViewById(R.id.amount);
        mCategoryText = (EditText) findViewById(R.id.autocomplete_category);
        mMemoText = (EditText) findViewById(R.id.autocomplete_memo);
        
        Button confirmButton = (Button) findViewById(R.id.confirm);
        mRowId = savedInstanceState != null ? savedInstanceState.getLong(DbAdapter.KEY_ROWID) 
        		: null;
        if (mRowId == null) {
        	Bundle extras = getIntent().getExtras();            
        	mRowId = extras != null ? extras.getLong(DbAdapter.KEY_ROWID) 
        			: null;
        }
        populate();
        confirmButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                setResult(RESULT_OK);
                finish();
            }
        });
        Button repeatButton = (Button) findViewById(R.id.repeat);
        repeatButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
            	repeat();
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    /**
     * Repeat an entry
     */
    private void repeat() {
        Intent i = new Intent(this, Repeat.class);
        Bundle values = new Bundle();
		GregorianCalendar cal = new GregorianCalendar(mYear, mMonth, mDay);
        values.putLong(DbAdapter.KEY_DATE, cal.getTimeInMillis());
        if (mPayeeText != null && mPayeeText.getText() != null)
        	values.putString(DbAdapter.KEY_PAYEE, mPayeeText.getText().toString());
        if (mAmountText != null && mAmountText.getText() != null)
        	values.putString(DbAdapter.KEY_AMOUNT, mAmountText.getText().toString());
        if (mCategoryText != null && mCategoryText.getText() != null)
        	values.putString(DbAdapter.KEY_CATEGORY, mCategoryText.getText().toString());
        if (mMemoText != null && mMemoText.getText() != null)
        	values.putString(DbAdapter.KEY_MEMO, mMemoText.getText().toString());
        i.putExtras(values);
        startActivity(i);
    }

    /**
     * Make a drop down autoComplete field.
     * @param autoCompleteViewId
     * @param editViewId
     * @param dbField
     */
    private void autoCompleteField(int autoCompleteViewId, int editViewId, String dbField) {
    	autoCompleteField(autoCompleteViewId, editViewId, dbField, null);
    }
    
    /**
     * Make a drop down autoComplete field, combining defaultValues with the
     * values from dbField.
     * @param autoCompleteViewId
     * @param editViewId
     * @param dbField
     * @param defaultValues
     */
    private void autoCompleteField(int autoCompleteViewId, int editViewId, String dbField, String[] defaultValues) {
		mDbHelper.open();
		AutoCompleteTextView textView = (AutoCompleteTextView) findViewById(autoCompleteViewId);
	    Cursor cursor = mDbHelper.fetch(dbField);
	    if (cursor != null && cursor.getCount() > 0) {
	    	startManagingCursor(cursor);
			int index = cursor.getColumnIndexOrThrow(dbField);
	    	List<String> field = new ArrayList<String>();
	    	if (defaultValues != null)
	    		for (String value: defaultValues)
	    			field.add(value);
	    	boolean last = false;
	    	while (!last) {
	    		field.add(cursor.getString(index));
	    		last = cursor.isLast();
	    		cursor.moveToNext();
	    	}
    		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, editViewId, field);
    		textView.setAdapter(adapter);
	    }
	    mDbHelper.close();
	}

    /**
     * Populate an entry with values from the database if it exists.
     */
    private void populate() {
        if (mRowId != null) {
            mDbHelper.open();
            Cursor cursor = mDbHelper.fetch(mRowId);
            startManagingCursor(cursor);
    		Date date = new Date();
    		Long dateTime = cursor.getLong(
    			cursor.getColumnIndexOrThrow(DbAdapter.KEY_DATE));
    		Calendar cal = Calendar.getInstance();
    		date.setTime(dateTime);
    		cal.setTime(date);
    		mYear = cal.get(Calendar.YEAR);
    		mMonth = cal.get(Calendar.MONTH);
    		mDay = cal.get(Calendar.DAY_OF_MONTH);
            mDatePickerText.setText(Utils.dateFormatter.format(date));
            mPayeeText.setText(cursor.getString(
                    cursor.getColumnIndexOrThrow(DbAdapter.KEY_PAYEE)));
            mAmountText.setText(cursor.getString(
                    cursor.getColumnIndexOrThrow(DbAdapter.KEY_AMOUNT)));
            mCategoryText.setText(cursor.getString(
                    cursor.getColumnIndexOrThrow(DbAdapter.KEY_CATEGORY)));
            mMemoText.setText(cursor.getString(
                    cursor.getColumnIndexOrThrow(DbAdapter.KEY_MEMO)));
            mDbHelper.close();
        } else { // new entry
			setToday();
			updateDate();
        }
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	if (mRowId != null)
    		outState.putLong(DbAdapter.KEY_ROWID, mRowId);
    }
    @Override
    protected void onPause() {
        super.onPause();
        saveState();
    }
    @Override
    protected void onResume() {
        super.onResume();
        populate();
    }
    
    /**
     * Save values for the current entry.
     */
    private void saveState() {
        ContentValues values = new ContentValues();
		GregorianCalendar cal = new GregorianCalendar(mYear, mMonth, mDay);
        values.put(DbAdapter.KEY_DATE, cal.getTimeInMillis());
        if (mPayeeText != null && mPayeeText.getText() != null)
        	values.put(DbAdapter.KEY_PAYEE, mPayeeText.getText().toString());
        if (mAmountText != null && mAmountText.getText() != null)
        	values.put(DbAdapter.KEY_AMOUNT, mAmountText.getText().toString());
        if (mCategoryText != null && mCategoryText.getText() != null)
        	values.put(DbAdapter.KEY_CATEGORY, mCategoryText.getText().toString());
        if (mMemoText != null && mMemoText.getText() != null)
        	values.put(DbAdapter.KEY_MEMO, mMemoText.getText().toString());

    	if (Utils.validate(values)) {
    		mDbHelper.open();
    		if (mRowId == null) {
    			long id = mDbHelper.create(values);
    			if (id > 0) {
    				mRowId = id;
    			}
    		} else {
    			mDbHelper.update(mRowId, values);
    		}
    		mDbHelper.close();
    	}
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