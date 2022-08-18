package gg.xp.xivsupport.custompartyoverlay.castbar;

import gg.xp.xivsupport.custompartyoverlay.AlwaysRepaintingPartyListComponent;
import gg.xp.xivsupport.custompartyoverlay.BasePartyListComponent;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.CastTracker;
import gg.xp.xivsupport.events.triggers.jobs.gui.CastBarComponent;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class CastBarPartyComponent extends AlwaysRepaintingPartyListComponent {

	private final ActiveCastRepository acr;
	private final CastBarComponent bar;
	private final CastBarComponentConfig config;

	public CastBarPartyComponent(ActiveCastRepository acr, CastBarComponentConfig config) {
		this.acr = acr;
		bar = new CastBarComponent();
		this.config = config;
		config.addAndRunListener(this::applySettings);
	}

	private void applySettings() {
		bar.setBackground(config.getBackgroundColor().get());
		bar.setSuccessColor(config.getSuccessColor().get());
		bar.setInProgressColor(config.getInProgressColor().get());
		bar.setInterruptedColor(config.getInterruptedColor().get());
		bar.setUnknownColor(config.getUnknownColor().get());
		bar.setTextColor(config.getTextColor().get());
		bar.repaint();

	}


	@Override
	protected Component makeComponent() {
		return bar;
	}

	@Override
	protected void reformatComponent(@NotNull XivPlayerCharacter xpc) {
		@Nullable CastTracker castFor = acr.getCastFor(xpc);
		if (castFor != null) {
			if (castFor.getCast().getEstimatedTimeSinceExpiry().toMillis() > 3_000) {
				castFor = null;
			}
		}
		bar.setData(castFor);
	}
}
