package gg.xp.xivsupport.events.triggers.marks.adv;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.services.ServiceSelector;
import gg.xp.xivsupport.events.triggers.marks.AutoMarkHandler;
import gg.xp.xivsupport.persistence.PersistenceProvider;

@ScanMe
public class AutoMarkServiceSelector extends ServiceSelector {

	private final AutoMarkHandler handler;

	public AutoMarkServiceSelector(PersistenceProvider pers, AutoMarkHandler handler) {
		super(pers, "automarks.service-selector");
		this.handler = handler;
	}

	@Override
	protected String name() {
		return "AutoMarks";
	}
}
