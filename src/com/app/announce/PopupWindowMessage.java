package com.app.announce;

import android.app.Activity;
import android.app.Dialog;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class PopupWindowMessage {
	public PopupWindowMessage(Activity activity, String message){
        final Dialog dialog = new Dialog(activity);
        dialog.setContentView(R.layout.help_message_dialog);
        dialog.setTitle(activity.getResources().getString(R.string.help_dialog_title));
        Button cancelBtn = (Button) dialog.findViewById(R.id.help_close_button);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
        cancelBtn.setFocusable(true);
        
        TextView tv = (TextView) dialog.findViewById(R.id.helpTextView);
        dialog.setCancelable(true);
        tv.setTextSize(18);
        int c = activity.getResources().getColor(android.R.color.white);
        tv.setTextColor(c);
        tv.setGravity(Gravity.LEFT);
        tv.setText(message);
        dialog.show();
		
	}
	public void show(){
	}
}
