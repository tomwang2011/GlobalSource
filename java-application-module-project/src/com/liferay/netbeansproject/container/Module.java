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

package com.liferay.netbeansproject.container;

import com.liferay.netbeansproject.util.GradleUtil;
import com.liferay.netbeansproject.util.ModuleUtil;
import com.liferay.netbeansproject.util.StringUtil;

import java.io.IOException;
import java.io.Writer;

import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author Tom Wang
 */
public class Module {

	public static Module createModule(
			Path modulePath, List<JarDependency> jarDependencies)
		throws IOException {

		if (jarDependencies == null) {
			jarDependencies = new ArrayList<>();
		}

		Path moduleLibPath = modulePath.resolve("lib");

		if (Files.exists(moduleLibPath)) {
			try (DirectoryStream<Path> directoryStream =
					Files.newDirectoryStream(moduleLibPath, "*.jar")) {

				for (Path jarPath : directoryStream) {
					jarDependencies.add(new JarDependency(jarPath, false));
				}
			}
		}

		String checksum = "";

		Path gradleFilePath = modulePath.resolve("build.gradle");

		if (Files.exists(gradleFilePath)) {
			try {
				MessageDigest messageDigest = MessageDigest.getInstance("MD5");

				byte[] hash = messageDigest.digest(
					Files.readAllBytes(gradleFilePath));

				checksum = StringUtil.bytesToHexString(hash);
			}
			catch (NoSuchAlgorithmException nsae) {
				throw new Error(nsae);
			}
		}

		return new Module(
			modulePath, _resolveSourcePath(modulePath),
			_resolveResourcePath(modulePath, "main"),
			_resolveTestPath(modulePath, true),
			_resolveResourcePath(modulePath, "test"),
			_resolveTestPath(modulePath, false),
			_resolveResourcePath(modulePath, "testIntegration"),
			GradleUtil.getModuleDependencies(modulePath), jarDependencies,
			checksum);
	}

	public static Module createModule(
		Path modulePath, Path sourcePath, Path sourceResourcePath,
		Path testUnitPath, Path testUnitResourcePath, Path testIntegrationPath,
		Path testIntegrationResourcePath, String checksum) {

		return new Module(
			modulePath, sourcePath, sourceResourcePath, testUnitPath,
			testUnitResourcePath, testIntegrationPath,
			testIntegrationResourcePath, null, null, checksum);
	}

	public String getChecksum() {
		return _checksum;
	}

	public List<JarDependency> getJarDependencies() {
		return _jarDependencies;
	}

	public List<ModuleDependency> getModuleDependencies() {
		return _moduleDependencies;
	}

	public String getModuleName() {
		return ModuleUtil.getModuleName(_modulePath);
	}

	public Path getModulePath() {
		return _modulePath;
	}

	public Path getSourcePath() {
		return _sourcePath;
	}

	public Path getSourceResourcePath() {
		return _sourceResourcePath;
	}

	public Path getTestIntegrationPath() {
		return _testIntegrationPath;
	}

	public Path getTestIntegrationResourcePath() {
		return _testIntegrationResourcePath;
	}

	public Path getTestUnitPath() {
		return _testUnitPath;
	}

	public Path getTestUnitResourcePath() {
		return _testUnitResourcePath;
	}

	public void saveToPropertiesFile(Path projectPath) throws IOException {
		Properties properties = new Properties();

		_putProperty(properties, "ModulePath", _modulePath);
		_putProperty(properties, "SourcePath", _sourcePath);
		_putProperty(properties, "SourceResourcePath", _sourceResourcePath);
		_putProperty(properties, "TestUnitPath", _testUnitPath);
		_putProperty(properties, "TestUnitResourcePath", _testUnitResourcePath);
		_putProperty(properties, "TestIntegrationPath", _testIntegrationPath);
		_putProperty(
			properties, "TestIntegrationResourcePath",
			_testIntegrationResourcePath);

		Path gradleFilePath = _modulePath.resolve("build.gradle");

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

	private static void _putProperty(
		Properties properties, String name, Object value) {

		if (value != null) {
			properties.put(name, String.valueOf(value));
		}
	}

	private static Path _resolveResourcePath(Path modulePath, String type) {
		Path resolvedResourcePath = modulePath.resolve(
			Paths.get("src", type, "resources"));

		if (Files.exists(resolvedResourcePath)) {
			return resolvedResourcePath;
		}

		return null;
	}

	private static Path _resolveSourcePath(Path modulePath) {
		Path sourcePath = modulePath.resolve(
			Paths.get("docroot", "WEB-INF", "src"));

		if (Files.exists(sourcePath)) {
			return sourcePath;
		}

		sourcePath = modulePath.resolve(Paths.get("src", "main", "java"));

		if (Files.exists(sourcePath)) {
			return sourcePath;
		}

		sourcePath = modulePath.resolve("src");

		if (Files.exists(sourcePath.resolve("test")) ||
			Files.exists(sourcePath.resolve("testIntegration"))) {

			return null;
		}

		return sourcePath;
	}

	private static Path _resolveTestPath(Path modulePath, boolean unit) {
		Path testPath = null;

		if (unit) {
			testPath = modulePath.resolve(Paths.get("src", "test", "java"));
		}
		else {
			testPath = modulePath.resolve(
				Paths.get("src", "testIntegration", "java"));
		}

		if (Files.exists(testPath)) {
			return testPath;
		}

		if (unit) {
			testPath = modulePath.resolve(Paths.get("test", "unit"));
		}
		else {
			testPath = modulePath.resolve(Paths.get("test", "integration"));
		}

		if (Files.exists(testPath)) {
			return testPath;
		}

		return null;
	}

	private Module(
		Path modulePath, Path sourcePath, Path sourceResourcePath,
		Path testUnitPath, Path testUnitResourcePath, Path testIntegrationPath,
		Path testIntegrationResourcePath,
		List<ModuleDependency> moduleDependencies,
		List<JarDependency> jarDependencies, String checksum) {

		_modulePath = modulePath;
		_sourcePath = sourcePath;
		_sourceResourcePath = sourceResourcePath;
		_testUnitPath = testUnitPath;
		_testUnitResourcePath = testUnitResourcePath;
		_testIntegrationPath = testIntegrationPath;
		_testIntegrationResourcePath = testIntegrationResourcePath;
		_moduleDependencies = moduleDependencies;
		_jarDependencies = jarDependencies;
		_checksum = checksum;
	}

	private final String _checksum;
	private final List<JarDependency> _jarDependencies;
	private final List<ModuleDependency> _moduleDependencies;
	private final Path _modulePath;
	private final Path _sourcePath;
	private final Path _sourceResourcePath;
	private final Path _testIntegrationPath;
	private final Path _testIntegrationResourcePath;
	private final Path _testUnitPath;
	private final Path _testUnitResourcePath;

}