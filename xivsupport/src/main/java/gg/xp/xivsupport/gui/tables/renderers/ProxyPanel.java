package gg.xp.xivsupport.gui.tables.renderers;

import org.jetbrains.annotations.NotNull;

import javax.accessibility.AccessibleContext;
import java.awt.*;
import java.awt.dnd.DropTarget;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyListener;
import java.awt.event.InputMethodListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.awt.im.InputContext;
import java.awt.im.InputMethodRequests;
import java.awt.image.ColorModel;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.awt.image.VolatileImage;
import java.beans.PropertyChangeListener;
import java.beans.Transient;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.EventListener;
import java.util.Locale;
import java.util.Set;

public class ProxyPanel extends Component {

	private final Component realComponent;

	public ProxyPanel(Component realComponent) {
		this.realComponent = realComponent;
	}

	@Override
	public String getName() {
		return realComponent.getName();
	}

	@Override
	public void setName(String name) {
		realComponent.setName(name);
	}

	@Override
	public Container getParent() {
		return realComponent.getParent();
	}

	@Override
	public void setDropTarget(DropTarget dt) {
		realComponent.setDropTarget(dt);
	}

	@Override
	public DropTarget getDropTarget() {
		return realComponent.getDropTarget();
	}

	@Override
	public GraphicsConfiguration getGraphicsConfiguration() {
		return realComponent.getGraphicsConfiguration();
	}

	@Override
	public Toolkit getToolkit() {
		return realComponent.getToolkit();
	}

	@Override
	public boolean isValid() {
		return realComponent.isValid();
	}

	@Override
	public boolean isDisplayable() {
		return realComponent.isDisplayable();
	}

	@Override
	@Transient
	public boolean isVisible() {
		return realComponent.isVisible();
	}

	@Override
	public Point getMousePosition() throws HeadlessException {
		return realComponent.getMousePosition();
	}

	@Override
	public boolean isShowing() {
		return realComponent.isShowing();
	}

	@Override
	public boolean isEnabled() {
		return realComponent.isEnabled();
	}

	@Override
	public void setEnabled(boolean b) {
		realComponent.setEnabled(b);
	}

	@Override
	@Deprecated
	public void enable() {
		realComponent.enable();
	}

	@Override
	@Deprecated
	public void enable(boolean b) {
		realComponent.enable(b);
	}

	@Override
	@Deprecated
	public void disable() {
		realComponent.disable();
	}

	@Override
	public boolean isDoubleBuffered() {
		return realComponent.isDoubleBuffered();
	}

	@Override
	public void enableInputMethods(boolean enable) {
		realComponent.enableInputMethods(enable);
	}

	@Override
	public void setVisible(boolean b) {
		realComponent.setVisible(b);
	}

	@Override
	@Deprecated
	public void show() {
		realComponent.show();
	}

	@Override
	@Deprecated
	public void show(boolean b) {
		realComponent.show(b);
	}

	@Override
	@Deprecated
	public void hide() {
		realComponent.hide();
	}

	@Override
	@Transient
	public Color getForeground() {
		return realComponent.getForeground();
	}

	@Override
	public void setForeground(Color c) {
		realComponent.setForeground(c);
	}

	@Override
	public boolean isForegroundSet() {
		return realComponent.isForegroundSet();
	}

	@Override
	@Transient
	public Color getBackground() {
		return realComponent.getBackground();
	}

	@Override
	public void setBackground(Color c) {
		realComponent.setBackground(c);
	}

	@Override
	public boolean isBackgroundSet() {
		return realComponent.isBackgroundSet();
	}

	@Override
	@Transient
	public Font getFont() {
		return realComponent.getFont();
	}

	@Override
	public void setFont(Font f) {
		realComponent.setFont(f);
	}

	@Override
	public boolean isFontSet() {
		return realComponent.isFontSet();
	}

	@Override
	public Locale getLocale() {
		return realComponent.getLocale();
	}

	@Override
	public void setLocale(Locale l) {
		realComponent.setLocale(l);
	}

	@Override
	public ColorModel getColorModel() {
		return realComponent.getColorModel();
	}

	@NotNull
	@Override
	public Point getLocation() {
		return realComponent.getLocation();
	}

	@Override
	public Point getLocationOnScreen() {
		return realComponent.getLocationOnScreen();
	}

	@NotNull
	@Override
	@Deprecated
	public Point location() {
		return realComponent.location();
	}

	@Override
	public void setLocation(int x, int y) {
		realComponent.setLocation(x, y);
	}

	@Override
	@Deprecated
	public void move(int x, int y) {
		realComponent.move(x, y);
	}

	@Override
	public void setLocation(@NotNull Point p) {
		realComponent.setLocation(p);
	}

	@Override
	public Dimension getSize() {
		return realComponent.getSize();
	}

	@Override
	@Deprecated
	public Dimension size() {
		return realComponent.size();
	}

	@Override
	public void setSize(int width, int height) {
		realComponent.setSize(width, height);
	}

	@Override
	@Deprecated
	public void resize(int width, int height) {
		realComponent.resize(width, height);
	}

	@Override
	public void setSize(Dimension d) {
		realComponent.setSize(d);
	}

	@Override
	@Deprecated
	public void resize(Dimension d) {
		realComponent.resize(d);
	}

	@Override
	public Rectangle getBounds() {
		return realComponent.getBounds();
	}

	@Override
	@Deprecated
	public Rectangle bounds() {
		return realComponent.bounds();
	}

	@Override
	public void setBounds(int x, int y, int width, int height) {
		realComponent.setBounds(x, y, width, height);
	}

	@Override
	@Deprecated
	public void reshape(int x, int y, int width, int height) {
		realComponent.reshape(x, y, width, height);
	}

	@Override
	public void setBounds(Rectangle r) {
		realComponent.setBounds(r);
	}

	@Override
	public int getX() {
		return realComponent.getX();
	}

	@Override
	public int getY() {
		return realComponent.getY();
	}

	@Override
	public int getWidth() {
		return realComponent.getWidth();
	}

	@Override
	public int getHeight() {
		return realComponent.getHeight();
	}

	@Override
	public Rectangle getBounds(Rectangle rv) {
		return realComponent.getBounds(rv);
	}

	@Override
	public Dimension getSize(Dimension rv) {
		return realComponent.getSize(rv);
	}

	@Override
	public Point getLocation(Point rv) {
		return realComponent.getLocation(rv);
	}

	@Override
	public boolean isOpaque() {
		return realComponent.isOpaque();
	}

	@Override
	public boolean isLightweight() {
		return realComponent.isLightweight();
	}

	@Override
	public void setPreferredSize(Dimension preferredSize) {
		realComponent.setPreferredSize(preferredSize);
	}

	@Override
	public boolean isPreferredSizeSet() {
		return realComponent.isPreferredSizeSet();
	}

	@Override
	public Dimension getPreferredSize() {
		return realComponent.getPreferredSize();
	}

	@Override
	@Deprecated
	public Dimension preferredSize() {
		return realComponent.preferredSize();
	}

	@Override
	public void setMinimumSize(Dimension minimumSize) {
		realComponent.setMinimumSize(minimumSize);
	}

	@Override
	public boolean isMinimumSizeSet() {
		return realComponent.isMinimumSizeSet();
	}

	@Override
	public Dimension getMinimumSize() {
		return realComponent.getMinimumSize();
	}

	@Override
	@Deprecated
	public Dimension minimumSize() {
		return realComponent.minimumSize();
	}

	@Override
	public void setMaximumSize(Dimension maximumSize) {
		realComponent.setMaximumSize(maximumSize);
	}

	@Override
	public boolean isMaximumSizeSet() {
		return realComponent.isMaximumSizeSet();
	}

	@Override
	public Dimension getMaximumSize() {
		return realComponent.getMaximumSize();
	}

	@Override
	public float getAlignmentX() {
		return realComponent.getAlignmentX();
	}

	@Override
	public float getAlignmentY() {
		return realComponent.getAlignmentY();
	}

	@Override
	public int getBaseline(int width, int height) {
		return realComponent.getBaseline(width, height);
	}

	@Override
	public BaselineResizeBehavior getBaselineResizeBehavior() {
		return realComponent.getBaselineResizeBehavior();
	}

	@Override
	public void doLayout() {
		realComponent.doLayout();
	}

	@Override
	@Deprecated
	public void layout() {
		realComponent.layout();
	}

	@Override
	public void validate() {
		realComponent.validate();
	}

	@Override
	public void invalidate() {
		realComponent.invalidate();
	}

	@Override
	public void revalidate() {
		realComponent.revalidate();
	}

	@Override
	public Graphics getGraphics() {
		return realComponent.getGraphics();
	}

	@Override
	public FontMetrics getFontMetrics(Font font) {
		return realComponent.getFontMetrics(font);
	}

	@Override
	public void setCursor(Cursor cursor) {
		realComponent.setCursor(cursor);
	}

	@NotNull
	@Override
	public Cursor getCursor() {
		return realComponent.getCursor();
	}

	@Override
	public boolean isCursorSet() {
		return realComponent.isCursorSet();
	}

	@Override
	public void paint(Graphics g) {
		realComponent.paint(g);
	}

	@Override
	public void update(Graphics g) {
		realComponent.update(g);
	}

	@Override
	public void paintAll(Graphics g) {
		realComponent.paintAll(g);
	}

	@Override
	public void repaint() {
		realComponent.repaint();
	}

	@Override
	public void repaint(long tm) {
		realComponent.repaint(tm);
	}

	@Override
	public void repaint(int x, int y, int width, int height) {
		realComponent.repaint(x, y, width, height);
	}

	@Override
	public void repaint(long tm, int x, int y, int width, int height) {
		realComponent.repaint(tm, x, y, width, height);
	}

	@Override
	public void print(Graphics g) {
		realComponent.print(g);
	}

	@Override
	public void printAll(Graphics g) {
		realComponent.printAll(g);
	}

	@Override
	public boolean imageUpdate(Image img, int infoflags, int x, int y, int w, int h) {
		return realComponent.imageUpdate(img, infoflags, x, y, w, h);
	}

	@Override
	public Image createImage(ImageProducer producer) {
		return realComponent.createImage(producer);
	}

	@Override
	public Image createImage(int width, int height) {
		return realComponent.createImage(width, height);
	}

	@Override
	public VolatileImage createVolatileImage(int width, int height) {
		return realComponent.createVolatileImage(width, height);
	}

	@Override
	public VolatileImage createVolatileImage(int width, int height, ImageCapabilities caps) throws AWTException {
		return realComponent.createVolatileImage(width, height, caps);
	}

	@Override
	public boolean prepareImage(Image image, ImageObserver observer) {
		return realComponent.prepareImage(image, observer);
	}

	@Override
	public boolean prepareImage(Image image, int width, int height, ImageObserver observer) {
		return realComponent.prepareImage(image, width, height, observer);
	}

	@Override
	public int checkImage(Image image, ImageObserver observer) {
		return realComponent.checkImage(image, observer);
	}

	@Override
	public int checkImage(Image image, int width, int height, ImageObserver observer) {
		return realComponent.checkImage(image, width, height, observer);
	}

	@Override
	public void setIgnoreRepaint(boolean ignoreRepaint) {
		realComponent.setIgnoreRepaint(ignoreRepaint);
	}

	@Override
	public boolean getIgnoreRepaint() {
		return realComponent.getIgnoreRepaint();
	}

	@Override
	public boolean contains(int x, int y) {
		return realComponent.contains(x, y);
	}

	@Override
	@Deprecated
	public boolean inside(int x, int y) {
		return realComponent.inside(x, y);
	}

	@Override
	public boolean contains(Point p) {
		return realComponent.contains(p);
	}

	@Override
	public Component getComponentAt(int x, int y) {
		return realComponent.getComponentAt(x, y);
	}

	@Override
	@Deprecated
	public Component locate(int x, int y) {
		return realComponent.locate(x, y);
	}

	@Override
	public Component getComponentAt(Point p) {
		return realComponent.getComponentAt(p);
	}

	@Override
	@Deprecated
	public void deliverEvent(Event e) {
		realComponent.deliverEvent(e);
	}

	@Override
	@Deprecated
	public boolean postEvent(Event e) {
		return realComponent.postEvent(e);
	}

	@Override
	public void addComponentListener(ComponentListener l) {
		realComponent.addComponentListener(l);
	}

	@Override
	public void removeComponentListener(ComponentListener l) {
		realComponent.removeComponentListener(l);
	}

	@Override
	public ComponentListener[] getComponentListeners() {
		return realComponent.getComponentListeners();
	}

	@Override
	public void addFocusListener(FocusListener l) {
		realComponent.addFocusListener(l);
	}

	@Override
	public void removeFocusListener(FocusListener l) {
		realComponent.removeFocusListener(l);
	}

	@Override
	public FocusListener[] getFocusListeners() {
		return realComponent.getFocusListeners();
	}

	@Override
	public void addHierarchyListener(HierarchyListener l) {
		realComponent.addHierarchyListener(l);
	}

	@Override
	public void removeHierarchyListener(HierarchyListener l) {
		realComponent.removeHierarchyListener(l);
	}

	@Override
	public HierarchyListener[] getHierarchyListeners() {
		return realComponent.getHierarchyListeners();
	}

	@Override
	public void addHierarchyBoundsListener(HierarchyBoundsListener l) {
		realComponent.addHierarchyBoundsListener(l);
	}

	@Override
	public void removeHierarchyBoundsListener(HierarchyBoundsListener l) {
		realComponent.removeHierarchyBoundsListener(l);
	}

	@Override
	public HierarchyBoundsListener[] getHierarchyBoundsListeners() {
		return realComponent.getHierarchyBoundsListeners();
	}

	@Override
	public void addKeyListener(KeyListener l) {
		realComponent.addKeyListener(l);
	}

	@Override
	public void removeKeyListener(KeyListener l) {
		realComponent.removeKeyListener(l);
	}

	@Override
	public KeyListener[] getKeyListeners() {
		return realComponent.getKeyListeners();
	}

	@Override
	public void addMouseListener(MouseListener l) {
		realComponent.addMouseListener(l);
	}

	@Override
	public void removeMouseListener(MouseListener l) {
		realComponent.removeMouseListener(l);
	}

	@Override
	public MouseListener[] getMouseListeners() {
		return realComponent.getMouseListeners();
	}

	@Override
	public void addMouseMotionListener(MouseMotionListener l) {
		realComponent.addMouseMotionListener(l);
	}

	@Override
	public void removeMouseMotionListener(MouseMotionListener l) {
		realComponent.removeMouseMotionListener(l);
	}

	@Override
	public MouseMotionListener[] getMouseMotionListeners() {
		return realComponent.getMouseMotionListeners();
	}

	@Override
	public void addMouseWheelListener(MouseWheelListener l) {
		realComponent.addMouseWheelListener(l);
	}

	@Override
	public void removeMouseWheelListener(MouseWheelListener l) {
		realComponent.removeMouseWheelListener(l);
	}

	@Override
	public MouseWheelListener[] getMouseWheelListeners() {
		return realComponent.getMouseWheelListeners();
	}

	@Override
	public void addInputMethodListener(InputMethodListener l) {
		realComponent.addInputMethodListener(l);
	}

	@Override
	public void removeInputMethodListener(InputMethodListener l) {
		realComponent.removeInputMethodListener(l);
	}

	@Override
	public InputMethodListener[] getInputMethodListeners() {
		return realComponent.getInputMethodListeners();
	}

	@Override
	public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
		return realComponent.getListeners(listenerType);
	}

	@Override
	public InputMethodRequests getInputMethodRequests() {
		return realComponent.getInputMethodRequests();
	}

	@Override
	public InputContext getInputContext() {
		return realComponent.getInputContext();
	}

	@Override
	@Deprecated
	public boolean handleEvent(Event evt) {
		return realComponent.handleEvent(evt);
	}

	@Override
	@Deprecated
	public boolean mouseDown(Event evt, int x, int y) {
		return realComponent.mouseDown(evt, x, y);
	}

	@Override
	@Deprecated
	public boolean mouseDrag(Event evt, int x, int y) {
		return realComponent.mouseDrag(evt, x, y);
	}

	@Override
	@Deprecated
	public boolean mouseUp(Event evt, int x, int y) {
		return realComponent.mouseUp(evt, x, y);
	}

	@Override
	@Deprecated
	public boolean mouseMove(Event evt, int x, int y) {
		return realComponent.mouseMove(evt, x, y);
	}

	@Override
	@Deprecated
	public boolean mouseEnter(Event evt, int x, int y) {
		return realComponent.mouseEnter(evt, x, y);
	}

	@Override
	@Deprecated
	public boolean mouseExit(Event evt, int x, int y) {
		return realComponent.mouseExit(evt, x, y);
	}

	@Override
	@Deprecated
	public boolean keyDown(Event evt, int key) {
		return realComponent.keyDown(evt, key);
	}

	@Override
	@Deprecated
	public boolean keyUp(Event evt, int key) {
		return realComponent.keyUp(evt, key);
	}

	@Override
	@Deprecated
	public boolean action(Event evt, Object what) {
		return realComponent.action(evt, what);
	}

	@Override
	public void addNotify() {
		realComponent.addNotify();
	}

	@Override
	public void removeNotify() {
		realComponent.removeNotify();
	}

	@Override
	@Deprecated
	public boolean gotFocus(Event evt, Object what) {
		return realComponent.gotFocus(evt, what);
	}

	@Override
	@Deprecated
	public boolean lostFocus(Event evt, Object what) {
		return realComponent.lostFocus(evt, what);
	}

	@Override
	@Deprecated
	public boolean isFocusTraversable() {
		return realComponent.isFocusTraversable();
	}

	@Override
	public boolean isFocusable() {
		return realComponent.isFocusable();
	}

	@Override
	public void setFocusable(boolean focusable) {
		realComponent.setFocusable(focusable);
	}

	@Override
	public void setFocusTraversalKeys(int id, Set<? extends AWTKeyStroke> keystrokes) {
		realComponent.setFocusTraversalKeys(id, keystrokes);
	}

	@Override
	public Set<AWTKeyStroke> getFocusTraversalKeys(int id) {
		return realComponent.getFocusTraversalKeys(id);
	}

	@Override
	public boolean areFocusTraversalKeysSet(int id) {
		return realComponent.areFocusTraversalKeysSet(id);
	}

	@Override
	public void setFocusTraversalKeysEnabled(boolean focusTraversalKeysEnabled) {
		realComponent.setFocusTraversalKeysEnabled(focusTraversalKeysEnabled);
	}

	@Override
	public boolean getFocusTraversalKeysEnabled() {
		return realComponent.getFocusTraversalKeysEnabled();
	}

	@Override
	public void requestFocus() {
		realComponent.requestFocus();
	}

	@Override
	public void requestFocus(FocusEvent.Cause cause) {
		realComponent.requestFocus(cause);
	}

	@Override
	public boolean requestFocusInWindow() {
		return realComponent.requestFocusInWindow();
	}

	@Override
	public boolean requestFocusInWindow(FocusEvent.Cause cause) {
		return realComponent.requestFocusInWindow(cause);
	}


	@Override
	public Container getFocusCycleRootAncestor() {
		return realComponent.getFocusCycleRootAncestor();
	}

	@Override
	public boolean isFocusCycleRoot(Container container) {
		return realComponent.isFocusCycleRoot(container);
	}

	@Override
	public void transferFocus() {
		realComponent.transferFocus();
	}

	@Override
	@Deprecated
	public void nextFocus() {
		realComponent.nextFocus();
	}

	@Override
	public void transferFocusBackward() {
		realComponent.transferFocusBackward();
	}

	@Override
	public void transferFocusUpCycle() {
		realComponent.transferFocusUpCycle();
	}

	@Override
	public boolean hasFocus() {
		return realComponent.hasFocus();
	}

	@Override
	public boolean isFocusOwner() {
		return realComponent.isFocusOwner();
	}

	@Override
	public void add(PopupMenu popup) {
		realComponent.add(popup);
	}

	@Override
	public void remove(MenuComponent popup) {
		realComponent.remove(popup);
	}

	@Override
	public String toString() {
		return realComponent.toString();
	}

	@Override
	public void list() {
		realComponent.list();
	}

	@Override
	public void list(PrintStream out) {
		realComponent.list(out);
	}

	@Override
	public void list(PrintStream out, int indent) {
		realComponent.list(out, indent);
	}

	@Override
	public void list(PrintWriter out) {
		realComponent.list(out);
	}

	@Override
	public void list(PrintWriter out, int indent) {
		realComponent.list(out, indent);
	}

	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		realComponent.addPropertyChangeListener(listener);
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		realComponent.removePropertyChangeListener(listener);
	}

	@Override
	public PropertyChangeListener[] getPropertyChangeListeners() {
		return realComponent.getPropertyChangeListeners();
	}

	@Override
	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		realComponent.addPropertyChangeListener(propertyName, listener);
	}

	@Override
	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		realComponent.removePropertyChangeListener(propertyName, listener);
	}

	@Override
	public PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
		return realComponent.getPropertyChangeListeners(propertyName);
	}


	@Override
	public void firePropertyChange(String propertyName, int oldValue, int newValue) {
		realComponent.firePropertyChange(propertyName, oldValue, newValue);
	}

	@Override
	public void firePropertyChange(String propertyName, byte oldValue, byte newValue) {
		realComponent.firePropertyChange(propertyName, oldValue, newValue);
	}

	@Override
	public void firePropertyChange(String propertyName, char oldValue, char newValue) {
		realComponent.firePropertyChange(propertyName, oldValue, newValue);
	}

	@Override
	public void firePropertyChange(String propertyName, short oldValue, short newValue) {
		realComponent.firePropertyChange(propertyName, oldValue, newValue);
	}

	@Override
	public void firePropertyChange(String propertyName, long oldValue, long newValue) {
		realComponent.firePropertyChange(propertyName, oldValue, newValue);
	}

	@Override
	public void firePropertyChange(String propertyName, float oldValue, float newValue) {
		realComponent.firePropertyChange(propertyName, oldValue, newValue);
	}

	@Override
	public void firePropertyChange(String propertyName, double oldValue, double newValue) {
		realComponent.firePropertyChange(propertyName, oldValue, newValue);
	}

	@Override
	public void setComponentOrientation(ComponentOrientation o) {
		realComponent.setComponentOrientation(o);
	}

	@Override
	public ComponentOrientation getComponentOrientation() {
		return realComponent.getComponentOrientation();
	}

	@Override
	public void applyComponentOrientation(ComponentOrientation orientation) {
		realComponent.applyComponentOrientation(orientation);
	}

	@Override
	public AccessibleContext getAccessibleContext() {
		return realComponent.getAccessibleContext();
	}

	@Override
	public void setMixingCutoutShape(Shape shape) {
		realComponent.setMixingCutoutShape(shape);
	}
}
