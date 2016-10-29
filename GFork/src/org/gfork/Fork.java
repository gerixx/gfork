/*
   Copyright 2010 Gerald Ehmayer
   
   This file is part of project GFork.

    GFork is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GFork is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with GFork.  If not, see <http://www.gnu.org/licenses/>.

*/

package org.gfork;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import org.gfork.helpers.ForkListenerAdapter;
import org.gfork.internal.SysPropTask;
import org.gfork.internal.run.ForkRunner;
import org.gfork.types.MethodArgumentsException;

/**
 * This class provides the method {@link #execute()} which runs some code 
 * (referred to as <b>task</b>) in a subprocess.
 * Any public method of a serializable task object can be executed and its return
 * value will be propagated back to the caller JVM (the parent process), 
 * as well as the probably changed task object itself.
 * 
 * @author Gerald Ehmayer
 *
 * @param <TASK_TYPE> type of task object that will be transferred to the remote JVM
 * @param <RETURN_TYPE> type of return value of the tasks method that will be executed in the remote JVM
 */
public class Fork<TASK_TYPE extends Serializable, RETURN_TYPE extends Serializable> implements Serializable {
	protected static final String FORK_WAS_NOT_STARTED_YET = "Fork was not started yet.";

	protected static final String FORK_IS_ALREADY_EXECUTING = "Task process is already executing.";
	
	public static final long serialVersionUID = 1L;

	private static final String FILE_PREFIX = "jforkTask";

	public static final String NL = System.getProperty("line.separator", "\n");

	private static final String OPT_MAXMEM = "-Xmx50m";

	private static final String JFORK_METHOD_RET_VAL = "jforkMethodRetVal";
	
	protected static String javaExe;

	protected static List<String> vmOptionsForAll;

	protected List<String> vmOptions;
	
	protected static boolean loggingEnabled;

	private static Properties remoteSystemProperties;
	
	protected File taskFile;

	protected Process exec;

	private BufferedReader taskErrorReader;

	private BufferedReader taskStdOutReader;

	protected final StringBuilder stdErrText = new StringBuilder();

	protected int stdErrSize = 2000;

	protected Writer stdErrWriter;

	private Thread stdErrThread;

	protected StringBuilder stdOutText = new StringBuilder();
	
	private int stdOutSize;

	private Writer stdOutWriter;

	private Thread stdOutThread;

	private String statusInfo;

	protected Throwable exception;

	private File exceptionFile;

	protected final Method method;
	
	protected final Object[] methodArgs;

	private final File methodRetValFile;

	protected File workingDir;

	protected boolean returnTypeVoid ;

	protected List<Listener<TASK_TYPE,RETURN_TYPE>> listeners;
	
	private boolean finished;

	private TASK_TYPE taskResult;

	private RETURN_TYPE returnValue;

	private boolean killed;

	public boolean skipMergeSystenProperties;

	private final Object waitForSignal = new Object();

	private boolean processListenersInitiated;

	private String classpath;

	/**
	 * Fork listener interface to be implemented to handle events.
	 * 
	 * @see ForkListenerAdapter
	 * @author Gerald Ehmayer
	 *
	 * @param <T> task type
	 * @param <V> return type of the task
	 */
	public interface Listener<T extends Serializable, V extends Serializable> {
		public void onFinish(final Fork<T,V>  fork, boolean wasKilled) throws IllegalAccessException, InterruptedException;
		/**
		 * Signals that fork process exited with an error code, see also {@link Fork#isError()}
		 * @param fork
		 * @throws IllegalAccessException
		 * @throws InterruptedException
		 */
		public void onError(final Fork<T,V>  fork) throws IllegalAccessException, InterruptedException;
		/**
		 * Signals that fork process ended with an exception, see also {@link Fork#isException()}
		 * @param fork
		 * @throws IllegalAccessException
		 * @throws InterruptedException
		 * @throws IOException
		 * @throws ClassNotFoundException
		 */
		public void onException(final Fork<T,V>  fork) throws IllegalAccessException, InterruptedException, IOException, ClassNotFoundException;
	}

	/**
	 * Convenience constructor to executed a method 'public void method run()' of the task object.
	 * See also {@link #Fork(Serializable, Method, Serializable...)}
	 * 
	 * @param task a serializable object that implements the task in a method 'public void run()'
	 * 
	 * @throws IOException 
	 * @throws NoSuchMethodException thrown if task does not implement a method 'public void run()' 
	 * @throws SecurityException 
	 * @throws MethodArgumentsException 
	 */
	@SuppressWarnings("unchecked")
	public <T extends Serializable & Runnable> Fork(final T task) throws IOException, SecurityException, NoSuchMethodException, MethodArgumentsException {
		this((TASK_TYPE)task, task.getClass().getMethod("run"));
	}

	/**
	 * Constructs a Fork object with a task and its method to be executed in a
	 * new child process, see also {@link #execute()}.
	 * 
	 * @param task a serializable object that contains parameter "method" to execute
	 * @param method public method of the tasks class that should be executed
	 * @param args method arguments
	 * @throws IOException 
	 * @throws MethodArgumentsException indicates that given arguments do not fit with given method
	 */
	public Fork(final TASK_TYPE task, final Method method, final Serializable... args) throws IOException, MethodArgumentsException {
		this(true, method, args);
		checkMethodArgs(method, args);
		checkReturnType(method);
		createTaskFile(task);
	}

	private Fork(final boolean flag, final Method method, final Serializable[] args) throws IOException {
		this.exceptionFile = File.createTempFile(FILE_PREFIX, "exception");
		this.method = method;
		this.methodArgs = args;
		this.methodRetValFile = File.createTempFile(JFORK_METHOD_RET_VAL, "object");
	}

	/**
	 * Optional, set JVM options to be used for all Fork objects. 
	 * Default option is: -Xmx50m
	 * 
	 * @param vmOptions 
	 */
	public static void setJvmOptionsForAll(final String... vmOptions) {
		if (vmOptions == null) {
			Fork.vmOptionsForAll = null;
		} else {
			Fork.vmOptionsForAll = new ArrayList<String>(Arrays.asList(vmOptions));
		}
	}

	public static void addJvmOptionForAll(final String option) {
		if (option == null) {
			throw new IllegalArgumentException("Parameter option must not be null.");
		}
		if (Fork.vmOptionsForAll == null) {
			Fork.vmOptionsForAll = new ArrayList<String>();
		}
		Fork.vmOptionsForAll.add(option);
	}
	
	/**
	 * Optional, set JVM options to be used for this Fork object. 
	 * Options set for all forks are kept, see {@link #setJvmOptionsForAll(String[])}.
	 * Default option is: -Xmx50m
	 * 
	 * @param vmOptions
	 */
	public void setJvmOptions(final String... vmOptions) {
		if (vmOptions == null) {
			this.vmOptions = null;
		} else {
			this.vmOptions  = new ArrayList<String>(Arrays.asList(vmOptions));
		}
	}

	public void addJvmOption(final String option) {
		if (option == null) {
			throw new IllegalArgumentException("Parameter option must not be null.");
		}
		if (vmOptions == null) {
			vmOptions = new ArrayList<String>();
		}
		vmOptions.add(option);
	}
	
	/**
	 * Set logging for remote task execution on/off (see {@link ForkRunner}). Default is off.
	 * @param b true enables logging for {@link ForkRunner} in remote task process
	 */
	public static void setLoggingEnabled(final boolean b) {
		loggingEnabled = b;
	}

	/**
	 * Optional, set the JVM process working directory.
	 * Default is the parent process working directory.
	 * A check is performed if the given folder exists and if it is readable.
	 * 
	 * @param workingDir a readable path to the working folder
	 */
	public void setWorkingDir(final String workingDir) {
		if (isExecuting()) {
			throw new IllegalStateException(FORK_IS_ALREADY_EXECUTING);
		}
		final File workingDirFile = new File(workingDir);
		if (!workingDirFile.isDirectory()) {
			throw new RuntimeException(String.format("Given working directory is not a folder '%s'", workingDirFile.getAbsolutePath()));
		}
		if (!workingDirFile.canRead()) {
			throw new RuntimeException(String.format("Working directory is not readable '%s'", workingDirFile.getAbsolutePath()));
		}
		this.workingDir = workingDirFile;
	}

	public void setClasspath(final String classpath) {
		this.classpath = classpath;
	}
	public synchronized void addListener(final Listener<TASK_TYPE, RETURN_TYPE> listener) {
		if (listeners == null) {
			listeners = new ArrayList<Listener<TASK_TYPE,RETURN_TYPE>>();
		}
		listeners.add(listener);
	}

	/**
	 * Starts a new java process which runs the task. 
	 * The subprocess inherits the environment including class path an
	 * system properties of the current process. The JVM is launched using
	 * executable derived from standard system property 'java.home'.
	 * <p>
	 * Standard output (System.out) of the task can be red by {@link #getStdOut()} or
	 * forwarded to a file, see {@link #setStdOutWriter(Writer)}.
	 * The same is possible for Standard error (System.err), 
	 * see {@link #getStdErr()} and {@link #setStdErrWriter(Writer)}.
	 * 
	 * @throws Exception
	 */
	public synchronized void execute() throws Exception {
		if (isExecuting()) {
			throw new IllegalStateException(FORK_IS_ALREADY_EXECUTING);
		}
		ProcessBuilder pb = new ProcessBuilder(createCmdArray());
		pb.directory(workingDir);
		exec = pb.start();
		
		taskStdOutReader = new BufferedReader(new InputStreamReader(exec.getInputStream()));
		taskErrorReader = new BufferedReader(new InputStreamReader(exec.getErrorStream()));
		readError();
		readStdOut();
	}

	/**
	 * Indicates if the fork process is already or still running.
	 * @return true if fork process is running
	 */
	public boolean isExecuting() {
		if (exec == null) {
			return false;
		}
		try {
			exec.exitValue();
			return false;
		} catch (IllegalThreadStateException e) {
			return true;
		}
	}

	/**
	 * Waits until task process is finished, notifies listeners and returns the process exit value.
	 * @return process exit value
	 * @throws InterruptedException
	 * @throws IllegalAccessException 
	 * @throws IllegalStateException the fork was not started yet, it is required to invoke {@link #execute()} at first
	 */
	public synchronized int waitFor() throws InterruptedException, IllegalAccessException {
		synchronized(waitForSignal) {
			processListenersInitiated = true;
		}
		return waitForInternal(false);
	}
	
	private int waitForInternal(final boolean skipJoinOutThread) throws InterruptedException, IllegalAccessException {
		if (exec == null) {
			throw new IllegalStateException(FORK_WAS_NOT_STARTED_YET);
		}
		if (finished) {
			return exec.exitValue();
		}
		
		stdErrThread.join();
		if (! skipJoinOutThread) {
			stdOutThread.join();
		}
		final int retVal = exec.waitFor();
		
		finished = true; // also used to avoid recursive calls
		processListeners();
		
		return retVal;
	}
	
	/**
	 * Retrieves finished status of the fork task, call is non blocking.
	 * @return true indicates fork task is finished and listener processing is completed
	 */
	public final boolean isFinished() {
		return finished;
	}

	/**
	 * Waits until task process is finished and retrieves the task method return value.
	 * @return object returned from the executed method
	 * @throws IllegalAccessException if return type is Void or default task of type Runnable is used
	 * @throws InterruptedException if the current thread is interrupted by another thread
	 */
	@SuppressWarnings("unchecked")
	public RETURN_TYPE getReturnValue() throws IllegalAccessException, InterruptedException {
		waitFor();
		if (isReturnTypeVoid()) {
			throw new IllegalAccessException(String.format("Return value type of method '%s' is void.", method.getName()));
		}
		if (!methodRetValFile.exists()) {
			return (RETURN_TYPE) null;
		}
		if (methodRetValFile.length() == 0) {
			throw new IllegalStateException(String.format("No return value exists for method '%s'.", method.getName()));
		}
		if (returnValue == null) {
			FileInputStream fin;
			try {
				fin = new FileInputStream(methodRetValFile);
				final ObjectInputStream oin = new ObjectInputStream(fin);
				returnValue = (RETURN_TYPE) oin.readObject();
				oin.close();
				return returnValue;
			} catch (final Exception e) {
				throw new RuntimeException(e);
			}
		} else {
			return returnValue;
		}
	}

	public boolean isReturnTypeVoid() {
		return returnTypeVoid;
	}

	/**
	 * Waits until task process is finished and returns the process exit value.
	 * @return process exit value
	 * @throws InterruptedException
	 * @throws IllegalAccessException
	 */
	public int getExitValue() throws InterruptedException, IllegalAccessException {
		return waitFor();
	}

	/**
	 * Waits until task process is finished and indicates if the task process exit value was not zero.
	 * See also {@link #getExitValue()}, {@link #getStdErr()}.
	 * 
	 * @return true indicates that the task process exited with an error (process exit value != 0)
	 * @throws InterruptedException
	 * @throws IllegalAccessException 
	 */
	public boolean isError() throws InterruptedException, IllegalThreadStateException, IllegalAccessException {
		return (getExitValue() != 0);
	}	

	/**
	 * Waits until task process is finished an indicates if an exception was thrown by the task process.
	 * See also {@link #getException()}.
	 * @return true if exception was thrown by the task process.
	 * @throws InterruptedException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 */
	public boolean isException() throws InterruptedException, IllegalAccessException, IOException, ClassNotFoundException {
		waitFor();
		return getException() != null;
	}

	/**
	 * Waits until task process is finished an retrieves the task exception
	 * that was thrown from task execution.
	 * @return returns null if task was killed or no exception was thrown
	 * @throws InterruptedException
	 * @throws IllegalAccessException
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public Throwable getException() throws InterruptedException, IllegalAccessException, IOException, ClassNotFoundException {
		waitFor();
		if (killed) {
			return null;
		} 
		if (exception != null) {
			return exception;
		} 
		if (exceptionFile.exists() && exceptionFile.length() > 0) {
			final FileInputStream fin = new FileInputStream(exceptionFile);
			final ObjectInputStream oin = new ObjectInputStream(fin);
			this.exception = (Throwable) oin.readObject();
			fin.close();
			return exception;
		}
		return null;
	}

	/**
	 * Retrieves error output snapshot (System.err) of the task process.
	 * Error output is hold in a buffer with limited size and will be truncated beginning at its starting point, 
	 * see also {@link #setStdErrSize(int)}.
	 * The {@link ForkRunner} implementation prints exceptions to System.err.
	 *  
	 * @return System.err output from the executed task process
	 * @throws InterruptedException
	 * @throws IllegalAccessException 
	 */
	public String getStdErr() throws InterruptedException, IllegalAccessException {
		return stdErrText.toString();
	}
	
	/**
	 * Set maximum error output buffer size. Default is 2000 characters.
	 * @param characters maximum output size in characters + buffering of 1000 characters
	 */
	public void setStdErrSize(final int characters) {
		stdErrSize = characters;
	}
	
	/**
	 * Causes the Fork object to write all standard output from the task process
	 * to the given writer. The writer will be closed when the process is finished.
	 *  
	 * @param stdErrWriter
	 */
	public void setStdErrWriter(final Writer stdErrWriter) {
		if (isExecuting()) {
			throw new IllegalStateException(FORK_IS_ALREADY_EXECUTING);
		}
		this.stdErrWriter = stdErrWriter;
	}

	/**
	 * Retrieves standard output snapshot (System.out) of the task process.
	 * Output is hold in a buffer with limited size and will be truncated beginning at its starting point, 
	 * see also {@link #setStdOutSize(int)}.
	 *  
	 * @return System.out output from the executed task process
	 * @throws InterruptedException
	 * @throws IllegalAccessException 
	 */
	public String getStdOut() throws InterruptedException, IllegalAccessException {
		synchronized(stdOutText) {
			return stdOutText.toString();
		}
	}

	/**
	 * Returns stdout buffer and deletes it.
	 * @see #getStdOut()
	 * @return current standard output buffer contentse
	 * @throws InterruptedException
	 * @throws IllegalAccessException
	 */
	public String pullStdOut() throws InterruptedException, IllegalAccessException {
		final String ret;
		synchronized(stdOutText) {
			ret = stdOutText.toString();
			stdOutText.delete(0, stdOutText.length());
		}
		return ret;
	}
	
	public boolean hasStdOut() {
		synchronized(stdOutText) {
			return stdOutText.length() > 0;
		}
	}
	
	/**
	 * Set maximum standard output buffer size. Default is 2000 characters.
	 * @param characters maximum output size in characters + buffering of 1000 characters
	 */
	public void setStdOutSize(final int characters) {
		stdOutSize = characters;
	}
	
	/**
	 * Causes the Fork object to write all standard output from the task process
	 * to the given writer. The writer will be closed when the process is finished.
	 *  
	 * @param stdOutWriter
	 */
	public void setStdOutWriter(final Writer stdOutWriter) {
		if (isExecuting()) {
			throw new IllegalStateException(FORK_IS_ALREADY_EXECUTING);
		}
		this.stdOutWriter = stdOutWriter;
	}

	/**
	 * Waits until task process is finished and retrieves the task object as it was after the task process was finished.
	 * 
	 * @return processed task object
	 * @throws InterruptedException 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 */
	@SuppressWarnings("unchecked")
	public TASK_TYPE getTask() throws InterruptedException, IOException, ClassNotFoundException, IllegalAccessException {
		waitFor();
		if (taskResult == null) {
			final FileInputStream fin = new FileInputStream(taskFile);
			final ObjectInputStream oin = new ObjectInputStream(fin);
			this.taskResult = (TASK_TYPE) oin.readObject();
			fin.close();
		}
		return taskResult;
	}
	
	/**
	 * Kills the subprocess immediately and notifies listeners if available.
	 * Calls Java API {@link Process#destroy()}.
	 * @throws IOException
	 * @throws InterruptedException 
	 * @throws IllegalAccessException 
	 */
	public void kill() throws IOException, IllegalAccessException, InterruptedException {
		if (exec == null) {
			throw new IllegalStateException(FORK_WAS_NOT_STARTED_YET);
		}
		killed = true;
		stdErrThread.interrupt();
		stdOutThread.interrupt();
		exec.destroy();
		waitFor();
	}

	protected void setStatusInfo(final String statusInfo) {
		this.statusInfo = statusInfo;
	}

	protected String getStatusInfo() {
		return statusInfo;
	}

	private void checkMethodArgs(final Method method, final Serializable[] args) throws MethodArgumentsException {
		final Class<?>[] parameterTypes = method.getParameterTypes();
		final boolean arrayExpected = parameterTypes.length == 1 && parameterTypes[0].isArray() ;
		if (arrayExpected && !args.getClass().isArray() && !(args.length == 1) &&
				!args[0].getClass().isArray()) {
			throw new MethodArgumentsException(
					String.format("Method '%s' expects array as argument wrapped in a Serializable[].", 
							method.getName()));
		}
		if (arrayExpected && parameterTypes[0] != args[0].getClass()) {
			throw new MethodArgumentsException(
					String.format("Method '%s' expects array of type %s as argument but was %s.", 
							method.getName(), parameterTypes[0].getClass(), args.getClass()));
		} 
		if (!arrayExpected){
			if (parameterTypes.length != args.length) {
				throw new MethodArgumentsException(
						String.format("Method '%s' expects %d arguments but %d were given.", 
								method.getName(), parameterTypes.length, args.length));
			}
			for (int i = 0; i < args.length; i++) {
				try {
					if (! isCompatiblePrimitive(parameterTypes[i], args[i].getClass())) {
						parameterTypes[i].cast(args[i]);
					}
				} catch (final Exception e) {
					e.printStackTrace();
					throw new MethodArgumentsException(
							String.format("Argument %d is of type '%s' but method expects type '%s'.", 
							i, args[i].getClass(), parameterTypes[i]));
				}
			}
		}
	}

	private boolean isCompatiblePrimitive(final Class<?> type, final Class<? extends Serializable> argType) {
		// argType cannot be primitive
		if (type.isPrimitive()) {
			if (int.class.equals(type) && argType.equals(Integer.class)) {
				return true;
			}
			if (long.class.equals(type) && argType.equals(Long.class)) {
				return true;
			}
			if (boolean.class.equals(type) && argType.equals(Boolean.class)) {
				return true;
			}
			if (float.class.equals(type) && argType.equals(Float.class)) {
				return true;
			}
			if (double.class.equals(type) && argType.equals(Double.class)) {
				return true;
			}
			if (byte.class.equals(type) && argType.equals(Byte.class)) {
				return true;
			}
			if (char.class.equals(type) && argType.equals(Character.class)) {
				return true;
			}
			if (short.class.equals(type) && argType.equals(Short.class)) {
				return true;
			}
			return true;
		}
		return false;
	}

	private final void checkReturnType(final Method method) {
		final Class<?> returnType = method.getReturnType();
		if (returnType.equals(void.class)) {
			returnTypeVoid = true;
			return;
		}
		if (!returnType.isPrimitive()) {
			method.getReturnType().asSubclass(Serializable.class);
		}
	}

	protected String[] createCmdArray() throws IOException {
		final List<String> vmArgs = new ArrayList<String>(20);
		// java executable
		vmArgs.add(getJavaExe());
		// java VM options
		createAndAddJvmOptions(vmArgs);
		// fork runner class name
		vmArgs.add(ForkRunner.class.getName());
		// 0: task object file
		vmArgs.add(taskFile.getAbsolutePath());
		// 1: task exception file (will be deleted by runner if no exception was thrown)
		vmArgs.add(exceptionFile.getAbsolutePath());
		// 2: logging on/off
		vmArgs.add(Boolean.toString(loggingEnabled));
		if (method != null) {
			// 3: task method return value file (will be deleted if method return type is void)
			vmArgs.add(methodRetValFile.getAbsolutePath());
			// 4: task method name to execute
			vmArgs.add(method.getName());
			// 5: task method arguments (optional)
			vmArgs.add(createMethodArgTypesAndValuesFile());
		}
		return (String[]) vmArgs.toArray(new String[vmArgs.size()]);
	}

	private void createAndAddJvmOptions(final List<String> vmArgs) {
		vmArgs.add("-cp");
		if (classpath != null) {
			vmArgs.add(classpath);
		} else {
			vmArgs.add(System.getProperty("java.class.path"));
		}
		if (vmOptions != null && !vmOptions.isEmpty()) {
			vmArgs.addAll(vmOptions);
		} 
		if (vmOptionsForAll != null && !vmOptionsForAll.isEmpty()) {
			vmArgs.addAll(vmOptionsForAll);
		} else {
			vmArgs.add(OPT_MAXMEM);
		}
		if (! skipMergeSystenProperties) {
			mergeSystemProperties(vmArgs);
		}
	}

	private void mergeSystemProperties(final List<String> vmArgs) {
		if (remoteSystemProperties == null) {
			Fork.remoteSystemProperties = retrieveRemoteSystemProperties();
		}
		final Set<Entry<Object,Object>> entrySet = System.getProperties().entrySet();
		for (final Entry<Object,Object> localProperty : entrySet) {
			if (! isPropertyAlreadySetAsOption(localProperty, vmArgs) && ! isPropertySetLocalAndRemote(localProperty)) {
				vmArgs.add("-D" + localProperty.getKey() + '=' + localProperty.getValue());
			}
		}
	}

	private boolean isPropertyAlreadySetAsOption(
			final Entry<Object, Object> localProperty, final List<String> vmArgs) {
		for (final String vmArg : vmArgs) { 
			if (vmArg.contains("-D" + localProperty.getKey() + '=')) { // could be optimized
				return true;
			}
		}
		return false;
	}

	private Properties retrieveRemoteSystemProperties() {
		try {
			final Fork<SysPropTask, Properties> f = new Fork<SysPropTask, Properties>(
					new SysPropTask(),
					SysPropTask.getSystemPropertiesMethod());
			f.skipMergeSystenProperties = true;
			f.execute();
			f.waitFor();
			if (f.isException()) {
				throw f.getException();
			}
			if (f.isError()) {
				throw new RuntimeException(f.getStdErr());
			}
			return f.getReturnValue();
		} catch (final Throwable e) {
			throw new RuntimeException(e);
		}
	}

	private boolean isPropertySetLocalAndRemote(final Entry<Object, Object> localProperty) {
		if (! remoteSystemProperties.containsKey(localProperty.getKey())) {
			return false;
		}
		final Object remoteValue = remoteSystemProperties.get(localProperty.getKey());
		return remoteValue.equals(localProperty.getValue());
	}

	private String createMethodArgTypesAndValuesFile() throws IOException {
		if (methodArgs != null && methodArgs.length > 0) { 
			final File argsObj = File.createTempFile(FILE_PREFIX, "object");
			final FileOutputStream fo = new FileOutputStream(argsObj);
			final ObjectOutputStream oo = new ObjectOutputStream(fo);
			final Class<?>[] methodArgTypes = method.getParameterTypes();
			oo.writeObject(methodArgTypes);
			oo.writeObject(methodArgs);
			fo.close();
			return argsObj.getAbsolutePath();
		} else {
			return ".nomethodargs";
		}
	}

	private final void createTaskFile(final TASK_TYPE task) throws IOException, FileNotFoundException {
		final File taskObjFile = File.createTempFile(FILE_PREFIX, "object");
		final FileOutputStream fo = new FileOutputStream(taskObjFile);
		final ObjectOutputStream oo = new ObjectOutputStream(fo);
		oo.writeObject(task);
		fo.close();
		this.taskFile = taskObjFile;
	}

	private void readError() throws IOException, InterruptedException {
		stdErrThread = new Thread("jforkReadError") {
			public void run() {
				String line;
				try {
					while((line = taskErrorReader.readLine()) != null) {
						stdErrText.append(line).append(NL);
						if (stdErrText.length() > stdErrSize + 1000) {
							stdErrText.delete(0, 500);
						}
						if (stdErrWriter != null) {
							stdErrWriter.write(line);
							stdErrWriter.write(NL);
							stdErrWriter.flush();
						}
					}
					taskErrorReader.close();
				} catch (final Exception e) {
					e.printStackTrace();
					stdErrText.append(String.format("ERROR thread jforkReadError: %s%n", e.toString()));
				}
			}
		};
		stdErrThread.start();
	}
	
	private void readStdOut() throws IOException {
		stdOutThread = new Thread("jforkReadStdOut") {
			public void run() {
				try {
					String line;
					while((line = taskStdOutReader.readLine()) != null) {
						if (getStatusInfo() == null) {
							setStatusInfo(line); // 1st line used for fork process status info
						} else {
							synchronized(stdOutText) {
								stdOutText.append(line).append(NL);
								if (stdOutText.length() > stdOutSize + 1000) {
									stdOutText.delete(0, 500);
								}
							}
							if (stdOutWriter != null) {
								stdOutWriter.write(line);
								stdOutWriter.write(NL);
								stdOutWriter.flush();
							}
						}
					}
					taskStdOutReader.close();
				} catch (final Exception e) {
					e.printStackTrace();
					synchronized(stdOutText) {
						stdOutText.append(String.format("ERROR thread jforkReadStdOut run: %s%n", e.toString()));
					}
				}
				try {
					synchronized(waitForSignal) {
						if (! processListenersInitiated) 
							waitForInternal(true); // needed to force listener processing
					}
				} catch (final Exception e) {
					e.printStackTrace();
					synchronized(stdOutText) {
						stdOutText.append(String.format("ERROR thread jforkReadStdOut wait: %s%n", e.toString()));
					}
				} 
			}

		};
		stdOutThread.start();
	}

	private static String getJavaExe() {
		if (javaExe == null) {
			javaExe = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java.exe";
			if (!(new File(javaExe)).exists()) {
				javaExe = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
			}
		} 
		return javaExe;
	}

	private synchronized void processListeners() throws IllegalAccessException, InterruptedException {
		if (listeners != null && !listeners.isEmpty()) {
			for (final Fork.Listener<TASK_TYPE, RETURN_TYPE> l : listeners) {
				l.onFinish(this, killed);
				if (!killed){
					if (isError()) {
						l.onError(this);
					}
					try {
						if (isException()) {
							l.onException(this);
						}
					} catch (final IOException e) {
						throw new RuntimeException(e);
					} catch (final ClassNotFoundException e) {
						throw new RuntimeException(e);
					}
				}
			}
			listeners = null;
		}
	}

	@Override
	protected void finalize() throws Throwable {
		kill();
		super.finalize();
	}
}
