package com.aircandi.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.widget.ListView;

import com.aircandi.R;

/**
 * 
 * @author declanshanaghy
 *         http://blog.350nice.com/wp/archives/240
 *         MultiChoice Preference Widget for Android
 * 
 * @contributor matiboy
 *              Added support for check all/none and custom separator defined in XML.
 *              IMPORTANT: The following attributes MUST be defined (probably inside attr.xml) for the code to even
 *              compile
 *              <declare-styleable name="ListPreferenceMultiSelect">
 *              <attr format="string" name="checkAll" />
 *              <attr format="string" name="separator" />
 *              </declare-styleable>
 *              Whether you decide to then use those attributes is up to you.
 * 
 */
@SuppressWarnings("ucd")
public class ListPreferenceMultiSelect extends ListPreference {
	private static final String	DEFAULT_SEPARATOR	= "OV=I=XseparatorX=I=VO";
	private String				mSeparator			= DEFAULT_SEPARATOR;
	private String				mCheckAllKey		= null;
	private boolean[]			mClickedDialogEntryIndices;

	public ListPreferenceMultiSelect(Context context) {
		this(context, null);
	}

	public ListPreferenceMultiSelect(Context context, AttributeSet attrs) {
		super(context, attrs);

		final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ListPreferenceMultiSelect);
		mCheckAllKey = ta.getString(R.styleable.ListPreferenceMultiSelect_checkAll);
		final String separator = ta.getString(R.styleable.ListPreferenceMultiSelect_separator);
		ta.recycle();

		if (separator != null) {
			mSeparator = separator;
		}

		/* Initialize the boolean array to the same size as number of entries */
		final CharSequence[] entries = getEntries();
		mClickedDialogEntryIndices = new boolean[(entries != null) ? entries.length : 0];		
	}

	@Override
	public void setEntries(CharSequence[] entries) {
		super.setEntries(entries);
		/* Initialize the boolean array to the same size as number of entries */
		mClickedDialogEntryIndices = new boolean[entries.length];
	}

	@Override
	protected void onPrepareDialogBuilder(Builder builder) {
		final CharSequence[] entries = getEntries();
		final CharSequence[] entryValues = getEntryValues();
		if (entries == null
				|| entryValues == null
				|| entries.length != entryValues.length) {
			throw new IllegalStateException(
					"ListPreference requires an entries array and an entryValues array which are both the same length");
		}

		restoreCheckedEntries();
		builder.setMultiChoiceItems(entries, mClickedDialogEntryIndices,
				new DialogInterface.OnMultiChoiceClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which, boolean val) {
						if (isCheckAllValue(which)) {
							checkAll(dialog, val);
						}
						mClickedDialogEntryIndices[which] = val;
					}
				});
	}

	private boolean isCheckAllValue(int which) {
		final CharSequence[] entryValues = getEntryValues();
		if (mCheckAllKey != null) {
			return entryValues[which].equals(mCheckAllKey);
		}
		return false;
	}

	private void checkAll(DialogInterface dialog, boolean val) {
		final ListView lv = ((AlertDialog) dialog).getListView();
		final int size = lv.getCount();
		for (int i = 0; i < size; i++) {
			lv.setItemChecked(i, val);
			mClickedDialogEntryIndices[i] = val;
		}
	}

	private String[] parseStoredValue(CharSequence val) {
		if (val.equals("")) {
			return null;
		}
		else {
			return ((String) val).split(mSeparator);
		}
	}

	private void restoreCheckedEntries() {
		final CharSequence[] entryValues = getEntryValues();

		// Explode the string read in sharedpreferences
		final String[] vals = parseStoredValue(getValue());

		if (vals != null) {
			final List<String> valuesList = Arrays.asList(vals);
			for (int i = 0; i < entryValues.length; i++) {
				CharSequence entry = entryValues[i];
				if (valuesList.contains(entry)) {
					mClickedDialogEntryIndices[i] = true;
				}
			}
		}
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		final ArrayList<String> values = new ArrayList<String>();

		final CharSequence[] entryValues = getEntryValues();
		if (positiveResult && entryValues != null) {
			for (int i = 0; i < entryValues.length; i++) {
				if (mClickedDialogEntryIndices[i]) {
					// Don't save the state of check all option - if any
					String val = (String) entryValues[i];
					if (mCheckAllKey == null || !val.equals(mCheckAllKey)) {
						values.add(val);
					}
				}
			}

			if (callChangeListener(values)) {
				setValue(join(values, mSeparator));
			}
		}
	}

	private static String join(Iterable<? extends Object> pColl, String separator)
	{
		final Iterator<? extends Object> oIter;
		if (pColl == null || (!(oIter = pColl.iterator()).hasNext())) {
			return "";
		}
		final StringBuilder stringBuilder = new StringBuilder(String.valueOf(oIter.next()));
		while (oIter.hasNext()) {
			stringBuilder.append(separator).append(oIter.next());
		}
		return stringBuilder.toString();
	}

	// TODO: Would like to keep this static but separator then needs to be put in by hand or use default separator "OV=I=XseparatorX=I=VO"...
	/**
	 * 
	 * @param straw
	 *            String to be found
	 * @param haystack
	 *            Raw string that can be read direct from preferences
	 * @param separator
	 *            Separator string. If null, static default separator will be used
	 * @return boolean True if the straw was found in the haystack
	 */
	public static boolean contains(String straw, String haystack, String separator) {
		if (separator == null) {
			separator = DEFAULT_SEPARATOR;
		}
		final String[] vals = haystack.split(separator);
		for (int i = 0; i < vals.length; i++) {
			if (vals[i].equals(straw)) {
				return true;
			}
		}
		return false;
	}
}
