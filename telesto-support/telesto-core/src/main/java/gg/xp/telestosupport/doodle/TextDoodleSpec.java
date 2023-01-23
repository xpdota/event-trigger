package gg.xp.telestosupport.doodle;

import java.io.Serial;

public class TextDoodleSpec extends DoodleSpec {
	@Serial
	private static final long serialVersionUID = 3263754140212173050L;
	public final DoodleLocation position;
	public final int size;
	public final String text;

	public TextDoodleSpec(DoodleLocation position, int size, String text) {
		this.position = position;
		this.size = size;
		this.text = text;
	}

	@Override
	public String type() {
		return "text";
	}
}
