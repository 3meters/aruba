package com.proxibase.aircandi.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.proxibase.sdk.android.util.ProxiConstants;

/**
 * @author Jayma
 */
public class Comment {
	@Expose
	@SerializedName("PostCommentId")
	public String	commentId;
	@Expose
	public String	parentCommentId;
	@Expose
	public String	commentDate;
	@Expose
	@SerializedName("Author")
	public String	authorId;
	@Expose
	public String	email;
	@Expose
	public String	website;
	@Expose
	public String	comment;
	@Expose
	public String	country;
	@Expose
	public Boolean	isApproved;
	@Expose
	public String	moderatedBy;
	@Expose
	public String	avatar;
	@Expose
	public Boolean	isSpam;
	@Expose
	public Boolean	isDeleted;
	
	public String getUriOdata()
	{
		String root = ProxiConstants.URL_PROXIBASE_SERVICE_ODATA;
		String entity = "Comments";
		String uri = root + entity + "(guid'" + this.commentId + "')";
		return uri;
	}

	public Comment() {}
}
