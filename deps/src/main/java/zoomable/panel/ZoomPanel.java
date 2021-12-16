package zoomable.panel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;

/**
 * @author Thanasis1101
 * @version 1.0
 */
public class ZoomPanel extends JPanel implements MouseWheelListener, MouseListener, MouseMotionListener {


	private double zoomFactor = 1;
	private double xOffset;
	private double yOffset;
	private Point startPoint;

	public ZoomPanel() {

		initComponent();

	}

	private void initComponent() {
		addMouseWheelListener(this);
		addMouseMotionListener(this);
		addMouseListener(this);
	}


//	@Override
//	public void paintAll(Graphics g) {
//		super.paintAll(getGraphics(g));
//	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(getGraphics(g));
	}

	private Graphics getGraphics(Graphics original) {
		Graphics2D g2 = (Graphics2D) original;

		AffineTransform at = g2.getTransform();

		at.translate(xOffset, yOffset);
		at.scale(zoomFactor, zoomFactor);
		g2.setTransform(at);

		return g2;
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {


		double prevZoomFactor = zoomFactor;
		//Zoom in
		if (e.getWheelRotation() < 0) {
			zoomFactor *= 1.1;
			repaint();
		}
		//Zoom out
		if (e.getWheelRotation() > 0) {
			zoomFactor /= 1.1;
			repaint();
		}
		double xRel = MouseInfo.getPointerInfo().getLocation().getX() - getLocationOnScreen().getX();
		double yRel = MouseInfo.getPointerInfo().getLocation().getY() - getLocationOnScreen().getY();

		double zoomDiv = zoomFactor / prevZoomFactor;

		xOffset = (zoomDiv) * (xOffset) + (1 - zoomDiv) * xRel;
		yOffset = (zoomDiv) * (yOffset) + (1 - zoomDiv) * yRel;
		repaint();
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		Point curPoint = e.getLocationOnScreen();
		double xDiff = curPoint.x - startPoint.x;
		double yDiff = curPoint.y - startPoint.y;
		xOffset += xDiff;
		yOffset += yDiff;
		startPoint = curPoint;

		repaint();

	}

	@Override
	public void mouseMoved(MouseEvent e) {
	}

	@Override
	public void mouseClicked(MouseEvent e) {

	}

	@Override
	public void mousePressed(MouseEvent e) {
		startPoint = MouseInfo.getPointerInfo().getLocation();
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		repaint();
	}

	@Override
	public void mouseEntered(MouseEvent e) {

	}

	@Override
	public void mouseExited(MouseEvent e) {

	}

}
