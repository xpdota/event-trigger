package gg.xp.telestosupport.easytriggers;

import gg.xp.reevent.events.Event;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.telestosupport.easytriggers.gui.TelestoCircleDoodleEditor;
import gg.xp.telestosupport.easytriggers.gui.TelestoLineDoodleEditor;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.triggers.easytriggers.EasyTriggers;
import gg.xp.xivsupport.events.triggers.easytriggers.model.ActionDescription;
import gg.xp.xivsupport.groovy.GroovyManager;
import org.picocontainer.PicoContainer;


@ScanMe
public class TelestoEasyTriggersAddons {

	public TelestoEasyTriggersAddons(EasyTriggers easyTriggers, PicoContainer container, XivState state, GroovyManager mgr) {
		easyTriggers.registerActionType(new ActionDescription<>(
				TelestoCircleDoodleAction.class,
				Event.class,
				"Telesto Circle Doodle",
				() -> new TelestoCircleDoodleAction(state, mgr),
				(action, trigger) -> new TelestoCircleDoodleEditor(action, container)
		));
		easyTriggers.registerActionType(new ActionDescription<>(
				TelestoLineDoodleAction.class,
				Event.class,
				"Telesto Line Doodle",
				() -> new TelestoLineDoodleAction(state, mgr),
				(action, trigger) -> new TelestoLineDoodleEditor(action, container)
		));
	}


}
