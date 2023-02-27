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

import com.liferay.petra.reflect.ReflectionUtil;
import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * @author Jiaxu Wei
 * @author Josef Sustacek
 */
public class EnvPropertiesUtil {

	public static void loadEnvOverrides(
		String liferayEnvPropsPrefix, long companyId,
		BiConsumer<String, String> biConsumer) {

		Map<String, String> env = System.getenv();

		_loadEnvOverrides(liferayEnvPropsPrefix, companyId, biConsumer, env);
	}

	private static void _loadEnvOverrides(
			String liferayEnvPropsPrefix, long companyId,
			BiConsumer<String, String> biConsumer, Map<String, String> env) {

		String allCompaniesEnvPropKey = liferayEnvPropsPrefix.concat(
			_PROPS_FOR_ALL_COMPANIES);
		String byCompanyEnvPrefix =
			liferayEnvPropsPrefix.concat(_PROPS_BY_COMPANY);
		String byCompanyEnvPropKey =
			byCompanyEnvPrefix.concat(String.valueOf(companyId));

		// <liferayEnvPropsPrefix>_PROPS=key1=value1\nkey2=value2
		// only exact match, no suffixes supported:
		// 		LIFERAY_PROPS=key1=value1\nkey2=value2
		Optional<Map.Entry<String, String>> allCompaniesEnvProp =
			Optional.ofNullable(
				env.containsKey(allCompaniesEnvPropKey)
					? new AbstractMap.SimpleEntry<>(
						allCompaniesEnvPropKey, env.get(allCompaniesEnvPropKey))
					: null);
			

		// <liferayEnvPropsPrefix>_PROPS_BY_COMPANY_<company_id>*=key1=value1\nkey2=value2
		// only exact match, no suffixes supported:
		// 		LIFERAY_PROPS_BY_COMPANY_10025=key1=value1\nkey2=value2
		Optional<Map.Entry<String, String>> givenCompanyEnvProp =
			Optional.ofNullable(
				env.containsKey(byCompanyEnvPropKey)
					? new AbstractMap.SimpleEntry<>(
						byCompanyEnvPropKey, env.get(byCompanyEnvPropKey))
					: null);

		// <liferayEnvPropsPrefix>_<encoded_key1>=value1
		// 		LIFERAY_<encoded_key1>=value1
		// 		LIFERAY_<encoded_key2>=value2
		Map<String, String> decodableEnvProps =
			env.entrySet().stream()
				.filter(x ->
					x.getKey().startsWith(liferayEnvPropsPrefix)
					&& !x.getKey().startsWith(allCompaniesEnvPropKey)
					&& !x.getKey().startsWith(byCompanyEnvPrefix))
				.collect(
					Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		// the order is important, multiple ENV vars might configure the same Liferay portal / system property

		_loadCompanyCompositeOverrides(
			allCompaniesEnvProp, companyId, biConsumer);
		_loadCompanyCompositeOverrides(
			givenCompanyEnvProp, companyId, biConsumer);
		_loadDecodableOverrides(
			decodableEnvProps, liferayEnvPropsPrefix, companyId, biConsumer);
	}

	private static void _loadCompanyCompositeOverrides(
		Optional<Map.Entry<String, String>> companyPropertiesEnvProp,
		long companyId, BiConsumer<String, String> biConsumer) {

		if (companyPropertiesEnvProp.isPresent()) {
			String envPropKey = companyPropertiesEnvProp.get().getKey();
			String envPropValue = companyPropertiesEnvProp.get().getValue();

			try {
				Properties properties = PropertiesUtil.load(envPropValue);

				properties.forEach(
					(portalPropKey, portalPropValue) ->  {
						biConsumer.accept(
							String.valueOf(portalPropKey),
							String.valueOf(portalPropValue));

						if (_log.isInfoEnabled()) {
							_log.info(
								StringBundler.concat(
									"Overrode property ",
									String.valueOf(portalPropKey),
									" for companyId ", companyId,
									" with the value from the environment variable ",
									envPropKey));
						}
					});
			}
			catch (IOException ioException) {
				ReflectionUtil.throwException(ioException);
			}
		}
	}

	private static void _loadDecodableOverrides(
		Map<String, String> decodableEnvVars,
		String liferayEnvPropsPrefix, long companyId,
		BiConsumer<String, String> biConsumer) {

		for (Map.Entry<String, String> entry : decodableEnvVars.entrySet()) {
			String envPropKey = entry.getKey();
			String envPropValue = entry.getValue();

			String decodedPortalPropKey = _decode(
				StringUtil.toLowerCase(
					envPropKey.substring(liferayEnvPropsPrefix.length())));

			if (decodedPortalPropKey.equals("include-and-override")) {
				continue;
			}

			biConsumer.accept(decodedPortalPropKey, envPropValue);

			if (_log.isInfoEnabled()) {
				_log.info(
					StringBundler.concat(
						"Overrode property ", decodedPortalPropKey,
						" for companyId ", companyId,
						" with the value from the environment variable ",
						envPropKey));
			}
		}
	}

	private static String _decode(String s) {
		int index = -1;
		int openUnderLine = -1;
		int position = 0;
		StringBundler sb = new StringBundler();

		while ((index = s.indexOf(CharPool.UNDERLINE, index + 1)) != -1) {
			if (openUnderLine == -1) {
				sb.append(s.substring(position, index));

				openUnderLine = index;
				position = index;

				continue;
			}

			String encoded = s.substring(openUnderLine + 1, index);

			Character character = _charPoolChars.get(
				StringUtil.toUpperCase(encoded));

			if (character == null) {
				int value = GetterUtil.get(encoded, -1);

				if (Character.isDefined(value)) {
					sb.append(new String(Character.toChars(value)));
				}
				else {
					if (_log.isWarnEnabled()) {
						_log.warn(
							StringBundler.concat(
								"Unable to decode part \"", encoded,
								"\" from \"", s, "\", preserve it literally"));
					}

					sb.append(s.substring(openUnderLine, index + 1));
				}
			}
			else {
				sb.append(character);
			}

			openUnderLine = -1;
			position = index + 1;
		}

		sb.append(s.substring(position));

		return sb.toString();
	}

	private static Map<String, Character> _getCharPoolChars() {
		try {
			Map<String, Character> charPoolChars = new HashMap<>();

			for (Field field : CharPool.class.getFields()) {
				if (Modifier.isStatic(field.getModifiers()) &&
					(field.getType() == char.class)) {

					charPoolChars.put(
						StringUtil.removeChar(
							field.getName(), CharPool.UNDERLINE),
						field.getChar(null));
				}
			}

			return charPoolChars;
		}
		catch (Exception exception) {
			throw new ExceptionInInitializerError(exception);
		}
	}

	private static final String _PROPS_BY_COMPANY = "PROPS_BY_COMPANY_";
	private static final String _PROPS_FOR_ALL_COMPANIES = "PROPS";

	private static final Log _log = LogFactoryUtil.getLog(
		EnvPropertiesUtil.class);

	private static final Map<String, Character> _charPoolChars =
		_getCharPoolChars();

}