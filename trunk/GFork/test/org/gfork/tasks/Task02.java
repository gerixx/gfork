package org.gfork.tasks;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

@SuppressWarnings("serial")
public class Task02 implements Serializable {

	private Date date;
	private String name;

	public Task02() {
	}
	
	public Task02(Date date) {
		this(date, null);
	}
	
	public Task02(Date date, String name) {
		this.date = date;
		this.name = name;
	}
	
	public Date getDate() {
		return date;
	}

	public Date getDate(When dist) {
		switch(dist) {
			case TODAY: return new Date();
			case TOMORROW: {
				Calendar cal = GregorianCalendar.getInstance();
				cal.add(Calendar.DAY_OF_MONTH, 1);
				return cal.getTime();
			}
			case YESTERDAY: {
				Calendar cal = GregorianCalendar.getInstance();
				cal.add(Calendar.DAY_OF_MONTH, -1);
				return cal.getTime();
			}
		}
		return date;
	}
	
	public void endless() throws InterruptedException {
		while(1==1) {
			System.out.println(this.getClass().getName() + " - " + name);
			Thread.sleep(300);
		}
	}
	
	public void printToStdOut() throws InterruptedException {
		for(int i = 0; i < 50; i++) {
			System.out.println("task output: " + getClass().getName());
		}
	}
	
	public String getValue(int delayInMilis) throws InterruptedException {
		return "getValue(int) - ok";
	}
	
	public String getValue(Integer delayInMilis) throws InterruptedException {
		return "getValue(Integer) - ok";
	}
	
	public String delay(int millis) throws InterruptedException {
		long begin = System.currentTimeMillis();
		Thread.sleep(millis);
		return "delay - ok (" + (System.currentTimeMillis() - begin) + ")";
	}
	
	public void exception() {
		throw new RuntimeException("test exception");
	}
}
