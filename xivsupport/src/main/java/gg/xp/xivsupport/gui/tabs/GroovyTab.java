package gg.xp.xivsupport.gui.tabs;

import gg.xp.xivsupport.gui.groovy.GroovyManager;
import gg.xp.xivsupport.gui.groovy.GroovyPanel;
import gg.xp.xivsupport.gui.groovy.GroovyScriptHolder;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;

public class GroovyTab extends JPanel {

	private final JTabbedPane tabPane;

	public GroovyTab(GroovyManager mgr) {
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
	}

	private void addTab(GroovyScriptHolder holder) {
		// getTitleAt is overridden
		tabPane.addTab("Ignored", new GroovyPanel(holder));
	}

}
