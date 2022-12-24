package gg.xp.xivsupport.timelines.intl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public record LanguageReplacements(
		Map<Pattern, String> replaceSync,
		Map<Pattern, String> replaceText
) {
	@JsonCreator
	public static LanguageReplacements fromRaw(@JsonProperty("replaceSync") Map<String, String> replaceSync, @JsonProperty("replaceText") Map<String, String> replaceText) {
		return new LanguageReplacements(
				replaceSync.entrySet().stream().collect(Collectors.toMap(e -> Pattern.compile(e.getKey()), Map.Entry::getValue)),
				replaceText.entrySet().stream().collect(Collectors.toMap(e -> Pattern.compile(e.getKey()), Map.Entry::getValue))
		);
	}

	public static LanguageReplacements empty() {
		return new LanguageReplacements(Collections.emptyMap(), Collections.emptyMap());
	}
}
