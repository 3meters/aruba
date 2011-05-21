package com.proxibase.aircandi.model;

import com.google.gson.annotations.Expose;
import com.proxibase.sdk.android.core.ProxibaseService;

/**
 * @author Jayma
 */
public class Message
{
	// Annotation syntax: @Expose (serialize = false, deserialize = false)
	@Expose
	public String	messageId;
	@Expose
	public String	targetId;
	@Expose
	public String	sourceId;
	@Expose
	public String	commandType;
	@Expose
	public String	parameter1 = "";
	@Expose
	public String	parameter2 = "";
	@Expose
	public String	parameter3 = "";
	@Expose
	public String	priority;
	@Expose
	public String	messageDate;
	@Expose
	public Boolean	deleted = false;

	public Message() {}
	
	public Message(String commandType, String parameter1, String parameter2, String parameter3, String priority, String targetId, String sourceId, String messageDate) 
	{
		this.commandType = commandType;
		this.parameter1 = parameter1;
		this.parameter2 = parameter2;
		this.parameter3 = parameter3;
		this.priority = priority;
		this.targetId = targetId;
		this.sourceId = sourceId;
		this.messageDate = messageDate;
	}
	
	public String getUriOdata()
	{
		String root = ProxibaseService.URL_PROXIBASE_SERVICE_ODATA;
		String entity = "Messages";
		String uri = root + entity + "(guid'" + this.messageId + "')";
		return uri;
	}
}