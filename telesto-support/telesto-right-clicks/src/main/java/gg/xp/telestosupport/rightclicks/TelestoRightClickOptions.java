package gg.xp.telestosupport.rightclicks;

import gg.xp.reevent.events.EventMaster;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.telestosupport.doodle.CircleDoodleSpec;
import gg.xp.telestosupport.doodle.CreateDoodleRequest;
import gg.xp.telestosupport.doodle.DoodleSpec;
import gg.xp.telestosupport.doodle.EntityDoodleLocation;
import gg.xp.telestosupport.doodle.LineDoodleSpec;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.triggers.marks.adv.MarkerSign;
import gg.xp.xivsupport.events.triggers.marks.adv.SpecificAutoMarkRequest;
import gg.xp.xivsupport.gui.tables.CustomRightClickOption;
import gg.xp.xivsupport.gui.tables.RightClickOptionRepo;
import gg.xp.xivsupport.models.XivEntity;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;

import java.awt.*;

@ScanMe
public class TelestoRightClickOptions {

	private final BooleanSetting enableExtraOptions;

	public TelestoRightClickOptions(RightClickOptionRepo repo, EventMaster master, PersistenceProvider pers, XivState state) {
		this.enableExtraOptions = new BooleanSetting(pers, "telesto-support.right-click-options.enabled", false);
		repo.addOption(CustomRightClickOption.forRow("Mark with Circle",
				XivPlayerCharacter.class,
				xpc -> master.pushEvent(new SpecificAutoMarkRequest(xpc, MarkerSign.CIRCLE)),
				ignored -> enableExtraOptions.get()));
		repo.addOption(CustomRightClickOption.forRow("Draw Circle on Entity",
				XivEntity.class,
				xe -> {
					DoodleSpec spec = new CircleDoodleSpec(new EntityDoodleLocation(xe), 50.0d, true);
					spec.color = new Color(255, 0, 255, 192);
					master.pushEvent(new CreateDoodleRequest(spec));
				},
				ignored -> enableExtraOptions.get()));
		repo.addOption(CustomRightClickOption.forRow("Draw Line to Entity",
				XivEntity.class,
				xe -> {
					DoodleSpec spec = new LineDoodleSpec(new EntityDoodleLocation(state.getPlayer()), new EntityDoodleLocation(xe), 50.0d);
					spec.color = new Color(255, 0, 255, 192);
					master.pushEvent(new CreateDoodleRequest(spec));
				},
				ignored -> enableExtraOptions.get()));
	}

	public BooleanSetting getEnableExtraOptions() {
		return enableExtraOptions;
	}
}
