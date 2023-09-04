package gg.xp.xivsupport.timelines;

import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface LabelResolver {

	@Nullable Double resolve(String label);

}
