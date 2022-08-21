package gg.xp.xivsupport.custompartyoverlay;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.custompartyoverlay.name.NameComponent;
import gg.xp.xivsupport.custompartyoverlay.name.NameComponentConfig;
import gg.xp.xivsupport.events.actionresolution.SequenceIdTracker;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.gui.tables.StandardColumns;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoContainer;

@ScanMe
public class CustomPartyOverlayComponentFactory {

	private final MutablePicoContainer container;

	public CustomPartyOverlayComponentFactory(
			MutablePicoContainer container
	) {
		this.container = container.makeChildContainer();
	}

	public @Nullable RefreshablePartyListComponent makeComponent(CustomOverlayComponentSpec spec) {
		if (!spec.enabled) {
			return null;
		}
		CustomPartyOverlayComponentType type = spec.componentType;
		Class<? extends RefreshablePartyListComponent> component = type.getComponentClass();
		container.removeComponent(component);
		container.addComponent(component);
		return container.getComponent(component);
	}
}
