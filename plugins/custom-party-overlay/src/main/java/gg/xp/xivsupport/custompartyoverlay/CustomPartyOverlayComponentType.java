package gg.xp.xivsupport.custompartyoverlay;

import gg.xp.xivsupport.custompartyoverlay.buffs.CustomBuffsBarComponentGui;
import gg.xp.xivsupport.custompartyoverlay.buffs.NormalBuffsBarComponentGui;
import gg.xp.xivsupport.custompartyoverlay.buffs.CustomBuffsBarPartyComponent;
import gg.xp.xivsupport.custompartyoverlay.buffs.NormalBuffsBarPartyComponent;
import gg.xp.xivsupport.custompartyoverlay.castbar.CastBarComponentGui;
import gg.xp.xivsupport.custompartyoverlay.castbar.CastBarPartyComponent;
import gg.xp.xivsupport.custompartyoverlay.hpbar.HpBarComponent;
import gg.xp.xivsupport.custompartyoverlay.hpbar.HpBarComponentGui;
import gg.xp.xivsupport.custompartyoverlay.mpbar.MpBarComponent;
import gg.xp.xivsupport.custompartyoverlay.mpbar.MpBarComponentGui;
import gg.xp.xivsupport.custompartyoverlay.name.NameComponent;
import gg.xp.xivsupport.custompartyoverlay.name.NameComponentGui;
import gg.xp.xivsupport.gui.util.HasFriendlyName;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public enum CustomPartyOverlayComponentType implements HasFriendlyName {
	NOTHING("Dummy Component", DoNothingComponent.class) {
		@Override
		public boolean shouldBePresent() {
			return false;
		}
	},
	NAME("Name", NameComponent.class, NameComponentGui.class),
	JOB("Job", JobComponent.class),
	HP("HP/Shield Bar", HpBarComponent.class, HpBarComponentGui.class),
	BUFFS_WITH_TIMERS("Buffs", NormalBuffsBarPartyComponent.class, NormalBuffsBarComponentGui.class),
	CAST_BAR("Cast Bar", CastBarPartyComponent.class, CastBarComponentGui.class),
	MP_BAR("MP Bar", MpBarComponent.class, MpBarComponentGui.class),
	CUSTOM_BUFFS("Custom Buffs", CustomBuffsBarPartyComponent.class, CustomBuffsBarComponentGui.class);

	private final String friendlyName;
	private final Class<? extends RefreshablePartyListComponent> componentClass;
	private final @Nullable Class<? extends Component> configGuiClass;

	CustomPartyOverlayComponentType(String friendlyName, Class<? extends RefreshablePartyListComponent> componentClass) {
		this(friendlyName, componentClass, null);
	}

	CustomPartyOverlayComponentType(String friendlyName, Class<? extends RefreshablePartyListComponent> componentClass, @Nullable Class<? extends Component> configGuiClass) {
		this.friendlyName = friendlyName;
		this.componentClass = componentClass;
		this.configGuiClass = configGuiClass;
	}

	@Override
	public String getFriendlyName() {
		return friendlyName;
	}

	public Class<? extends RefreshablePartyListComponent> getComponentClass() {
		return componentClass;
	}

	public @Nullable Class<? extends Component> getConfigGuiClass() {
		return configGuiClass;
	}

	public boolean shouldBePresent() {
		return true;
	}
}
