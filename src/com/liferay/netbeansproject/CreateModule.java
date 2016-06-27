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

package com.liferay.netbeansproject;

import com.liferay.netbeansproject.container.Dependency;
import com.liferay.netbeansproject.container.Module;
import com.liferay.netbeansproject.util.FileUtil;
import com.liferay.netbeansproject.util.FreeMarkerUtil;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.Writer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Tom Wang
 */
public class CreateModule {

	public static void createModule(
			Module module, Path projectPath, String excludedTypes,
			String portalLibJars, Path portalPath)
		throws Exception {

		Path projectModulePath = projectPath.resolve(
			Paths.get("modules", module.getModuleName()));

		FileUtil.unZip(projectModulePath);

		_generateBuildXML(module, projectModulePath.resolve("build.xml"));

		_appendProperties(
			module, excludedTypes, portalLibJars, portalPath,
			projectModulePath.resolve("nbproject/project.properties"));

		_createProjectXML(
			module, portalPath.getParent(),
			projectModulePath.resolve("nbproject/project.xml"));
	}

	private static void _appendProjectDependencies(
		String moduleName, StringBuilder projectSB, StringBuilder javacSB) {

		projectSB.append("project.");
		projectSB.append(moduleName);
		projectSB.append("=../");
		projectSB.append(moduleName);
		projectSB.append('\n');
		projectSB.append("reference.");
		projectSB.append(moduleName);
		projectSB.append(".jar=${project.");
		projectSB.append(moduleName);
		projectSB.append("}/dist/");
		projectSB.append(moduleName);
		projectSB.append(".jar\n");

		javacSB.append("\t${reference.");
		javacSB.append(moduleName);
		javacSB.append(".jar}:\\\n");
	}

	private static void _appendProperties(
			Module module, String excludeTypes, String portalLibJars,
			Path portalPath, Path projectPropertiesPath)
		throws Exception {

		String projectName = module.getModuleName();

		StringBuilder projectSB = new StringBuilder();

		projectSB.append("excludes=");
		projectSB.append(excludeTypes);
		projectSB.append('\n');

		projectSB.append("application.title=");
		projectSB.append(module.getModulePath());
		projectSB.append('\n');

		projectSB.append("dist.jar=${dist.dir}/");
		projectSB.append(projectName);
		projectSB.append(".jar\n");

		_appendSourcePaths(module, projectSB);

		StringBuilder javacSB = new StringBuilder("javac.classpath=\\\n");

		StringBuilder testSB = new StringBuilder("javac.test.classpath=\\\n");

		testSB.append("\t${build.classes.dir}:\\\n");
		testSB.append("\t${javac.classpath}:\\\n");

		_resolveJarDependencySet(module, javacSB, testSB);

		_resolveProjectDependencySet(module, projectSB, javacSB, testSB);

		javacSB.append(portalLibJars);

		if (projectName.equals("portal-impl")) {
			projectSB.append("\nfile.reference.portal-test-integration-src=");
			projectSB.append(portalPath);
			projectSB.append("/portal-test-integration/src\n");
			projectSB.append("src.test.dir=");
			projectSB.append("${file.reference.portal-test-integration-src}");
		}

		if (projectName.equals("portal-kernel")) {
			projectSB.append("\nfile.reference.portal-test-src=");
			projectSB.append(portalPath);
			projectSB.append("/portal-test/src\n");
			projectSB.append("src.test.dir=${file.reference.portal-test-src}");
		}

		try (BufferedWriter bufferedWriter = Files.newBufferedWriter(
				projectPropertiesPath, StandardOpenOption.APPEND)) {

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

	private static void _appendSourcePathIndividual(
		Path path, String prefix, String name, String subfix,
		StringBuilder sb) {

		if (path == null) {
			return;
		}

		sb.append("file.reference.");
		sb.append(name);
		sb.append('-');
		sb.append(subfix);
		sb.append('=');
		sb.append(path);
		sb.append('\n');
		sb.append(prefix);
		sb.append('.');
		sb.append(name);
		sb.append('.');
		sb.append(subfix);
		sb.append(".dir=${file.reference.");
		sb.append(name);
		sb.append('-');
		sb.append(subfix);
		sb.append("}\n");
	}

	private static void _appendSourcePaths(
		Module module, StringBuilder projectSB) {

		String moduleName = module.getModuleName();

		_appendSourcePathIndividual(
			module.getSourcePath(), "src", moduleName, "src", projectSB);
		_appendSourcePathIndividual(
			module.getSourceResourcePath(), "src", moduleName, "resources",
			projectSB);
		_appendSourcePathIndividual(
			module.getTestUnitPath(), "test", moduleName, "test-unit",
			projectSB);
		_appendSourcePathIndividual(
			module.getTestUnitResourcePath(), "test", moduleName,
			"test-unit-resources", projectSB);
		_appendSourcePathIndividual(
			module.getTestIntegrationPath(), "test", moduleName,
			"test-integration", projectSB);
		_appendSourcePathIndividual(
			module.getTestIntegrationPath(), "test", moduleName,
			"test-integration-resources", projectSB);
	}

	private static void _createProjectXML(
			Module module, Path portalParentPath, Path projectXMLPath)
		throws Exception {

		Map<String, Object> data = new HashMap<>();

		data.put("module", module);
		data.put(
			"moduleDisplayName",
			portalParentPath.relativize(module.getModulePath()));

		try (Writer writer = new FileWriter(projectXMLPath.toFile())) {
			FreeMarkerUtil.process("resources/projectXML.ftl", data, writer);
		}
	}

	private static void _generateBuildXML(Module module, Path buildXMLPath)
		throws Exception {

		Map<String, String> data = new HashMap<>();

		data.put("projectName", module.getModuleName());

		try (Writer writer = new FileWriter(buildXMLPath.toFile())) {
			FreeMarkerUtil.process("resources/buildXML.ftl", data, writer);
		}
	}

	private static void _resolveJarDependencySet(
		Module module, StringBuilder javacSB, StringBuilder testSB) {

		StringBuilder sb = null;

		for (Dependency dependency : module.getJarDependencies()) {
			Path jarPath = dependency.getPath();

			if (dependency.isTest()) {
				sb = testSB;
			}
			else {
				sb = javacSB;
			}

			sb.append('\t');
			sb.append(jarPath);
			sb.append(":\\\n");
		}
	}

	private static void _resolveProjectDependencySet(
		Module module, StringBuilder projectSB, StringBuilder javacSB,
		StringBuilder testSB) {

		for (Dependency dependency : module.getModuleDependencies()) {
			Path dependencyModulePath = dependency.getPath();

			if (dependency.isTest()) {
				_appendProjectDependencies(
					String.valueOf(dependencyModulePath.getFileName()),
					projectSB, testSB);
			}
			else {
				_appendProjectDependencies(
					String.valueOf(dependencyModulePath.getFileName()),
					projectSB, javacSB);
			}
		}

		for (String moduleName : module.getPortalModuleDependencies()) {
			if (!moduleName.isEmpty()) {
				_appendProjectDependencies(moduleName, projectSB, javacSB);
			}
		}
	}

}