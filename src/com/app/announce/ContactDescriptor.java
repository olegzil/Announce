/**
 * 
 */
package com.app.announce;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.widget.Button;

/**
 * @author olegzilberman
 *
 */
public class ContactDescriptor {
	private ArrayList< BroadcastDescriptor > mBroadcastTargets;
	private String mAnnouncementName;
	private Button mUIElement;
	ContactDescriptor(){
		this.mBroadcastTargets = new ArrayList<BroadcastDescriptor>();
	}
	public ArrayList<BroadcastDescriptor> getBroadcastTargets(){
		return this.mBroadcastTargets;
	}
	public void setBroadcastTargets(ArrayList<BroadcastDescriptor> source){
		this.mBroadcastTargets = source; 
	}
	public void setAnnouncement(String source){
		this.mAnnouncementName = source;
	}
	public String getAnnouncement(){
		return this.mAnnouncementName;
	}
	public Button getUIElement(){
		return this.mUIElement;
	}
	public void setUIElement(Button source){
		this.mUIElement = source;
	}
	public void Broadcast()
	{
		for (BroadcastDescriptor item : this.mBroadcastTargets)
			if (item.isEnabled())
				item.execute();
	}
	public void Edit(Activity a, int id, String caption)
	{
    	Intent intent = new Intent(a, ContactSelectionActivity.class);
    	intent.putExtra("id", id);
    	intent.putExtra("caption", caption);
        a.startActivityForResult(intent, AnnounceActivity.PICK_CONTACT_REQUEST);
	}
}
