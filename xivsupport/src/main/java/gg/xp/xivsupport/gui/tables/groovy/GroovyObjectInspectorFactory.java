package gg.xp.xivsupport.gui.tables.groovy;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.gui.tables.RightClickOptionRepo;

import javax.swing.*;

@ScanMe
public class GroovyObjectInspectorFactory {

	private final RightClickOptionRepo repo;

	public GroovyObjectInspectorFactory(RightClickOptionRepo repo) {
		this.repo = repo;
	}

	public GroovyObjectInspector createInspector(Object obj) {
		return new GroovyObjectInspector(obj, repo);
	}

	public void openInspector(Object obj) {
		if (obj == null) {
			return;
		}
		JFrame frame = new JFrame("Inspector: " + obj);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.add(createInspector(obj));
		frame.pack();
		frame.setSize(800, 600);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
}
