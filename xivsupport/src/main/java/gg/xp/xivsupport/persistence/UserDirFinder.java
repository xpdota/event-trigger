package gg.xp.xivsupport.persistence;

import ch.qos.logback.core.PropertyDefinerBase;

public class UserDirFinder extends PropertyDefinerBase {

	@Override
	public String getPropertyValue() {
		return Platform.getTriggeventDir().toString();
	}

}
