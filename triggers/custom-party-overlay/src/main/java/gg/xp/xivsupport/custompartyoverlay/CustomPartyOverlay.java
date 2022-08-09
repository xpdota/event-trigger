package gg.xp.xivsupport.custompartyoverlay;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.gui.overlay.OverlayConfig;
import gg.xp.xivsupport.gui.overlay.XivOverlay;
import gg.xp.xivsupport.persistence.PersistenceProvider;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

@ScanMe
public class CustomPartyOverlay extends XivOverlay {


	private final XivState state;

	private final List<CustomOverlayComponentSpec> components;
	private int yOffset;


	public CustomPartyOverlay(OverlayConfig oc, PersistenceProvider persistence, XivState state) {
		super("Custom Party Overlay", "custom-party-overlay", oc, persistence);
		this.state = state;
		CustomOverlayComponentSpec comp = new CustomOverlayComponentSpec();
		components = Collections.singletonList(comp);
	}

	private void placeComponents() {
		JPanel panel = getPanel();
		panel.removeAll();
		for (int i = 0; i < 8; i++) {
			int offset = i * yOffset;
			for (CustomOverlayComponentSpec component : components) {

			}
		}
	}


}
