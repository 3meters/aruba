package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;

import android.graphics.Bitmap;

import com.aircandi.service.Expose;
import com.aircandi.service.SerializedName;

/**
 * @author Jayma
 */
@SuppressWarnings("ucd")
public class Comment extends ServiceObject implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 4362288672244719448L;

	@Expose
	public String				title;
	@Expose
	public String				description;
	@Expose
	public String				name;
	@Expose
	public String				location;
	@Expose
	public String				imageUri;
	@Expose
	@SerializedName("_creator")
	public String				creatorId;
	@Expose
	public Number				createdDate;

	/* For client use only */
	public Bitmap				imageBitmap;

	public Comment() {}

	public static Comment setPropertiesFromMap(Comment comment, Map map) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		comment.title = (String) map.get("title");
		comment.description = (String) map.get("description");
		comment.name = (String) map.get("name");
		comment.location = (String) map.get("location");
		comment.imageUri = (String) map.get("imageUri");
		comment.creatorId = (String) map.get("_creator");
		comment.createdDate = (Number) map.get("createdDate");
		return comment;
	}

	@SuppressWarnings("ucd")
	public static class SortCommentsByDate implements Comparator<Comment> {

		@Override
		public int compare(Comment comment1, Comment comment2) {

			if (comment1.createdDate.longValue() < comment2.createdDate.longValue()) {
				return 1;
			}
			else if (comment1.createdDate.longValue() == comment2.createdDate.longValue()) {
				return 0;
			}
			return -1;
		}
	}

}