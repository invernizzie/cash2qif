package com.cash2qif.www;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class Import extends Activity {
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
            	importFile(mFileName.getText().toString());
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    /**
     * Tries to import from a .QIF file.
     * @return
     */
    protected void importFile(String fileName) {
        mDbHelper.open();
        Cursor cursor = mDbHelper.fetchAll();
        startManagingCursor(cursor);
        try {
            File root = Environment.getExternalStorageDirectory();
            if (root.canRead()){
                File file = new File(root, fileName);
                FileReader reader = new FileReader(file);
                BufferedReader in = new BufferedReader(reader);
                String line = in.readLine(); // discard header
                Character c;
                ContentValues values = new ContentValues();
                int created = 0;
                while (line != null) {
                    line = in.readLine();
                    if (line != null) {
                        c = line.charAt(0);
                        switch(c) {
                        case Main.QIF_DATE:
                        	String dateString = line.substring(1);
                        	long dateTime = -1;
                        	if (dateString != null) {
                        		dateTime = Utils.parseDateString(dateString);
                        		values.put(DbAdapter.KEY_DATE, dateTime);
                        	}
                            break;
                        case Main.QIF_PAYEE:
                          	values.put(DbAdapter.KEY_PAYEE, line.substring(1));
                            break;
                        case Main.QIF_AMOUNT:
                            StringBuilder builder = new StringBuilder();
                        	String amount = line.substring(1);
                            Utils.invertSign(builder, amount);
                          	values.put(DbAdapter.KEY_AMOUNT, builder.toString());
                            break;
                        case Main.QIF_CATEGORY:
                          	values.put(DbAdapter.KEY_CATEGORY, line.substring(1));
                            break;
                        case Main.QIF_MEMO:
                          	values.put(DbAdapter.KEY_MEMO, line.substring(1));
                            break;
                        case Main.QIF_DIVIDER:
                			mDbHelper.create(values);
                			created++;
                			break;
                        }
                    }
                }
                in.close();
                Toast.makeText(this, created + " entries imported.", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Error reading file.", Toast.LENGTH_LONG).show();
        }
        mDbHelper.close();
    }
}