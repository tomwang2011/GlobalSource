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

package com.liferay.netbeansproject.groupedproject;

import com.liferay.netbeansproject.container.Module;
import com.liferay.netbeansproject.container.Module.JarDependency;
import com.liferay.netbeansproject.container.Module.ModuleDependency;
import com.liferay.netbeansproject.resolvers.ProjectDependencyResolver;
import com.liferay.netbeansproject.resolvers.ProjectDependencyResolverImpl;
import com.liferay.netbeansproject.util.StringUtil;
import com.liferay.netbeansproject.util.ZipUtil;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
/**
 * @author tom
 */
public class GroupProjectCreator {

	public static void createGroupProject(
			Map<Path, Map<String, Module>> projectMap, Properties properties)
		throws IOException {

		Path portalDirPath = Paths.get(properties.getProperty("portal.dir"));

		ProjectDependencyResolver projectDependencyResolver =
			new ProjectDependencyResolverImpl(projectMap, portalDirPath);

		for (Path groupPath : projectMap.keySet()) {
			if (!groupPath.equals(portalDirPath)) {
				_createGroupModule(
					projectMap, groupPath, projectDependencyResolver,
					properties);
			}
		}
	}

	private static void _appendDependencyJar(Path jarPath, StringBuilder sb) {
		sb.append("\t");
		sb.append(jarPath);
		sb.append(":\\\n");
	}

	private static void _appendProjectDependencies(
		String moduleName, StringBuilder sb, StringBuilder javacSB) {

		sb.append("project.");
		sb.append(moduleName);
		sb.append("=");

		Path path = Paths.get("..", moduleName);

		sb.append(path);
		sb.append("\n");
		sb.append("reference.");
		sb.append(moduleName);
		sb.append(".jar=${project.");
		sb.append(moduleName);

		path = Paths.get("}", "dist", moduleName + ".jar");

		sb.append(path);
		sb.append("\n");

		javacSB.append("\t${reference.");
		javacSB.append(moduleName);
		javacSB.append(".jar}:\\\n");
	}

	private static void _appendSourcePath(
		Map<String, Module> moduleMap, StringBuilder projectSB) {

		for (Module module : moduleMap.values()) {
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
			projectSB.append(
				_appendSourcePathIndividual(
					path, prefix, name, subfix));
		}
	}

	private static void _createGroupModule(
			Map<Path, Map<String, Module>> projectMap, Path groupPath,
			ProjectDependencyResolver projectDependencyResolver,
			Properties properties)
		throws IOException {

		Path projectDirPath = Paths.get(properties.getProperty("project.dir"));

		Path modulesDirPath = projectDirPath.resolve("group-modules");

		Path groupName = groupPath.getFileName();

		ZipUtil.unZip(
			Paths.get("CleanProject.zip"), modulesDirPath.resolve(groupName));

		_replaceProjectName(groupName, modulesDirPath);

		Set<Path> solvedSet = new HashSet<>();

		solvedSet.add(groupPath);

		_prepareProjectPropertyFile(
			projectMap, groupName, groupPath, modulesDirPath,
			projectDependencyResolver, properties, solvedSet);
	}

	private static void _prepareProjectPropertyFile(
			Map<Path, Map<String, Module>> projectMap, Path groupName,
			Path groupPath, Path modulesDirPath,
			ProjectDependencyResolver projectDependencyResolver,
			Properties properties, Set<Path> solvedSet)
		throws IOException {

		StringBuilder projectSB = new StringBuilder();

		projectSB.append("excludes=");
		projectSB.append(properties.getProperty("exclude.types"));
		projectSB.append("\n");

		projectSB.append("application.title=");
		projectSB.append(groupPath);
		projectSB.append("\n");

		projectSB.append("dist.jar=${dist.dir}/");
		projectSB.append(groupName);
		projectSB.append(".jar\n");

		Path projectPropertiesPath =
			Paths.get(
				modulesDirPath.toString(), groupName.toString(), "nbproject",
				"project.properties");

		_appendSourcePath(projectMap.get(groupPath), projectSB);

		StringBuilder javacSB = new StringBuilder("javac.classpath=\\\n");
		StringBuilder testSB = new StringBuilder(
			"javac.test.classpath=\\\n");

		testSB.append("\t${build.classes.dir}:\\\n");
		testSB.append("\t${javac.classpath}:\\\n");

		Map<Path, Boolean> solvedJars = new HashMap<>();

		_resolveDependencyJarSet(
			projectMap.get(groupPath), solvedJars, javacSB, testSB);

		Queue<ModuleDependency> projectDependencyQueue = new LinkedList<>();

		Map<String, Module> moduleMap = projectMap.get(groupPath);

		for (Module module : moduleMap.values()) {
			projectDependencyQueue.addAll(module.getModuleDependencies());
		}

		ModuleDependency moduleDependency = null;

		while ((moduleDependency = projectDependencyQueue.poll()) != null) {
			Module dependencyModule =
				moduleDependency.getModule(projectDependencyResolver);

			Path dependencyModulePath = dependencyModule.getModulePath();

			Path dependencyGroupPath = dependencyModulePath.getParent();

			if (!solvedSet.contains(dependencyGroupPath)) {
				Map<String, Module> dependencyGroupModuleMap = projectMap.get(
					dependencyGroupPath);

				for (
					Module dependencyGroupModule :
					dependencyGroupModuleMap.values()) {

					projectDependencyQueue.addAll(
						dependencyGroupModule.getModuleDependencies());
				}

				Path dependencyGroupPathName =
					dependencyGroupPath.getFileName();

				if (moduleDependency.isTest()) {
					_appendProjectDependencies(
						dependencyGroupPathName.toString(), projectSB,
						testSB);
				}
				else {
					_appendProjectDependencies(
						dependencyGroupPathName.toString(), projectSB,
						javacSB);
				}

				_resolveDependencyJarSet(
					projectMap.get(dependencyGroupPath), solvedJars, javacSB,
					testSB);
			}

			solvedSet.add(dependencyGroupPath);
		}

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

	private static void _replaceProjectName(Path moduleName, Path modulesDir)
		throws IOException {

		Path modulePath = modulesDir.resolve(moduleName);

		Path buildXmlPath = modulePath.resolve("build.xml");

		String content = new String(Files.readAllBytes(buildXmlPath));

		content = StringUtil.replace(
			content, "%placeholder%", moduleName.toString());

		Files.write(buildXmlPath, content.getBytes());
	}

	private static void _resolveDependencyJarSet(
		Map<String, Module> moduleMap, Map<Path, Boolean> solvedJars,
		StringBuilder projectSB, StringBuilder testSB) {

		for (Module module : moduleMap.values()) {
			for (
				JarDependency jarDependency :
				module.getModuleJarDependencies()) {

				Path jarDependencyPath = jarDependency.getJarPath();

				Boolean jarDependencyTest = jarDependency.isTest();
				Boolean isTest = solvedJars.get(jarDependency.getJarPath());

				if (isTest == null) {
					if (jarDependencyTest) {
						_appendDependencyJar(jarDependencyPath, testSB);
					}
					else {
						_appendDependencyJar(jarDependencyPath, projectSB);
					}

					solvedJars.put(jarDependencyPath, jarDependencyTest);
				}
				else if (isTest == true && jarDependencyTest == false) {
					_appendDependencyJar(jarDependencyPath, projectSB);

					solvedJars.put(jarDependencyPath, false);
				}
			}
		}
	}

}