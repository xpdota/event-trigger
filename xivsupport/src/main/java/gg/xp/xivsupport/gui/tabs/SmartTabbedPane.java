package gg.xp.xivsupport.gui.tabs;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SmartTabbedPane extends JTabbedPane implements TabAware {

	private static final Color warningTabColor = new Color(62, 27, 27);

	public SmartTabbedPane() {
	}

	public SmartTabbedPane(int tabPlacement) {
		super(tabPlacement);
	}

	public SmartTabbedPane(int tabPlacement, int tabLayoutPolicy) {
		super(tabPlacement, tabLayoutPolicy);
	}

	public List<Component> getTabs() {
		int count = getTabCount();
		List<Component> out = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			out.add(getComponent(i));
		}
		return out;
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
