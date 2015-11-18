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

package com.liferay.netbeansproject.individualmoduleproject;

import com.liferay.netbeansproject.container.Module;
import com.liferay.netbeansproject.container.Module.JarDependency;
import com.liferay.netbeansproject.util.StringUtil;
import com.liferay.netbeansproject.util.ZipUtil;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Tom Wang
 */
public class IndividualModuleProjectCreator {

	public static void createIndividualModuleProject(
			Map<Path, Map<String, Module>> projectMap, Properties properties)
		throws IOException {

		for (Map<String, Module> moduleMap : projectMap.values()) {
			for (Module module : moduleMap.values()) {
				_createModuleProject(module, properties, "modules");
			}
		}
	}

	private static void _appendDependencyJar(Path jarPath, StringBuilder sb) {
		sb.append("\t");
		sb.append(jarPath);
		sb.append(":\\\n");
	}

	private static void _appendSourcePath(
		Module module, StringBuilder projectSB) {

		String moduleName = module.getModuleName();

		_checkPathExists(
			module.getSourcePath(), "src", moduleName, "src", projectSB);
		_checkPathExists(
			module.getSourceResourcePath(), "src", moduleName, "resources",
			projectSB);
		_checkPathExists(
			module.getTestUnitPath(), "test", moduleName, "test-unit",
			projectSB);
		_checkPathExists(
			module.getTestUnitResourcePath(), "test", moduleName,
			"test-unit-resources", projectSB);
		_checkPathExists(
			module.getTestIntegrationPath(), "test", moduleName,
			"test-integration", projectSB);
		_checkPathExists(
			module.getTestIntegrationPath(), "test", moduleName,
			"test-integration-resources", projectSB);
	}

	private static String _appendSourcePathIndividual(
		Path path, String prefix, String name, String subfix) {

		StringBuilder sb = new StringBuilder();

		sb.append("file.reference.");
		sb.append(name);
		sb.append("-");
		sb.append(subfix);
		sb.append("=");
		sb.append(path);
		sb.append("\n");
		sb.append(prefix);
		sb.append(".");
		sb.append(name);
		sb.append(".");
		sb.append(subfix);
		sb.append(".dir=${file.reference.");
		sb.append(name);
		sb.append("-");
		sb.append(subfix);
		sb.append("}\n");

		return sb.toString();
	}

	private static void _checkPathExists(
		Path path, String prefix, String name, String subfix,
		StringBuilder projectSB) {

		if (path != null) {
			projectSB.append(_appendSourcePathIndividual(
				path, prefix, name, subfix));
		}
	}

	private static void _createModuleProject(
			Module module, Properties properties, String moduleFolderName)
		throws IOException {

		Path projectDirPath = Paths.get(properties.getProperty("project.dir"));

		Path modulesDirPath = projectDirPath.resolve(moduleFolderName);

		String moduleName = module.getModuleName();

		ZipUtil.unZip(
			Paths.get("CleanProject.zip"), modulesDirPath.resolve(moduleName));

		_replaceProjectName(module.getModuleName(), modulesDirPath);

		_prepareProjectPropertyFile(module, modulesDirPath, properties);
	}

	private static void _prepareProjectPropertyFile(
			Module module, Path moduleDirPath, Properties properties)
		throws IOException {

		String moduleName = module.getModuleName();

		Path projectPropertiesPath = Paths.get(
			moduleDirPath.toString(), moduleName, "nbproject",
			"project.properties");

		StringBuilder projectSB = new StringBuilder();

		projectSB.append("excludes=");
		projectSB.append(properties.getProperty("exclude.types"));
		projectSB.append("\n");

		projectSB.append("application.title=");
		projectSB.append(module.getModulePath());
		projectSB.append("\n");

		projectSB.append("dist.jar=${dist.dir}");
		projectSB.append(File.separator);
		projectSB.append(moduleName);
		projectSB.append(".jar\n");

		_appendSourcePath(module, projectSB);

		StringBuilder javacSB = new StringBuilder("javac.classpath=\\\n");
		StringBuilder testSB = new StringBuilder(
			"javac.test.classpath=\\\n");

		testSB.append("\t${build.classes.dir}:\\\n");
		testSB.append("\t${javac.classpath}:\\\n");

		Map<Path, Boolean> solvedJars = new HashMap<>();

		_resolveDependencyJarSet(solvedJars, module, javacSB, testSB);

		try (BufferedWriter bufferedWriter = Files.newBufferedWriter(
			projectPropertiesPath, Charset.defaultCharset(),
			StandardOpenOption.APPEND)) {

			bufferedWriter.append(projectSB);
			bufferedWriter.newLine();

			javacSB.setLength(javacSB.length() - 3);

			bufferedWriter.append(javacSB);
			bufferedWriter.newLine();

			testSB.setLength(testSB.length() - 3);

			bufferedWriter.append(testSB);
			bufferedWriter.newLine();
		}
	}

	private static void _replaceProjectName(String moduleName, Path modulesDir)
		throws IOException {

		Path modulePath = modulesDir.resolve(moduleName);

		Path buildXmlPath = modulePath.resolve("build.xml");

		String content = new String(Files.readAllBytes(buildXmlPath));

		content = StringUtil.replace(content, "%placeholder%", moduleName);

		Files.write(buildXmlPath, content.getBytes());
	}

	private static void _resolveDependencyJarSet(
		Map<Path, Boolean> solvedJars, Module module, StringBuilder projectSB,
		StringBuilder testSB) {

		for (JarDependency jarDependency : module.getModuleJarDependencies()) {

			Boolean isTestInSet = solvedJars.get(jarDependency.getJarPath());

			Path jarPath = jarDependency.getJarPath();

			boolean isTest = jarDependency.isTest();

			if (isTestInSet == null) {

				if (isTest) {
					_appendDependencyJar(jarPath, testSB);
				}
				else {
					_appendDependencyJar(jarPath, projectSB);
				}

				solvedJars.put(jarPath, isTest);
			}
			else if (isTestInSet == true && isTest == false) {
				_appendDependencyJar(jarPath, projectSB);

				solvedJars.put(jarPath, isTest);
			}
		}
	}

}