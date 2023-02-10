package gg.xp.telestosupport.doodle;

import gg.xp.telestosupport.doodle.img.HAlign;
import gg.xp.telestosupport.doodle.img.VAlign;

import java.io.Serial;

public class ImageIconDoodleSpec extends DoodleSpec {
	@Serial
	private static final long serialVersionUID = -931021673974918666L;
	public final DoodleLocation position;
	public final long icon;
	// TODO: width/height specs
	public final HAlign halign;
	public final VAlign valign;

	public ImageIconDoodleSpec(DoodleLocation position, long icon, HAlign halign, VAlign valign) {
		this.position = position;
		this.icon = icon;
		this.halign = halign;
		this.valign = valign;
	}

	@Override
	public String type() {
		return "image";
	}
}
