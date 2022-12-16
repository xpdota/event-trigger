package gg.xp.xivsupport.custompartyoverlay.buffs;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.persistence.settings.LongListSetting;

import java.util.Arrays;

@ScanMe
public class CustomBuffsBarConfig extends BuffsBarConfig {

	private long[] sorted = new long[0];
	private final LongListSetting buffs;

	public CustomBuffsBarConfig(PersistenceProvider pers) {
		super(pers, "custom-party-overlay.custom-buffs.");
		buffs = new LongListSetting(pers, "custom-party-overlay.custom-buffs.list-of-buffs", new long[]{});
		registerListener(buffs);
		buffs.addAndRunListener(this::getAndSortBuffs);
	}

	private void getAndSortBuffs() {
		sorted = buffs.get()
				.stream()
				.sorted()
				.mapToLong(l -> l)
				.toArray();
	}

	public boolean isBuffAllowed(long id) {
		// TODO: this could be faster
		return Arrays.binarySearch(sorted, id) >= 0;
	}

	public LongListSetting getBuffs() {
		return buffs;
	}


}
