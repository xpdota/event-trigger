package gg.xp.telestosupport.easytriggers;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.telestosupport.easytriggers.gui.TelestoCircleDoodleEditor;
import gg.xp.telestosupport.easytriggers.gui.TelestoCommandEditor;
import gg.xp.telestosupport.easytriggers.gui.TelestoImageDoodleEditor;
import gg.xp.telestosupport.easytriggers.gui.TelestoLineDoodleEditor;
import gg.xp.telestosupport.easytriggers.gui.TelestoTextDoodleEditor;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.triggers.easytriggers.EasyTriggers;
import gg.xp.xivsupport.events.triggers.easytriggers.model.ActionDescription;
import gg.xp.xivsupport.groovy.GroovyManager;
import gg.xp.xivsupport.groovy.GroovyScriptProcessor;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;
import org.picocontainer.PicoContainer;


@ScanMe
public class TelestoEasyTriggersAddons {

	private final BooleanSetting doodleAddonsSetting;

	public TelestoEasyTriggersAddons(EasyTriggers easyTriggers, PicoContainer container, XivState state, GroovyManager mgr, PersistenceProvider pers) {
		doodleAddonsSetting = new BooleanSetting(pers, "telesto-support.doodle-support.et-addons.enable", false);
		easyTriggers.registerActionType(new ActionDescription<>(
				TelestoCircleDoodleAction.class,
				Event.class,
				"Telesto Circle Doodle",
				() -> new TelestoCircleDoodleAction(state, mgr),
				(action, trigger) -> new TelestoCircleDoodleEditor(action, container),
				doodleAddonsSetting::get
		));
		easyTriggers.registerActionType(new ActionDescription<>(
				TelestoLineDoodleAction.class,
				Event.class,
				"Telesto Line Doodle",
				() -> new TelestoLineDoodleAction(state, mgr),
				(action, trigger) -> new TelestoLineDoodleEditor(action, container),
				doodleAddonsSetting::get
		));
		easyTriggers.registerActionType(new ActionDescription<>(
				TelestoTextDoodleAction.class,
				Event.class,
				"Telesto Text Doodle",
				() -> new TelestoTextDoodleAction(state, mgr),
				(action, trigger) -> new TelestoTextDoodleEditor(action, container),
				doodleAddonsSetting::get
		));
		// TODO: rectangle
		easyTriggers.registerActionType(new ActionDescription<>(
				TelestoImageDoodleAction.class,
				Event.class,
				"Telesto Image Doodle",
				() -> new TelestoImageDoodleAction(state, mgr),
				(action, trigger) -> new TelestoImageDoodleEditor(action, container),
				doodleAddonsSetting::get
		));
		easyTriggers.registerActionType(new ActionDescription<>(
				TelestoCommandAction.class,
				Event.class,
				"Telesto In-Game Command",
				() -> new TelestoCommandAction(container.getComponent(GroovyScriptProcessor.class), mgr),
				(action, trigger) -> new TelestoCommandEditor(action, container),
				() -> true
		));
	}


	public BooleanSetting getEnableAddons() {
		return doodleAddonsSetting;
	}
}
