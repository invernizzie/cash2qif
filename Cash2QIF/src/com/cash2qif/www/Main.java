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

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class Main extends ListActivity {
	public static final int ACTIVITY_CREATE = 0;
    public static final int ACTIVITY_EDIT = 1;
    public static final int ACTIVITY_DELETE_ALL = 2;
    public static final int ACTIVITY_EXPORT = 3;
    public static final int ACTIVITY_IMPORT = 4;
    private static final int INSERT_ID = Menu.FIRST;
    private static final int EXPORT_ID = Menu.FIRST + 1;
    private static final int DELETE_ALL_ID = Menu.FIRST + 2;
    private static final int DELETE_ID = Menu.FIRST + 3;
    private static final int IMPORT_ID = Menu.FIRST + 4;
    public static final String QIF_HEADER = "!Type:Cash ";
    public static final char QIF_DATE = 'D';
    public static final char QIF_AMOUNT = 'T';
    public static final char QIF_PAYEE = 'P';
    public static final char QIF_CATEGORY = 'L';
    public static final char QIF_MEMO = 'M';
    public static final char QIF_DIVIDER = '^';
	private int COL_DATE;
	private int COL_PAYEE;
	private int COL_AMOUNT;
	private int COL_CATEGORY;
	private int COL_MEMO;
	private DecimalFormat amountFormatter = new DecimalFormat("#,##0.00");
    private DbAdapter mDbHelper = new DbAdapter(this);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list);
        fillData();
        registerForContextMenu(getListView());
    }

    /**
     * Fill data for the list page.
     */
	private void fillData() {
        mDbHelper.open();
    	Cursor cursor = mDbHelper.fetchAll();
        startManagingCursor(cursor);
        COL_DATE = cursor.getColumnIndex(DbAdapter.KEY_DATE);
        COL_PAYEE = cursor.getColumnIndex(DbAdapter.KEY_PAYEE);
        COL_AMOUNT = cursor.getColumnIndex(DbAdapter.KEY_AMOUNT);
        COL_CATEGORY = cursor.getColumnIndex(DbAdapter.KEY_CATEGORY);
        COL_MEMO = cursor.getColumnIndex(DbAdapter.KEY_MEMO);
        String[] from = new String[] {DbAdapter.KEY_DATE, DbAdapter.KEY_PAYEE, DbAdapter.KEY_AMOUNT, DbAdapter.KEY_CATEGORY, DbAdapter.KEY_MEMO};
        int[] to = new int[] {R.id.date, R.id.payee, R.id.amount, R.id.category, R.id.memo};
        SimpleCursorAdapter list = 
        	    new SimpleCursorAdapter(this, R.layout.row, cursor, from, to);
        list.setViewBinder(mViewBinder);
        setListAdapter(list);
        mDbHelper.close();
    }
    
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, INSERT_ID, 0, R.string.menu_insert);
        menu.add(1, EXPORT_ID, 0, R.string.menu_export);
        menu.add(2, DELETE_ALL_ID, 0, R.string.menu_delete_all);
        menu.add(3, IMPORT_ID, 0, R.string.menu_import);
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
        case IMPORT_ID:
        	importFile();
	        fillData();
        	return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }
	
    /**
     * Confirmation to avoid accidentally deleting all.
     */
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
            mDbHelper.open();
	        mDbHelper.delete(info.id);
	        fillData();
	        mDbHelper.close();
	        return true;
		}
		return super.onContextItemSelected(item);
	}
	
    /**
     * Create a new entry.
     */
    private void create() {
        Intent i = new Intent(this, Edit.class);
        startActivityForResult(i, ACTIVITY_CREATE);
    }
    
    /**
     * Export all entries.
     */
    private void export() {
        Intent i = new Intent(this, Export.class);
        startActivityForResult(i, ACTIVITY_EXPORT);
    }

    /**
     * Import from a .QIF file.
     */
    private void importFile() {
        Intent i = new Intent(this, Import.class);
        startActivityForResult(i, ACTIVITY_IMPORT);
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
     * Map from view fields to database columns.
     */
    private SimpleCursorAdapter.ViewBinder mViewBinder = new SimpleCursorAdapter.ViewBinder() {
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
			TextView textview = (TextView) view;
			String text;
			if (columnIndex == COL_DATE) {
				Long dateTime = cursor.getLong(columnIndex);
				Date date = new Date();
				date.setTime(dateTime);
				text = Utils.dateFormatter.format(date);
				textview.setText(text);
			}
			if (columnIndex == COL_PAYEE) {
				String payee = cursor.getString(columnIndex);
				textview.setText(payee);
			}
			if (columnIndex == COL_AMOUNT) {
				Double amount = cursor.getDouble(columnIndex);
				if (amount != null)
					textview.setText(amountFormatter.format(amount));
			}
			if (columnIndex == COL_CATEGORY) {
				String category = cursor.getString(columnIndex);
				textview.setText(category);
			}
			if (columnIndex == COL_MEMO) {
				String memo = cursor.getString(columnIndex);
				textview.setText(memo);
			}
			return true;
		}
	};
	
	/**
	 * Create a calendar given a dateTime.
	 * @param dateTime
	 * @return
	 */
	protected Calendar timeToCalendar(Long dateTime) {
		Date date = new Date();
		date.setTime(dateTime);
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		return cal;
	}
}