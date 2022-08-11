package gg.xp.xivsupport.custompartyoverlay;

import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.CastTracker;
import gg.xp.xivsupport.events.triggers.jobs.gui.CastBarComponent;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class CastBarPartyComponent extends BasePartyListComponent {

	private final ActiveCastRepository acr;
	private final CastBarComponent bar;

	public CastBarPartyComponent(ActiveCastRepository acr) {
		this.acr = acr;
		bar = new CastBarComponent();
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
