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

package org.gfork.sample.swing;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.io.BufferedInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;

import org.gfork.CallableTask;
import org.gfork.swing.JFrameTask;

/**
 * This task class paints the duke image to a given position in a {@link JFrame}.
 * The parent process controls the painting and its position thru remote calls to 
 * {@link DukePaintTask#paintDuke(Point)}, see also callable task class {@link CallableTask}.
 * 
 * @author Gerald Ehmayer
 *
 */
public class DukePaintTask extends JFrameTask {

	private static final long serialVersionUID = 0L;
	private transient DukeCanvas dukeCanvas;
	private boolean ready;

	public DukePaintTask(JFrame frame) {
		super(frame);
	}
	
	@Override
	protected void initialize() throws IOException {
		super.initialize();
		dukeCanvas = new DukeCanvas();
		frame.getContentPane().add(dukeCanvas);
		ready = true;
	}

	public void paintDuke(Point data) {
		dukeCanvas.setPoint(data);
		dukeCanvas.repaint();
	}

	@Override
	public DukePaintTask getImplementingObject() {
		return this;
	}

	@Override
	public boolean isReadyToBeCalled() {
		return ready;
	}

}

class DukeCanvas extends JComponent {
	
	private static final long serialVersionUID = 1L;
	private Point pos = new Point(10,10);
	private Image dukeImg;
	
	public DukeCanvas() throws IOException {
		dukeImg = ImageIO.read(new BufferedInputStream(getClass().getResourceAsStream("DukeWave.gif")));
	}

	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		g2.drawImage(dukeImg, pos.x, pos.y, this);
		g2.finalize();
	}

	public void setPoint(Point pos) {
		this.pos  = pos;
	}
}
