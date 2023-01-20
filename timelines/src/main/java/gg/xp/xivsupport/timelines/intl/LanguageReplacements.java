package gg.xp.xivsupport.timelines.intl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

	/*
		The purpose of these @JsonProperty methods is to convert the "Pattern" back to a "String" so that it
		can be sorted.
	 */
	@JsonProperty("replaceSync")
	public Map<String, String> sortedReplaceSync() {
		return replaceSync.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().pattern(), e -> e.getValue()));
	}

	@JsonProperty("replaceText")
	public Map<String, String> sortedReplaceText() {
		return replaceText.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().pattern(), e -> e.getValue()));
	}

	public static LanguageReplacements empty() {
		return new LanguageReplacements(Collections.emptyMap(), Collections.emptyMap());
	}

	public static LanguageReplacements combine(List<LanguageReplacements> all) {
		if (all.isEmpty()) {
			return empty();
		}
		else if (all.size() == 1) {
			return all.get(0);
		}
		else {
			Map<String, String> syncRaw = new HashMap<>();
			Map<String, String> textRaw = new HashMap<>();
			for (LanguageReplacements lr : all) {
				syncRaw.putAll(lr.sortedReplaceSync());
				textRaw.putAll(lr.sortedReplaceText());
			}
			return fromRaw(syncRaw, textRaw);
		}
	}
}
