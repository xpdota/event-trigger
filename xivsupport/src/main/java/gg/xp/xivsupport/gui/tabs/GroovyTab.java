package gg.xp.xivsupport.gui.tabs;

import gg.xp.xivsupport.gui.groovy.GroovyManager;
import gg.xp.xivsupport.gui.groovy.GroovyPanel;
import gg.xp.xivsupport.gui.groovy.GroovyScriptHolder;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GroovyTab extends JPanel {

	private final JTabbedPane tabPane;
	private final GroovyManager mgr;
	private final Map<GroovyScriptHolder, GroovyPanel> componentCache = new HashMap<>();

	public GroovyTab(GroovyManager mgr) {
		this.mgr = mgr;
		// Idea: have an "Add New" button just be a tab
		tabPane = new JTabbedPane(JTabbedPane.LEFT) {
			//
			@Override
			public String getTitleAt(int index) {
				Component comp = getComponentAt(index);
				if (comp instanceof GroovyPanel gp) {
					return StringUtils.abbreviate(gp.getName(), 30);
				}
				else {
					return super.getTitleAt(index);
				}
			}

		};
		setLayout(new BorderLayout(0, 0));
		mgr.getScripts().forEach(this::addTab);
		add(tabPane, BorderLayout.CENTER);
		mgr.addListener(this::resetAllTabs);
	}

	private void addTab(GroovyScriptHolder holder) {
		// getTitleAt is overridden
		GroovyPanel component = new GroovyPanel(mgr, this, holder);
		componentCache.put(holder, component);
		tabPane.addTab("Ignored", component);
	}

	private void clear() {
		tabPane.removeAll();
		componentCache.clear();
	}

	private @Nullable GroovyPanel getTabForScript(GroovyScriptHolder holder) {
		return componentCache.get(holder);
	}

	private void resetAllTabs() {
		GroovyPanel selectedTab = (GroovyPanel) tabPane.getSelectedComponent();
		GroovyScriptHolder previouslySelectedScript = selectedTab == null ? null : selectedTab.getScript();
		File previouslySelectedFile = previouslySelectedScript == null ? null : previouslySelectedScript.getFile();
		int index = tabPane.getSelectedIndex();
		List<GroovyScriptHolder> newScripts = mgr.getScripts();
		SwingUtilities.invokeLater(() -> {
			GroovyScriptHolder newEquivalentHolder;
			clear();
			newScripts.forEach(this::addTab);
			if (previouslySelectedScript != null && newScripts.contains(previouslySelectedScript)) {
				GroovyPanel tab = getTabForScript(previouslySelectedScript);
				tabPane.setSelectedComponent(tab);
			}
			else if ((newEquivalentHolder = newScripts.stream().filter(s -> Objects.equals(s.getFile(), previouslySelectedFile)).findFirst().orElse(null)) != null) {
				tabPane.setSelectedComponent(getTabForScript(newEquivalentHolder));
			}
			else {
				tabPane.setSelectedIndex(Math.max(0, Math.max(index, tabPane.getTabCount() - 1)));
			}
		});
	}

	public void selectScript(GroovyScriptHolder newScript) {
		SwingUtilities.invokeLater(() -> {
			tabPane.setSelectedComponent(getTabForScript(newScript));
		});
	}
}
