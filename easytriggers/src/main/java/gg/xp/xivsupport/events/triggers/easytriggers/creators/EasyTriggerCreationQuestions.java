package gg.xp.xivsupport.events.triggers.easytriggers.creators;

import org.jetbrains.annotations.Nullable;

public interface EasyTriggerCreationQuestions {

	default @Nullable String askCalloutText() {
		return askOptionalString("Enter Callout Text (or leave blank for default)");
	}

	@Nullable String askOptionalString(String label);

	boolean askYesNo(String label, String yesButton, String noButton);

}
