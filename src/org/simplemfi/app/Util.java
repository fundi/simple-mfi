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

import java.util.HashSet;
import java.util.Set;

import org.mantasync.Store;
import org.mantasync.Store.Meta_Mapping;
import org.mantasync.Store.Meta_Table;
import org.simplemfi.app.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.text.Editable;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.MeasureSpec;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class Util {
    private static final String TAG = "ClientActivity";
    
    /**
     * The default sort order for most Simple MFI tables
     */
	static public String DEFAULT_SORT_ORDER = "name asc";
	
    public static final int REQUEST_INITIAL_SYNC_ACTIVITY = 30000;
    
	static private boolean mSyncIsNecessary = true;
	
	
	static public boolean startInitialSyncIfNecessary(ContentResolver resolver, final Activity context) {
		if (!mSyncIsNecessary) {
			return false;
		}
		
		ProviderInfo info = context.getPackageManager().resolveContentProvider(Store.AUTHORITY, 0);
		if (info == null) {
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			String app_name = context.getString(R.string.app_name);
			builder.setMessage(app_name + " can't be started, because the needed application "
					+ "\"" + Store.APPLICATION_NAME + "\" is missing.\n\n"
					+ "Please install the application \"" + Store.APPLICATION_NAME + "\", and then try again.")
			       .setCancelable(false)
			       .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			                context.finish();
			           }
			       })
			       .setTitle("Error")
			       .setIcon(android.R.drawable.ic_dialog_alert);
			AlertDialog alert = builder.create();
			alert.show();
			return true;
		}

		// We were just launched. Check to see if we have our initial data.
	    Cursor c = resolver.query(Meta_Mapping.CONTENT_URI.buildUpon().appendEncodedPath(Constants.APP).build(),
	    		null, null, null, null);
	    if (!c.moveToFirst()) {
	    	askForApplicationMapping(resolver, context);
	    	return true;
	    }
		
	    org.mantasync.Util.enableGoogleAccountsForSync(context);
		resolver.insert(Meta_Table.CONTENT_URI.buildUpon().appendEncodedPath(Constants.APP + "/Officer").build(), new ContentValues());
	    boolean tablesPresent = org.mantasync.Util.neededTablesArePresent(
	    		Meta_Table.CONTENT_URI.buildUpon().appendEncodedPath(Constants.APP + "/").build(),
	    		resolver);
	    
	    if (!tablesPresent) {
	    	// launch activity
	    	context.startActivityForResult(
	    			new Intent(Intent.ACTION_SYNC, Meta_Table.CONTENT_URI.buildUpon().appendEncodedPath(Constants.APP + "/").build()),
	    			Util.REQUEST_INITIAL_SYNC_ACTIVITY);
	    } else {
	    	mSyncIsNecessary = false;
	    }
	    return mSyncIsNecessary;
	}
	
	static public void askForApplicationMapping(final ContentResolver resolver, final Activity context) {
   		final EditText input = new EditText(context);
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder
	    .setTitle("Select Organization")
	    .setMessage("Enter the organization name to use with this application:")
	    .setView(input)
	    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	            Editable value = input.getText();
	    	    ContentValues values = new ContentValues();
	    	    values.put(Meta_Mapping.MAPPED_APP, value.toString());
	    	    resolver.insert(Meta_Mapping.CONTENT_URI.buildUpon().appendEncodedPath(Constants.APP).build(), values);
	            dialog.dismiss();
	            // Restart the initial sync check, which should now get past the mapping check.
	            startInitialSyncIfNecessary(resolver, context);
	        }
	    }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	            context.finish();
	        }
	    });
		Dialog alert = builder.create();
		alert.show();
	}
	
	static public void addSyncedOfficer(ContentResolver resolver, String officerid) {
		ContentValues values = new ContentValues();
	    resolver.insert(Meta_Table.CONTENT_URI.buildUpon().appendEncodedPath(Constants.APP + "/Group").
	    		appendQueryParameter("officerid", officerid).build(), values);
	    resolver.insert(Meta_Table.CONTENT_URI.buildUpon().appendEncodedPath(Constants.APP + "/Client").
	    		appendQueryParameter("officerid", officerid).build(), values);
	    resolver.insert(Meta_Table.CONTENT_URI.buildUpon().appendEncodedPath(Constants.APP + "/Loan").
	    		appendQueryParameter("officerid", officerid).build(), values);
	    resolver.insert(Meta_Table.CONTENT_URI.buildUpon().appendEncodedPath(Constants.APP + "/Transaction").
	    		appendQueryParameter("officerid", officerid).build(), values);
	    
	    mSyncIsNecessary = true;
	}
	
	static public Set<String> getSyncedOfficers(ContentResolver resolver) {
		Set<String> officerids = new HashSet<String>();
        Cursor c = resolver.query(Meta_Table.CONTENT_URI.buildUpon().appendEncodedPath(Constants.APP + "/").build(), 
        		null, null, null, null);
        c.moveToFirst();
        while (!c.isAfterLast()) {
        	String pathQuery = c.getString(c.getColumnIndex(Meta_Table.PATH_QUERY));
        	Uri uri = Uri.parse(Meta_Table.CONTENT_URI.toString() + pathQuery);
        	String officerid = uri.getQueryParameter("officerid");
        	if (officerid != null) {
        		officerids.add(officerid);
        	}
        	c.moveToNext();
        }
        c.close();
        return officerids;
	}
	
	static public String getFieldForKey(Uri uri, ContentResolver resolver, String field) {
		String result = "";
		Cursor c = resolver.query(uri, new String[] { field }, null, null, null);
        c.moveToFirst();
        while (!c.isAfterLast()) {
        	if (result.length() > 0) {
        		result += ", ";
        	}
        	result += c.getString(c.getColumnIndex(field));
        	c.moveToNext();
        }
        c.close();
        return result;
	}
	
	public static void setListViewHeightBasedOnChildren(ListView listView) {
        final ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return;
        }
        int totalHeight = 0;
        int desiredWidth = MeasureSpec.makeMeasureSpec(listView.getWidth(), MeasureSpec.AT_MOST);
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(desiredWidth, MeasureSpec.UNSPECIFIED);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }

	public static void addReportErrorContextMenu(final Activity activity, ListView listView, final Uri baseUri) {
		listView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
			@Override
			public void onCreateContextMenu(ContextMenu menu, View v,
					ContextMenuInfo menuInfo) {
		    	MenuInflater inflater = activity.getMenuInflater();
		    	inflater.inflate(R.menu.report_error_context_menu, menu);
		        AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuInfo;
		        ListView listView = (ListView)v;
                Cursor cursor = (Cursor)listView.getAdapter().getItem(info.position);
                int loc = cursor.getColumnIndex("key");
                if (loc != -1) {
                	String key = cursor.getString(loc);
                	Uri uri = baseUri.buildUpon().appendPath(key).query("").build();
                	menu.getItem(0).setIntent(new Intent(Intent.ACTION_EDIT, uri));
                }
			}
		});
	}
	
	public static boolean handleReportErrorContextMenu(MenuItem item, ContentResolver resolver) {
		if (item.getItemId() == R.id.report_error) {
			Uri uri = item.getIntent().getData();
			if (uri != null) {
				ContentValues values = new ContentValues();
				values.put("error_flagged", true);
				if (resolver.update(uri, values, null, null) != 1) {
					Log.e(TAG, "Could not report error for URI: " + uri.toString());
				}
			}
			return true;
		}
		return false;
	}
}
