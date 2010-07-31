package org.gfork.tasks;

import java.io.Serializable;

public class KeyValue implements Serializable {

	private static final long serialVersionUID = 1L;
	
	public KeyValue(String key, String value) {
		super();
		this.key = key;
		this.value = value;
	}
	
	public String key;
	public String value;
}
