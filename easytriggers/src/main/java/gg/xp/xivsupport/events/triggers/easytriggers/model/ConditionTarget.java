package gg.xp.xivsupport.events.triggers.easytriggers.model;

/**
 * Enum to specify which type of trigger a condition can be applied to.
 * This helps prevent user confusion by only showing relevant conditions.
 */
public enum ConditionTarget {
    /**
     * Condition can only be applied to EasyTrigger instances
     */
    TRIGGER_ONLY,
    
    /**
     * Condition can only be applied to TriggerFolder instances
     */
    FOLDER_ONLY,
    
    /**
     * Condition can be applied to both EasyTrigger and TriggerFolder instances
     */
    BOTH
}