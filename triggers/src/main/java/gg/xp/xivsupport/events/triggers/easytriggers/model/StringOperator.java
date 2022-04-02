package gg.xp.xivsupport.events.triggers.easytriggers.model;

import gg.xp.xivsupport.gui.util.HasFriendlyName;

import java.util.Objects;
import java.util.function.BiPredicate;

public enum StringOperator implements HasFriendlyName {

	EQ("Equal To", "Equals", Objects::equals),
	STARTS_WITH("Starts With", "Starts With", String::startsWith),
	ENDS_WITH("Ends With", "Ends With", String::endsWith),
	CONTAINS("Contains", "Contains", String::contains);

	private final String description;
	private final String shortLabel;
	private final BiPredicate<String, String> stringPredicate;

	StringOperator(String description, String shortLabel, BiPredicate<String, String> stringPredicate) {
		this.description = description;
		this.shortLabel = shortLabel;
		this.stringPredicate = stringPredicate;
	}

	public boolean checkString(String a, String b) {
		return stringPredicate.test(a, b);
	}

	@Override
	public String getFriendlyName() {
		return shortLabel;
	}
}
