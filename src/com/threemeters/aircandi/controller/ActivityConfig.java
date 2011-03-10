package com.threemeters.aircandi.controller;

import com.google.gson.annotations.Expose;

/**
 * @author Jayma
 */
public class ActivityConfig
{
	@Expose
	public Boolean	showHeader = false;
	@Expose
	public String	titleText = "No Title";
	@Expose
	public String	iconResource = "none";
	@Expose
	public Boolean	headerButton1Visible = false;
	@Expose
	public String	headerButton1Text = "text";
	@Expose
	public String	headerButton1Tag = "action";
	@Expose
	public Boolean	showFooter = false;
	@Expose
	public Boolean	footerButton1Visible = false;
	@Expose
	public String	footerButton1Text = "text";
	@Expose
	public String	footerButton1Tag = "action";
	
	public ActivityConfig() {}
}
