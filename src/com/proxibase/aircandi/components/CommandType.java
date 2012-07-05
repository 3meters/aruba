package com.proxibase.aircandi.components;

/**
 * Encapsulates everything required to execute a command including any data
 * that has to be passed to another activity. In some cases, the data included
 * is used to lookup data that is required.
 * 
 * @author Jayma
 */
public enum CommandType {
	None,
	View,
	Edit,
	New,
	Dialog,
	ChunkEntities,
	ChunkChildEntities
}
