package gg.xp.xivsupport.custompartyoverlay;

import gg.xp.xivdata.data.HasIconURL;
import gg.xp.xivdata.data.StatusEffectLibrary;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.gui.tables.renderers.ComponentListRenderer;
import gg.xp.xivsupport.gui.tables.renderers.IconTextRenderer;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class BuffsComponent extends BasePartyListComponent{
	private final ComponentListRenderer listRenderer;
	private final StatusEffectRepository buffRepo;

	public BuffsComponent(StatusEffectRepository buffRepo) {
		this.buffRepo = buffRepo;
		listRenderer = new ComponentListRenderer(0);
	}

	@Override
	protected Component makeComponent() {
		return listRenderer;
	}

	@Override
	protected void reformatComponent(@NotNull XivPlayerCharacter xpc) {
		List<BuffApplied> buffs = buffRepo.sortedStatusesOnTarget(xpc);
		List<Component> components = new ArrayList<>();
		for (BuffApplied buff : buffs) {
			HasIconURL icon = StatusEffectLibrary.iconForId(buff.getBuff().getId(), buff.getStacks());
			Component component = IconTextRenderer.getIconOnly(icon);
			components.add(component);
		}
		listRenderer.setComponents(components);

	}
}
