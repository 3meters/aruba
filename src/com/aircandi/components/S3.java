/*
 * Copyright 2010-2011 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
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
import com.aircandi.CandiConstants;
import com.aircandi.service.ProxibaseService;
import com.aircandi.service.ProxibaseServiceException;
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
@SuppressWarnings("ucd")
public class S3 {

	private static AmazonS3		s3			= null;
	//	private static ObjectListing	objListing	= null;
	public static final String	BUCKET_NAME	= "_bucket_name";
	public static final String	OBJECT_NAME	= "_object_name";

	static {
		System.setProperty("org.xml.sax.driver", "org.xmlpull.v1.sax2.Driver");
		try {
			@SuppressWarnings("unused")
			XMLReader reader = XMLReaderFactory.createXMLReader();
		}
		catch (SAXException e) {
			Log.e("SAXException", e.getMessage());
		}
	}

	public static AmazonS3 getInstance() {
		if (s3 == null) {
			s3 = new AmazonS3Client(Aircandi.awsCredentials);
		}

		return s3;
	}
	
	/* Jayma: Added routines */

	public static void putImage(String imageKey, Bitmap bitmap, Integer quality) throws ProxibaseServiceException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
		byte[] bitmapBytes = outputStream.toByteArray();
		ByteArrayInputStream inputStream = new ByteArrayInputStream(bitmapBytes);
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(bitmapBytes.length);
		metadata.setContentType("image/jpeg");

		try {
			S3.getInstance().putObject(CandiConstants.S3_BUCKET_IMAGES, imageKey, inputStream, metadata);
			S3.getInstance().setObjectAcl(CandiConstants.S3_BUCKET_IMAGES, imageKey, CannedAccessControlList.PublicRead);
		}
		catch (final AmazonServiceException exception) {
			throw ProxibaseService.makeProxibaseServiceException(null, exception);
		}
		catch (final AmazonClientException exception) {
			throw ProxibaseService.makeProxibaseServiceException(null, exception);
		}
		finally {
			try {
				outputStream.close();
				inputStream.close();
			}
			catch (IOException exception) {
				throw ProxibaseService.makeProxibaseServiceException(null, exception);
			}
		}
	}

	/**
	 * Push image byte array to S3. 
	 * 
	 * @param imageKey
	 * @param bitmapBytes
	 * @throws ProxibaseServiceException
	 */
	public static void putImageByteArray(String imageKey, byte[] bitmapBytes, String imageFormat) throws ProxibaseServiceException {
		ByteArrayInputStream inputStream = new ByteArrayInputStream(bitmapBytes);
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(bitmapBytes.length);
		metadata.setContentType("image/" + imageFormat);

		try {
			S3.getInstance().putObject(CandiConstants.S3_BUCKET_IMAGES, imageKey, inputStream, metadata);
			S3.getInstance().setObjectAcl(CandiConstants.S3_BUCKET_IMAGES, imageKey, CannedAccessControlList.PublicRead);
		}
		catch (final AmazonServiceException exception) {
			throw ProxibaseService.makeProxibaseServiceException(null, exception);
		}
		catch (final AmazonClientException exception) {
			throw ProxibaseService.makeProxibaseServiceException(null, exception);
		}
		finally {
			try {
				inputStream.close();
			}
			catch (IOException exception) {
				throw ProxibaseService.makeProxibaseServiceException(null, exception);
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
