package gg.xp.xivsupport.overlays.castbar;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.CastTracker;
import gg.xp.xivsupport.events.triggers.jobs.gui.CastBarComponent;
import gg.xp.xivsupport.gui.overlay.OverlayConfig;
import gg.xp.xivsupport.gui.overlay.RefreshLoop;
import gg.xp.xivsupport.gui.overlay.XivOverlay;
import gg.xp.xivsupport.gui.tables.renderers.ComponentListRenderer;
import gg.xp.xivsupport.gui.tables.renderers.IconTextRenderer;
import gg.xp.xivsupport.persistence.PersistenceProvider;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;

@ScanMe
public class PersonalCastBarOverlay extends XivOverlay {
	private final ComponentListRenderer iconRenderer;
	private final CastBarComponent castBar;
	private final ActiveCastRepository acr;
	private final XivState state;

	public PersonalCastBarOverlay(OverlayConfig oc, PersistenceProvider persistence, ActiveCastRepository acr, XivState state) {
		super("Cast Bar", "overlays.personal-cast-bar", oc, persistence);
		this.acr = acr;
		this.state = state;
		iconRenderer = new ComponentListRenderer(0);
		iconRenderer.setPreferredSize(new Dimension(20, 20));
		castBar = new CastBarComponent();
		castBar.setPreferredSize(new Dimension(100, 20));
		redoLayout();
		RefreshLoop<PersonalCastBarOverlay> refresher = new RefreshLoop<>("CastBarOverlay", this, PersonalCastBarOverlay::refresh, dt -> dt.calculateScaledFrameTime(50));
		refresher.start();
	}

	private void redoLayout() {
		JPanel panel = getPanel();
		panel.removeAll();
		panel.setLayout(new BorderLayout());
		panel.add(iconRenderer, BorderLayout.WEST);
		panel.add(castBar, BorderLayout.CENTER);
		panel.revalidate();
		repackSize();
	}


	private void refresh() {
		CastTracker tracker = acr.getCastFor(state.getPlayer());
		if (tracker == null || tracker.getEstimatedTimeSinceExpiry().toMillis() > 5_000) {
			iconRenderer.setComponents(Collections.emptyList());
			castBar.setData(null);
		}
		else {
			long aid = tracker.getCast().getAbility().getId();
			iconRenderer.setComponents(Collections.singletonList(IconTextRenderer.getIconOnly(ActionLibrary.iconForId(aid))));
			castBar.setData(tracker);
		}
		SwingUtilities.invokeLater(() -> getPanel().repaint());
	}

}
