package gg.xp.xivsupport.gui.tables.renderers;

/**
 * interval is out of 1 i.e. 0.1 means draw 10 ticks on the bar
 * <p>
 * offset is how much to slide over the first tick
 */
public record TickRenderInfo(double offset, double interval) {

}
