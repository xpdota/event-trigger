package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.*;
import gg.xp.xivsupport.gui.library.ActionTableFactory;
import gg.xp.xivsupport.gui.library.StatusTable;
import gg.xp.xivsupport.gui.library.ZonesTable;
import gg.xp.xivsupport.gui.tables.filters.TextFieldWithValidation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

@ScanMe
public class IdPickerFactory {
	private static final Logger log = LoggerFactory.getLogger(IdPickerFactory.class);

	private final ActionTableFactory actionTableFactory;

	public IdPickerFactory(ActionTableFactory actionTableFactory) {
		this.actionTableFactory = actionTableFactory;
	}

	public <X> Component pickerFor(Class<X> clazz, boolean required, Supplier<Long> getter, Consumer<Long> setter) {
		if (clazz.equals(ActionInfo.class)) {
			return new IdPicker<>(required, getter, setter, ActionLibrary::forId, ActionInfo::actionid, ActionInfo::name, actionTableFactory::pickItem, ActionInfo::getIcon);
		}
		else if (clazz.equals(StatusEffectInfo.class)) {
			return new IdPicker<>(required, getter, setter, StatusEffectLibrary::forId, StatusEffectInfo::statusEffectId, StatusEffectInfo::name, StatusTable::pickItem, statusEffectInfo -> statusEffectInfo.getIcon(0));
		}
		else if (clazz.equals(ZoneInfo.class)) {
			return new IdPicker<>(required, getter, setter, id -> ZoneLibrary.infoForZoneOrUnknown(id.intValue()), zi -> (long) zi.id(), ZoneInfo::getCapitalizedName, ZonesTable::pickItem, zone -> null);
		}
		else {
			log.error("No picker for {}, falling back to basic text field with validation", clazz);
			return new TextFieldWithValidation<>(Long::parseLong, setter, () -> String.valueOf(getter.get()));
		}
	}

}
