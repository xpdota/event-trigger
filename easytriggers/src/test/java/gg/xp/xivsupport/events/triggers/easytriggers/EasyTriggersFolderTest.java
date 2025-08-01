package gg.xp.xivsupport.events.triggers.easytriggers;

import gg.xp.reevent.events.EventDistributor;
import gg.xp.reevent.events.TestEventCollector;
import gg.xp.xivsupport.events.ExampleSetup;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.ZoneChangeEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.triggers.easytriggers.actions.CalloutAction;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.AbilityIdFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.GroovyEventFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.GroovyFolderFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.conditions.ZoneIdFilter;
import gg.xp.xivsupport.events.triggers.easytriggers.model.ConditionDescription;
import gg.xp.xivsupport.events.triggers.easytriggers.model.ConditionTarget;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTrigger;
import gg.xp.xivsupport.events.triggers.easytriggers.model.EasyTriggerContext;
import gg.xp.xivsupport.events.triggers.easytriggers.model.NumericOperator;
import gg.xp.xivsupport.events.triggers.easytriggers.model.TriggerFolder;
import gg.xp.xivsupport.groovy.GroovyManager;
import gg.xp.xivsupport.models.XivAbility;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivZone;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.speech.CalloutEvent;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jenkinsci.plugins.scriptsecurity.sandbox.RejectedAccessException;
import org.picocontainer.MutablePicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;

/**
 * Tests for folder-specific functionality, including GroovyFolderFilter
 */
public class EasyTriggersFolderTest {

    private static final Logger log = LoggerFactory.getLogger(EasyTriggersFolderTest.class);

    // Test data
    private static final XivCombatant caster = new XivCombatant(10, "Caster");
    private static final XivAbility matchingAbility = new XivAbility(123, "Foo Ability");
    private static final XivAbility otherAbility = new XivAbility(456, "Bar Ability");
    private static final XivCombatant target = new XivCombatant(11, "Target");
    private static final AbilityUsedEvent abilityUsed1 = new AbilityUsedEvent(matchingAbility, caster, target, Collections.emptyList(), 123, 0, 1);
    private static final AbilityUsedEvent abilityUsed2 = new AbilityUsedEvent(otherAbility, caster, target, Collections.emptyList(), 123, 0, 1);
    private static final ZoneChangeEvent zoneChange = new ZoneChangeEvent(new XivZone(987, "Some Zone"));
    private static final ZoneChangeEvent otherZoneChange = new ZoneChangeEvent(new XivZone(654, "Other Zone"));

    /**
     * Test basic functionality of GroovyFolderFilter
     */
    @Test
    void testGroovyFolderFilter() {
        MutablePicoContainer pico = ExampleSetup.setup();
        TestEventCollector coll = new TestEventCollector();
        EventDistributor dist = pico.getComponent(EventDistributor.class);
        dist.registerHandler(coll);

        EasyTriggers ez = pico.getComponent(EasyTriggers.class);
        GroovyManager groovyManager = pico.getComponent(GroovyManager.class);

        // Create a folder with a GroovyFolderFilter
        TriggerFolder folder = new TriggerFolder();
        folder.setName("Test Folder");
        
        // Add a GroovyFolderFilter that always returns true
        GroovyFolderFilter alwaysTrue = new GroovyFolderFilter(groovyManager);
        alwaysTrue.setGroovyScript("return true");
        folder.addCondition(alwaysTrue);
        
        // Create a trigger inside the folder
        EasyTrigger<AbilityUsedEvent> trigger = new EasyTrigger<>();
        trigger.setName("Test Trigger");
        trigger.setEventType(AbilityUsedEvent.class);
        
        // Add a condition to the trigger
        AbilityIdFilter abilityFilter = new AbilityIdFilter();
        abilityFilter.operator = NumericOperator.EQ;
        abilityFilter.expected = 123;
        trigger.addCondition(abilityFilter);
        
        // Add an action to the trigger
        CalloutAction callout = new CalloutAction();
        callout.setText("Ability used: {event.getAbility().getName()}");
        callout.setTts("Ability used");
        trigger.addAction(callout);
        
        // Add the trigger to the folder
        folder.addChildTrigger(trigger);
        
        // Add the folder to EasyTriggers
        ez.addTrigger(null, folder);
        
        // Send events
        dist.acceptEvent(abilityUsed2); // Should not trigger (wrong ability ID)
        dist.acceptEvent(abilityUsed1); // Should trigger
        
        // Verify that the callout was made
        List<CalloutEvent> calls = coll.getEventsOf(CalloutEvent.class);
        Assert.assertEquals(calls.size(), 1, "Expected 1 callout");
        CalloutEvent theCall = calls.get(0);
        Assert.assertEquals(theCall.getVisualText(), "Ability used: Foo Ability");
        Assert.assertEquals(theCall.getCallText(), "Ability used");
        
        // Now change the GroovyFolderFilter to always return false
        alwaysTrue.setGroovyScript("return false");
        
        // Clear the collector
        coll.clear();
        
        // Send events again
        dist.acceptEvent(abilityUsed1); // Should not trigger (folder condition is false)
        
        // Verify that no callout was made
        calls = coll.getEventsOf(CalloutEvent.class);
        Assert.assertEquals(calls.size(), 0, "Expected 0 callouts");
    }
    
    /**
     * Test complex conditions in GroovyFolderFilter
     */
    @Test
    void testComplexGroovyFolderFilter() {
        MutablePicoContainer pico = ExampleSetup.setup();
        TestEventCollector coll = new TestEventCollector();
        EventDistributor dist = pico.getComponent(EventDistributor.class);
        dist.registerHandler(coll);

        EasyTriggers ez = pico.getComponent(EasyTriggers.class);
        GroovyManager groovyManager = pico.getComponent(GroovyManager.class);
        XivState xivState = pico.getComponent(XivState.class);

        // Create a folder with a GroovyFolderFilter
        TriggerFolder folder = new TriggerFolder();
        folder.setName("Complex Test Folder");
        
        // Add a GroovyFolderFilter that checks the current zone
        GroovyFolderFilter zoneCheck = new GroovyFolderFilter(groovyManager);
        zoneCheck.setGroovyScript("return xivState.getZone()?.getId() == 987");
        folder.addCondition(zoneCheck);
        
        // Create a trigger inside the folder
        EasyTrigger<AbilityUsedEvent> trigger = new EasyTrigger<>();
        trigger.setName("Complex Test Trigger");
        trigger.setEventType(AbilityUsedEvent.class);
        
        // Add a condition to the trigger
        AbilityIdFilter abilityFilter = new AbilityIdFilter();
        abilityFilter.operator = NumericOperator.EQ;
        abilityFilter.expected = 123;
        trigger.addCondition(abilityFilter);
        
        // Add an action to the trigger
        CalloutAction callout = new CalloutAction();
        callout.setText("Ability used in correct zone");
        callout.setTts("Ability used in correct zone");
        trigger.addAction(callout);
        
        // Add the trigger to the folder
        folder.addChildTrigger(trigger);
        
        // Add the folder to EasyTriggers
        ez.addTrigger(null, folder);
        
        // Send events in wrong zone
        dist.acceptEvent(otherZoneChange); // Set zone to 654
        dist.acceptEvent(abilityUsed1); // Should not trigger (wrong zone)
        
        // Verify that no callout was made
        List<CalloutEvent> calls = coll.getEventsOf(CalloutEvent.class);
        Assert.assertEquals(calls.size(), 0, "Expected 0 callouts in wrong zone");
        
        // Send events in correct zone
        dist.acceptEvent(zoneChange); // Set zone to 987
        dist.acceptEvent(abilityUsed1); // Should trigger
        
        // Verify that the callout was made
        calls = coll.getEventsOf(CalloutEvent.class);
        Assert.assertEquals(calls.size(), 1, "Expected 1 callout in correct zone");
        CalloutEvent theCall = calls.get(0);
        Assert.assertEquals(theCall.getVisualText(), "Ability used in correct zone");
        Assert.assertEquals(theCall.getCallText(), "Ability used in correct zone");
    }
    
    /**
     * Test error handling in GroovyFolderFilter
     */
    @Test
    void testGroovyFolderFilterErrors() {
        MutablePicoContainer pico = ExampleSetup.setup();
        TestEventCollector coll = new TestEventCollector();
        EventDistributor dist = pico.getComponent(EventDistributor.class);
        dist.registerHandler(coll);

        EasyTriggers ez = pico.getComponent(EasyTriggers.class);
        GroovyManager groovyManager = pico.getComponent(GroovyManager.class);

        // Test syntax error handling
        GroovyFolderFilter syntaxError = new GroovyFolderFilter(groovyManager);
        
        // First set a valid script to initialize groovyCompiledScript
        syntaxError.setGroovyScript("return true");
        
        // Then use assertThrows to verify that setting an invalid script throws an exception
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            // This is invalid Groovy syntax and should throw an exception
            syntaxError.setGroovyScript("return 1 + ");
        });
        
        // Test a script with a variable that doesn't exist
        // Create a folder with a GroovyFolderFilter that uses a non-existent variable
        TriggerFolder folder = new TriggerFolder();
        folder.setName("Error Test Folder");
        
        GroovyFolderFilter errorScript = new GroovyFolderFilter(groovyManager);
        // This will compile but should return false when the variable doesn't exist
        errorScript.setGroovyScript("try { return someVariable != null } catch (Exception e) { return false }");
        folder.addCondition(errorScript);
        
        // Create a trigger inside the folder
        EasyTrigger<AbilityUsedEvent> trigger = new EasyTrigger<>();
        trigger.setName("Error Test Trigger");
        trigger.setEventType(AbilityUsedEvent.class);
        
        // Add a condition to the trigger
        AbilityIdFilter abilityFilter = new AbilityIdFilter();
        abilityFilter.operator = NumericOperator.EQ;
        abilityFilter.expected = 123;
        trigger.addCondition(abilityFilter);
        
        // Add an action to the trigger
        CalloutAction callout = new CalloutAction();
        callout.setText("This should not be called");
        callout.setTts("This should not be called");
        trigger.addAction(callout);
        
        // Add the trigger to the folder
        folder.addChildTrigger(trigger);
        
        // Add the folder to EasyTriggers
        ez.addTrigger(null, folder);
        
        // Send events - the script should handle the error and return false
        dist.acceptEvent(abilityUsed1);
        
        // Verify that no callout was made
        List<CalloutEvent> calls = coll.getEventsOf(CalloutEvent.class);
        Assert.assertEquals(calls.size(), 0, "Expected 0 callouts with error script");
    }
    
    /**
     * Test basic script functionality in GroovyFolderFilter
     * Tests scripts that return true and false
     */
    @Test
    void testGroovyFolderFilterBasicScripts() {
        MutablePicoContainer pico = ExampleSetup.setup();
        TestEventCollector coll = new TestEventCollector();
        EventDistributor dist = pico.getComponent(EventDistributor.class);
        dist.registerHandler(coll);

        EasyTriggers ez = pico.getComponent(EasyTriggers.class);
        GroovyManager groovyManager = pico.getComponent(GroovyManager.class);

        // Test a valid script first
        GroovyFolderFilter validScript = new GroovyFolderFilter(groovyManager);
        validScript.setGroovyScript("return true");
        
        // Create a folder with the valid GroovyFolderFilter
        TriggerFolder folder = new TriggerFolder();
        folder.setName("Valid Script Test Folder");
        folder.addCondition(validScript);
        
        // Create a trigger inside the folder
        EasyTrigger<AbilityUsedEvent> trigger = new EasyTrigger<>();
        trigger.setName("Valid Script Test Trigger");
        trigger.setEventType(AbilityUsedEvent.class);
        
        // Add a condition to the trigger
        AbilityIdFilter abilityFilter = new AbilityIdFilter();
        abilityFilter.operator = NumericOperator.EQ;
        abilityFilter.expected = 123;
        trigger.addCondition(abilityFilter);
        
        // Add an action to the trigger
        CalloutAction callout = new CalloutAction();
        callout.setText("Valid script callout");
        callout.setTts("Valid script callout");
        trigger.addAction(callout);
        
        // Add the trigger to the folder
        folder.addChildTrigger(trigger);
        
        // Add the folder to EasyTriggers
        ez.addTrigger(null, folder);
        
        // Send events - this should work fine since the script is valid
        dist.acceptEvent(abilityUsed1);
        
        // Verify that the callout was made
        {
            List<CalloutEvent> calls = coll.getEventsOf(CalloutEvent.class);
            Assert.assertEquals(calls.size(), 1, "Expected 1 callout with valid script");
        }
        
        // Now test a script that returns false
        TriggerFolder folder2 = new TriggerFolder();
        folder2.setName("False Script Test Folder");
        
        GroovyFolderFilter falseScript = new GroovyFolderFilter(groovyManager);
        falseScript.setGroovyScript("return false");
        folder2.addCondition(falseScript);
        
        // Create a trigger inside the folder
        EasyTrigger<AbilityUsedEvent> trigger2 = new EasyTrigger<>();
        trigger2.setName("False Script Test Trigger");
        trigger2.setEventType(AbilityUsedEvent.class);
        
        // Add a condition to the trigger
        AbilityIdFilter abilityFilter2 = new AbilityIdFilter();
        abilityFilter2.operator = NumericOperator.EQ;
        abilityFilter2.expected = 123;
        trigger2.addCondition(abilityFilter2);
        
        // Add an action to the trigger
        CalloutAction callout2 = new CalloutAction();
        callout2.setText("This should not be called");
        callout2.setTts("This should not be called");
        trigger2.addAction(callout2);
        
        // Add the trigger to the folder
        folder2.addChildTrigger(trigger2);
        
        // Add the folder to EasyTriggers
        ez.addTrigger(null, folder2);
        
        // Disable the first folder to prevent it from responding to events
        folder.setEnabled(false);
        
        // Clear the collector
        coll.clear();
        
        // Send events - the script returns false, so the trigger should not fire
        dist.acceptEvent(abilityUsed1);
        
        // Verify that no callout was made
        {
            List<CalloutEvent> calls = coll.getEventsOf(CalloutEvent.class);
            Assert.assertEquals(calls.size(), 0, "Expected 0 callouts with script returning false");
        }
    }
    
    /**
     * Test sandbox violations in GroovyFolderFilter
     * Verifies that attempts to use restricted classes are properly rejected
     */
    @Test
    void testGroovyFolderFilterSandboxViolation() {
        MutablePicoContainer pico = ExampleSetup.setup();
        GroovyManager groovyManager = pico.getComponent(GroovyManager.class);
        
        // Create a GroovyFolderFilter with a script that attempts to use a restricted class
        GroovyFolderFilter sandboxViolatingScript = new GroovyFolderFilter(groovyManager);
        sandboxViolatingScript.setGroovyScript("return new java.io.File(\"Foo\").exists()");
        
        // Execute the filter and verify it fails with RejectedAccessException
        EasyTriggerContext context = new EasyTriggerContext(null);
        Assert.assertFalse(sandboxViolatingScript.test(context, new Object()));
        MatcherAssert.assertThat(sandboxViolatingScript.getLastError(), 
                                Matchers.instanceOf(RejectedAccessException.class));
    }
    
    /**
     * Test folder enable/disable propagation
     */
    @Test
    void testFolderEnableDisable() {
        MutablePicoContainer pico = ExampleSetup.setup();
        TestEventCollector coll = new TestEventCollector();
        EventDistributor dist = pico.getComponent(EventDistributor.class);
        dist.registerHandler(coll);

        EasyTriggers ez = pico.getComponent(EasyTriggers.class);

        // Create a folder
        TriggerFolder folder = new TriggerFolder();
        folder.setName("Enable/Disable Test Folder");
        
        // Create a trigger inside the folder
        EasyTrigger<AbilityUsedEvent> trigger = new EasyTrigger<>();
        trigger.setName("Enable/Disable Test Trigger");
        trigger.setEventType(AbilityUsedEvent.class);
        
        // Add a condition to the trigger
        AbilityIdFilter abilityFilter = new AbilityIdFilter();
        abilityFilter.operator = NumericOperator.EQ;
        abilityFilter.expected = 123;
        trigger.addCondition(abilityFilter);
        
        // Add an action to the trigger
        CalloutAction callout = new CalloutAction();
        callout.setText("Ability used");
        callout.setTts("Ability used");
        trigger.addAction(callout);
        
        // Add the trigger to the folder
        folder.addChildTrigger(trigger);
        
        // Add the folder to EasyTriggers
        ez.addTrigger(null, folder);
        
        // Send events with folder enabled
        dist.acceptEvent(abilityUsed1); // Should trigger
        
        // Verify that the callout was made
        List<CalloutEvent> calls = coll.getEventsOf(CalloutEvent.class);
        Assert.assertEquals(calls.size(), 1, "Expected 1 callout with folder enabled");
        
        // Disable the folder
        folder.setEnabled(false);
        
        // Clear the collector
        coll.clear();
        
        // Send events with folder disabled
        dist.acceptEvent(abilityUsed1); // Should not trigger
        
        // Verify that no callout was made
        calls = coll.getEventsOf(CalloutEvent.class);
        Assert.assertEquals(calls.size(), 0, "Expected 0 callouts with folder disabled");
        
        // Enable the folder but disable the trigger
        folder.setEnabled(true);
        trigger.setEnabled(false);
        
        // Send events with folder enabled but trigger disabled
        dist.acceptEvent(abilityUsed1); // Should not trigger
        
        // Verify that no callout was made
        calls = coll.getEventsOf(CalloutEvent.class);
        Assert.assertEquals(calls.size(), 0, "Expected 0 callouts with trigger disabled");
        
        // Enable both
        trigger.setEnabled(true);
        
        // Clear the collector
        coll.clear();
        
        // Send events with both enabled
        dist.acceptEvent(abilityUsed1); // Should trigger
        
        // Verify that the callout was made
        calls = coll.getEventsOf(CalloutEvent.class);
        Assert.assertEquals(calls.size(), 1, "Expected 1 callout with both enabled");
    }
    
    /**
     * Test complex folder structures
     */
    @Test
    void testComplexFolderStructure() {
        MutablePicoContainer pico = ExampleSetup.setup();
        TestEventCollector coll = new TestEventCollector();
        EventDistributor dist = pico.getComponent(EventDistributor.class);
        dist.registerHandler(coll);

        EasyTriggers ez = pico.getComponent(EasyTriggers.class);
        GroovyManager groovyManager = pico.getComponent(GroovyManager.class);

        // Create a top-level folder
        TriggerFolder topFolder = new TriggerFolder();
        topFolder.setName("Top Folder");
        
        // Add a condition to the top folder
        GroovyFolderFilter topCondition = new GroovyFolderFilter(groovyManager);
        topCondition.setGroovyScript("return true");
        topFolder.addCondition(topCondition);
        
        // Create a mid-level folder
        TriggerFolder midFolder = new TriggerFolder();
        midFolder.setName("Mid Folder");
        
        // Add a condition to the mid folder
        ZoneIdFilter midCondition = new ZoneIdFilter(pico.getComponent(XivState.class));
        midCondition.expected = 987;
        midCondition.operator = NumericOperator.EQ;
        midFolder.addCondition(midCondition);
        
        // Create a bottom-level folder
        TriggerFolder bottomFolder = new TriggerFolder();
        bottomFolder.setName("Bottom Folder");
        
        // Add a condition to the bottom folder
        GroovyFolderFilter bottomCondition = new GroovyFolderFilter(groovyManager);
        bottomCondition.setGroovyScript("return true");
        bottomFolder.addCondition(bottomCondition);
        
        // Create a trigger inside the bottom folder
        EasyTrigger<AbilityUsedEvent> trigger = new EasyTrigger<>();
        trigger.setName("Complex Structure Test Trigger");
        trigger.setEventType(AbilityUsedEvent.class);
        
        // Add a condition to the trigger
        AbilityIdFilter abilityFilter = new AbilityIdFilter();
        abilityFilter.operator = NumericOperator.EQ;
        abilityFilter.expected = 123;
        trigger.addCondition(abilityFilter);
        
        // Add an action to the trigger
        CalloutAction callout = new CalloutAction();
        callout.setText("Deep nested trigger activated");
        callout.setTts("Deep nested trigger activated");
        trigger.addAction(callout);
        
        // Build the hierarchy
        bottomFolder.addChildTrigger(trigger);
        midFolder.addChildTrigger(bottomFolder);
        topFolder.addChildTrigger(midFolder);
        ez.addTrigger(null, topFolder);
        
        // Send events in wrong zone
        dist.acceptEvent(otherZoneChange); // Set zone to 654
        dist.acceptEvent(abilityUsed1); // Should not trigger (wrong zone)
        
        // Verify that no callout was made
        List<CalloutEvent> calls = coll.getEventsOf(CalloutEvent.class);
        Assert.assertEquals(calls.size(), 0, "Expected 0 callouts in wrong zone");
        
        // Send events in correct zone
        dist.acceptEvent(zoneChange); // Set zone to 987
        dist.acceptEvent(abilityUsed1); // Should trigger
        
        // Verify that the callout was made
        calls = coll.getEventsOf(CalloutEvent.class);
        Assert.assertEquals(calls.size(), 1, "Expected 1 callout in correct zone");
        CalloutEvent theCall = calls.get(0);
        Assert.assertEquals(theCall.getVisualText(), "Deep nested trigger activated");
        Assert.assertEquals(theCall.getCallText(), "Deep nested trigger activated");
        
        // Disable the mid folder
        midFolder.setEnabled(false);
        
        // Clear the collector
        coll.clear();
        
        // Send events with mid folder disabled
        dist.acceptEvent(abilityUsed1); // Should not trigger
        
        // Verify that no callout was made
        calls = coll.getEventsOf(CalloutEvent.class);
        Assert.assertEquals(calls.size(), 0, "Expected 0 callouts with mid folder disabled");
    }
    
    /**
     * Test folder-specific condition filtering
     */
    @Test
    void testFolderSpecificConditionFiltering() {
        MutablePicoContainer pico = ExampleSetup.setup();
        EasyTriggers ez = pico.getComponent(EasyTriggers.class);
        
        // Create a folder and a trigger
        TriggerFolder folder = new TriggerFolder();
        EasyTrigger<AbilityUsedEvent> trigger = new EasyTrigger<>();
        trigger.setEventType(AbilityUsedEvent.class);
        
        // Get conditions applicable to folder
        List<ConditionDescription<?, ?>> folderConditions = ez.getConditionsApplicableTo(folder);
        
        // Get conditions applicable to trigger
        List<ConditionDescription<?, ?>> triggerConditions = ez.getConditionsApplicableTo(trigger);
        
        // Verify that folder-only conditions are only available to folders
        boolean folderHasGroovyFolderFilter = folderConditions.stream()
                .anyMatch(cd -> cd.clazz().equals(GroovyFolderFilter.class));
        boolean triggerHasGroovyFolderFilter = triggerConditions.stream()
                .anyMatch(cd -> cd.clazz().equals(GroovyFolderFilter.class));
        
        Assert.assertTrue(folderHasGroovyFolderFilter, "Folder should have GroovyFolderFilter available");
        Assert.assertFalse(triggerHasGroovyFolderFilter, "Trigger should not have GroovyFolderFilter available");
        
        // Verify that trigger-only conditions are only available to triggers
        boolean folderHasGroovyEventFilter = folderConditions.stream()
                .anyMatch(cd -> cd.clazz().equals(GroovyEventFilter.class));
        boolean triggerHasGroovyEventFilter = triggerConditions.stream()
                .anyMatch(cd -> cd.clazz().equals(GroovyEventFilter.class));
        
        Assert.assertFalse(folderHasGroovyEventFilter, "Folder should not have GroovyEventFilter available");
        Assert.assertTrue(triggerHasGroovyEventFilter, "Trigger should have GroovyEventFilter available");
        
        // Verify that "both" conditions are available to both
        boolean folderHasZoneIdFilter = folderConditions.stream()
                .anyMatch(cd -> cd.clazz().equals(ZoneIdFilter.class));
        boolean triggerHasZoneIdFilter = triggerConditions.stream()
                .anyMatch(cd -> cd.clazz().equals(ZoneIdFilter.class));
        
        Assert.assertTrue(folderHasZoneIdFilter, "Folder should have ZoneIdFilter available");
        Assert.assertTrue(triggerHasZoneIdFilter, "Trigger should have ZoneIdFilter available");
    }
    
    /**
     * Test serialization and deserialization of folder structures with GroovyFolderFilter
     */
    @Test
    void testFolderSerialization() {
        PersistenceProvider pers;
        {
            MutablePicoContainer pico = ExampleSetup.setup();
            pers = pico.getComponent(PersistenceProvider.class);
            TestEventCollector coll = new TestEventCollector();
            EventDistributor dist = pico.getComponent(EventDistributor.class);
            dist.registerHandler(coll);

            EasyTriggers ez = pico.getComponent(EasyTriggers.class);
            GroovyManager groovyManager = pico.getComponent(GroovyManager.class);

            // Create a folder with a GroovyFolderFilter
            TriggerFolder folder = new TriggerFolder();
            folder.setName("Serialization Test Folder");
            
            // Add a GroovyFolderFilter
            GroovyFolderFilter filter = new GroovyFolderFilter(groovyManager);
            filter.setGroovyScript("return true");
            folder.addCondition(filter);
            
            // Create a trigger inside the folder
            EasyTrigger<AbilityUsedEvent> trigger = new EasyTrigger<>();
            trigger.setName("Serialization Test Trigger");
            trigger.setEventType(AbilityUsedEvent.class);
            
            // Add a condition to the trigger
            AbilityIdFilter abilityFilter = new AbilityIdFilter();
            abilityFilter.operator = NumericOperator.EQ;
            abilityFilter.expected = 123;
            trigger.addCondition(abilityFilter);
            
            // Add an action to the trigger
            CalloutAction callout = new CalloutAction();
            callout.setText("Serialization test");
            callout.setTts("Serialization test");
            trigger.addAction(callout);
            
            // Add the trigger to the folder
            folder.addChildTrigger(trigger);
            
            // Add the folder to EasyTriggers
            ez.addTrigger(null, folder);
            
            // Send events
            dist.acceptEvent(abilityUsed1); // Should trigger
            
            // Verify that the callout was made
            List<CalloutEvent> calls = coll.getEventsOf(CalloutEvent.class);
            Assert.assertEquals(calls.size(), 1, "Expected 1 callout before serialization");
        }
        
        // Now load the serialized version and make sure it all still works
        {
            MutablePicoContainer pico = ExampleSetup.setup(pers);
            TestEventCollector coll = new TestEventCollector();
            EventDistributor dist = pico.getComponent(EventDistributor.class);
            dist.registerHandler(coll);

            EasyTriggers ez = pico.getComponent(EasyTriggers.class);
            
            // Check deserialization
            MatcherAssert.assertThat(ez.getChildTriggers(), Matchers.hasSize(1));
            TriggerFolder folder = (TriggerFolder) ez.getChildTriggers().get(0);
            Assert.assertEquals(folder.getName(), "Serialization Test Folder");
            MatcherAssert.assertThat(folder.getConditions(), Matchers.hasSize(1));
            Assert.assertTrue(folder.getConditions().get(0) instanceof GroovyFolderFilter);
            MatcherAssert.assertThat(folder.getChildTriggers(), Matchers.hasSize(1));
            EasyTrigger<?> trigger = (EasyTrigger<?>) folder.getChildTriggers().get(0);
            Assert.assertEquals(trigger.getName(), "Serialization Test Trigger");
            
            // Send events
            dist.acceptEvent(abilityUsed1); // Should trigger
            
            // Verify that the callout was made
            List<CalloutEvent> calls = coll.getEventsOf(CalloutEvent.class);
            Assert.assertEquals(calls.size(), 1, "Expected 1 callout after serialization");
            CalloutEvent theCall = calls.get(0);
            Assert.assertEquals(theCall.getVisualText(), "Serialization test");
            Assert.assertEquals(theCall.getCallText(), "Serialization test");
        }
    }
}