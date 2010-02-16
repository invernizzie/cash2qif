package com.google.code.p.cash2qif;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

public class DeleteAll extends Activity {
    public static final int DIALOG_DELETE_CONFIRMATION_ID = 0;
    private DbAdapter mDbHelper;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    	showDialog(DIALOG_DELETE_CONFIRMATION_ID);
        mDbHelper = new DbAdapter(this);
        mDbHelper.open();
    }
    
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
    
    public boolean deleteAll() {
    	return mDbHelper.deleteAll();
    }
}