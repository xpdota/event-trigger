package gg.xp.telestosupport.doodle;

import java.io.Serial;

public class ScreenPositionDoodleLocation extends DoodleLocation {

	@Serial
	private static final long serialVersionUID = 6093386088903985937L;
	public final int x;
	public final int y;

	public ScreenPositionDoodleLocation(int x, int y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public String coordsType() {
		return "screen";
	}
}
