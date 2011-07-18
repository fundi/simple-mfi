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

import org.mantasync.Store.Meta;
import org.simplemfi.app.R;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TabHost;

public class GroupTabActivity extends TabActivity {
    
    // Menu item ids
    public static final int MENU_ITEM_SYNC = Menu.FIRST;
    public static final int MENU_ITEM_CHANGE_GROUP = Menu.FIRST + 1;
    
    /** Called when the activity is first created. */
	private Uri mURI;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        
        mURI = getIntent().getData();
        
        String groupName = Util.getFieldForKey(
        		mURI.buildUpon().path(mURI.getPathSegments().get(0)).appendPath("Group").build(),
        		getContentResolver(), "name");
        if (groupName.length() == 0) {
        	groupName = getString(R.string.client_list_title);
        }
        setTitle(groupName);
        getIntent().putExtra("group_name", groupName);
        
        Resources res = getResources(); // Resource object to get Drawables
        TabHost tabHost = getTabHost();  // The activity TabHost
        TabHost.TabSpec spec;  // Resusable TabSpec for each tab
        Intent intent; // Reusable Intent for each tab

        // Create an Intent to launch an Activity for the tab (to be reused)
        Uri collectionUri = mURI.buildUpon().path(mURI.getPathSegments().get(0)).
        	appendPath("Client.Loan").build();
        intent = new Intent(Intent.ACTION_VIEW, collectionUri, this, CollectionSheetActivity.class);
        // Initialize a TabSpec for each tab and add it to the TabHost
        spec = tabHost.newTabSpec("collection").setIndicator("Collection Sheet",
                          res.getDrawable(android.R.drawable.ic_menu_agenda))
                      .setContent(intent);
        tabHost.addTab(spec);

        // Do the same for the other tabs
        Uri receiptUri = mURI.buildUpon().path(mURI.getPathSegments().get(0)).appendPath("Client.Transaction").build();
        intent = new Intent(Intent.ACTION_VIEW, receiptUri, this, GroupReceiptsActivity.class);
        spec = tabHost.newTabSpec("receipt").setIndicator("Receipts",
                          res.getDrawable(android.R.drawable.ic_dialog_email))
                      .setContent(intent);
        tabHost.addTab(spec);
        
        // Do the same for the other tabs
        intent = new Intent(Intent.ACTION_VIEW, mURI, this, ClientListActivity.class);
        spec = tabHost.newTabSpec("clients").setIndicator("Clients",
                          res.getDrawable(R.drawable.ic_menu_allfriends))
                      .setContent(intent);
        tabHost.addTab(spec);

        tabHost.setCurrentTab(0);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        
        menu.add(0, MENU_ITEM_SYNC, 0, R.string.menu_sync)
        .setShortcut('4', 's')
        .setIcon(android.R.drawable.ic_menu_share);
        
        menu.add(0, MENU_ITEM_CHANGE_GROUP, 0, R.string.menu_change_group)
        .setShortcut('5', 'g')
        .setIcon(R.drawable.ic_menu_friendslist);
        
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
    	case MENU_ITEM_SYNC:
    		// Launch activity to sync
    		startActivity(new Intent(Intent.ACTION_SYNC, 
    				Meta.CONTENT_URI.buildUpon().appendEncodedPath("app/").build()));
    		return true;
    		
    	case MENU_ITEM_CHANGE_GROUP:
    		Uri.Builder builder = getIntent().getData().buildUpon();
        	List<String> pathSegments = getIntent().getData().getPathSegments();
        	builder.path(pathSegments.get(0) + "/Group");
    		String officerid = getIntent().getStringExtra("officerid");
    		if (officerid == null) {
    			officerid = Util.getFieldForKey(
    	        		mURI.buildUpon().path(mURI.getPathSegments().get(0)).appendPath("Group").build(),
    	        		getContentResolver(), "officerid");
    		}
    		builder.query("");
    		if (officerid.length() > 0) {
    			builder.encodedQuery("officerid=" + officerid);
    		}
        	Uri clientUri = builder.build();
        	Intent officerIntent = new Intent(Intent.ACTION_PICK, clientUri);
        	startActivity(officerIntent);
    		return true;
    	}
        return super.onOptionsItemSelected(item);
    }
    
}
