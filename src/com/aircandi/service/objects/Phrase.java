package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import com.aircandi.service.Expose;

/**
 * @author Jayma
 */
public class Phrase extends ServiceObject implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 455904759787968585L;

	@Expose
	public String				phrase;
	@Expose
	public String				sampleText;
	@Expose
	public Number				sampleCount;

	public Phrase() {}

	@Override
	public Phrase clone() {
		try {
			final Phrase phrase = (Phrase) super.clone();
			return phrase;
		}
		catch (final CloneNotSupportedException ex) {
			throw new AssertionError();
		}
	}

	public static Phrase setPropertiesFromMap(Phrase phrase, HashMap map) {
		phrase.phrase = (String) map.get("phrase");
		HashMap sampleMap = (HashMap) map.get("sample");
		phrase.sampleText = (String) sampleMap.get("text");
		List<LinkedHashMap<String, Object>> entityMaps = (List<LinkedHashMap<String, Object>>) sampleMap.get("entities");
		for (LinkedHashMap<String, Object> entityMap : entityMaps) {
			ArrayList<Number> indices = (ArrayList<Number>) entityMap.get("indices");
			phrase.sampleCount = indices.size();
		}

		return phrase;
	}
}