/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.kernel.util;

import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.test.log.LogCapture;
import com.liferay.portal.test.log.LogEntry;
import com.liferay.portal.test.log.LoggerTestUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.logging.Level;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Shuyang Zhou
 * @author Josef Sustacek
 */
public class EnvPropertiesUtilTest {

	@Test
	public void testDecode() {

		// Nothing to decode

		Assert.assertEquals("abcDEF", _decode("abcDEF"));

		// Incompleted encoded content

		Assert.assertEquals("abc_DEF", _decode("abc_DEF"));

		// Encoded with CharPool chars

		Assert.assertEquals(
			"abc:D,^E[F]g_H",
			_decode(
				"abc_COLON_D_COMMA__CARET_E_OPENBRACKET_F_CLOSEBRACKET_" +
					"_LOWERCASEG__UNDERLINE__UPPERCASEH_"));

		// Encoded with unicode chars

		Assert.assertEquals(
			"abc:D,^E[F]", _decode("abc_58_D_44__94_E_91_F_93_"));

		// Encoded with illegal content

		try (LogCapture logCapture = LoggerTestUtil.configureJDKLogger(
				EnvPropertiesUtil.class.getName(), Level.WARNING)) {

			String s = "abc_xyz_D_-1__DEF__GH";

			Assert.assertEquals(s, _decode(s));

			List<LogEntry> logEntries = logCapture.getLogEntries();

			Assert.assertEquals(logEntries.toString(), 3, logEntries.size());

			LogEntry logEntry = logEntries.get(0);

			Assert.assertEquals(
				"Unable to decode part \"xyz\" from \"" + s +
					"\", preserve it literally",
				logEntry.getMessage());

			logEntry = logEntries.get(1);

			Assert.assertEquals(
				"Unable to decode part \"-1\" from \"" + s +
					"\", preserve it literally",
				logEntry.getMessage());

			logEntry = logEntries.get(2);

			Assert.assertEquals(
				"Unable to decode part \"DEF\" from \"" + s +
					"\", preserve it literally",
				logEntry.getMessage());
		}
	}

	@Test
	public void test_loadEnvOverrides_simple() {
		// assuming
		Map<String, String> env = TreeMapBuilder
			.put("OTHER_KEY", "other_value")
			.put("LIFERAY_PROPS", "key1=value1\nkey2=value2\nupgrade.database.auto.run=true_A")
			.put("LIFERAY_PROPS_BY_COMPANY_1", "key1=value1_overA\nkey4=value4\nupgrade.database.auto.run=true_B")
			.put("LIFERAY_PROPS_BY_COMPANY_12345", "key1=value1_overB\nkey6=value6")
			.put("LIFERAY_PROPS_BY_COMPANY_20048", "key1=value1_overC\nkey8=value8")
			.put("LIFERAY_UPGRADE_PERIOD_DATABASE_PERIOD_AUTO_PERIOD_RUN", "true_C")
			.build();

		// companyId = 1 => allCompanies, byCompanyId_1, decoded

		try (LogCapture logCapture = LoggerTestUtil.configureJDKLogger(
			EnvPropertiesUtil.class.getName(), Level.INFO)) {

			// when
			SimpleBiConsumer<String, String> companyConsumer = new SimpleBiConsumer<>();

			_loadEnvOverrides(
				"LIFERAY_", 1, companyConsumer, env);

			if (_PRINT_LOG_ENTRIES_TO_STDOUT) {
				System.out.println("test_loadEnvOverrides_simple#1");

				logCapture.getLogEntries().stream().forEach(x ->
					System.out.println(x.getPriority() + " " + x.getMessage()));
			}

			// then
			Assert.assertEquals(4, companyConsumer.getConsumedItems().size());
			Assert.assertEquals(
				"true_C", companyConsumer.getConsumedItems().get("upgrade.database.auto.run"));
			Assert.assertEquals(
				"value1_overA", companyConsumer.getConsumedItems().get("key1"));
			Assert.assertEquals(
				"value2", companyConsumer.getConsumedItems().get("key2"));
			Assert.assertEquals(
				"value4", companyConsumer.getConsumedItems().get("key4"));


			// we cannot predict the exact order in which properties are loaded and therefore log messages written
			List<LogEntry> logEntries = logCapture.getLogEntries();

			Assert.assertEquals(logEntries.toString(), 7, logEntries.size());

			Assert.assertTrue(
				logEntries.stream().anyMatch( x ->
					x.getMessage().equals(
						"Overrode property upgrade.database.auto.run for companyId 1 with the value from the environment variable LIFERAY_UPGRADE_PERIOD_DATABASE_PERIOD_AUTO_PERIOD_RUN")));
			Assert.assertTrue(
				logEntries.stream().anyMatch( x ->
					x.getMessage().equals(
						"Overrode property key1 for companyId 1 with the value from the environment variable LIFERAY_PROPS")));
			Assert.assertTrue(
				logEntries.stream().anyMatch( x ->
					x.getMessage().equals(
						"Overrode property key2 for companyId 1 with the value from the environment variable LIFERAY_PROPS")));
			Assert.assertTrue(
				logEntries.stream().anyMatch( x ->
					x.getMessage().equals(
						"Overrode property upgrade.database.auto.run for companyId 1 with the value from the environment variable LIFERAY_PROPS")));
			Assert.assertTrue(
				logEntries.stream().anyMatch( x ->
					x.getMessage().equals(
						"Overrode property key1 for companyId 1 with the value from the environment variable LIFERAY_PROPS_BY_COMPANY_1")));
			Assert.assertTrue(
				logEntries.stream().anyMatch( x ->
					x.getMessage().equals(
						"Overrode property key4 for companyId 1 with the value from the environment variable LIFERAY_PROPS_BY_COMPANY_1")));
			Assert.assertTrue(
				logEntries.stream().anyMatch( x ->
					x.getMessage().equals(
						"Overrode property upgrade.database.auto.run for companyId 1 with the value from the environment variable LIFERAY_PROPS_BY_COMPANY_1")));
		}

		// companyId = 12345 => allCompanies, byCompanyId_12345, decoded

		try (LogCapture logCapture = LoggerTestUtil.configureJDKLogger(
			EnvPropertiesUtil.class.getName(), Level.INFO)) {

			// when
			SimpleBiConsumer<String, String> companyConsumer = new SimpleBiConsumer<>();

			_loadEnvOverrides(
				"LIFERAY_", 12345, companyConsumer, env);

			if (_PRINT_LOG_ENTRIES_TO_STDOUT) {
				System.out.println("test_loadEnvOverrides_simple#2");

				logCapture.getLogEntries().stream().forEach(x ->
					System.out.println(x.getPriority() + " " + x.getMessage()));
			}

			// then
			Assert.assertEquals(
				4, companyConsumer.getConsumedItems().size());
			Assert.assertEquals(
				"true", companyConsumer.getConsumedItems().get("upgrade.database.auto.run"));
			Assert.assertEquals(
				"value1_overB", companyConsumer.getConsumedItems().get("key1"));
			Assert.assertEquals(
				"value2", companyConsumer.getConsumedItems().get("key2"));
			Assert.assertEquals(
				"value6", companyConsumer.getConsumedItems().get("key6"));

			List<LogEntry> logEntries = logCapture.getLogEntries();

			Assert.assertEquals(logEntries.toString(), 5, logEntries.size());

			Assert.assertTrue(
				logEntries.stream().anyMatch(x ->
					x.getMessage().equals(
						"Overrode property upgrade.database.auto.run for companyId 12345 with the value from the environment variable LIFERAY_UPGRADE_PERIOD_DATABASE_PERIOD_AUTO_PERIOD_RUN")));
			Assert.assertTrue(
				logEntries.stream().anyMatch( x ->
					x.getMessage().equals(
						"Overrode property key1 for companyId 12345 with the value from the environment variable LIFERAY_PROPS")));
			Assert.assertTrue(
				logEntries.stream().anyMatch( x ->
					x.getMessage().equals(
						"Overrode property key2 for companyId 12345 with the value from the environment variable LIFERAY_PROPS")));
			Assert.assertTrue(
				logEntries.stream().anyMatch( x ->
					x.getMessage().equals(
						"Overrode property key1 for companyId 12345 with the value from the environment variable LIFERAY_PROPS_BY_COMPANY_12345")));
			Assert.assertTrue(
				logEntries.stream().anyMatch( x ->
					x.getMessage().equals(
						"Overrode property key6 for companyId 12345 with the value from the environment variable LIFERAY_PROPS_BY_COMPANY_12345")));
		}

		// companyId = 20048 => allCompanies, byCompanyId_20048, decoded

		try (LogCapture logCapture = LoggerTestUtil.configureJDKLogger(
			EnvPropertiesUtil.class.getName(), Level.INFO)) {

			// when
			SimpleBiConsumer<String, String> companyConsumer = new SimpleBiConsumer<>();

			_loadEnvOverrides(
				"LIFERAY_", 20048, companyConsumer, env);

			if (_PRINT_LOG_ENTRIES_TO_STDOUT) {
				System.out.println("test_loadEnvOverrides_simple#3");

				logCapture.getLogEntries().stream().forEach(x ->
					System.out.println(x.getPriority() + " " + x.getMessage()));
			}

			// then
			Assert.assertEquals(
				4, companyConsumer.getConsumedItems().size());
			Assert.assertEquals(
				"true", companyConsumer.getConsumedItems().get("upgrade.database.auto.run"));
			Assert.assertEquals(
				"value1_overC", companyConsumer.getConsumedItems().get("key1"));
			Assert.assertEquals(
				"value2", companyConsumer.getConsumedItems().get("key2"));
			Assert.assertEquals(
				"value8", companyConsumer.getConsumedItems().get("key8"));


			List<LogEntry> logEntries = logCapture.getLogEntries();

			Assert.assertEquals(logEntries.toString(), 5, logEntries.size());

			Assert.assertTrue(
				logEntries.stream().anyMatch( x ->
					x.getMessage().equals(
						"Overrode property upgrade.database.auto.run for companyId 20048 with the value from the environment variable LIFERAY_UPGRADE_PERIOD_DATABASE_PERIOD_AUTO_PERIOD_RUN")));
			Assert.assertTrue(
				logEntries.stream().anyMatch( x ->
					x.getMessage().equals(
						"Overrode property key1 for companyId 20048 with the value from the environment variable LIFERAY_PROPS")));
			Assert.assertTrue(
				logEntries.stream().anyMatch( x ->
					x.getMessage().equals(
						"Overrode property key2 for companyId 20048 with the value from the environment variable LIFERAY_PROPS")));
			Assert.assertTrue(
				logEntries.stream().anyMatch( x ->
					x.getMessage().equals(
						"Overrode property key1 for companyId 20048 with the value from the environment variable LIFERAY_PROPS_BY_COMPANY_20048")));
			Assert.assertTrue(
				logEntries.stream().anyMatch( x ->
					x.getMessage().equals(
						"Overrode property key8 for companyId 20048 with the value from the environment variable LIFERAY_PROPS_BY_COMPANY_20048")));
		}

		// companyId = 3 => allCompanies, decoded
		try (LogCapture logCapture = LoggerTestUtil.configureJDKLogger(
			EnvPropertiesUtil.class.getName(), Level.INFO)) {

			// when
			SimpleBiConsumer<String, String> companyConsumer = new SimpleBiConsumer<>();

			_loadEnvOverrides(
				"LIFERAY_", 3, companyConsumer, env);

			if (_PRINT_LOG_ENTRIES_TO_STDOUT) {
				System.out.println("test_loadEnvOverrides_simple#4");

				logCapture.getLogEntries().stream().forEach(x ->
					System.out.println(x.getPriority() + " " + x.getMessage()));
			}

			// then
			Assert.assertEquals(3, companyConsumer.getConsumedItems().size());
			Assert.assertEquals(
				"true", companyConsumer.getConsumedItems().get("upgrade.database.auto.run"));
			Assert.assertEquals(
				"value1", companyConsumer.getConsumedItems().get("key1"));
			Assert.assertEquals(
				"value2", companyConsumer.getConsumedItems().get("key2"));


			List<LogEntry> logEntries = logCapture.getLogEntries();

			Assert.assertEquals(logEntries.toString(), 3, logEntries.size());

			Assert.assertTrue(
				logEntries.stream().anyMatch( x ->
					x.getMessage().equals(
						"Overrode property upgrade.database.auto.run for companyId 3 with the value from the environment variable LIFERAY_UPGRADE_PERIOD_DATABASE_PERIOD_AUTO_PERIOD_RUN")));
			Assert.assertTrue(
				logEntries.stream().anyMatch( x ->
					x.getMessage().equals(
						"Overrode property key1 for companyId 3 with the value from the environment variable LIFERAY_PROPS")));
			Assert.assertTrue(
				logEntries.stream().anyMatch( x ->
					x.getMessage().equals(
						"Overrode property key2 for companyId 3 with the value from the environment variable LIFERAY_PROPS")));
		}
	}

	@Test
	public void test_loadEnvOverrides_multipleEnvVarsForCompany() {
		// assuming
		Map<String, String> env = TreeMapBuilder
			.put("OTHER_KEY", "other_value")
			.put("LIFERAY_PROPS", "key1=value1\nkey2=value2")
			.put("LIFERAY_PROPS_2", "key1=value1_overB\nkey2=value2")
			.put("LIFERAY_PROPS_10", "key1=value1_overA\nkey2=value2")
			.put("LIFERAY_PROPS_BY_COMPANY_2", "key3=value3\nkey4=value4")
			.put("LIFERAY_PROPS_BY_COMPANY_2_2", "key3=value3_overB\nkey4=value4")
			.put("LIFERAY_PROPS_BY_COMPANY_2_10", "key3=value3_overA\nkey4=value4")
			.put("LIFERAY_PROPS_BY_COMPANY_234", "key3=value3_overC\nkey4=value4")
			.put("LIFERAY_PROPS_BY_COMPANY_30096", "key3=value3_overD\nkey4=value4")
			.put("LIFERAY_UPGRADE_PERIOD_DATABASE_PERIOD_AUTO_PERIOD_RUN", "true_C")
			.build();

		// companyId = 2 => allCompanies, byCompanyId_2, decoded

		try (LogCapture logCapture = LoggerTestUtil.configureJDKLogger(
			EnvPropertiesUtil.class.getName(), Level.INFO)) {

			// when
			SimpleBiConsumer<String, String> companyConsumer = new SimpleBiConsumer<>();

			_loadEnvOverrides(
				"LIFERAY_", 2, companyConsumer, env);

			if (_PRINT_LOG_ENTRIES_TO_STDOUT) {
				System.out.println("test_loadEnvOverrides_multipleEnvVarsForCompany#1");

				logCapture.getLogEntries().stream().forEach(x ->
					System.out.println(x.getPriority() + " " + x.getMessage()));
			}

			// then
			Assert.assertEquals(5, companyConsumer.getConsumedItems().size());
			Assert.assertEquals(
				"value1", companyConsumer.getConsumedItems().get("key1"));
			Assert.assertEquals(
				"value2", companyConsumer.getConsumedItems().get("key2"));
			Assert.assertEquals(
				"value3", companyConsumer.getConsumedItems().get("key3"));
			Assert.assertEquals(
				"value4", companyConsumer.getConsumedItems().get("key4"));
			Assert.assertEquals(
				"true_C", companyConsumer.getConsumedItems().get(
					"upgrade.database.auto.run"));

			List<LogEntry> logEntries = logCapture.getLogEntries();

			Assert.assertEquals(logEntries.toString(), 5, logEntries.size());

			Assert.assertTrue(
				logEntries.stream().anyMatch( x ->
					x.getMessage().equals(
						"Overrode property upgrade.database.auto.run for companyId 2 with the value from the environment variable LIFERAY_UPGRADE_PERIOD_DATABASE_PERIOD_AUTO_PERIOD_RUN")));

			Assert.assertTrue(
				logEntries.stream().anyMatch( x ->
					x.getMessage().equals(
						"Overrode property key1 for companyId 2 with the value from the environment variable LIFERAY_PROPS")));
			Assert.assertTrue(
				logEntries.stream().anyMatch( x ->
					x.getMessage().equals(
						"Overrode property key2 for companyId 2 with the value from the environment variable LIFERAY_PROPS")));

			Assert.assertTrue(
				logEntries.stream().anyMatch( x ->
					x.getMessage().equals(
						"Overrode property key3 for companyId 2 with the value from the environment variable LIFERAY_PROPS_BY_COMPANY_2")));
			Assert.assertTrue(
				logEntries.stream().anyMatch( x ->
					x.getMessage().equals(
						"Overrode property key4 for companyId 2 with the value from the environment variable LIFERAY_PROPS_BY_COMPANY_2")));
		}
	}

	@Test
	public void test_loadEnvOverrides_noEnvVarsForCompany() {
		// assuming
		Map<String, String> env = TreeMapBuilder
			.put("OTHER_KEY", "other_value")
			.put("LIFERAY_PROPS_BY_COMPANY_1", "key1=value1\nkey2=value2\nupgrade.database.auto.run=true_B")
			.put("LIFERAY_UPGRADE_PERIOD_DATABASE_PERIOD_AUTO_PERIOD_RUN", "true_C")
			.build();

		// companyId = 1 => byCompanyId_1, decoded

		try (LogCapture logCapture = LoggerTestUtil.configureJDKLogger(
			EnvPropertiesUtil.class.getName(), Level.INFO)) {

			// when
			SimpleBiConsumer<String, String> companyConsumer = new SimpleBiConsumer<>();

			_loadEnvOverrides(
				"LIFERAY_", 1, companyConsumer, env);

			if (_PRINT_LOG_ENTRIES_TO_STDOUT) {
				System.out.println("test_loadEnvOverrides_noEnvVarsForCompany#1");

				logCapture.getLogEntries().stream().forEach(x ->
					System.out.println(x.getPriority() + " " + x.getMessage()));
			}

			// then
			Assert.assertEquals(3, companyConsumer.getConsumedItems().size());
			Assert.assertEquals(
				"true_C", companyConsumer.getConsumedItems().get(
					"upgrade.database.auto.run"));
			Assert.assertEquals(
				"value1", companyConsumer.getConsumedItems().get(
					"key1"));
			Assert.assertEquals(
				"value2", companyConsumer.getConsumedItems().get(
					"key2"));

			List<LogEntry> logEntries = logCapture.getLogEntries();

			Assert.assertEquals(logEntries.toString(), 4, logEntries.size());

			Assert.assertTrue(
				logEntries.stream().anyMatch( x ->
					x.getMessage().equals(
						"Overrode property upgrade.database.auto.run for companyId 1 with the value from the environment variable LIFERAY_UPGRADE_PERIOD_DATABASE_PERIOD_AUTO_PERIOD_RUN")));
			Assert.assertTrue(
				logEntries.stream().anyMatch( x ->
					x.getMessage().equals(
						"Overrode property key1 for companyId 1 with the value from the environment variable LIFERAY_PROPS_BY_COMPANY_1")));
			Assert.assertTrue(
				logEntries.stream().anyMatch( x ->
					x.getMessage().equals(
						"Overrode property key2 for companyId 1 with the value from the environment variable LIFERAY_PROPS_BY_COMPANY_1")));
			Assert.assertTrue(
				logEntries.stream().anyMatch( x ->
					x.getMessage().equals(
						"Overrode property upgrade.database.auto.run for companyId 1 with the value from the environment variable LIFERAY_PROPS_BY_COMPANY_1")));
		}

		// companyId = 2 => decoded

		try (LogCapture logCapture = LoggerTestUtil.configureJDKLogger(
			EnvPropertiesUtil.class.getName(), Level.INFO)) {

			// when
			SimpleBiConsumer<String, String> companyConsumer = new SimpleBiConsumer<>();

			_loadEnvOverrides(
				"LIFERAY_", 2, companyConsumer, env);

			if (_PRINT_LOG_ENTRIES_TO_STDOUT) {
				System.out.println("test_loadEnvOverrides_noEnvVarsForCompany#2");

				logCapture.getLogEntries().stream().forEach(x ->
					System.out.println(x.getPriority() + " " + x.getMessage()));
			}

			// then
			Assert.assertEquals(1, companyConsumer.getConsumedItems().size());
			Assert.assertEquals(
				"true_C", companyConsumer.getConsumedItems().get(
					"upgrade.database.auto.run"));
			
			List<LogEntry> logEntries = logCapture.getLogEntries();

			Assert.assertEquals(logEntries.toString(), 1, logEntries.size());

			Assert.assertTrue(
				logEntries.stream().anyMatch( x ->
					x.getMessage().equals(
						"Overrode property upgrade.database.auto.run for companyId 2 with the value from the environment variable LIFERAY_UPGRADE_PERIOD_DATABASE_PERIOD_AUTO_PERIOD_RUN")));
		}
	}

	@Test
	public void test_loadEnvOverrides_otherEnvPrefix() {
		// assuming
		Map<String, String> env = TreeMapBuilder
			.put("OTHER_KEY", "other_value")
			.put("ACME_PROPS", "key1=value1\nkey2=value2")
			.put("ACME_PROPS_BY_COMPANY_3", "key3=value3\nkey4=value4")
			.put(
				"ACME_UPGRADE_PERIOD_DATABASE_PERIOD_AUTO_PERIOD_RUN",
				"true")
			.build();

		SimpleBiConsumer<String, String> company3Consumer =
			new SimpleBiConsumer<>();

		try (LogCapture logCapture = LoggerTestUtil.configureJDKLogger(
			EnvPropertiesUtil.class.getName(), Level.INFO)) {

			// when
			_loadEnvOverrides(
				"ACME_", 3, company3Consumer, env);

			if (_PRINT_LOG_ENTRIES_TO_STDOUT) {
				System.out.println("test_loadEnvOverrides_otherEnvPrefix#1");

				logCapture.getLogEntries().stream().forEach(x ->
					System.out.println(x.getPriority() + " " + x.getMessage()));
			}

			// then
			Assert.assertEquals(5, company3Consumer.getConsumedItems().size());
			Assert.assertEquals(
				"value1", company3Consumer.getConsumedItems().get("key1"));
			Assert.assertEquals(
				"value2", company3Consumer.getConsumedItems().get("key2"));
			Assert.assertEquals(
				"value3", company3Consumer.getConsumedItems().get("key3"));
			Assert.assertEquals(
				"value4", company3Consumer.getConsumedItems().get("key4"));
			Assert.assertEquals(
				"true", company3Consumer.getConsumedItems().get(
					"upgrade.database.auto.run"));

			List<LogEntry> logEntries = logCapture.getLogEntries();

			Assert.assertEquals(logEntries.toString(), 5, logEntries.size());

			Assert.assertTrue(
				logEntries.stream().anyMatch( x ->
					x.getMessage().equals(
						"Overrode property upgrade.database.auto.run for companyId 3 with the value from the environment variable ACME_UPGRADE_PERIOD_DATABASE_PERIOD_AUTO_PERIOD_RUN")));
			Assert.assertTrue(
				logEntries.stream().anyMatch( x ->
					x.getMessage().equals(
						"Overrode property key1 for companyId 3 with the value from the environment variable ACME_PROPS")));
			Assert.assertTrue(
				logEntries.stream().anyMatch( x ->
					x.getMessage().equals(
						"Overrode property key2 for companyId 3 with the value from the environment variable ACME_PROPS")));
			Assert.assertTrue(
				logEntries.stream().anyMatch( x ->
					x.getMessage().equals(
						"Overrode property key3 for companyId 3 with the value from the environment variable ACME_PROPS_BY_COMPANY_3")));
			Assert.assertTrue(
				logEntries.stream().anyMatch( x ->
					x.getMessage().equals(
						"Overrode property key4 for companyId 3 with the value from the environment variable ACME_PROPS_BY_COMPANY_3")));
		}
	}

	private String _decode(String s) {
		return ReflectionTestUtil.invoke(
			EnvPropertiesUtil.class, "_decode", new Class<?>[] {String.class},
			s);
	}

	private void _loadEnvOverrides(
		String liferayEnvPropsPrefix, long companyId,
		BiConsumer<String, String> biConsumer, Map<String, String> env) {

		ReflectionTestUtil.invoke(
			EnvPropertiesUtil.class, "_loadEnvOverrides",
			new Class<?>[] {String.class, long.class, BiConsumer.class, Map.class},
			liferayEnvPropsPrefix, companyId, biConsumer, env);
	}

	private static final class SimpleBiConsumer<T, U> implements BiConsumer<T, U> {

		private final Map<T, U> _consumedItems = new HashMap<>();

		@Override
		public void accept(T t, U u) {
			_consumedItems.put(t, u);
		}

		public Map<T, U> getConsumedItems() {
			return Collections.unmodifiableMap(_consumedItems);
		}
	}

	public static final boolean _PRINT_LOG_ENTRIES_TO_STDOUT = true;

}