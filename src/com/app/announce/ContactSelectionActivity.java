package com.app.announce;

import java.util.Enumeration;
import java.util.Hashtable;

import com.app.announce.AnnounceActivity.ButtonDescriptor;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class ContactSelectionActivity extends Activity {
    private boolean bCancel=false;
    private Hashtable<String, ContactInfo> retVal;
    private ContactInfo[] uiData;
    int _id;
    String _caption;
    
    /**
     * An SDK-specific instance of {@link ContactAccessor}.  The activity does not need
     * to know what SDK it is running in: all idiosyncrasies of different SDKs are
     * encapsulated in the implementations of the ContactAccessor class.
     */
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = this.getIntent();
        _id = intent.getIntExtra("id", 0);
        _caption = intent.getStringExtra("caption");
        retVal = new Hashtable<String, ContactInfo>();
        setContentView(R.layout.main);
        
        ButtonDescriptor bd = AnnounceActivity.g_GlobalData.getContacts(_id);	//get the button descriptor the user is about to change
        Enumeration<ContactInfo> eci = bd.contacts.elements();					//get all the contact info records associated with this message
        while(eci.hasMoreElements()){
        	ContactInfo ci = eci.nextElement();
        	retVal.put(ci.getPhoneNumber(), ci);								//populate the return value with previously set data, so that nothig is lost.
        }
        showContacts();
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && this.bCancel != true) {
        	AnnounceActivity.g_GlobalData.setContacts(_id, retVal);
	        Intent intent=new Intent();
	        ContactSelectionActivity.this.setResult(RESULT_OK, intent);
	        finish();
        } else { 
            return super.onKeyDown(keyCode, event);
        }
        return super.onKeyDown(keyCode, event);
    }    
    /**
     * returns a hash table key = name value = ContactInfo. 
     * Sorted in ascending order based on name.
     * */
    private ContactInfo[] getDirectoryListing(){
        // Run query
    	final int indexName = 1;
    	final int indexPhone = 2;
        Uri uri = Phone.CONTENT_URI;
        String[] projection = new String[] {
                ContactsContract.Contacts.LOOKUP_KEY,
                ContactsContract.Contacts.DISPLAY_NAME,
                Phone.NUMBER
        };
        String[] selectionArgs = null;
        String selection = ContactsContract.Contacts.IN_VISIBLE_GROUP + " = '" +  "1"  + "'";
        String sortOrder = ContactsContract.Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC";
    	Cursor cursor = managedQuery(uri, projection, selection, selectionArgs, sortOrder);
    	ContactInfo[] data = new ContactInfo[cursor.getCount()];
    	try{
    		int index = 0;
	    	for (boolean b = cursor.moveToFirst(); b != false; b = cursor.moveToNext())
	    	{
	    		ContactInfo contact = new ContactInfo();
	    		contact.setDisplayName(cursor.getString(indexName));
	    		contact.setPhoneNumber(cursor.getString(indexPhone));
	    		data[index++] = contact;
    		}
    	}
    	finally	{}
    	return data;
    }
    protected void showContacts() {
    	uiData = getDirectoryListing();	
		ScrollView sv = new ScrollView(this); 		//create a scroll view to show contact info
		LinearLayout ll = new LinearLayout(this);	//layout object
		ll.setOrientation(LinearLayout.VERTICAL);	//initial orientation
		sv.addView(ll);								//connect the two
		int checkBoxIndex = 0;
		int textBoxIndex = uiData.length+1;
    	ButtonDescriptor bd = AnnounceActivity.g_GlobalData.getContacts(_id);

        for (int i=0; i< uiData.length; ++i){
			TextView text = new TextView(this);
			final CheckBox box = new CheckBox(this);
			ContactInfo contact = uiData[i]; 
			
		    LinearLayout row = new LinearLayout(this);
		    row.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
	        box.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
	        box.setId(checkBoxIndex++);
	        
		    text.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		    text.setId(textBoxIndex++);
	        row.addView(box);
	        row.addView(text);
	        
			text.append(contact.getDisplayName()+" ");
			text.append(contact.getPhoneNumber());
			
			String keyPhone = contact.getPhoneNumber();
			if (bd.contacts.containsKey(keyPhone) == true){
				box.setChecked(true);
			}
			ll.addView(row);
			text.setOnClickListener(new View.OnClickListener() {
				
				public void onClick(View v) {
					box.setChecked(!box.isChecked());
				}
			});
	        box.setOnCheckedChangeListener(new OnCheckedChangeListener(){
	        	
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
	        	{
	    			int key = buttonView.getId();
	    			String keyPhone = uiData[key].getPhoneNumber();
	    			if (isChecked)
	    			{
	    				if (retVal.containsKey(keyPhone) == false){
	    					retVal.put(keyPhone, uiData[key]);
	    				}
	    			}
	    			else
	    			{
	    				if (retVal.containsKey(keyPhone) == true)
	    					retVal.remove(keyPhone);
	    			}
	        		
	        	}
	        });  
		}
	    Button b = new Button(this);
	    b.setText("Cancel");
        b.setOnClickListener(new View.OnClickListener() {  
			public void onClick(View v) {
				bCancel = true;
				finish();
			}
		});      
		ll.addView(b);
		this.setContentView(sv);
    }
    @Override
    protected void onRestart() {
        super.onRestart();
    }
 
    @Override
    protected void onResume(){
    	super.onResume();
    }

    @Override
    protected void onPause(){
    	super.onPause();
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
