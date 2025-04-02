package gg.xp.util;

public final class MathUtils {
	private MathUtils() {
	}

	public static boolean closeTo(double first, double second, double error) {
		return Math.abs(first - second) < error;
	}

	public static boolean closeTo(float first, float second, float error) {
		return Math.abs(first - second) < error;
	}
}
