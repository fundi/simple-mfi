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


import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.mantasync.Store;
import org.simplemfi.app.R;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
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
    
    private static final SimpleDateFormat sDueDateFormat = new SimpleDateFormat("yyyy-MM-dd");

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
	
	private Cursor mCursor = null;
	private Cursor mStatementCursor = null;
	private Cursor mScheduleCursor = null;
	
	private SimpleCursorAdapter mAdapter = null;
	private ListView mListView = null;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.loan_detail_title);
        setContentView(R.layout.loan);
       
        getContentResolver().registerContentObserver(getIntent().getData(), false, new ChangeObserver());
        mListView = ((ListView)findViewById(R.id.loan_schedule));
        
        updateDisplay();
 
        mAdapter = new SimpleCursorAdapter(this, R.layout.loan_schedule_item, mScheduleCursor,
                new String[] { "installment", "due_date", "amount", "paid" }, 
                new int[] { android.R.id.text1, android.R.id.text2, R.id.text3, R.id.text4 });
        mListView.setAdapter(mAdapter);
        Util.setListViewHeightBasedOnChildren(mListView);
        
        final ScrollView scrollView = (ScrollView)findViewById(R.id.loan_scroll);
        scrollView.fullScroll(ScrollView.FOCUS_UP);
        scrollView.post(new Runnable() { 
            public void run() { 
            	scrollView.fullScroll(ScrollView.FOCUS_UP); 
            } 
        }); 
    }
	
    private void setScheduleCursor(Cursor cursor) {
    	if (mScheduleCursor != null) {
    		mScheduleCursor.close();
    	}
    	mScheduleCursor = cursor;
    	if (mAdapter != null) {
    		mAdapter.changeCursor(mScheduleCursor);
    	}
    	if (mListView != null) {
	    	mScheduleCursor.registerContentObserver(new ContentObserver(new Handler()) {
	        	@Override
	        	public void onChange(boolean selfChange) {
	        		Util.setListViewHeightBasedOnChildren(mListView);
	        	}
			});
    	}
    }
    
	public void updateDisplay() {
		if (mCursor == null || !mCursor.requery()) {
			mCursor = managedQuery(getIntent().getData(),
					new String[] { "key", "clientid", "date(application_date, 'unixepoch') as application_date", 
					"date(issued_date, 'unixepoch') as issued_date", "amount", "installments", "status", 
					"date(disbursement_date, 'unixepoch') as disbursement_date", "balance", "payment_due",
					"arrears", "principal_arrears_30", "principal_arrears_90", "principal_arrears_180", 
					"principal_arrears_over180", "grace_period", "grace_period_pays_interest",
					"(interest_rate * 100) as interest_rate", "interest_method", 
					"(disbursement_date) as disbursement_date_secs"
					} , 
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
		setTextView(R.id.loan_normal_payment, "payment_due", 0);
		setTextView(R.id.loan_grace_period, "grace_period", 0);
		setTextViewMap(R.id.loan_grace_period_pays_interest, "grace_period_pays_interest", R.array.grace_period_pays_interest_map);
		setTextView(R.id.loan_interest_rate, "interest_rate", 0);
		setTextViewMap(R.id.loan_interest_method, "interest_method", R.array.interest_method_map);
		setTextView(R.id.loan_arrears, "arrears", 0);
		setTextView(R.id.loan_arrears_30, "principal_arrears_30", 0, true);
		setTextView(R.id.loan_arrears_90, "principal_arrears_90", 0, true);
		setTextView(R.id.loan_arrears_180, "principal_arrears_180", 0, true);
		setTextView(R.id.loan_arrears_over180, "principal_arrears_over180", 0, true);
		setTextView(R.id.loan_balance, "balance", 0);
		
		MatrixCursor matrix = new MatrixCursor(new String[]{ "_id", "installment", "due_date", "amount", "paid"});
	
		double amount = getNamedDouble("amount"), interest_rate = getNamedDouble("interest_rate"), 
			payment_due = getNamedDouble("payment_due");
		int grace_period = getNamedInt("grace_period"), 
			grace_period_pays_interest = getNamedInt("grace_period_pays_interest"),
			installments = getNamedInt("installments"),
			interest_method = getNamedInt("interest_method");
		long disbursement_date = getNamedInt("disbursement_date_secs");
		
		String client_id = getNamedString("clientid");
		
		if (mStatementCursor == null || !mCursor.requery()) {
	        Uri uri = getIntent().getData();
	        List<String> path = uri.getPathSegments();
	        final Uri statementUri = uri.buildUpon().path(path.get(0)).appendPath("Transaction").build();
	        mStatementCursor = managedQuery(statementUri, new String[] { 
	        		"((strftime('%Y', posting_date - " + disbursement_date + ", 'unixepoch', '-15 days') - 1970) * 12) + "+
	        		"strftime('%m', posting_date - " + disbursement_date + ", 'unixepoch', '-15 days')  as installment", 
	        		"round(sum(amount)) as amount" }, 
	        		"clientid = ? and \"Transaction\".posting_date > " + disbursement_date + 
	        		" and (transaction_type = 3 or transaction_type = 6) group by " +
	        		"strftime('%Y-%m', posting_date - " + disbursement_date + ", 'unixepoch', '-15 days') ",
	        		new String[] { client_id }, 
	        		"installment asc");
		}
		int oldPos = mStatementCursor.getPosition();
		mStatementCursor.moveToFirst();

		int installmentCol = mStatementCursor.getColumnIndex("installment");
		int amountCol = mStatementCursor.getColumnIndex("amount");
		
		double balance = amount;
		for (int i = 0; i < grace_period + installments; ++i) {
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(disbursement_date * 1000);
			cal.add(Calendar.MONTH, i + 1);
			Date due_date = cal.getTime();
			double principal_payment = 0;
			double interest_payment = 0;
			int interest_installments = grace_period_pays_interest > 0 ? installments + grace_period : installments;
			switch (interest_method) {
			// TODO Use an enum for these values, defined in R.arrays.interest_method_map
			case 2:
				interest_payment = ((interest_rate / 100) / (interest_installments)) * amount;
				break;
			case 3:
				interest_payment = ((interest_rate / 100) / 12) * balance;
				break;
			default:
				interest_payment = 0;
				break;
			}
			principal_payment = payment_due - interest_payment;
			double payment = interest_payment + principal_payment;
			
			if (i < grace_period) {
				principal_payment = 0;
				if (grace_period_pays_interest > 0) {
					payment = interest_payment;
				} else {
					payment = 0;
				}
			}
			double paidAmount = 0;
			while (!mStatementCursor.isAfterLast() && mStatementCursor.getInt(installmentCol) < i + 1) {
				mStatementCursor.moveToNext();
			}
			if (!mStatementCursor.isAfterLast() && mStatementCursor.getInt(installmentCol) == i + 1) {
				paidAmount = mStatementCursor.getDouble(amountCol);
			}
			
			matrix.addRow(new Object[]{ i, i + 1, sDueDateFormat.format(due_date), Math.round(payment), -Math.round(paidAmount) });
			Log.e(TAG, "Schedule: " +  (i + 1) + " " + sDueDateFormat.format(due_date) + " " + payment + " " + paidAmount);
			balance -= principal_payment;
		}
		mStatementCursor.moveToPosition(oldPos);
		
		setScheduleCursor(matrix);
	}

	public double getNamedDouble(String column_name) {
		int col = mCursor.getColumnIndex(column_name);
		if (col != -1) {
			return mCursor.getDouble(col);
		}
		return 0.0;
	}
	
	public int getNamedInt(String column_name) {
		int col = mCursor.getColumnIndex(column_name);
		if (col != -1) {
			return mCursor.getInt(col);
		}
		return 0;
	}
	
	public long getNamedLong(String column_name) {
		int col = mCursor.getColumnIndex(column_name);
		if (col != -1) {
			return mCursor.getLong(col);
		}
		return 0;
	}
	
	public String getNamedString(String column_name) {
		int col = mCursor.getColumnIndex(column_name);
		if (col != -1) {
			return mCursor.getString(col);
		}
		return "";
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
