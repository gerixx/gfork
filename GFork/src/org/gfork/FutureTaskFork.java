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

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.gfork.internal.types.TaskDelegate;
import org.gfork.types.MethodArgumentsException;

/**
 * This class provides the same interface as {@link FutureTask} of the Java Concurrency API.
 * Instead of running a task in a thread, this implementation spawns a fork subprocess to run a 
 * given task.
 * 
 * @author Gerald Ehmayer
 *
 * @param <V>
 */
public class FutureTaskFork<V extends Serializable> implements RunnableFuture<V> {

	private static final int MINIMUM_TIMEOUT_MILLIS = 2000;
	private static final int FINISH_CHECK_INTERVAL_MILLIS = 200;

	protected Fork<TaskDelegate<V>, V> fork;
	private final V result;
	private boolean runnableTask;
	private boolean cancelled;
	
	public <TASK_TYPE extends Serializable & Callable<V>> FutureTaskFork(final TASK_TYPE callable) throws SecurityException, IOException, MethodArgumentsException, NoSuchMethodException {
		this.fork = new Fork<TaskDelegate<V>, V>(new TaskDelegate<V>(callable), callable.getClass().getMethod("call"));
		this.result = null;
		this.runnableTask = false;
	}
	
	public <TASK_TYPE extends Serializable & Runnable> FutureTaskFork(final TASK_TYPE runnable, final V result) throws SecurityException, IOException, NoSuchMethodException, MethodArgumentsException {
		this.fork = new Fork<TaskDelegate<V>, V>(new TaskDelegate<V>(runnable, result));
		this.result = result;
		this.runnableTask = true;
	}
	
	@Override
	public void run() {
		try {
			fork.execute();
			synchronized(this) {
				notifyAll();
			}
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean cancel(final boolean mayInterruptIfRunning) {
		if (fork.isFinished()) {
			return false;
		}
		try {
			fork.kill();
			cancelled = true;
		} catch (Exception e) {
		}
		return fork.isFinished();
	}

	@Override
	public V get() throws InterruptedException, ExecutionException {
		try {
			synchronized(this) {
				wait();
			}
			fork.waitFor();
			return retrieveValue();
		} catch (final Exception e) {
			throw new ExecutionException(e);
		}
	}

	@Override
	public V get(final long timeout, final TimeUnit unit) throws InterruptedException,
			ExecutionException, TimeoutException {
		final long timeOutInMillis = unit.toMillis(timeout);
		if (timeOutInMillis < 2000) {
			throw new IllegalArgumentException(String.format("Given timeout value of %d millis does not make sense for forked tasks. Minimum expected value is %d.", timeOutInMillis, MINIMUM_TIMEOUT_MILLIS));
		}
		final long startTime = System.currentTimeMillis();
		synchronized(this) {
			wait();
		}
		final Thread waiter = new Thread() {
			@Override
			public void run() {
				try {
					long waitTime = System.currentTimeMillis() - startTime;
					while (! fork.isFinished() && waitTime < timeOutInMillis) {
						Thread.sleep(FINISH_CHECK_INTERVAL_MILLIS);
						waitTime += FINISH_CHECK_INTERVAL_MILLIS;
					}
				} catch (final InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		try {
			waiter.start();
			waiter.join();
			if (fork.isFinished()) {
				return retrieveValue();
			} 
		} catch (Exception e) {
			throw new ExecutionException(e);
		}
		throw new TimeoutException(String.format(
				"Task was not finished after %d milliseconds.",
				timeOutInMillis));
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public boolean isDone() {
		return fork.isFinished();
	}

	private V retrieveValue() throws IllegalAccessException, InterruptedException {
		if (runnableTask) {
			return result;
		} else {
			return fork.getReturnValue();
		}
	}

}
