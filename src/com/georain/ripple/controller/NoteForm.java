package com.georain.ripple.controller;

import java.io.IOException;
import java.util.ArrayList;
import org.apache.http.client.ClientProtocolException;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;
import com.georain.ripple.model.BaseModifyListener;
import com.georain.ripple.model.BaseQueryListener;
import com.georain.ripple.model.Post;
import com.georain.ripple.model.Query;
import com.georain.ripple.model.RippleRunner;
import com.georain.ripple.model.RippleService;

public class NoteForm extends RippleActivity
{
	private static final int	NOTE_ACTION_EDIT	= 4;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		setContentView(R.layout.note_form);
		super.onCreate(savedInstanceState);

		// Load the content for display
		populateWebView();
	}

	public void populateWebView()
	{
		WebView webView = (WebView) findViewById(R.id.WebView);
		String data = "<h2>" + getCurrentPost().title + "</h2>";
		data += getCurrentPost().postContent;
		webView.loadData(data, "text/html", "UTF-8");
	}

	@Override
	public void onActivityButtonClick(View view)
	{
		if (view.getTag().equals("delete"))
		{
			startProgress();
			RippleRunner ripple = new RippleRunner();
			ripple.delete(getCurrentPost().getUriOdata(), new NoteModifyListener());
		}
		else if (view.getTag().equals("edit"))
		{
			Intent intent = new Intent(this, NoteEditor.class);
			startActivityForResult(intent, NOTE_ACTION_EDIT);
		}
	}

	public class NoteModifyListener extends BaseModifyListener
	{
		public void onComplete()
		{
			NoteForm.this.runOnUiThread(new Runnable() {
				public void run()
				{
					setCurrentPost(null);
					setResult(RESULT_OK);
					stopProgress();
					finish();
				}
			});
		}

		@Override
		public void onClientProtocolException(ClientProtocolException e)
		{
			super.onClientProtocolException(e);
			RippleUI.showToastNotification(NoteForm.this, "Failed to delete", Toast.LENGTH_SHORT);
			setResult(RESULT_CANCELED);
			finish();
		}

		@Override
		public void onIOException(IOException e)
		{
			super.onIOException(e);
			RippleUI.showToastNotification(NoteForm.this, "Failed to delete", Toast.LENGTH_SHORT);
			setResult(RESULT_CANCELED);
			finish();
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (resultCode == RESULT_OK)
		{
			if (requestCode == NOTE_ACTION_EDIT)
			{
				// We need to reload the post because it was updated
				startProgress();
				Query query = new Query("Posts").filter("PostId eq guid'" + getCurrentPost().postId + "'");
				RippleRunner ripple = new RippleRunner();
				ripple.select(query, Post.class, new NoteQueryListener());
			}
		}
	}

	public class NoteQueryListener extends BaseQueryListener
	{
		public void onComplete(String response)
		{
			ArrayList<Object> posts = RippleService.convertJsonToObjects(response, Post.class);
			if (posts != null && posts.size() > 0)
			{
				setCurrentPost((Post) posts.get(0));
				// Post the processed result back to the UI thread
				NoteForm.this.runOnUiThread(new Runnable() {
					public void run()
					{
						populateWebView();
						stopProgress();
					}
				});
			}
		}
	}
}