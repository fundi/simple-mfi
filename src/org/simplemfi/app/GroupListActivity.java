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
import org.simplemfi.app.R;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.OnItemClickListener;

public class GroupListActivity extends ListActivity {

    // Menu item ids
    public static final int MENU_ITEM_CHANGE_OFFICER = Menu.FIRST;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.officer_selection_title);
        
        Intent mainIntent = getIntent();
        if (mainIntent.getData() == null) {
        	mainIntent.setData(
        			JsonStore.Base.CONTENT_URI_BASE.buildUpon().appendEncodedPath("app/Group").build());
        } 
       
        final Uri uri = mainIntent.getData();
        Cursor cursor = managedQuery(uri, new String[] { "key", "name" }, null, null, "name asc");

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, cursor,
                new String[] { "name" }, new int[] { android.R.id.text1 });
        setListAdapter(adapter);
        
        getListView().setOnItemClickListener(new OnItemClickListener() {
        	public void onItemClick(AdapterView<?> parent, View view,
        			int position, long id) {
                Cursor cursor = (Cursor)getListAdapter().getItem(position);
                int loc = cursor.getColumnIndex("key");
                if (loc != -1) {
                	Uri.Builder builder = getIntent().getData().buildUpon();
                	List<String> pathSegments = getIntent().getData().getPathSegments();
                	builder.path(pathSegments.get(0) + "/Client");
                	builder.encodedQuery("groupid=" + cursor.getString(loc));
                	Uri clientUri = builder.build();
                	Intent groupIntent = new Intent(Intent.ACTION_PICK_ACTIVITY, clientUri);
                	groupIntent.putExtra("groupid", cursor.getString(loc));
                	startActivity(groupIntent);
                }
        	}
        });
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        
        menu.add(0, MENU_ITEM_CHANGE_OFFICER, 0, R.string.menu_change_officer)
        .setShortcut('4', 'o')
        .setIcon(R.drawable.ic_menu_cc);
        
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {    		
    	case MENU_ITEM_CHANGE_OFFICER:
    		Uri.Builder builder = getIntent().getData().buildUpon();
        	List<String> pathSegments = getIntent().getData().getPathSegments();
        	builder.path(pathSegments.get(0) + "/Officer");
    		builder.query("");
        	Uri clientUri = builder.build();
        	Intent officerIntent = new Intent(Intent.ACTION_PICK, clientUri);
        	startActivity(officerIntent);
    		return true;
    	}
        return super.onOptionsItemSelected(item);
    }
}
