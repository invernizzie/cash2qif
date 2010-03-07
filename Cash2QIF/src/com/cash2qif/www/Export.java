package com.cash2qif.www;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class Export extends Activity {
    private DbAdapter mDbHelper = new DbAdapter(this);
    private EditText mFileName;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file);

        mFileName = (EditText) findViewById(R.id.fileName);
		String fileName = Utils.dateFormatter.format(new Date()) + ".qif";
        mFileName.setText(fileName);
        Button confirmButton = (Button) findViewById(R.id.confirm);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
            	export(mFileName.getText().toString());
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    /**
     * Exports each entry into an Excel readable .QIF file on the
     * SD card.
     * @return
     */
	protected String export(String fileName) {
		mDbHelper.open();
    	Cursor cursor = mDbHelper.fetchAll();
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
//            Log.e(TAG, "Could not write file " + e.getMessage());
        }
        mDbHelper.close();
        return result;
	}
}