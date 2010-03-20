package com.cash2qif.www;

import android.app.Activity;
import android.content.ContentValues;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class Repeat extends Activity {
    private static final int DAY = 24*60*60*1000;
	private EditText mRepeatEvery;
    private EditText mRepeatFor;
    private Bundle extras;
    private DbAdapter mDbHelper = new DbAdapter(this);

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.repeat);

        mRepeatEvery = (EditText) findViewById(R.id.repeat_every);
        mRepeatFor = (EditText) findViewById(R.id.repeat_for);
        
        extras = getIntent().getExtras();
        
        Button confirmButton = (Button) findViewById(R.id.confirm);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
            	saveRepeats();
                setResult(RESULT_OK);
                finish();
            }
        });
    }
    
    /**
     * Create copies of an entry.
     */
    private void saveRepeats() {
        ContentValues values = new ContentValues();
        if (extras != null) {
        	values.put(DbAdapter.KEY_DATE, extras.getLong(DbAdapter.KEY_DATE));
        	values.put(DbAdapter.KEY_PAYEE, extras.getString(DbAdapter.KEY_PAYEE));
        	values.put(DbAdapter.KEY_AMOUNT, extras.getString(DbAdapter.KEY_AMOUNT));
        	values.put(DbAdapter.KEY_CATEGORY, extras.getString(DbAdapter.KEY_CATEGORY));
        	values.put(DbAdapter.KEY_MEMO, extras.getString(DbAdapter.KEY_MEMO));
        }
        
        int interval = parseNum(mRepeatEvery.getText().toString());
        int repeats = parseNum(mRepeatFor.getText().toString());
        
    	if (Utils.validate(values)) {
    		mDbHelper.open();
    		for (int r = 0; r < repeats; r++) {
            	long date = values.getAsLong(DbAdapter.KEY_DATE);
    			values.put(DbAdapter.KEY_DATE, date + interval*DAY);
    			mDbHelper.create(values);
    		}
    		mDbHelper.close();
    	}
    }
    
    /**
     * Parse an int from a String
     * @param s
     * @return
     */
	private int parseNum(String s) {
		int num = 0;
        if (s != null && s.length() > 0)
        	num = Integer.parseInt(s);
		return num;
	}
}