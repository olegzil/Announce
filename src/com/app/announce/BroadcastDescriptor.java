/**
 * 
 */
package com.app.announce;

/**
 * @author olegzilberman
 *
 */
public interface BroadcastDescriptor {
	void execute();
	String getAddress();
	void enable(boolean flag);
	boolean isEnabled();
}
