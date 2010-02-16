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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class Main extends ListActivity {
    private static final int ACTIVITY_CREATE = 0;
    private static final int ACTIVITY_EDIT = 1;
    private static final int ACTIVITY_DELETE_ALL = 2;
    private static final int INSERT_ID = Menu.FIRST;
    private static final int EXPORT_ID = Menu.FIRST + 1;
    private static final int DELETE_ALL_ID = Menu.FIRST + 2;
    private static final int DELETE_ID = Menu.FIRST + 3;
    private static final String MINUS = "-";
	private static final String QIF_HEADER = "!Type:Cash ";
    private static final String QIF_DATE = "D";
    private static final String QIF_AMOUNT = "T";
    private static final String QIF_PAYEE = "P";
    private static final String QIF_CATEGORY = "L";
    private static final String QIF_MEMO = "M";
    private static final String QIF_DIVIDER = "^";

    private DbAdapter mDbHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list);
        mDbHelper = new DbAdapter(this);
        mDbHelper.open();
        fillData();
        registerForContextMenu(getListView());
    }

	private void fillData() {
    	Cursor cursor = mDbHelper.fetchAll();
        startManagingCursor(cursor);
        String[] from = new String[] {DbAdapter.KEY_DATE, DbAdapter.KEY_PAYEE, DbAdapter.KEY_AMOUNT, DbAdapter.KEY_CATEGORY, DbAdapter.KEY_MEMO};
        int[] to = new int[] {R.id.date, R.id.payee, R.id.amount, R.id.category, R.id.memo};
        SimpleCursorAdapter list = 
        	    new SimpleCursorAdapter(this, R.layout.row, cursor, from, to);
        setListAdapter(list);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, INSERT_ID, 0, R.string.menu_insert);
        menu.add(1, EXPORT_ID, 0, R.string.menu_export);
        menu.add(2, DELETE_ALL_ID, 0, R.string.menu_delete_all);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
        case INSERT_ID:
            create();
            return true;
        case EXPORT_ID:
        	export();
        	return true;
        case DELETE_ALL_ID:
        	confirmDeleteAll();
	        fillData();
        	return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }
	
    private void confirmDeleteAll() {
        Intent i = new Intent(this, DeleteAll.class);
        startActivityForResult(i, ACTIVITY_DELETE_ALL);
    }

    @Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, DELETE_ID, 0, R.string.menu_delete);
	}

    @Override
	public boolean onContextItemSelected(MenuItem item) {
		switch(item.getItemId()) {
    	case DELETE_ID:
    		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
	        mDbHelper.delete(info.id);
	        fillData();
	        return true;
		}
		return super.onContextItemSelected(item);
	}
	
    private void create() {
        Intent i = new Intent(this, Edit.class);
        startActivityForResult(i, ACTIVITY_CREATE);
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent i = new Intent(this, Edit.class);
        i.putExtra(DbAdapter.KEY_ROWID, id);
        startActivityForResult(i, ACTIVITY_EDIT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, 
    		Intent intent) {
    	if (requestCode == ACTIVITY_DELETE_ALL && resultCode == RESULT_OK)
    		startActivity(intent);
		super.onActivityResult(requestCode, resultCode, intent);
		fillData();
    }

    /**
     * Exports each entry into an Excel readable .QIF file on the
     * SD card.
     * @return
     */
	protected String export() {
    	Cursor cursor = mDbHelper.fetchAll();
        startManagingCursor(cursor);
    	StringBuilder builder = new StringBuilder();
        if (cursor.getCount() > 0) {
        	cursor.moveToFirst();
    		builder.append(QIF_HEADER);
    		builder.append("\n");
        	boolean last = false;
        	String amount;
        	while (!last) {
        		amount = cursor.getString(cursor.getColumnIndexOrThrow(DbAdapter.KEY_AMOUNT));
        		builder.append(QIF_DATE);
        		builder.append(cursor.getString(cursor.getColumnIndexOrThrow(DbAdapter.KEY_DATE)));
        		builder.append("\n");
        		builder.append(QIF_PAYEE);
        		builder.append(cursor.getString(cursor.getColumnIndexOrThrow(DbAdapter.KEY_PAYEE)));
        		builder.append("\n");
        		builder.append(QIF_AMOUNT);
        		if (amount != null && amount.length() > 0) {
        			if (!MINUS.equals(amount.substring(0, 1))) {
        				builder.append(MINUS); // debit, so prepend a minus
        				builder.append(amount);
        			} else builder.append(amount.substring(1, amount.length())); // credit, so remove minus
        		}
        		builder.append("\n");
        		builder.append(QIF_CATEGORY);
        		builder.append(cursor.getString(cursor.getColumnIndexOrThrow(DbAdapter.KEY_CATEGORY)));
        		builder.append("\n");
        		builder.append(QIF_MEMO);
        		builder.append(cursor.getString(cursor.getColumnIndexOrThrow(DbAdapter.KEY_MEMO)));
        		builder.append("\n");
        		builder.append(QIF_DIVIDER);
        		builder.append("\n");
        		last = cursor.isLast();
        		cursor.moveToNext();
        	}
        }
        String result = builder.toString();
        try {
            File root = Environment.getExternalStorageDirectory();
            if (root.canWrite()){
    			final Calendar c = Calendar.getInstance();
    			int year = c.get(Calendar.YEAR);
    			int monthOfYear = c.get(Calendar.MONTH);
    			int dayOfMonth = c.get(Calendar.DAY_OF_MONTH);
    			String date = Edit.dateToText(year, monthOfYear, dayOfMonth);
    			File file = new File(root, date + ".qif");
                FileWriter writer = new FileWriter(file);
                BufferedWriter out = new BufferedWriter(writer);
                out.write(result);
                out.close();
                Toast.makeText(this, file + " created.", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
//            Log.e(TAG, "Could not write file " + e.getMessage());
        }
        return result;
	}
}