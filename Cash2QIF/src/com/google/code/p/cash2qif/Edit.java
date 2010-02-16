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
package com.google.code.p.cash2qif;

import java.util.Calendar;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;

public class Edit extends Activity {
	private static final String DATE_DIVIDER = "-";
	static final int ID_DATEPICKER = 0;
	private int myYear, myMonth, myDay;
	private Button mDatePickerText;
    private EditText mPayeeText;
    private EditText mAmountText;
    private EditText mCategoryText;
    private EditText mMemoText;
    private Long mRowId;
    private DbAdapter mDbHelper;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDbHelper = new DbAdapter(this);
        mDbHelper.open();        
        setContentView(R.layout.edit);
		Button datePickerButton = (Button)findViewById(R.id.datepickerbutton);
		datePickerButton.setOnClickListener(datePickerButtonOnClickListener);
        
        mDatePickerText = (Button) findViewById(R.id.datepickerbutton);
        mPayeeText = (EditText) findViewById(R.id.payee);
        mAmountText = (EditText) findViewById(R.id.amount);
        mCategoryText = (EditText) findViewById(R.id.category);
        mMemoText = (EditText) findViewById(R.id.memo);
        
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
    }

    private void populate() {
        if (mRowId != null) {
            Cursor cursor = mDbHelper.fetch(mRowId);
            startManagingCursor(cursor);
            mDatePickerText.setText(cursor.getString(
                        cursor.getColumnIndexOrThrow(DbAdapter.KEY_DATE)));
            mPayeeText.setText(cursor.getString(
                    cursor.getColumnIndexOrThrow(DbAdapter.KEY_PAYEE)));
            mAmountText.setText(cursor.getString(
                    cursor.getColumnIndexOrThrow(DbAdapter.KEY_AMOUNT)));
            mCategoryText.setText(cursor.getString(
                    cursor.getColumnIndexOrThrow(DbAdapter.KEY_CATEGORY)));
            mMemoText.setText(cursor.getString(
                    cursor.getColumnIndexOrThrow(DbAdapter.KEY_MEMO)));
        } else { // new entry
			final Calendar c = Calendar.getInstance();
			String date = dateToText(c.get(Calendar.YEAR), 
					c.get(Calendar.MONTH), 
					c.get(Calendar.DAY_OF_MONTH));
            mDatePickerText.setText(date);
        }
    }

	private Button.OnClickListener datePickerButtonOnClickListener
	= new Button.OnClickListener() {
		@Override
		public void onClick(View v) {
			final Calendar c = Calendar.getInstance();
			myYear = c.get(Calendar.YEAR);
			myMonth = c.get(Calendar.MONTH);
			myDay = c.get(Calendar.DAY_OF_MONTH);
			showDialog(ID_DATEPICKER);
		}
	};

	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id) {
		case ID_DATEPICKER:
			return new DatePickerDialog(this,
					myDateSetListener,
					myYear, myMonth, myDay);
		default:
			return null;
		}
	}
	
	private DatePickerDialog.OnDateSetListener myDateSetListener
	= new DatePickerDialog.OnDateSetListener() {
		@Override
		public void onDateSet(DatePicker view, int year, 
				int monthOfYear, int dayOfMonth) {
			String date = dateToText(year, monthOfYear, dayOfMonth);
            mDatePickerText.setText(date);
		} 
	};
	public static String dateToText(int year, int monthOfYear, int dayOfMonth) {
		StringBuilder builder = new StringBuilder();
		builder.append(String.valueOf(monthOfYear+1));
		builder.append(DATE_DIVIDER);
		builder.append(String.valueOf(dayOfMonth));
		builder.append(DATE_DIVIDER);
		builder.append(String.valueOf(year));
		return builder.toString(); 
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
    private void saveState() {
        String date = null;
        String payee = null;
        String amount = null;
        String category = null;
        String memo = null;
        if (mDatePickerText != null && mDatePickerText.getText() != null)
        	date = mDatePickerText.getText().toString();
        if (mPayeeText != null && mPayeeText.getText() != null)
        	payee = mPayeeText.getText().toString();
        if (mAmountText != null && mAmountText.getText() != null)
        	amount = mAmountText.getText().toString();
        if (mCategoryText != null && mCategoryText.getText() != null)
        	category = mCategoryText.getText().toString();
        if (mMemoText != null && mMemoText.getText() != null)
        	memo = mMemoText.getText().toString();

        if (mRowId == null) {
        	if (date != null && payee != null && amount != null && 
        			category != null && memo != null) {
        		long id = mDbHelper.create(date, payee, amount, category, memo);
        		if (id > 0) {
        			mRowId = id;
        		}
        	}
        } else {
            mDbHelper.update(mRowId, date, payee, amount, category, memo);
        }
    }
}