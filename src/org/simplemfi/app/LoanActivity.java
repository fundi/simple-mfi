/*******************************************************************************
 * Copyright 2011 Kevin Gibbs and The Simple MFI Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.simplemfi.app;


import java.util.List;

import org.mantasync.Store;
import org.simplemfi.app.R;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class LoanActivity extends Activity {

    private static final String TAG = "LoanActivity";

    // Menu item ids
    public static final int MENU_ITEM_SYNC = Menu.FIRST;
    
	private class ChangeObserver extends ContentObserver {
        public ChangeObserver() {
            super(new Handler());
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
    		Log.e(TAG, "Content change reported by ContentResolver, updating display.");
            updateDisplay();
        }
    }
	
	private Cursor mCursor;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.loan_detail_title);
        setContentView(R.layout.loan);
       
        Uri uri = getIntent().getData();
        List<String> path = uri.getPathSegments();
        getContentResolver().registerContentObserver(getIntent().getData(), false, new ChangeObserver());
        
        updateDisplay();
        
        {
    		String client_id = "";
    		int clientCol = mCursor.getColumnIndex("clientid");
    		if (clientCol != -1) {
    			client_id = mCursor.getString(clientCol);
    		}
    		long issued_date = 0;
    		int issuedDateCol = mCursor.getColumnIndex("issued_date");
    		if (clientCol != -1) {
    			issued_date = mCursor.getLong(issuedDateCol);
    		}
        	
            final Uri statementUri = uri.buildUpon().path(path.get(0)).appendPath("Transaction").build();
            Cursor cursor = managedQuery(statementUri, new String[] { 
            		"strftime('%Y-%m', posting_date,'unixepoch') as posting_date", "sum(amount) as amount" }, 
            		"clientid = ? and \"Transaction\".posting_date > " + issued_date + " and transaction_type = 3 group by "
            		+"strftime('%Y-%m', posting_date,'unixepoch')", 
            		new String[] { client_id }, 
            		"posting_date asc");

            SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.loan_repayment_item, cursor,
                    new String[] { "posting_date", "amount" }, 
                    new int[] { android.R.id.text1, android.R.id.text2 });
            final ListView listView = ((ListView)findViewById(R.id.loan_schedule));
            listView.setAdapter(adapter);
            cursor.registerContentObserver(new ContentObserver(new Handler()) {
            	@Override
            	public void onChange(boolean selfChange) {
            		Util.setListViewHeightBasedOnChildren(listView);
            	}
			});
            Util.setListViewHeightBasedOnChildren(listView);
        }
        final ScrollView scrollView = (ScrollView)findViewById(R.id.loan_scroll);
        scrollView.fullScroll(ScrollView.FOCUS_UP);
        scrollView.post(new Runnable() { 
            public void run() { 
            	scrollView.fullScroll(ScrollView.FOCUS_UP); 
            } 
        }); 
    }
	
	public void updateDisplay() {
		if (mCursor == null || !mCursor.requery()) {
			mCursor = managedQuery(getIntent().getData(),
					new String[] { "key", "clientid", "date(application_date, 'unixepoch') as application_date", 
					"date(issued_date, 'unixepoch') as issued_date", "amount", "installments", "status", 
					"date(disbursement_date, 'unixepoch') as disbursement_date", "balance", "payment_due",
					"arrears", "principal_arrears_30", "principal_arrears_90", "principal_arrears_180", 
					"principal_arrears_over180", } , 
					null, null, null);
		}
		
		mCursor.moveToFirst();
		if (mCursor.isAfterLast()) {
			return;
		}
		int clientCol = mCursor.getColumnIndex("clientid");
		if (clientCol != -1) {
			String client_id = mCursor.getString(clientCol);
			String client_name = Util.getFieldForKey(
					Store.Base.CONTENT_URI_BASE.buildUpon().appendEncodedPath("app/Client/" + client_id).build(),
					getContentResolver(), "name");

			TextView view = (TextView)findViewById(R.id.loan_client_name);
			view.setText(client_name);
		}
		
		setTextView(R.id.loan_name, "key", R.string.loan_prefix);
		setTextView(R.id.loan_application_date, "application_date", 0);
		setTextView(R.id.loan_issued_date, "issued_date", 0);
		setTextView(R.id.loan_amount, "amount", 0);
		setTextView(R.id.loan_installments, "installments", 0);
		if (setTextViewMap(R.id.loan_status, "status", R.array.status_map) >= 4) {
			setTextView(R.id.loan_disbursement_date, "disbursement_date", 0);
		} else {
			clearTextView(R.id.loan_disbursement_date);
		}
		setTextView(R.id.loan_balance, "balance", 0);
		setTextView(R.id.loan_arrears, "arrears", 0);
		setTextView(R.id.loan_arrears_30, "principal_arrears_30", 0, true);
		setTextView(R.id.loan_arrears_90, "principal_arrears_90", 0, true);
		setTextView(R.id.loan_arrears_180, "principal_arrears_180", 0, true);
		setTextView(R.id.loan_arrears_over180, "principal_arrears_over180", 0, true);
		setTextView(R.id.loan_normal_payment, "payment_due", 0);
		
	}

	public void setTextView(int viewid, String column_name, int prefix_id) {
		setTextView(viewid, column_name, prefix_id, false);
	}
	
	public void setTextView(int viewid, String column_name, int prefix_id, boolean hideIfZero) {
		int col = mCursor.getColumnIndex(column_name);
		if (col != -1) {
			TextView view = (TextView)findViewById(viewid);
			String prefix = "";
			if (prefix_id != 0) {
				prefix = getString(prefix_id) + " ";
			}
			String value = mCursor.getString(col);
			if (mCursor.isNull(col) || value.toLowerCase().equals("null")) {
				value = "";
			} else if (hideIfZero) {
				double doubleValue = -1;
				try {
					doubleValue = Double.parseDouble(value);
				} catch (NumberFormatException e) {
					// Do nothing
				}
				LinearLayout layout = (LinearLayout)view.getParent();
				layout.setVisibility(doubleValue == 0 ? View.GONE : View.VISIBLE);
			}
			view.setText(prefix + value);
		}
	}
	
	public void clearTextView(int viewid) {
		TextView view = (TextView)findViewById(viewid);
		view.setText("");
	}
	
	public int setTextViewMap(int viewid, String column_name, int arrayid) {
		String[] valueMap = getResources().getStringArray(arrayid);
		int col = mCursor.getColumnIndex(column_name);
		int index = -1;
		if (col != -1) {
			TextView view = (TextView)findViewById(viewid);
			index = mCursor.getInt(col);
			if (index >= 0 && index < valueMap.length) {
				view.setText(valueMap[index]);
			}
		}
		return index;
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        
        menu.add(0, MENU_ITEM_SYNC, 0, R.string.menu_sync)
        .setShortcut('4', 's')
        .setIcon(android.R.drawable.ic_menu_share);

        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
    	case MENU_ITEM_SYNC:
    		// Launch activity to sync
            ContentResolver.requestSync(null, Store.AUTHORITY, new Bundle());
    		return true;
    	}
        return super.onOptionsItemSelected(item);
    }

}
