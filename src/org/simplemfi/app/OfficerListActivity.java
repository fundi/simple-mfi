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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jsonstore.JsonStore;
import org.jsonstore.JsonStore.Meta;
import org.simplemfi.app.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

public class OfficerListActivity extends ListActivity {
	@SuppressWarnings("unused")
	private static final String TAG = "OfficerListActivity";

	// Menu item ids
	public static final int MENU_ITEM_ADD_OFFICER = Menu.FIRST;

	// Dialog ids
	public static final int DIALOG_ADD_OFFICER = 0;
	public static final int DIALOG_NEED_ADDITIONAL_OFFICER = 1;

	// Activity request ids
	public static final int REQUEST_SYNC_ACTIVITY = 0;
	public static final int REQUEST_INITIAL_SYNC_ACTIVITY = Util.REQUEST_INITIAL_SYNC_ACTIVITY;

	/** Called when the activity is first created. */
	Bundle mSavedInstanceState;

	public boolean mCreationComplete = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mSavedInstanceState = savedInstanceState;

		Intent intent = getIntent();
		if (intent.getData() == null) {
			intent.setData(
					JsonStore.Base.CONTENT_URI_BASE.buildUpon().appendEncodedPath("app/Officer").build());
		} 

		if (Util.startInitialSyncIfNecessary(getContentResolver(), this)) {
			return;
		}

		setTitle(R.string.officer_selection_title);

		Uri uri = getIntent().getData();
		Uri queryUri = uri.buildUpon().encodedPath(uri.getEncodedPath() + ".Client").build();
		final Cursor cursor = managedQuery(queryUri, new String[] { "Officer.key as key", "Officer.name" }, 
				"group by Officer.key", null, "Officer.name asc");

		final MatrixCursor addOfficer = new MatrixCursor(new String[] { "key", "Officer.name", "_id" });
		addOfficer.addRow(new Object[] { "add_officer", "Add Officer...", new Integer(Integer.MAX_VALUE) } );
		MergeCursor merge = new MergeCursor(new Cursor[] { cursor, addOfficer } );

		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, merge,
				new String[] { "Officer.name" }, new int[] { android.R.id.text1 });
		setListAdapter(adapter);

		getListView().setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Cursor cursor = (Cursor)getListAdapter().getItem(position);
				final int loc = cursor.getColumnIndex("key");
				String key = cursor.getString(loc);
				if (key.equals("add_officer")) {
					showDialog(DIALOG_ADD_OFFICER);
				} else {
					Uri.Builder builder = getIntent().getData().buildUpon();
					List<String> pathSegments = getIntent().getData().getPathSegments();
					builder.path(pathSegments.get(0) + "/Group");
					builder.encodedQuery("officerid=" + cursor.getString(loc));
					Uri clientUri = builder.build();
					Intent officerIntent = new Intent(Intent.ACTION_PICK, clientUri);
					officerIntent.putExtra("officerid", cursor.getString(loc));
					startActivity(officerIntent);
				}
			}
		});

		mCreationComplete = true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		case REQUEST_INITIAL_SYNC_ACTIVITY:
			if (resultCode == RESULT_OK) {
				// Select an officer, which will cause more sync to occur.
				showDialog(DIALOG_ADD_OFFICER);
			} else {
				// Just exit, the child died or had an error.
				finish();
			}
			break;
		case REQUEST_SYNC_ACTIVITY:
			// Ensure that we actually have data and that sync completed. 
			if (!org.jsonstore.Util.neededTablesArePresent(
		    		Meta.CONTENT_URI.buildUpon().appendEncodedPath("app/").build(),
		    		getContentResolver())) {
				// If not, ask for another officer to be added.
				showDialog(DIALOG_NEED_ADDITIONAL_OFFICER);
			} else {
				// Finish creating the activity, now that we have all the data.
				if (!mCreationComplete) {
					onCreate(mSavedInstanceState);
				}
			}
			break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(0, MENU_ITEM_ADD_OFFICER, 0, R.string.menu_add_officer)
		.setShortcut('4', 'a')
		.setIcon(android.R.drawable.ic_menu_add);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {    		
		case MENU_ITEM_ADD_OFFICER:
			showDialog(DIALOG_ADD_OFFICER);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected Dialog onCreateDialog(final int id) {
		switch (id) {
		case DIALOG_ADD_OFFICER:
		{
			final Cursor cursor = managedQuery(getIntent().getData(), new String[] { "key", "name" }, 
					null, null, "name asc");
			List<String> itemList = new ArrayList<String>();
			List<String> keyList = new ArrayList<String>();
			Set<String> currentOfficers = Util.getSyncedOfficers(getContentResolver());
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				String name = cursor.getString(cursor.getColumnIndex("name"));
				String key = cursor.getString(cursor.getColumnIndex("key"));
				if (!currentOfficers.contains(key)) {
					itemList.add(name);
					keyList.add(key);
				}
				cursor.moveToNext();
			}
			final String[] items = itemList.toArray(new String[itemList.size()]);
			final String[] keys = keyList.toArray(new String[itemList.size()]);
			
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Pick an officer");
			builder.setItems(items, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					Util.addSyncedOfficer(getContentResolver(), keys[item]);

					Toast.makeText(getApplicationContext(), 
							"Added officer " + items[item] + ". Starting sync.", Toast.LENGTH_SHORT).show();
					dialog.dismiss();
					removeDialog(id);

					startActivityForResult(
							new Intent(Intent.ACTION_SYNC, 
									Meta.CONTENT_URI.buildUpon().appendEncodedPath("app/").build()),
									REQUEST_SYNC_ACTIVITY);
				}
			});
			AlertDialog alert = builder.create();
			return alert;
		}
			
		case DIALOG_NEED_ADDITIONAL_OFFICER:
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("The officer you selected has no associated loan or transaction data. " 
					+ "Please select another officer.")
			       .setCancelable(false)
			       .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			        	   dialog.dismiss();
			        	   showDialog(DIALOG_ADD_OFFICER);
			           }
			       })
			       .setTitle("Error")
			       .setIcon(android.R.drawable.ic_dialog_alert);
			AlertDialog alert = builder.create();
			alert.show();
			return null;
		}
		}

		return super.onCreateDialog(id);
	}

}
