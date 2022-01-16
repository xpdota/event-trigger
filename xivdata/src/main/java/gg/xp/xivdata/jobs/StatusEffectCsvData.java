package gg.xp.xivdata.jobs;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

public class StatusEffectCsvData {
	private final long baseIconId;
	private final long numStacks;

	public StatusEffectCsvData(long baseIconId, long numStacks) {
		this.baseIconId = baseIconId;
		this.numStacks = numStacks;
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
}
