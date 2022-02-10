package gg.xp.xivdata.jobs;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

public class StatusEffectInfo {
	private final long baseIconId;
	private final long numStacks;
	private final String name;
	private final String description;

	public StatusEffectInfo(long baseIconId, long numStacks, String name, String description) {
		this.baseIconId = baseIconId;
		this.numStacks = numStacks;
		this.name = name;
		this.description = description;
	}

	public long getBaseIconId() {
		return baseIconId;
	}

	public long getNumStacks() {
		return numStacks;
	}

	public List<Long> getAllIconIds() {
		if (numStacks == 0 || numStacks == 1) {
			return Collections.singletonList(baseIconId);
		}
		else {
			return LongStream.range(baseIconId, baseIconId + numStacks).boxed().collect(Collectors.toList());
		}
	}

	public long iconForStackCount(long stacks) {
		if (stacks == 0) {
			return baseIconId;
		}
		else {
			return baseIconId + stacks - 1;
		}
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}
}
