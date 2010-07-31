package org.gfork.tasks;

import java.io.Serializable;
import java.util.concurrent.Callable;

public class TaskCallable implements Serializable, Callable<String> {

	private static final long serialVersionUID = 1L;

	@Override
	public String call() throws Exception {
		return "callable called";
	}

}
