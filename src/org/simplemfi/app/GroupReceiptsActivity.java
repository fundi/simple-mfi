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

import org.simplemfi.app.R;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SimpleCursorAdapter.ViewBinder;

public class GroupReceiptsActivity extends ListActivity {
	static final String TAG = "GroupReceiptsActivity";
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.receipts);
        
        Uri uri = getIntent().getData();
        Cursor cursor = managedQuery(uri, new String[] { "Client.key", "name", 
        	"(date(posting_date,'unixepoch') || ': #' || documentid) as section", "sum(abs(amount)) as amount" }, 
        		"transaction_type != 2 group by Client.key, name, documentid", null, 
        		"posting_date desc, " + Util.DEFAULT_SORT_ORDER);
        
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.collection_item, cursor,
                new String[] { "name", "amount", "section" }, 
                new int[] { android.R.id.text1, android.R.id.text2, R.id.section } );
        setListAdapter(adapter);
        
        getListView().setOnItemClickListener(new OnItemClickListener() {
        	public void onItemClick(AdapterView<?> parent, View view,
        			int position, long id) {
                Cursor cursor = (Cursor)getListAdapter().getItem(position);
                int loc = cursor.getColumnIndex("Client.key");
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
        
        // Quick and easy way to create sections, from a group by.
        // TODO Extract this to a utility class
        adapter.setViewBinder(new ViewBinder() {
        	int mSectionColumn = -1;
			@Override
			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
				if (mSectionColumn == -1) {
					mSectionColumn = cursor.getColumnIndex("section");
				}
				if (columnIndex != mSectionColumn) {
					return false;
				}
				String thisSection = cursor.getString(mSectionColumn);
			    String prevSection = "";

			    // previous row, for comparison
			    if (cursor.getPosition() > 0 && cursor.moveToPrevious()) {
			        prevSection = cursor.getString(mSectionColumn);
			        cursor.moveToNext();
			    }
				
			    boolean isSection = !thisSection.contentEquals(prevSection);
			    view.setVisibility(isSection ? View.VISIBLE : View.GONE);
			    view.setFocusable(!isSection);
			    // Have the normal ViewBinder set the text for us.
				return false;
			}
		});
        
    }
}
