package org.gfork.internal.remote;

public enum Command {
	NAC, 
	connect, connectOk, 
	run, runMethod, getMethodReturnValue, runOk, 
	waitFor, waitForFinished, waitForError, 
	getTask, 
	getStdErr, 
	getStdOut, 
	getExitValue, 
	kill, killOk,
	connectClose, connectCloseOk
}
