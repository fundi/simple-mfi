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

import java.util.Date;
import java.util.List;

import org.simplemfi.app.R;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SimpleCursorAdapter.ViewBinder;
import android.widget.TextView;

public class CollectionSheetActivity extends ListActivity {
	static final String TAG = "CollectionSheetActivity";
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.collection);
        
        TextView header = (TextView)findViewById(R.id.collection_header);
        header.setText(DateFormat.format("MMMM yyyy", new Date()));
        
        Uri uri = getIntent().getData();
        Cursor cursor = managedQuery(uri, new String[] { "key", "name", 
        		"round(case when Loan.balance - arrears > 0 then payment_due + arrears else arrears end) as payment_due", 
        		"arrears", "principal_arrears_30", "principal_arrears_90", "principal_arrears_180", "principal_arrears_over180" }, 
        		"Loan.balance > 0", null, Util.DEFAULT_SORT_ORDER);

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.collection_item, cursor,
                new String[] { "name", "payment_due", "arrears" }, 
                new int[] { android.R.id.text1, android.R.id.text2, R.id.list_item });
        setListAdapter(adapter);
        
        getListView().setOnItemClickListener(new OnItemClickListener() {
        	public void onItemClick(AdapterView<?> parent, View view,
        			int position, long id) {
                Cursor cursor = (Cursor)getListAdapter().getItem(position);
                int loc = cursor.getColumnIndex("key");
                if (loc != -1) {
                	Uri.Builder builder = getIntent().getData().buildUpon();
                	List<String> pathSegments = getIntent().getData().getPathSegments();
                	builder.path(pathSegments.get(0) + "/Client/" + cursor.getString(loc));
                	builder.query("");
                	Uri clientUri = builder.build();
                	startActivity(new Intent(Intent.ACTION_VIEW, clientUri));
                }
        	}	
        });
        
        // Color rows based on arrears amount and severity.
        adapter.setViewBinder(new ViewBinder() {
        	int mArrearsColumn = -1;
        	int mFirstCategory = -1;
        	final int mNumCategories = 4;
			@Override
			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
				if (mArrearsColumn == -1) {
					mArrearsColumn = cursor.getColumnIndex("arrears");
					mFirstCategory = mArrearsColumn + 1;
				}
				if (columnIndex != mArrearsColumn) {
					return false;
				}
				double arrears = cursor.getDouble(mArrearsColumn);
				if (arrears <= 0) {
					view.setBackgroundResource(R.layout.list_item_black);
					return true;
				}
				int category = 0;
				for (int i = mFirstCategory; i < mFirstCategory + mNumCategories; ++i) {
					if (cursor.getDouble(i) > 0) {
						category = i - mFirstCategory;
					}
				}
				switch (category) {
				case 0:
					view.setBackgroundResource(R.layout.list_item_yellow);
					break;
				case 1:
					view.setBackgroundResource(R.layout.list_item_orange);
					break;
				case 2:
				case 3:
					view.setBackgroundResource(R.layout.list_item_red);
					break;
				}
				return true;
			}
		});
    }
}
