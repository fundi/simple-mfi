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

import org.jsonstore.JsonStore;
import org.jsonstore.JsonStore.Meta;
import org.simplemfi.app.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;

public class Util {

    /**
     * The default sort order for this table
     */
    public static final int REQUEST_INITIAL_SYNC_ACTIVITY = 30000;
    
	static private boolean mSyncIsNecessary = true;
	
	static public String DEFAULT_SORT_ORDER = "name asc";
	
	static public boolean startInitialSyncIfNecessary(ContentResolver resolver, final Activity context) {
		if (!mSyncIsNecessary) {
			return false;
		}
		
		ProviderInfo info = context.getPackageManager().resolveContentProvider(JsonStore.AUTHORITY, 0);
		if (info == null) {
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			String app_name = context.getString(R.string.app_name);
			builder.setMessage(app_name + " can't be started, because the needed application "
					+ "\"" + JsonStore.APPLICATION_NAME + "\" is missing.\n\n"
					+ "Please install the application \"" + JsonStore.APPLICATION_NAME + "\", and then try again.")
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
	    org.jsonstore.Util.enableGoogleAccountsForSync(context);
	    
		resolver.insert(Meta.CONTENT_URI.buildUpon().appendEncodedPath("app/Officer").build(), new ContentValues());
	
	    boolean tablesPresent = org.jsonstore.Util.neededTablesArePresent(
	    		Meta.CONTENT_URI.buildUpon().appendEncodedPath("app/").build(),
	    		resolver);
	    
	    if (!tablesPresent) {
	    	// launch activity
	    	context.startActivityForResult(
	    			new Intent(Intent.ACTION_SYNC, Meta.CONTENT_URI.buildUpon().appendEncodedPath("app/").build()),
	    			Util.REQUEST_INITIAL_SYNC_ACTIVITY);
	    } else {
	    	mSyncIsNecessary = false;
	    }
	    return mSyncIsNecessary;
	}
	
	static public void addSyncedOfficer(ContentResolver resolver, String officerid) {
		ContentValues values = new ContentValues();
	    resolver.insert(Meta.CONTENT_URI.buildUpon().appendEncodedPath("app/Group").
	    		appendQueryParameter("officerid", officerid).build(), values);
	    resolver.insert(Meta.CONTENT_URI.buildUpon().appendEncodedPath("app/Client").
	    		appendQueryParameter("officerid", officerid).build(), values);
	    resolver.insert(Meta.CONTENT_URI.buildUpon().appendEncodedPath("app/Loan").
	    		appendQueryParameter("officerid", officerid).build(), values);
	    resolver.insert(Meta.CONTENT_URI.buildUpon().appendEncodedPath("app/Transaction").
	    		appendQueryParameter("officerid", officerid).build(), values);
	    
	    mSyncIsNecessary = true;
	}
	
	static public Set<String> getSyncedOfficers(ContentResolver resolver) {
		Set<String> officerids = new HashSet<String>();
        Cursor c = resolver.query(Meta.CONTENT_URI.buildUpon().appendEncodedPath("app/").build(), 
        		null, null, null, null);
        c.moveToFirst();
        while (!c.isAfterLast()) {
        	String pathQuery = c.getString(c.getColumnIndex(Meta.PATH_QUERY));
        	Uri uri = Uri.parse(Meta.CONTENT_URI.toString() + pathQuery);
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
}
