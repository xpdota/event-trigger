package gg.xp.reevent.scan;

import java.util.List;

public class CombinedInitFailuresException extends RuntimeException {
	public CombinedInitFailuresException(List<InitException> causes) {
		super("One or more modules failed to initialize");
		causes.forEach(this::addSuppressed);
	}
}
