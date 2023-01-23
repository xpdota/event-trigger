package gg.xp.telestosupport.doodle;

import java.io.Serial;

public class LineDoodleSpec extends DoodleSpec {
	@Serial
	private static final long serialVersionUID = -3779679731096507451L;
	public final DoodleLocation start;
	public final DoodleLocation end;
	public final double thickness;

	public LineDoodleSpec(DoodleLocation start, DoodleLocation end, double thickness) {
		this.start = start;
		this.end = end;
		this.thickness = thickness;
	}

	@Override
	public String type() {
		return "line";
	}
}
