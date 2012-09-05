package com.aircandi.components;

/**
 * The Class Thumbnail.
 * Used for bing image searches.
 */
public class Thumbnail
{

	/** The url. */
	protected String	mediaUrl;

	/** The content type. */
	protected String	contentType;

	/** The width. */
	protected Long		width;

	/** The height. */
	protected Long		height;

	/** The file size. */
	protected Long		fileSize;

	/** The run time. */
	protected Long		runTime;

	/**
	 * Gets the url.
	 * 
	 * @return the url
	 */
	public String getUrl() {
		return mediaUrl;
	}

	/**
	 * Sets the url.
	 * 
	 * @param value
	 *            the new url
	 */
	public void setUrl(String value) {
		this.mediaUrl = value;
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
	 * Gets the run time.
	 * 
	 * @return the run time
	 */
	public Long getRunTime() {
		return runTime;
	}

	/**
	 * Sets the run time.
	 * 
	 * @param value
	 *            the new run time
	 */
	public void setRunTime(Long value) {
		this.runTime = value;
	}
}
