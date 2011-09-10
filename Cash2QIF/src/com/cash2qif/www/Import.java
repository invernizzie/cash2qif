package com.cash2qif.www;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

public class Import extends Activity {
    private static final int GONE = 8;
	private static final int VISIBLE = 0;
	private static final String EMPTY = "";
    private DbAdapter mDbHelper = new DbAdapter(this);
    private final Handler mHandler = new Handler();
	protected int mResults;
    private ProgressBar mProgress;

    protected final static String MESSAGE = "msg";
	protected final static String TITLE = "title";
	protected final static String SUCCESS = "success";

	protected final static int DIALOG_FINISHED = 1;
	protected final static int DIALOG_IMPORTING = 2;
	public static final String SAVE_POSITION = "position";

	protected String m_ext = "qif";
	protected Spinner m_fileSelector;
	protected Button mConfirmButton;

	protected static int s_title;
	protected static String s_message;
	protected static boolean s_success = false;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.import_file);

        mProgress = (ProgressBar) findViewById(R.id.progress_bar);

        mConfirmButton = (Button) findViewById(R.id.confirm);
        mConfirmButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
            	runImport();
                setResult(RESULT_OK);
            }
        });
        m_fileSelector = (Spinner) findViewById(R.id.simple_spinner_dropdown_item);
        populateFileSelector();
    }

    /**
     * Create runnable for posting.
     */
    final Runnable mUpdateResults = new Runnable() {
        public void run() {
            updateResultsInUI();
        }
    };

    /**
     * Run import in a separate thread.
     */
    protected void runImport() {
        Toast.makeText(this, "Import started.", Toast.LENGTH_LONG).show();
        mProgress.setVisibility(VISIBLE);
        m_fileSelector.setVisibility(GONE);
        mConfirmButton.setVisibility(GONE);
        Thread t = new Thread() {
            public void run() {
                mResults = importFile((String)m_fileSelector.getSelectedItem());
                mHandler.post(mUpdateResults);
            }
        };
        t.start();
    }

    /**
     * Display the results.
     */
    private void updateResultsInUI() {
    	Toast.makeText(this, mResults + " entries imported.", Toast.LENGTH_LONG).show();
    	finish();
    }

    /**
     * Tries to import from a .QIF file.
     * @return
     */
    protected int importFile(String fileName) {
    	int created = 0;
    	mDbHelper.open();
    	Cursor cursor = mDbHelper.fetchAll();
    	startManagingCursor(cursor);
    	if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
    		try {
    			File root = Environment.getExternalStorageDirectory();
    			if (root.canRead()){
    				File dir = new File(root + Utils.DIRECTORY);
    				dir.mkdirs();
    				File file = new File(root + Utils.DIRECTORY, fileName != null ? fileName : "");
    				FileReader reader = new FileReader(file);
    				BufferedReader in = new BufferedReader(reader);
    				String line = in.readLine(); // discard header
    				Character c;
    				ContentValues values = emptyValues();
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
    							String category = line.substring(1);
    							if (category.contains(Main.TAG_DIVIDER)) {
    								int location = category.indexOf(Main.TAG_DIVIDER);
    								values.put(DbAdapter.KEY_TAG, category.substring(location + 1));
    								category = category.substring(0, location);
    							}
    							values.put(DbAdapter.KEY_CATEGORY, category);
    							break;
    						case Main.QIF_MEMO:
    							values.put(DbAdapter.KEY_MEMO, line.substring(1));
    							break;
    						case Main.QIF_DIVIDER:
    							mDbHelper.create(values);
    							created++;
    							values = emptyValues();
    							break;
    						}
    					}
    				}
    				in.close();
    			}
    		} catch (IOException e) {
				Log.e("Import", "Error reading file. " + e.getMessage());
    		}
        }
        mDbHelper.close();
        return created;
    }

    /**
     * Create a new ContentValues with non null values.
     * @return
     */
	private ContentValues emptyValues() {
		ContentValues values = new ContentValues();
		values.put(DbAdapter.KEY_DATE, 0);
		values.put(DbAdapter.KEY_PAYEE, EMPTY);
		values.put(DbAdapter.KEY_AMOUNT, EMPTY);
		values.put(DbAdapter.KEY_CATEGORY, EMPTY);
		values.put(DbAdapter.KEY_MEMO, EMPTY);
		values.put(DbAdapter.KEY_TAG, EMPTY);
		return values;
	}

	/**
	 * Populate the import spinner with available .qif files.
	 */
	protected void populateFileSelector() {
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			File root = Environment.getExternalStorageDirectory();
			File directory = new File(root.getAbsoluteFile() + Utils.DIRECTORY);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			String[] files = directory.list(m_filter);
			if (files != null) {
				Arrays.sort(files);
				for (String file : files) {
					adapter.add(file);
				}
			}
		}
		m_fileSelector.setAdapter(adapter);
	}

	@Override
	protected Dialog onCreateDialog(int which) {
		switch (which) {
		case DIALOG_FINISHED:
			return new AlertDialog.Builder(this).setTitle(s_title).setMessage(s_message).setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dismissDialog(DIALOG_FINISHED);
				}
			}).create();
		case DIALOG_IMPORTING:
			ProgressDialog dialog = new ProgressDialog(this);
			dialog.setTitle(R.string.importing_title);
			dialog.setMessage(getString(R.string.importing));
			return dialog;
		}
		return null;
	}

	/**
	 * Filter out files that don't end with an extension of m_ext.
	 */
	protected FilenameFilter m_filter = new FilenameFilter() {
		public boolean accept(File dir, String filename) {
			int begin = filename.lastIndexOf(".");
			if (begin >= 0) {
				String ext = filename.substring(begin + 1);
				return ext.equalsIgnoreCase(m_ext);
			}
			return false;
		}
	};
}