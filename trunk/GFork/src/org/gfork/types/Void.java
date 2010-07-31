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

package org.gfork.types;

import java.io.Serializable;

import org.gfork.Fork;

/**
 * Helper class used to specify a void return type for a Fork task. 
 * Use this instead of java.lang.Void, it cannot be used,
 * as it does not implement interface Serializable which is the 
 * required template upper bound type for return types (see {@link Fork}).
 * 
 * @author Gerald Ehmayer
 *
 */
public class Void implements Serializable {

	private static final long serialVersionUID = 1L;

}
