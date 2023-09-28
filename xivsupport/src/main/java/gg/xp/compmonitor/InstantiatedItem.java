package gg.xp.compmonitor;

public record InstantiatedItem<X>(Class<X> cls, X instance) {
}
