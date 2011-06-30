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

import org.simplemfi.app.R;

import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.database.Cursor;

public class ClientListActivity extends ListActivity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.clients);
        
        final Uri uri = getIntent().getData();
        Cursor cursor = managedQuery(uri, new String[] { "key", "name" }, null, null,
                Util.DEFAULT_SORT_ORDER);

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, cursor,
                new String[] { "name" }, new int[] { android.R.id.text1 });
        setListAdapter(adapter);
        
        getListView().setOnItemClickListener(new OnItemClickListener() {
        	public void onItemClick(AdapterView<?> parent, View view,
        			int position, long id) {
                Cursor cursor = (Cursor)getListAdapter().getItem(position);
                int loc = cursor.getColumnIndex("key");
                if (loc != -1) {
                	Uri clientUri = uri.buildUpon().appendPath(cursor.getString(loc)).query("").build();
                	startActivity(new Intent(Intent.ACTION_VIEW, clientUri));
                }
        	}	
        });
        
    }
}
