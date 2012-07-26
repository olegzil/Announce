package com.app.announce;

import java.util.ArrayList;

public class ContactData {
	ContactData(){
		contacts = new ArrayList<ContactInfo>();
		good = false;
	}
	public ArrayList<ContactInfo> contacts;
	public boolean good;
}
