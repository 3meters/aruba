package com.aircandi.service.objects;

import java.io.Serializable;
import java.util.HashMap;

import com.aircandi.service.Expose;


/**
 * @author Jayma
 */
public class Phrase implements Cloneable, Serializable {

	private static final long	serialVersionUID	= 455904759787968585L;

	@Expose
	public String				phrase;
	@Expose
	public String				sampleText;

	public Phrase() {}

	public static Phrase setFromPropertiesFromMap(Phrase phrase, HashMap map) {
		phrase.phrase = (String) map.get("phrase");
		HashMap sampleMap = (HashMap) map.get("sample");
		phrase.sampleText = (String) sampleMap.get("text");
		return phrase;
	}
}