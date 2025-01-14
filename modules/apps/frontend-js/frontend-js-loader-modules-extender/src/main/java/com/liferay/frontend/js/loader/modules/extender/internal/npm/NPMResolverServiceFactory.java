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

package com.liferay.frontend.js.loader.modules.extender.internal.npm;

import com.liferay.frontend.js.loader.modules.extender.npm.NPMRegistry;
import com.liferay.frontend.js.loader.modules.extender.npm.NPMResolver;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.util.StringUtil;

import java.io.IOException;

import java.net.URL;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

/**
 * @author Iván Zaera Avellón
 */
public class NPMResolverServiceFactory implements ServiceFactory<NPMResolver> {

	public NPMResolverServiceFactory(
		JSONFactory jsonFactory, NPMRegistry npmRegistry) {

		_jsonFactory = jsonFactory;
		_npmRegistry = npmRegistry;
	}

	@Override
	public NPMResolver getService(
		Bundle bundle, ServiceRegistration<NPMResolver> serviceRegistration) {

		URL packageURL = bundle.getEntry("META-INF/resources/package.json");

		if (packageURL == null) {
			return new InvalidNPMResolverImpl(bundle);
		}

		JSONObject packageJSONObject = _createJSONObject(packageURL);

		URL manifestURL = bundle.getEntry("META-INF/resources/manifest.json");

		if (manifestURL == null) {
			return new UnsupportedNPMResolverImpl(bundle);
		}

		JSONObject manifestJSONObject = _createJSONObject(manifestURL);

		JSONObject packagesJSONObject = manifestJSONObject.getJSONObject(
			"packages");

		if (packagesJSONObject == null) {
			return new UnsupportedNPMResolverImpl(bundle);
		}

		return new NPMResolverImpl(
			bundle, _npmRegistry, packageJSONObject, packagesJSONObject);
	}

	@Override
	public void ungetService(
		Bundle bundle, ServiceRegistration<NPMResolver> serviceRegistration,
		NPMResolver npmResolver) {
	}

	private JSONObject _createJSONObject(URL url) {
		try {
			return _jsonFactory.createJSONObject(
				StringUtil.read(url.openStream()));
		}
		catch (IOException | JSONException exception) {
			throw new RuntimeException("Unable to read " + url, exception);
		}
	}

	private final JSONFactory _jsonFactory;
	private final NPMRegistry _npmRegistry;

}