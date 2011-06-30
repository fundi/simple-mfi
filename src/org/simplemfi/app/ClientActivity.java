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

import org.jsonstore.JsonStore;
import org.jsonstore.JsonStore.Base;
import org.simplemfi.app.R;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class ClientActivity extends Activity {

    private static final String TAG = "ClientActivity";

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
        
        Uri uri = getIntent().getData();
        List<String> path = uri.getPathSegments();
        
        setContentView(R.layout.client);
        
        {
            final Uri loanUri = uri.buildUpon().path(path.get(0)).appendPath("Loan").build();
            Cursor cursor = managedQuery(loanUri, new String[] { "key", "loanid", "amount",
            		"case balance when 'NULL' then '--' else round(balance) end as balance" }, 
            		"clientid = ?", new String[] { path.get(2) }, Base.DEFAULT_SORT_ORDER);

            SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.client_item, cursor,
                    new String[] { "loanid", "amount", "balance" }, 
                    new int[] { android.R.id.text1, android.R.id.text2, R.id.text3 });
            final ListView listView = ((ListView)findViewById(R.id.loans));
            listView.setAdapter(adapter);
            
            listView.setOnItemClickListener(new OnItemClickListener() {
            	public void onItemClick(AdapterView<?> parent, View view,
            			int position, long id) {
                    Cursor cursor = (Cursor)listView.getAdapter().getItem(position);
                    int loc = cursor.getColumnIndex("key");
                    if (loc != -1) {
                    	Uri uri = loanUri.buildUpon().appendPath(cursor.getString(loc)).query("").build();
                    	startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    }
            	}	
            });
        }
        
        {
            Uri paymentUri = uri.buildUpon().path(path.get(0)).appendPath("Transaction").build();
            Cursor cursor = managedQuery(paymentUri, 
            		new String[] { "date(posting_date,'unixepoch') as posting_date", "description", "round(amount) as amount" }, 
            		"clientid = ?", new String[] { path.get(2) }, "posting_date desc");

            SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.client_item, cursor,
            		new String[] { "posting_date", "description", "amount" }, 
            		new int[] { android.R.id.text1, android.R.id.text2, R.id.text3 });
            ((ListView)findViewById(R.id.transactions)).setAdapter(adapter);
        }
        
        getContentResolver().registerContentObserver(getIntent().getData(), false, new ChangeObserver());
        
        updateDisplay();
    }
	
	public void updateDisplay() {
		if (mCursor == null || !mCursor.requery()) {
			mCursor = managedQuery(getIntent().getData(), null, null, null, null);
		}

		mCursor.moveToFirst();
		if (mCursor.isAfterLast()) {
			return;
		}
		int keyCol = mCursor.getColumnIndex(JsonStore.Base.KEY);
		int nameCol = mCursor.getColumnIndex("name");
		if (keyCol != -1 && nameCol != -1) {
			TextView name = (TextView)findViewById(R.id.client_name);
			name.setText(mCursor.getString(keyCol) + ": " + mCursor.getString(nameCol));
		}
		
		int balanceCol = mCursor.getColumnIndex("balance");
		if (balanceCol != -1) {
			TextView savings = (TextView)findViewById(R.id.savings);
			String savings_title = getString(R.string.client_savings);
			savings.setText(savings_title + " " + mCursor.getLong(balanceCol));
		}
		
		Uri uri = getIntent().getData();

		String groupName = getIntent().getStringExtra("group_name");
		if (groupName == null) {
			List<String> path = uri.getPathSegments();
			Uri groupUri = uri.buildUpon().path(path.get(0)).appendPath("Group")
							.appendPath(mCursor.getString(mCursor.getColumnIndex("groupid"))).build();
			groupName = Util.getFieldForKey(groupUri, getContentResolver(), "name");
		}
        setTitle(groupName);
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
            ContentResolver.requestSync(null, JsonStore.AUTHORITY, new Bundle());
    		return true;
    	}
        return super.onOptionsItemSelected(item);
    }

}
