/*
 * Copyright 2010 Nabeel Mukhtar
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aircandi.components;

/**
 * The Class ImageResult.
 */
public class ImageResult
{
	/** The title. */
	protected String			title;

	/** The media url. */
	protected String			mediaUrl;

	/** The url. */
	protected String			url;

	/** The display url. */
	protected String			displayUrl;

	/** The width. */
	protected Long				width;

	/** The height. */
	protected Long				height;

	/** The file size. */
	protected Long				fileSize;

	/** The content type. */
	protected String			contentType;

	/** The thumbnail. */
	protected Thumbnail			thumbnail;

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
