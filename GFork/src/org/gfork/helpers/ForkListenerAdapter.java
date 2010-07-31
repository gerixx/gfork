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

package org.gfork.helpers;

import java.io.Serializable;

import org.gfork.Fork;

/**
 * Default implementation of the Fork notification interface {@link Fork.Listener}. Use it to conveniently 
 * implement a Fork listener by overriding what you need.
 * 
 * @see Fork.Listener
 * @author Gerald Ehmayer
 *
 * @param <T> task type
 * @param <V> return type of the task
 */
public abstract class ForkListenerAdapter<T extends Serializable, V extends Serializable> implements Fork.Listener<T,V> {

	@Override
	public void onFinish(Fork<T,V> fork, boolean wasKilled) throws IllegalAccessException,
			InterruptedException {
	}

	@Override
	public void onError(Fork<T, V> fork) throws IllegalAccessException,
			InterruptedException {
	}

	@Override
	public void onException(Fork<T, V> fork) throws IllegalAccessException,
			InterruptedException {
	}
	
}
