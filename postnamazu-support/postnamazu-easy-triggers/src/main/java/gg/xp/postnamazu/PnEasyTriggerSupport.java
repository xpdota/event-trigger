package gg.xp.postnamazu;

import gg.xp.postnamazu.gui.PnCommandEditor;
import gg.xp.reevent.events.Event;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.events.triggers.easytriggers.EasyTriggers;
import gg.xp.xivsupport.events.triggers.easytriggers.model.ActionDescription;
import gg.xp.xivsupport.groovy.GroovyManager;
import gg.xp.xivsupport.groovy.GroovyScriptProcessor;
import org.picocontainer.PicoContainer;

@ScanMe
public class PnEasyTriggerSupport {

	public PnEasyTriggerSupport(EasyTriggers et, PicoContainer container, GroovyScriptProcessor gsp, GroovyManager gm) {
		et.registerActionType(new ActionDescription<>(
				PnCommandAction.class,
				Event.class,
				"PostNamazu Game Command",
				() -> new PnCommandAction(gsp, gm),
				(action, trigger) -> new PnCommandEditor(action, container)
		));
	}

}
