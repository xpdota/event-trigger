package gg.xp.xivdata.data;

import tools.jackson.databind.ObjectMapper;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CombatJobSelectionTest {

	@Test
	public void testSerialization() {
		ObjectMapper mapper = new ObjectMapper();

		// Test 'all'
		CombatJobSelection all = CombatJobSelection.all();
		String allJson = mapper.writeValueAsString(all);
		CombatJobSelection allDeserialized = mapper.readValue(allJson, CombatJobSelection.class);
		Assert.assertEquals(allDeserialized, all, "Deserialized 'all' should match original");

		// Test 'none'
		CombatJobSelection none = CombatJobSelection.none();
		String noneJson = mapper.writeValueAsString(none);
		CombatJobSelection noneDeserialized = mapper.readValue(noneJson, CombatJobSelection.class);
		Assert.assertEquals(noneDeserialized, none, "Deserialized 'none' should match original");

		// Test custom selection
		CombatJobSelection custom = CombatJobSelection.none();
		custom.changeCategoryState(JobType.TANK, true);
		custom.changeJobState(Job.WHM, true);
		String customJson = mapper.writeValueAsString(custom);
		CombatJobSelection customDeserialized = mapper.readValue(customJson, CombatJobSelection.class);
		Assert.assertEquals(customDeserialized, custom, "Deserialized custom should match original");
		Assert.assertTrue(customDeserialized.stateForCategory(JobType.TANK).countsAsEnabled());
		Assert.assertTrue(customDeserialized.enabledForJob(Job.WHM));
		Assert.assertFalse(customDeserialized.enabledForJob(Job.SGE));
	}
}
