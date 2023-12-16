package gg.xp.xivsupport.timelines.cbevents;

import org.intellij.lang.annotations.Language;

/**
 * Cactbot field mapping
 *
 * @param <X> The Triggevent event type
 * @param cbField The cactbot field name
 * @param ourLabel Groovy script for the event on our end
 * @param conversion How to convert the value to a predicate
 */
public record CbfMap<X>(String cbField, @Language("Groovy") String ourLabel, CbConversion<X> conversion) {

}
