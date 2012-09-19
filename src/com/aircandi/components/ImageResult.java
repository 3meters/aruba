package com.aircandi.components;

import java.util.HashMap;

/**
 * The Class ImageResult.
 * Used for bing image searches.
 */
public class ImageResult
{
	/** The title. */
	protected String	title;

	/** The media url. */
	protected String	mediaUrl;

	/** The url. */
	protected String	url;

	/** The display url. */
	protected String	displayUrl;

	/** The width. */
	protected Long		width;

	/** The height. */
	protected Long		height;

	/** The file size. */
	protected Long		fileSize;

	/** The content type. */
	protected String	contentType;

	/** The thumbnail. */
	protected Thumbnail	thumbnail;

	public static ImageResult setFromPropertiesFromMap(ImageResult imageResult, HashMap map) {
		/*
		 * Properties involved with editing are copied from one entity to another.
		 */
		imageResult.title = (String) map.get("Title");
		imageResult.mediaUrl = (String) map.get("MediaUrl");
		imageResult.url = (String) map.get("Url");
		imageResult.displayUrl = (String) map.get("DisplayUrl");
		imageResult.width = Long.parseLong((String) map.get("Width"));
		imageResult.height = Long.parseLong((String) map.get("Height"));
		imageResult.fileSize = Long.parseLong((String) map.get("FileSize"));
		imageResult.contentType = (String) map.get("ContentType");
		if (map.get("Thumbnail") != null) {
			imageResult.thumbnail = Thumbnail.setFromPropertiesFromMap(new Thumbnail(), (HashMap<String, Object>) map.get("Thumbnail"));
		}

		return imageResult;
	}

	/**
	 * Gets the title.
	 * 
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Sets the title.
	 * 
	 * @param value
	 *            the new title
	 */
	public void setTitle(String value) {
		this.title = value;
	}

	/**
	 * Gets the media url.
	 * 
	 * @return the media url
	 */
	public String getMediaUrl() {
		return mediaUrl;
	}

	/**
	 * Sets the media url.
	 * 
	 * @param value
	 *            the new media url
	 */
	public void setMediaUrl(String value) {
		this.mediaUrl = value;
	}

	/**
	 * Gets the url.
	 * 
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Sets the url.
	 * 
	 * @param value
	 *            the new url
	 */
	public void setUrl(String value) {
		this.url = value;
	}

	/**
	 * Gets the display url.
	 * 
	 * @return the display url
	 */
	public String getDisplayUrl() {
		return displayUrl;
	}

	/**
	 * Sets the display url.
	 * 
	 * @param value
	 *            the new display url
	 */
	public void setDisplayUrl(String value) {
		this.displayUrl = value;
	}

	/**
	 * Gets the width.
	 * 
	 * @return the width
	 */
	public Long getWidth() {
		return width;
	}

	/**
	 * Sets the width.
	 * 
	 * @param value
	 *            the new width
	 */
	public void setWidth(Long value) {
		this.width = value;
	}

	/**
	 * Gets the height.
	 * 
	 * @return the height
	 */
	public Long getHeight() {
		return height;
	}

	/**
	 * Sets the height.
	 * 
	 * @param value
	 *            the new height
	 */
	public void setHeight(Long value) {
		this.height = value;
	}

	/**
	 * Gets the file size.
	 * 
	 * @return the file size
	 */
	public Long getFileSize() {
		return fileSize;
	}

	/**
	 * Sets the file size.
	 * 
	 * @param value
	 *            the new file size
	 */
	public void setFileSize(Long value) {
		this.fileSize = value;
	}

	/**
	 * Gets the content type.
	 * 
	 * @return the content type
	 */
	public String getContentType() {
		return contentType;
	}

	/**
	 * Sets the content type.
	 * 
	 * @param value
	 *            the new content type
	 */
	public void setContentType(String value) {
		this.contentType = value;
	}

	/**
	 * Gets the thumbnail.
	 * 
	 * @return the thumbnail
	 */
	public Thumbnail getThumbnail() {
		return thumbnail;
	}

	/**
	 * Sets the thumbnail.
	 * 
	 * @param value
	 *            the new thumbnail
	 */
	public void setThumbnail(Thumbnail value) {
		this.thumbnail = value;
	}
}
