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

import com.aircandi.CandiConstants;
import com.aircandi.service.ProxibaseService;
import com.aircandi.service.ProxibaseServiceException;
import com.aircandi.ui.CandiRadar;
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
			s3 = new AmazonS3Client(CandiRadar.mAwsCredentials);
		}

		return s3;
	}

	//	public static List<String> getBucketNames() {
	//		List buckets = getInstance().listBuckets();
	//
	//		List<String> bucketNames = new ArrayList<String>(buckets.size());
	//		Iterator<Bucket> bIter = buckets.iterator();
	//		while (bIter.hasNext()) {
	//			bucketNames.add((bIter.next().getName()));
	//		}
	//		return bucketNames;
	//	}
	//
	//	public static List<String> getObjectNamesForBucket(String bucketName) {
	//		ObjectListing objects = getInstance().listObjects(bucketName);
	//		objListing = objects;
	//		List<String> objectNames = new ArrayList<String>(objects.getObjectSummaries().size());
	//		Iterator<S3ObjectSummary> oIter = objects.getObjectSummaries().iterator();
	//		while (oIter.hasNext()) {
	//			objectNames.add(oIter.next().getKey());
	//		}
	//		return objectNames;
	//	}
	//
	//	public static List<String> getObjectNamesForBucket(String bucketName, int numItems) {
	//		ListObjectsRequest req = new ListObjectsRequest();
	//		req.setMaxKeys(Integer.valueOf(numItems));
	//		req.setBucketName(bucketName);
	//		ObjectListing objects = getInstance().listObjects(req);
	//		objListing = objects;
	//		List<String> objectNames = new ArrayList<String>(objects.getObjectSummaries().size());
	//		Iterator<S3ObjectSummary> oIter = objects.getObjectSummaries().iterator();
	//		while (oIter.hasNext()) {
	//			objectNames.add(oIter.next().getKey());
	//		}
	//
	//		return objectNames;
	//	}

	//	public static List<String> getMoreObjectNamesForBucket() {
	//		try {
	//			ObjectListing objects = getInstance().listNextBatchOfObjects(objListing);
	//			objListing = objects;
	//			List<String> objectNames = new ArrayList<String>(objects.getObjectSummaries().size());
	//			Iterator<S3ObjectSummary> oIter = objects.getObjectSummaries().iterator();
	//			while (oIter.hasNext()) {
	//				objectNames.add(oIter.next().getKey());
	//			}
	//			return objectNames;
	//		}
	//		catch (NullPointerException e) {
	//			return new ArrayList<String>();
	//		}
	//
	//	}
	//
	//	public static void createBucket(String bucketName) {
	//		getInstance().createBucket(bucketName);
	//	}
	//
	//	public static void deleteBucket(String bucketName) {
	//		getInstance().deleteBucket(bucketName);
	//	}
	//
	//	public static void createObjectForBucket(String bucketName, String objectName, String data) {
	//		try {
	//			ByteArrayInputStream bais = new ByteArrayInputStream(data.getBytes());
	//			ObjectMetadata metadata = new ObjectMetadata();
	//			metadata.setContentLength(data.getBytes().length);
	//			getInstance().putObject(bucketName, objectName, bais, metadata);
	//		}
	//		catch (Exception exception) {
	//			Log.e("TODO", "createObjectForBucket");
	//		}
	//	}
	//
	//	public static void deleteObject(String bucketName, String objectName) {
	//		getInstance().deleteObject(bucketName, objectName);
	//	}
	//
	//	public static String getDataForObject(String bucketName, String objectName) {
	//		return read(getInstance().getObject(bucketName, objectName).getObjectContent());
	//	}
	//
	//	protected static String read(InputStream stream) {
	//		try {
	//			ByteArrayOutputStream baos = new ByteArrayOutputStream(8196);
	//			byte[] buffer = new byte[1024];
	//			int length = 0;
	//			while ((length = stream.read(buffer)) > 0) {
	//				baos.write(buffer, 0, length);
	//			}
	//
	//			return baos.toString();
	//		}
	//		catch (Exception exception) {
	//			return exception.getMessage();
	//		}
	//	}

	/* Jayma: Added routines */

	public static void putImage(String imageKey, Bitmap bitmap) throws ProxibaseServiceException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
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
