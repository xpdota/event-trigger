package gg.xp.xivsupport.timelines;

import java.io.Serializable;

public record TimelineReference(double time, String name, String pattern) implements Serializable {
}
