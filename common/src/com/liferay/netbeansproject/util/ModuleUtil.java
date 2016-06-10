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

package com.liferay.netbeansproject.util;

import com.liferay.netbeansproject.container.Module;

import java.io.IOException;
import java.io.Writer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.Properties;

/**
 * @author Tom Wang
 */
public class ModuleUtil {

	public static void createModuleInfo(Module module, Path projectPath)
		throws IOException {

		Properties properties = new Properties();

		Path modulePath = module.getModulePath();

		_putProperty(properties, "ModulePath", modulePath);
		_putProperty(properties, "SourcePath", module.getSourcePath());
		_putProperty(
			properties, "SourceResourcePath", module.getSourceResourcePath());
		_putProperty(properties, "TestUnitPath", module.getTestUnitPath());
		_putProperty(
			properties, "TestUnitResourcePath",
			module.getTestUnitResourcePath());
		_putProperty(
			properties, "TestIntegrationPath", module.getTestIntegrationPath());
		_putProperty(
			properties, "TestIntegrationResourcePath",
			module.getTestIntegrationResourcePath());

		Path gradleFilePath = modulePath.resolve("build.gradle");

		try {
			if (Files.exists(gradleFilePath)) {
				MessageDigest messageDigest = MessageDigest.getInstance("MD5");

				byte[] hash = messageDigest.digest(
					Files.readAllBytes(gradleFilePath));

				properties.put("checksum", StringUtil.bytesToHexString(hash));
			}
		}
		catch (NoSuchAlgorithmException nsae) {
			throw new Error(nsae);
		}

		Files.createDirectories(projectPath);

		try (Writer writer = Files.newBufferedWriter(
				projectPath.resolve("ModuleInfo.properties"),
				StandardCharsets.UTF_8)) {

			properties.store(writer, null);
		}
	}

	public static String getModuleName(Path modulePath) {
		Path moduleName = modulePath.getFileName();

		if ("WEB-INF".equals(moduleName.toString())) {
			moduleName = modulePath.getName(modulePath.getNameCount() - 3);
		}

		return moduleName.toString();
	}

	private static void _putProperty(
		Properties properties, String name, Object value) {

		if (value != null) {
			properties.put(name, String.valueOf(value));
		}
	}

}