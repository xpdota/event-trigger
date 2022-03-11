package gg.xp.xivsupport.events.triggers.easytriggers.model;

import java.util.function.BiPredicate;

@SuppressWarnings("FloatingPointEquality")
public enum NumericOperator {
	EQ("Equal To", "=", (a, b) -> a.longValue() == b.longValue(), (a, b) -> a.doubleValue() == b.doubleValue()),
	NE("Not Equal To", "!=", (a, b) -> a.longValue() != b.longValue(), (a, b) -> a.doubleValue() != b.doubleValue()),
	GREATER_THAN("Greater Than", ">", (a, b) -> a > b, (a, b) -> a > b),
	GREATER_THAN_OR_EQUAL("Greater Than or Equal To", ">=", (a, b) -> a >= b, (a, b) -> a >= b),
	LESS_THAN("Less Than", "<", (a, b) -> a < b, (a, b) -> a < b),
	LESS_THAN_OR_EQUAL("Less Than or Equal To", "<=", (a, b) -> a <= b, (a, b) -> a <= b);

	private final String description;
	private final String shortLabel;
	private final BiPredicate<Long, Long> predicate;
	private final BiPredicate<Double, Double> doublePredicate;

	NumericOperator(String description, String shortLabel, BiPredicate<Long, Long> predicate, BiPredicate<Double, Double> doublePredicate) {
		this.description = description;
		this.shortLabel = shortLabel;
		this.predicate = predicate;
		this.doublePredicate = doublePredicate;
	}

	public boolean checkLong(long a, long b) {
		return predicate.test(a, b);
	}

	public boolean checkDouble(double a, double b) {
		return doublePredicate.test(a, b);
	}


}
