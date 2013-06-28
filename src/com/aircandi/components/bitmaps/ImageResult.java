package com.aircandi.components.bitmaps;

import java.util.HashMap;
import java.util.Map;

import com.aircandi.service.objects.Photo;

/**
 * The Class ImageResult.
 * Used for bing image searches.
 */
public class ImageResult
{
	private String		title;
	private String		mediaUrl;
	private String		url;
	private String		displayUrl;
	private Long		width;
	private Long		height;
	private Long		fileSize;
	private String		contentType;
	private Thumbnail	thumbnail;
	
	/* Client only */
	private Photo		photo;

	public static ImageResult setPropertiesFromMap(ImageResult imageResult, Map map, Boolean nameMapping) {
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
			imageResult.thumbnail = Thumbnail.setPropertiesFromMap(new Thumbnail(), (HashMap<String, Object>) map.get("Thumbnail"));
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
		title = value;
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
		mediaUrl = value;
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
		url = value;
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
		displayUrl = value;
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
		width = value;
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
		height = value;
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
		fileSize = value;
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
		contentType = value;
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
		thumbnail = value;
	}

	// --------------------------------------------------------------------------------------------
	// Inner classes
	// --------------------------------------------------------------------------------------------

	public Photo getPhoto() {
		return photo;
	}

	public void setPhoto(Photo photo) {
		this.photo = photo;
	}

	public static class Thumbnail
	{

		private String	mediaUrl;
		private String	contentType;
		private Long	width;
		private Long	height;
		private Long	fileSize;
		private Long	runTime;

		private static Thumbnail setPropertiesFromMap(Thumbnail thumbnail, Map map) {
			/*
			 * Properties involved with editing are copied from one entity to another.
			 */
			thumbnail.mediaUrl = (String) map.get("MediaUrl");
			thumbnail.width = Long.parseLong((String) map.get("Width"));
			thumbnail.height = Long.parseLong((String) map.get("Height"));
			thumbnail.fileSize = Long.parseLong((String) map.get("FileSize"));
			thumbnail.contentType = (String) map.get("ContentType");
			if (map.get("RunTime") != null) {
				thumbnail.runTime = Long.parseLong((String) map.get("RunTime"));
			}

			return thumbnail;
		}

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
			mediaUrl = value;
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
			contentType = value;
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
			width = value;
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
			height = value;
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
			fileSize = value;
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
			runTime = value;
		}
	}
}
