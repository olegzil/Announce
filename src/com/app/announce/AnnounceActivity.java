package com.app.announce;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.app.announce.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.telephony.SmsManager;
import android.util.Log;


public class AnnounceActivity extends Activity {
    // Request code for the contact picker activity
    public static final int PICK_CONTACT_REQUEST = 1;
    private boolean mExternalStorageAvailable = false;
    private boolean mExternalStorageWriteable = false;
    private BroadcastReceiver mExternalStorageReceiver=null;

    /**
     * An SDK-specific instance of {@link ContactAccessor}.  The activity does not need
     * to know what SDK it is running in: all idiosyncrasies of different SDKs are
     * encapsulated in the implementations of the ContactAccessor class.
     */
	private ArrayList<ContactDescriptor> mContacts;
	private class SDDataPath{
		public String path;
		public String fname;
		public String qualifiedPath;
		public SDDataPath(){
		    fname = getString(R.string.contacts_file);
		    path =  getFilesDir() + "/";
		    qualifiedPath = path + fname;
		}
	}
	static public class ButtonDescriptor{
		public int id;
		public String caption;
		public Hashtable<String, ContactInfo> contacts;
	}
	
	class GlobalData
	{
		private Hashtable<Integer, ButtonDescriptor> mContactData;
		GlobalData()
		{
			mContactData = new Hashtable<Integer, ButtonDescriptor>();
		}
		public ButtonDescriptor getContacts(int key){
			if (mContactData.containsKey(key))
				return mContactData.get(key);
			return null;
		}
		public boolean setContacts(int key, Hashtable<String, ContactInfo> contacts){
			boolean retVal = false;
			if (mContactData.containsKey(key)){
				mContactData.get(key).contacts =  contacts;
				retVal = true;
			}
			return retVal;
		}
		public void set(int key, ButtonDescriptor value){
			mContactData.put(key, value);
		}
		public Hashtable<Integer, ButtonDescriptor> get()
		{
			return mContactData;
		}
	};
	private void createDefaultContactList(){
		Resources res = getResources();
		String[] messages = res.getStringArray(R.array.messages);
		for(int j = 0; j < messages.length; j++) {
			ButtonDescriptor bd = new ButtonDescriptor();
			bd.caption = messages[j];
			bd.id = 0xFF + j;
			bd.contacts = new Hashtable<String, ContactInfo>();
			g_GlobalData.set(bd.id, bd);
		}		
	}
	public static GlobalData g_GlobalData;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    	g_GlobalData = new GlobalData();
    }
    
	private void resizeUIElements(int newSize){
		Resources res = getResources();
		String[] messages = res.getStringArray(R.array.messages);
		for (ContactDescriptor b : mContacts)
			b.getUIElement().setHeight(newSize / messages.length);
	}
	private JSONObject toJson(){
		JSONObject root=new JSONObject(); //root json object
		JSONArray rootArray = new JSONArray(); //the root object contains a single array of buttons
		
		Enumeration<ButtonDescriptor> enumerator = AnnounceActivity.g_GlobalData.get().elements(); //button descriptors
		try {
		while(enumerator.hasMoreElements()){
			JSONObject item = new JSONObject();									//json object to contain a single descriptor
			ButtonDescriptor bd = enumerator.nextElement();
				item.put("id", bd.id);
				item.put("caption", bd.caption);
				JSONArray contactList = new JSONArray();							//contact info for a single button
				Enumeration<ContactInfo> contactEnum = bd.contacts.elements();	
				while(contactEnum.hasMoreElements()){
					ContactInfo ci = contactEnum.nextElement();
					JSONObject arrayItem = new JSONObject();					//json object to contain a single contact
					arrayItem.put("name", ci.getDisplayName());
					arrayItem.put("phone", ci.getPhoneNumber());
					contactList.put(arrayItem);									//add json object to contact list
				}
				item.put("contacts", contactList);								//add the contacts to its own object
				rootArray.put(item);											//add this item to the root array
			}
			root.put("buttons", rootArray);
		} catch (JSONException e) {
			e.printStackTrace();
			root = null;
		}
		return root;
	}
	private void writeFileToInternalStorage(String fileName, String data) {
		String eol = System.getProperty("line.separator");
		BufferedWriter writer = null;
		try {
		  writer = new BufferedWriter(new OutputStreamWriter(openFileOutput(fileName, MODE_PRIVATE)));
		  writer.write(data + eol);
		} catch (Exception e) {
				e.printStackTrace();
		} finally {
		  if (writer != null) {
			try {
				writer.close();
			} catch (IOException e) {
				Toast.makeText(getBaseContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
			}
		  }
		}
	}
	private String readFileFromInternalStorage(String fname) {
		FileInputStream in=null;
		try {
			in = openFileInput(fname); 
		    InputStreamReader inputStreamReader = new InputStreamReader(in);
		    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
		    StringBuilder sb = new StringBuilder();
		    String line;
			while ((line = bufferedReader.readLine()) != null) 
			    sb.append(line);
			return sb.toString();
		} catch (FileNotFoundException e1) {
			Toast.makeText(getBaseContext(), e1.getMessage(), Toast.LENGTH_SHORT).show();
		}catch (IOException e2) {
			Toast.makeText(getBaseContext(), e2.getMessage(), Toast.LENGTH_SHORT).show();
		}
		return null;
	}
	
	private boolean writeContacts(){
		boolean retVal = true;
		SDDataPath sdData = new SDDataPath();
	    FileOutputStream fout = null;
        
        JSONObject jsonObject = toJson();
        if (jsonObject == null)
        {
        	Log.e("=-=-=-= writeContacts", "failed to create json");
			Toast.makeText(getBaseContext(), "failed to create json", Toast.LENGTH_SHORT).show();
       	 	return false;
        }
        
	    try {
			fout = openFileOutput(sdData.fname, MODE_PRIVATE);
		   	fout.write(jsonObject.toString().getBytes());
		   	fout.flush();
		   	fout.close();
		} catch (FileNotFoundException e1) {
			Toast.makeText(getBaseContext(), e1.getMessage(), Toast.LENGTH_SHORT).show();
			Log.e("=-=-=-=-= writeContacts ", e1.getMessage());
			retVal = false;
		} catch (IOException e) {
			Toast.makeText(getBaseContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
			Log.e("=-=-=-=-= writeContacts ", e.getMessage());
			retVal = false;
		}
	    
         return retVal;
         
	}
	
	private JSONObject readContacts(){
		JSONObject retVal = null;
		SDDataPath sdData = new SDDataPath();
		String rawJson = readFileFromInternalStorage(sdData.fname);
		if (rawJson == null)
			return null;
		
        try {
           	retVal = new JSONObject(rawJson);
		} catch (JSONException e) {
			Log.e("=-=-=-=-= readContacts", e.getMessage());
		}
        return retVal;
	}
	private void createNewButton(){

		//set up dialog
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.createnewbutton);
        dialog.setTitle(getResources().getString(R.string.create_newbutton));
        dialog.setCancelable(true);
        Button cancel = (Button) dialog.findViewById(R.id.dlg1_cancel);
        final AnnounceActivity thisActivity = this;
        cancel.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				dialog.cancel();
				
			}
		});
        Button ok = (Button) dialog.findViewById(R.id.dlg1_ok);
        ok.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				EditText et = (EditText)dialog.findViewById(R.id.editText1);
				ButtonDescriptor bd = new ButtonDescriptor();
				bd.caption = et.getText().toString();
				bd.contacts = new Hashtable<String, ContactInfo>();
				bd.id = AnnounceActivity.g_GlobalData.mContactData.size()+1;
				AnnounceActivity.g_GlobalData.mContactData.put(bd.id, bd);
				populateButtonArray();
				thisActivity.populateButtonArray();
				dialog.cancel();
			}
		});
        dialog.show();
	}
	
	private void populateButtonArray()
	{
		mContacts = new ArrayList<ContactDescriptor>();
		ScrollView sv = new ScrollView(this); 		//create a scroll view in case we have to scroll the buttons
		final LinearLayout ll = new LinearLayout(this);	//layout object
		ll.setOrientation(LinearLayout.VERTICAL);	//initial orientation
		sv.addView(ll);								//connect the two
		Button debugButton=new Button(this);
		debugButton.setText("Create New Message");
		debugButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v){
				createNewButton();
				}
			});
		ll.addView(debugButton);
		Enumeration<ButtonDescriptor> enumerator = g_GlobalData.mContactData.elements();
		while(enumerator.hasMoreElements()) {
			ButtonDescriptor bd =enumerator.nextElement();
			ContactDescriptor descriptor = new ContactDescriptor();
			final Button b = new Button(this);
			b.setText(bd.caption);
			b.setId(bd.id);
			descriptor.setUIElement(b);
			descriptor.setAnnouncement(bd.caption);
			ll.addView(b);
			mContacts.add(descriptor);
			//setup onClick response
	        b.setOnClickListener(new View.OnClickListener() {  
				public void onClick(View v) {
					ButtonDescriptor bd = AnnounceActivity.g_GlobalData.getContacts(b.getId());
					Enumeration<ContactInfo> ciEnum = bd.contacts.elements(); 
			        SmsManager sms = SmsManager.getDefault();
					while (ciEnum.hasMoreElements()){
						ContactInfo ci = ciEnum.nextElement();
				        sms.sendTextMessage(ci.getPhoneNumber(), null, bd.caption, null, null);        
						
					}
				}  
			});      
	        
	        //setup onLongClick response
	        b.setOnLongClickListener(new View.OnLongClickListener() {  
				public boolean onLongClick(View v) {  
					pickContact( b.getId(), b.getText().toString());
					return true;
				}  
			});      
		}
		this.setContentView(sv);
	}
	
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
		Display display = getWindowManager().getDefaultDisplay();
        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        	resizeUIElements(display.getWidth());
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
        	resizeUIElements(display.getHeight());
        }
      }
    protected void pickContact(int id, String caption) {
    	Intent intent = new Intent(this, ContactSelectionActivity.class);
    	intent.putExtra("id", id);
    	intent.putExtra("caption", caption);
        startActivityForResult(intent, PICK_CONTACT_REQUEST);
    }

    /**
     * Invoked when the contact picker activity is finished. The {@code contactUri} parameter
     * will contain a reference to the contact selected by the user. We will treat it as
     * an opaque URI and allow the SDK-specific ContactAccessor to handle the URI accordingly.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data); 
        if (requestCode == PICK_CONTACT_REQUEST && resultCode == RESULT_OK) {
    		writeContacts(); //save the contacts the user modified
        }
    }

    
    void updateExternalStorageState() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            mExternalStorageAvailable = mExternalStorageWriteable = true;
			Toast.makeText(getBaseContext(), "SD card ready to for full i/o", Toast.LENGTH_SHORT).show();
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
			Toast.makeText(getBaseContext(), "SD card ready to for read only", Toast.LENGTH_SHORT).show();
        } else {
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }
        /**
        handleExternalStorageState(mExternalStorageAvailable,
                mExternalStorageWriteable);
        */
    }

    void startWatchingExternalStorage() {
        mExternalStorageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i("test", "Storage: " + intent.getData());
                updateExternalStorageState();
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_REMOVED);
        registerReceiver(mExternalStorageReceiver, filter);
        updateExternalStorageState();
    }

    boolean createContactsFromJson(JSONObject json){
    	boolean retVal = true;
    	try {
			JSONArray root = (JSONArray)json.get("buttons");
			for (int i = 0; i<root.length(); ++i)
			{
				ButtonDescriptor bd = new ButtonDescriptor();
				JSONObject buttonDescriptor = root.getJSONObject(i);
				bd.caption = buttonDescriptor.getString("caption");
				bd.id = buttonDescriptor.getInt("id");
				JSONArray contacts = buttonDescriptor.getJSONArray("contacts");
				bd.contacts = new Hashtable<String, ContactInfo>(); 
				for (int j=0; j<contacts.length(); ++j){
					JSONObject contact = contacts.getJSONObject(j);
					ContactInfo ci = new ContactInfo();
					ci.setDisplayName(contact.getString("name"));
					ci.setPhoneNumber(contact.getString("phone"));
					ci.setKey(0xff+j);
					bd.contacts.put(ci.getPhoneNumber(), ci);
				}
				g_GlobalData.set(bd.id, bd);
			}
		} catch (JSONException e) {
			Log.e("=-=-=-=-= createContactsFromJson", e.getMessage());
			retVal = false;
		}
    	return retVal;
    }
    
    void stopWatchingExternalStorage() {
        unregisterReceiver(mExternalStorageReceiver);
        mExternalStorageReceiver = null;
    }
    
    @Override
    protected void onRestart() {
        super.onRestart();
    }
 
    @Override
    protected void onResume(){
    	super.onResume();
    	startWatchingExternalStorage();
    	JSONObject json = readContacts();
    	if (json == null)
    		createDefaultContactList();
    	else if (createContactsFromJson(json) == false)
    		createDefaultContactList();
    		
        populateButtonArray();
    	resizeUIElements(getWindowManager().getDefaultDisplay().getHeight());
    }

    @Override
    protected void onPause(){
    	super.onPause();
    	stopWatchingExternalStorage();
    }

    @Override
    protected void onStop(){
    	super.onStop();
    }

    @Override
    protected void onDestroy(){
    	super.onDestroy();
    }
}