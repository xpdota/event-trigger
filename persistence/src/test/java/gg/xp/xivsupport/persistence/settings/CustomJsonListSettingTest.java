package gg.xp.xivsupport.persistence.settings;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.xp.xivsupport.persistence.InMemoryMapPersistenceProvider;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.Nullable;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class CustomJsonListSettingTest {

	private static boolean forceFailure;

	public static class MyTestDataClass {

		public MyTestDataClass() {
			if (forceFailure) {
				throw new RuntimeException("Intentional failure");
			}
		}

		@JsonProperty("ints")
		public List<Integer> numbers = new ArrayList<>();

		public String label;
	}

	private static final String settingKey = "my-setting";
	private static final String failuresKey = "my-setting-failures";
	private static final ObjectMapper mapper = new ObjectMapper();
	private static final Matcher<List<String>> emptyOrNull = Matchers.anyOf(Matchers.nullValue(), Matchers.empty());

	private record TestData(PersistenceProvider pers, CustomJsonListSetting<MyTestDataClass> setting) {
		String getSavedRaw() {
			return (pers.get(settingKey, String.class, null));
		}

		List<String> getSaved() {
			try {
				List<JsonNode> nodes = mapper.readValue((pers.get(settingKey, String.class, "[]")), new TypeReference<>() {
				});
				return nodes.stream().map(Object::toString).toList();
			}
			catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
		}

		List<String> getFailures() {
			try {
				return mapper.readValue((pers.get(failuresKey, String.class, "[]")), new TypeReference<>() {
				});
			}
			catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private TestData fromSaved(@Nullable String saved, @Nullable String existingFailures) {
		InMemoryMapPersistenceProvider pers = new InMemoryMapPersistenceProvider();
		if (saved != null) {
			pers.save(settingKey, saved);
		}
		if (existingFailures != null) {
			pers.save(failuresKey, existingFailures);
		}
		CustomJsonListSetting<MyTestDataClass> setting = CustomJsonListSetting.builder(pers, new TypeReference<MyTestDataClass>() {
		}, settingKey, failuresKey).build();
		return new TestData(pers, setting);
	}

	@BeforeMethod
	void reset() {
		forceFailure = false;
	}

	@Test
	void testDeserialization() {
		String mySetting = """
				[ { "ints": [3, 4, 5], "label": "Foo" } ]
				""";
		TestData data = fromSaved(mySetting, null);
		CustomJsonListSetting<MyTestDataClass> setting = data.setting;
		List<MyTestDataClass> items = setting.getItems();
		MatcherAssert.assertThat(items, Matchers.hasSize(1));
		MatcherAssert.assertThat(items.get(0).label, Matchers.equalTo("Foo"));
		MatcherAssert.assertThat(items.get(0).numbers, Matchers.equalTo(List.of(3, 4, 5)));
		MatcherAssert.assertThat(setting.getFailedItems(), Matchers.empty());
		MatcherAssert.assertThat(data.getSaved(), Matchers.hasSize(1));
		MatcherAssert.assertThat(data.getFailures(), emptyOrNull);
	}

	@Test
	void testDeserializationWithFailure() {
		String mySetting = """
				[ { "ints": [3, 4, 5], "label": "Foo" } ,
				{ "ints": [3, 4, 5], "larbel": "Foo" } ]
				""";
		TestData data = fromSaved(mySetting, null);
		CustomJsonListSetting<MyTestDataClass> setting = data.setting;
		{
			List<MyTestDataClass> items = setting.getItems();
			MatcherAssert.assertThat(items, Matchers.hasSize(1));
			MatcherAssert.assertThat(items.get(0).label, Matchers.equalTo("Foo"));
			MatcherAssert.assertThat(items.get(0).numbers, Matchers.equalTo(List.of(3, 4, 5)));
			List<String> failures = setting.getFailedItems();
			MatcherAssert.assertThat(failures, Matchers.hasSize(1));
			MatcherAssert.assertThat(failures.get(0), Matchers.equalTo("""
					{"ints":[3,4,5],"larbel":"Foo"}"""));
			MatcherAssert.assertThat(data.getSaved(), Matchers.hasSize(1));
			MatcherAssert.assertThat(data.getFailures(), Matchers.hasSize(1));
		}
		{
			setting.tryRecoverFailures();
			setting.tryRecoverFailures();
			setting.tryRecoverFailures();

			List<MyTestDataClass> items = setting.getItems();
			MatcherAssert.assertThat(items, Matchers.hasSize(1));
			MatcherAssert.assertThat(items.get(0).label, Matchers.equalTo("Foo"));
			MatcherAssert.assertThat(items.get(0).numbers, Matchers.equalTo(List.of(3, 4, 5)));
			List<String> failures = setting.getFailedItems();
			MatcherAssert.assertThat(failures, Matchers.hasSize(1));
			MatcherAssert.assertThat(failures.get(0), Matchers.equalTo("""
					{"ints":[3,4,5],"larbel":"Foo"}"""));
			MatcherAssert.assertThat(data.getSaved(), Matchers.hasSize(1));
			MatcherAssert.assertThat(data.getFailures(), Matchers.hasSize(1));
		}
	}


	@Test
	void testDeserializationFailureRecovery() {
		forceFailure = true;
		String mySetting = """
				[ { "ints": [1, 2, 3], "label": "Foo" } ,
				{ "ints": [4, 5, 6] } ]
				""";
		TestData data = fromSaved(mySetting, null);
		CustomJsonListSetting<MyTestDataClass> setting = data.setting;
		{
			List<MyTestDataClass> items = setting.getItems();
			MatcherAssert.assertThat(items, Matchers.empty());
			List<String> failures = setting.getFailedItems();
			MatcherAssert.assertThat(failures, Matchers.hasSize(2));
			MatcherAssert.assertThat(data.getSaved(), Matchers.hasSize(0));
			MatcherAssert.assertThat(data.getFailures(), Matchers.hasSize(2));
//			MatcherAssert.assertThat(pers.get(settingKey, String.class, null), Matchers.hasSize(Matchers.greaterThan(20)));
////			MatcherAssert.assertThat(pers.get(settingKey, String.class, null), Matchers.hasSize(Matchers.greaterThan(20)));
		}
		{
			setting.tryRecoverFailures();
			setting.tryRecoverFailures();
			setting.tryRecoverFailures();

			List<MyTestDataClass> items = setting.getItems();
			MatcherAssert.assertThat(items, Matchers.empty());
			List<String> failures = setting.getFailedItems();
			MatcherAssert.assertThat(failures, Matchers.hasSize(2));

			MatcherAssert.assertThat(failures.get(0), Matchers.equalTo("""
					{"ints":[1,2,3],"label":"Foo"}"""));
			MatcherAssert.assertThat(failures.get(1), Matchers.equalTo("""
					{"ints":[4,5,6]}"""));
			MatcherAssert.assertThat(data.getSaved(), Matchers.hasSize(0));
			MatcherAssert.assertThat(data.getFailures(), Matchers.hasSize(2));
		}
		forceFailure = false;
		{
			setting.tryRecoverFailures();

			List<MyTestDataClass> items = setting.getItems();
			MatcherAssert.assertThat(items, Matchers.hasSize(2));
			List<String> failures = setting.getFailedItems();
			MatcherAssert.assertThat(failures, Matchers.empty());

			MatcherAssert.assertThat(items.get(0).label, Matchers.equalTo("Foo"));
			MatcherAssert.assertThat(items.get(0).numbers, Matchers.equalTo(List.of(1, 2, 3)));
			MatcherAssert.assertThat(items.get(1).label, Matchers.nullValue());
			MatcherAssert.assertThat(items.get(1).numbers, Matchers.equalTo(List.of(4, 5, 6)));
			MatcherAssert.assertThat(data.getSaved(), Matchers.hasSize(2));
			MatcherAssert.assertThat(data.getFailures(), Matchers.hasSize(0));
		}
	}

	@Test
	void testSavedFailuresRecovered() {
		String mySetting = """
				[ { "ints": [1, 2, 3], "label": "Foo" } ,
				{ "ints": [4, 5, 6] } ]
				""";
		String myFailure = """
				["{\\"ints\\":[10,11]}"]
				""";
		TestData data = fromSaved(mySetting, myFailure);
		CustomJsonListSetting<MyTestDataClass> setting = data.setting;
		{
			List<MyTestDataClass> items = setting.getItems();
			MatcherAssert.assertThat(items, Matchers.hasSize(2));
			List<String> failures = setting.getFailedItems();
			MatcherAssert.assertThat(failures, Matchers.hasSize(1));
			MatcherAssert.assertThat(data.getSaved(), Matchers.hasSize(2));
			MatcherAssert.assertThat(data.getFailures(), Matchers.hasSize(1));
		}
		{
			setting.tryRecoverFailures();
			setting.tryRecoverFailures();
			setting.tryRecoverFailures();

			List<MyTestDataClass> items = setting.getItems();
			MatcherAssert.assertThat(items, Matchers.hasSize(3));
			List<String> failures = setting.getFailedItems();
			MatcherAssert.assertThat(failures, Matchers.hasSize(0));
			MatcherAssert.assertThat(data.getSaved(), Matchers.hasSize(3));
			MatcherAssert.assertThat(data.getFailures(), Matchers.hasSize(0));
		}
	}

	@Test
	void testSavedFailuresNotRecovered() {
		String mySetting = """
				[ { "ints": [1, 2, 3], "label": "Foo" } ,
				{ "ints": [4, 5, 6] } ]
				""";
		String myFailure = """
				["{\\"ints\\":\\"bad\\"}"]
				""";
		CustomJsonListSetting<MyTestDataClass> setting = fromSaved(mySetting, myFailure).setting;
		{
			List<MyTestDataClass> items = setting.getItems();
			MatcherAssert.assertThat(items, Matchers.hasSize(2));
			List<String> failures = setting.getFailedItems();
			MatcherAssert.assertThat(failures, Matchers.hasSize(1));
		}
		{
			setting.tryRecoverFailures();
			setting.tryRecoverFailures();
			setting.tryRecoverFailures();

			List<MyTestDataClass> items = setting.getItems();
			MatcherAssert.assertThat(items, Matchers.hasSize(2));
			List<String> failures = setting.getFailedItems();
			MatcherAssert.assertThat(failures, Matchers.hasSize(1));
		}
	}
}