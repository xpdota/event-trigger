package gg.xp.xivsupport.speech;

import java.util.Map;

public interface CalloutTraceInfo {

	String getOriginDescription();

	String getRawTts();

	String getRawText();

	Map<String, Object> getArgs();

}
