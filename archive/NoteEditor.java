package com.proxibase.aircandi.activities;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.proxibase.aircandi.activities.R;
import com.proxibase.aircandi.models.Post;
import com.proxibase.aircandi.utils.AircandiUI;
import com.proxibase.aircandi.utils.DateUtils;
import com.proxibase.sdk.android.proxi.service.ProxibaseRunner;
import com.proxibase.sdk.android.proxi.service.ProxibaseService.BaseModifyListener;

public class NoteEditor extends AircandiActivity
{
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		setContentView(R.layout.note_editor);
		super.onCreate(savedInstanceState);

		// If there is a current post then load it for editing
		if (getCurrentPost() != null)
		{
			String content = getCurrentPost().postContent;
			String replaced = content.replaceAll("<br />", "\n");

			((EditText) findViewById(R.id.Note_Title)).setText(getCurrentPost().title);
			((EditText) findViewById(R.id.Note_Body)).setText(replaced);
		}
	}

	public void onSaveClick(View view)
	{
		// Load the content and update with the service
		ProxibaseRunner ripple = new ProxibaseRunner();
		startProgress();
		
		// Clean up the content first
		String content = ((EditText) findViewById(R.id.Note_Body)).getEditableText().toString();
		String replaced = content.replaceAll("\n", "<br />");
		
		if (getCurrentPost() != null)
		{
			getCurrentPost().title = ((EditText) findViewById(R.id.Note_Title)).getEditableText().toString();
			getCurrentPost().postContent = replaced;
			ripple.update(getCurrentPost(), getCurrentPost().getUriOdata(), new NoteModifyListener()); // Update
		}
		// Load the content and insert with the service
		else
		{
			Post mPost = new Post();
			mPost.title = ((EditText) findViewById(R.id.Note_Title)).getEditableText().toString();
			mPost.postContent = ((EditText) findViewById(R.id.Note_Body)).getEditableText().toString();
			mPost.dateCreated = DateUtils.nowString();
			mPost.dateModified = DateUtils.nowString();
			mPost.authorId = "Anonymous";
			mPost.entityId = getCurrentEntity().getEntityProxy().entityProxyId;
			mPost.isPublished = true;
			mPost.isCommentEnabled = true;
			ripple.insert(mPost, "Posts", "", new NoteModifyListener()); // Insert
		}
	}
	
	public class NoteModifyListener extends BaseModifyListener
	{
		public void onComplete()
		{
			NoteEditor.this.runOnUiThread(new Runnable() {
				public void run()
				{
					stopProgress();
					setResult(RESULT_OK);
					finish();
				}
			});
		}

		@Override
		public void onClientProtocolException(ClientProtocolException e)
		{
			super.onClientProtocolException(e);
			NoteEditor.this.runOnUiThread(new Runnable() {
				public void run()
				{
					stopProgress();
					AircandiUI.showToastNotification(NoteEditor.this, "Failed to insert or modify note", Toast.LENGTH_SHORT);
					setResult(RESULT_CANCELED);
					finish();
				}
			});
		}

		@Override
		public void onIOException(IOException e)
		{
			super.onIOException(e);
			NoteEditor.this.runOnUiThread(new Runnable() {
				public void run()
				{
					stopProgress();
					AircandiUI.showToastNotification(NoteEditor.this, "Failed to insert or modify note", Toast.LENGTH_SHORT);
					setResult(RESULT_CANCELED);
					finish();
				}
			});
		}
	}

	public void onCancelClick(View view)
	{
		setResult(RESULT_CANCELED);
		finish();
	}
}