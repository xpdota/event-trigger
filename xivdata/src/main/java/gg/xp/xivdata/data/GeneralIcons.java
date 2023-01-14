package gg.xp.xivdata.data;

import java.net.URL;

public final class GeneralIcons {
	private GeneralIcons() {
	}

	private static URL iconById(int id) {
		return StatusEffectIcon.class.getResource(String.format("/xiv/icon/%06d_hr1.png", id));
	}

	private static final URL _DAMAGE_PHYS = iconById(60011);
	private static final URL _DAMAGE_MAGIC = iconById(60012);
	private static final URL _DAMAGE_OTHER = iconById(60013);

	public static final HasIconURL DAMAGE_PHYS = () -> _DAMAGE_PHYS;
	public static final HasIconURL DAMAGE_MAGIC = () -> _DAMAGE_MAGIC;
	public static final HasIconURL DAMAGE_OTHER = () -> _DAMAGE_OTHER;
}
