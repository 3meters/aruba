package com.aircandi.components;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import android.graphics.Bitmap;
import android.util.Log;

import com.aircandi.Aircandi;
import com.aircandi.Constants;
import com.aircandi.service.HttpService;
import com.aircandi.service.HttpServiceException;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;

/*
 * How much of AWS am I using just to push images?
 * 
 * import com.amazonaws.AmazonClientException;
 * import com.amazonaws.AmazonServiceException;
 * import com.amazonaws.services.s3.AmazonS3;
 * import com.amazonaws.services.s3.AmazonS3Client;
 * import com.amazonaws.services.s3.model.CannedAccessControlList;
 * import com.amazonaws.services.s3.model.ObjectMetadata;
 */
public class S3 {

	static {
		System.setProperty("org.xml.sax.driver", "org.xmlpull.v1.sax2.Driver");
		try {
			@SuppressWarnings("unused")
			final XMLReader reader = XMLReaderFactory.createXMLReader(); // $codepro.audit.disable variableUsage
		}
		catch (SAXException e) {
			Log.e("SAXException", e.getMessage());
		}
	}

	private static class AmazonS3Holder {
		public static final AmazonS3	instance	= new AmazonS3Client(Aircandi.awsCredentials);
	}

	public static AmazonS3 getInstance() {
		return AmazonS3Holder.instance;
	}

	/* Jayma: Added routines */

	public static void putImage(String imageKey, Bitmap bitmap, Integer quality) throws HttpServiceException {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
		final byte[] bitmapBytes = outputStream.toByteArray();
		final ByteArrayInputStream inputStream = new ByteArrayInputStream(bitmapBytes);
		final ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(bitmapBytes.length);
		metadata.setContentType("image/jpeg");

		try {
			S3.getInstance().putObject(Constants.S3_BUCKET_IMAGES, imageKey, inputStream, metadata);
			S3.getInstance().setObjectAcl(Constants.S3_BUCKET_IMAGES, imageKey, CannedAccessControlList.PublicRead);
		}
		catch (final AmazonServiceException exception) {
			throw HttpService.makeHttpServiceException(null, null, exception);
		}
		catch (final AmazonClientException exception) {
			throw HttpService.makeHttpServiceException(null, null, exception);
		}
		finally {
			try {
				outputStream.close();
				inputStream.close();
			}
			catch (IOException exception) {
				throw HttpService.makeHttpServiceException(null, null, exception);
			}
		}
	}

	/**
	 * Push image byte array to S3.
	 * 
	 * @param imageKey
	 * @param bitmapBytes
	 * @throws HttpServiceException
	 */
	@SuppressWarnings("ucd")
	public static void putImageByteArray(String imageKey, byte[] bitmapBytes, String imageFormat) throws HttpServiceException {
		final ByteArrayInputStream inputStream = new ByteArrayInputStream(bitmapBytes);
		final ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(bitmapBytes.length);
		metadata.setContentType("image/" + imageFormat);

		try {
			S3.getInstance().putObject(Constants.S3_BUCKET_IMAGES, imageKey, inputStream, metadata);
			S3.getInstance().setObjectAcl(Constants.S3_BUCKET_IMAGES, imageKey, CannedAccessControlList.PublicRead);
		}
		catch (final AmazonServiceException exception) {
			throw HttpService.makeHttpServiceException(null, null, exception);
		}
		catch (final AmazonClientException exception) {
			throw HttpService.makeHttpServiceException(null, null, exception);
		}
		finally {
			try {
				inputStream.close();
			}
			catch (IOException exception) {
				throw HttpService.makeHttpServiceException(null, null, exception);
			}
		}
	}

	//	public static void deleteImage(String imageKey) throws ProxibaseServiceException {
	//
	//		/* If the image is stored with S3 then it will be deleted */
	//		try {
	//			S3.getInstance().deleteObject(CandiConstants.S3_BUCKET_IMAGES, imageKey);
	//		}
	//		catch (final AmazonServiceException exception) {
	//			throw new ProxibaseServiceException(exception.getMessage(), ErrorType.Service, ErrorCode.AmazonServiceException, exception);
	//		}
	//		catch (final AmazonClientException exception) {
	//			throw new ProxibaseServiceException(exception.getMessage(), ErrorType.Client, ErrorCode.AmazonClientException, exception);
	//		}
	//	}
	//
	//	public static void flagImageForDeletion(String imageKey, long deleteDate) throws ProxibaseServiceException {
	//
	//		/* If the image is stored with S3 then it will be deleted */
	//		try {
	//			CopyObjectRequest req = new CopyObjectRequest(CandiConstants.S3_BUCKET_IMAGES, imageKey, CandiConstants.S3_BUCKET_IMAGES, imageKey);
	//			ObjectMetadata metadata = S3.getInstance().getObjectMetadata(CandiConstants.S3_BUCKET_IMAGES, imageKey);
	//			metadata.addUserMetadata("deleteDate", String.valueOf(deleteDate));
	//			req.setNewObjectMetadata(metadata);
	//			S3.getInstance().copyObject(req);
	//		}
	//		catch (final AmazonServiceException exception) {
	//			throw new ProxibaseServiceException(exception.getMessage(), ErrorType.Service, ErrorCode.AmazonServiceException, exception);
	//		}
	//		catch (final AmazonClientException exception) {
	//			throw new ProxibaseServiceException(exception.getMessage(), ErrorType.Client, ErrorCode.AmazonClientException, exception);
	//		}
	//	}
}
