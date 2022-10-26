package gg.xp.xivsupport.custompartyoverlay;

import gg.xp.reevent.scan.ScanMe;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.MutablePicoContainer;

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
