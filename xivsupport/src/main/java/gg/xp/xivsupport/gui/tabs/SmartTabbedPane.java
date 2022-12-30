package gg.xp.xivsupport.gui.tabs;

import gg.xp.xivsupport.gui.extra.TabDef;
import gg.xp.xivsupport.gui.util.GuiUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Special tabbed pane with three new features over a normal JTabbedPane:
 *
 * 1. Tab warning color - a tab can indicate that something needs attention and it will be tinted dark red
 * 2. Deferred tabs - a tab may defer instantiation of its actual component until the tab is selected
 * 3. Splitting to a new window - a tab may be split off onto a new window at the user's request
 */
public class SmartTabbedPane extends JTabbedPane implements TabAware {

	private static final Logger log = LoggerFactory.getLogger(SmartTabbedPane.class);
	private static final Color warningTabColor = new Color(62, 27, 27);

	public SmartTabbedPane() {
	}

	public SmartTabbedPane(int tabPlacement) {
		super(tabPlacement);
	}

	public SmartTabbedPane(int tabPlacement, int tabLayoutPolicy) {
		super(tabPlacement, tabLayoutPolicy);
	}

	{
		JPopupMenu jpm = new JPopupMenu();
		jpm.addPopupMenuListener(new PopupMenuListener() {
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				jpm.removeAll();
				// This is set by our mouse listener
				int index = lastRightClick;
				log.trace("Popup: index ({})", index);
				JMenuItem menuItem;
				// Determine whether to offer the split or the unsplit option for this tab
				if (isTabSplit(index)) {
					menuItem = new JMenuItem("Return to This Window");
					menuItem.addActionListener(l -> {
						SplitWindowComponent swc = (SplitWindowComponent) getComponentAt(index);
						setComponentAt(index, swc.actualComponent);
						swc.window.setVisible(false);
						swc.window.dispose();
					});
				}
				else {
					menuItem = new JMenuItem("Split to New Window");
					menuItem.addActionListener(l -> {
						splitToNewWindow(index);
					});
				}
				jpm.add(menuItem);
				jpm.revalidate();
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {

			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {

			}
		});
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				// If we right click on a tab, figure out which one, and show the popup menu for that tab
				if (e.getButton() == MouseEvent.BUTTON3) {
					int tabForCoordinate = getUI().tabForCoordinate(SmartTabbedPane.this, e.getX(), e.getY());
					log.trace("tabForCoordinate({})", tabForCoordinate);
					if (tabForCoordinate < 0) {
						return;
					}
					lastRightClick = tabForCoordinate;
					jpm.show(SmartTabbedPane.this, e.getX(), e.getY());
				}
			}
		});
	}

	public List<Component> getTabs() {
		int count = getTabCount();
		List<Component> out = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			out.add(getComponent(i));
		}
		return out;
	}

	/**
	 * Dummy "Component" which represents a deferred tab.
	 */
	private static final class DummyMarkerComponent extends Component {

		private final Supplier<Component> actualComponentFunction;

		private DummyMarkerComponent(Supplier<Component> actualComponentFunction) {
			this.actualComponentFunction = actualComponentFunction;
		}

		private Component getActualComponent() {
			return actualComponentFunction.get();
		}
	}

	/**
	 * Dummy "Component" which represents a tab that has been split off to a new window
	 */
	private static final class SplitWindowComponent extends Component {

		private final Component actualComponent;
		private final JFrame window;

		private SplitWindowComponent(Component actualComponent, JFrame window) {
			this.actualComponent = actualComponent;
			this.window = window;
		}
	}

	private void splitToNewWindow(int index) {
		log.info("splitToNewWindow({})", index);
		Component tabAt = getComponentAt(index);
		if (tabAt instanceof SplitWindowComponent) {
			// nothing to do
			return;
		}
		else if (tabAt instanceof DummyMarkerComponent dmc) {
			tabAt = dmc.getActualComponent();
		}
		String name = getTitleAt(index);
		JFrame newWindow = new JFrame(name);
		SplitWindowComponent swc = new SplitWindowComponent(tabAt, newWindow);
		setComponentAt(index, swc);
		newWindow.setContentPane((Container) tabAt);
		newWindow.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		Window parent = SwingUtilities.getWindowAncestor(this);
		newWindow.setLocationRelativeTo(parent);
		newWindow.setSize(parent.getSize());
		newWindow.setLocation(new Point(parent.getX() + 100, parent.getY() + 100));
		newWindow.setVisible(true);
		newWindow.revalidate();
		int goodIndex = findGoodTab();
		SwingUtilities.invokeLater(() -> {
			setSelectedIndex(goodIndex);
		});
	}

	private volatile int lastGoodIndex = -1;
	private volatile int lastRightClick;

	private boolean isTabSelectable(int index) {
		return !(getComponentAt(index) instanceof SplitWindowComponent);
	}

	private boolean isTabSplit(int index) {
		return getComponentAt(index) instanceof SplitWindowComponent;
	}

	private int findGoodTab() {
		int last = lastGoodIndex;
		if (last >= 0) {
			if (isTabSelectable(last)) {
				return last;
			}
		}
		int selected = getSelectedIndex();
		int startIndex = 0;
		int endIndex = getTabCount() - 1;
		for (int i = selected; i <= endIndex; i++) {
			if (isTabSelectable(i)) {
				return i;
			}
		}
		for (int i = selected - 1; i >= startIndex; i--) {
			if (isTabSelectable(i)) {
				return i;
			}
		}
		return 0;
	}

	@Override
	public void setSelectedIndex(int index) {
		log.trace("setSelectedIndex({})", index);
		Component tabAt = getComponentAt(index);
		if (tabAt instanceof SplitWindowComponent swc) {
			// TODO
			if (swc.window.isDisplayable()) {
				swc.window.requestFocus();
				int goodIndex = findGoodTab();
				super.setSelectedIndex(goodIndex);
				return;
//				SwingUtilities.invokeLater(() -> {
//					setSelectedIndex(goodIndex);
//				});
			}
			else {
				setComponentAt(index, swc.actualComponent);
				tabAt = getComponentAt(index);
			}
		}
		lastGoodIndex = super.getSelectedIndex();
		super.setSelectedIndex(index);
		if (tabAt instanceof DummyMarkerComponent dmc) {
			log.trace("Replacing tab at index {}", index);
			SwingUtilities.invokeLater(() -> {
				GuiUtil.displayWaitCursorWhile(this, () -> {
					setComponentAt(index, dmc.getActualComponent());
				});
			});
		}
	}

	private final Map<Object, Integer> registeredKeys = new HashMap<>();

	// TODO: doesn't work if tabs are removed or rearranged after the fact
	public void addTab(TabDef def) {
		addTab(def.getTabName(), def.getTabContents());
		int i = getTabCount() - 1;
		def.keys().forEach(k -> {
			registeredKeys.put(k, i);
		});
	}

	public void addTabLazy(TabDef def) {
		int i = addTabLazy(def.getTabName(), def::getTabContents);
		def.keys().forEach(k -> {
			registeredKeys.put(k, i);
		});
	}

	public void selectTabByKey(Object key) {
		Integer index = registeredKeys.get(key);
		if (index != null) {
			setSelectedIndex(index);
		}
		else {
			log.warn("No mapping for key: ({})", key);
		}
	}

	public int addTabLazy(String title, Supplier<Component> componentFunc) {
		addTab(title, new DummyMarkerComponent(componentFunc));
		int index = getTabCount() - 1;
		registeredKeys.put(title, index);
		return index;
	}

	@Override
	public void addTab(String title, Component component) {
		super.addTab(title, component);
		int index = getTabCount() - 1;
		registeredKeys.put(title, index);
	}

	@Override
	public void setSelectedComponent(Component c) {
		Component[] components = getComponents();
		for (Component component : components) {
			if (component instanceof SplitWindowComponent swc && swc.actualComponent == c) {
				super.setSelectedComponent(swc);
				return;
			}
		}
		super.setSelectedComponent(c);
	}

	@Override
	public boolean hasWarning() {
		return Arrays.stream(getComponents()).anyMatch(tab -> (tab instanceof TabAware aware && aware.hasWarning()));
	}

	public void recheckTabs() {
		SwingUtilities.invokeLater(this::repaint);
		notifyParents();
	}

	@Override
	public Color getBackgroundAt(int index) {
		Component comp = getComponentAt(index);
		if (comp instanceof TabAware tabAware && tabAware.hasWarning()) {
			return warningTabColor;
		}
		return super.getBackgroundAt(index);
	}
}
