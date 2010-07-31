package org.gfork.tasks;

import java.io.PrintWriter;

import org.gfork.helpers.PipedTextTask;

@SuppressWarnings("serial")
public class AppenderTask extends PipedTextTask {

	@Override
	protected void process(final PrintWriter out, String line) {
		super.process(out, line + "_A");
	}

}
