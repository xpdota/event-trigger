package gg.xp.xivsupport.gui.tabs;

import gg.xp.xivsupport.groovy.GroovyScriptManager;
import gg.xp.xivsupport.gui.extra.TabDef;
import gg.xp.xivsupport.gui.groovy.GroovyPanel;
import gg.xp.xivsupport.gui.groovy.GroovyScriptHolder;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class GroovyTab extends JPanel {

	private final SmartTabbedPane tabPane;
	private final GroovyScriptManager mgr;
	private final Map<GroovyScriptHolder, GroovyPanel> componentCache = new ConcurrentHashMap<>();

	public GroovyTab(GroovyScriptManager mgr) {
		this.mgr = mgr;
		// Idea: have an "Add New" button just be a tab
		tabPane = new SmartTabbedPane(JTabbedPane.LEFT);
		setLayout(new BorderLayout(0, 0));
		mgr.getScripts().forEach(this::addTabLazy);
		add(tabPane, BorderLayout.CENTER);
		mgr.addListener(this::resetAllTabs);
	}

	private void addTab(GroovyScriptHolder holder) {
		// getTitleAt is overridden
		GroovyPanel component = new GroovyPanel(mgr, this, holder);
		componentCache.put(holder, component);
		tabPane.addTab("Ignored", component);
	}

	private void addTabLazy(GroovyScriptHolder holder) {
		tabPane.addTabLazy(new TabDef() {
			@Override
			public String getTabName() {
				return holder.getScriptName();
			}

			@Override
			public Component getTabContents() {
				GroovyPanel component = new GroovyPanel(mgr, GroovyTab.this, holder);
				componentCache.put(holder, component);
				return component;
			}

			@Override
			public List<Object> keys() {
				File file = holder.getFile();
				if (file == null) {
					return List.of(this, getTabName(), holder);
				}
				return List.of(this, getTabName(), holder, file);
			}
		});
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
			newScripts.forEach(this::addTabLazy);
			if (previouslySelectedScript != null && newScripts.contains(previouslySelectedScript)) {
				tabPane.selectTabByKey(previouslySelectedScript);
			}
			else if ((newEquivalentHolder = newScripts.stream().filter(s -> Objects.equals(s.getFile(), previouslySelectedFile)).findFirst().orElse(null)) != null) {
				tabPane.selectTabByKey(newEquivalentHolder.getFile());
			}
			else {
				tabPane.setSelectedIndex(Math.max(0, Math.max(index, tabPane.getTabCount() - 1)));
			}
		});
	}

	public void selectScript(GroovyScriptHolder newScript) {
		SwingUtilities.invokeLater(() -> {
			tabPane.selectTabByKey(newScript);
		});
	}
}
