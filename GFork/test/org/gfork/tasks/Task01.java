package org.gfork.tasks;

import java.io.File;
import java.io.Serializable;
import java.util.logging.Logger;

@SuppressWarnings("serial")
public class Task01 implements Serializable, Runnable /*only needed together with FutureTaskFork*/ {
	
	private final static Logger log = Logger.getLogger(Task01.class.getName());
	
	private String state;
	private Boolean checkVmOptions = false;

	public void run() {
		System.out.println("Task01.run()");
		state = "executed";
		checkVmOptions();
	}
	
	public boolean checkLibPath(String comparewith) {
		return System.getProperty("library.path", "null").equals(comparewith);
	}
	
	public boolean checkSystemProperties(KeyValue[] properties) {
		boolean ok = true;
		log.info(String.format("checkSystemProperties: %d properties to check", properties.length));
		for (KeyValue keyValue : properties) {
			String localPropertyValue = System.getProperty(keyValue.key, "_undef_");
			ok = ok && localPropertyValue.equals(keyValue.value);
			log.info(String.format("property '%s' value was '%s' - ok=%b", keyValue.key, localPropertyValue, ok));
		}
		return ok;
	}
	
	public void printEnv() {
		System.getProperties().list(System.out);
		System.out.println("----");
		System.out.printf("working dir: %s%n", (new File("./")).getAbsolutePath());
	}
	
	public String getWorkingDir() {
		return ((new File("")).getAbsolutePath());
	}
	
	public String getNullValue() {
		return null;
	}
	
	public void voidMethod() {
		
	}

	public String getState() {
		return state;
	}
	
	public boolean set(int v) {
		return true;
	}
	
	public boolean set(short v) {
		return true;
	}
	
	public boolean set(long v) {
		return true;
	}
	
	public boolean set(char v) {
		return true;
	}
	
	public boolean set(byte v) {
		return true;
	}
	
	public boolean set(double v) {
		return true;
	}
	
	public boolean set(float v) {
		return true;
	}
	
	public boolean set(boolean v) {
		return true;
	}
	
	public boolean checkVmOptions() {
		return (checkVmOptions = System.getProperty("test.property.all", "undefined").equals("hello1"));
	}

	public Boolean getCheckVmOptions() {
		return checkVmOptions;
	}
}
