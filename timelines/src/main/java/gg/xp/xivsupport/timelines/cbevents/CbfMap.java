package gg.xp.xivsupport.timelines.cbevents;

/**
 * Cactbot field mapping
 */
public record CbfMap<X>(String cbField, String ourLabel, CbConversion<X> conversion) {

}
