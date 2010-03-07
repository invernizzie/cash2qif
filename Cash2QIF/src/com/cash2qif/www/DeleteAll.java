package com.cash2qif.www;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

public class DeleteAll extends Activity {
    public static final int DIALOG_DELETE_CONFIRMATION_ID = 0;
    private DbAdapter mDbHelper = new DbAdapter(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    	showDialog(DIALOG_DELETE_CONFIRMATION_ID);
    }
    
    /**
     * Dialog to confirm deleteAll.
     */
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        switch(id) {
        case DIALOG_DELETE_CONFIRMATION_ID:
        	AlertDialog.Builder builder = new AlertDialog.Builder(this);
        	builder.setMessage("Are you sure you want to delete all entries?")
        	.setCancelable(false)
        	.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
        		public void onClick(DialogInterface dialog, int id) {
        			deleteAll();
        			DeleteAll.this.finish();
        		}
        	})
        	.setNegativeButton("No", new DialogInterface.OnClickListener() {
        		public void onClick(DialogInterface dialog, int id) {
        			dialog.cancel();
        			DeleteAll.this.finish();
        		}
        	});
        	dialog = builder.create();
        }
        return dialog;
    }
    
    /**
     * Delete all in the database.
     * @return
     */
    public boolean deleteAll() {
        mDbHelper.open();
    	boolean result = mDbHelper.deleteAll();
        mDbHelper.close();
        return result;
    }
}