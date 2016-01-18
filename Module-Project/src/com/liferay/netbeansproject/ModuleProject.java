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

import com.liferay.netbeansproject.container.Module.JarDependency;
import com.liferay.netbeansproject.util.PropertiesUtil;
import com.liferay.netbeansproject.util.StringUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author Tom Wang
 */
public class ModuleProject {

	public static void main(String[] args) throws Exception {
		Properties properties = PropertiesUtil.loadProperties(
			Paths.get("build.properties"));

		Path projectDirPath = Paths.get(properties.getProperty("project.dir"));

		_clean(projectDirPath);

		Path portalDirPath = Paths.get(properties.getProperty("portal.dir"));

		Boolean displayGradleOutput = Boolean.valueOf(
			properties.getProperty("display-gradle-process-output"));

		Map<String, List<JarDependency>> jarDependenciesMap =
			_processGradle(displayGradleOutput, portalDirPath, projectDirPath);
	}

	private static void _clean(Path projectDirPath) throws IOException {
		if (Files.exists(projectDirPath)) {
			Files.walkFileTree(
				projectDirPath,
				new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult visitFile(
						Path path, BasicFileAttributes basicFileAttributes)
					throws IOException {

					Files.delete(path);

					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(
						Path path, IOException ioe)
					throws IOException {

					Files.delete(path);

					return FileVisitResult.CONTINUE;
				}
			});

		}
	}

	private static Map<String, List<JarDependency>> _processGradle(
			boolean displayGradleOutput,
			Path portalDirPath, Path projectDirPath)
		throws Exception {

		List<String> gradleTask = new ArrayList<>();

		Path gradlewPath = portalDirPath.resolve("gradlew");

		gradleTask.add(gradlewPath.toString());
		gradleTask.add("--init-script=dependency.gradle");
		gradleTask.add("printDependencies");
		gradleTask.add("-p");

		Path moduleDirPath = portalDirPath.resolve("modules");

		gradleTask.add(moduleDirPath.toString());

		Path dependenciesDirPath = projectDirPath.resolve("dependencies");

		Files.createDirectories(dependenciesDirPath);

		gradleTask.add("-PdependencyDirectory=" + dependenciesDirPath);

		ProcessBuilder processBuilder = new ProcessBuilder(gradleTask);

		Process process = processBuilder.start();

		if (displayGradleOutput) {
			String line;

			try(BufferedReader br = new BufferedReader(new InputStreamReader(
				process.getInputStream()))) {

				while ((line = br.readLine()) != null) {
					System.out.println(line);
				}
			}

			try(BufferedReader br = new BufferedReader(new InputStreamReader(
				process.getErrorStream()))) {

				while ((line = br.readLine()) != null) {
					System.out.println(line);
				}
			}
		}
		else {
			int exitCode = process.waitFor();

			if (exitCode != 0) {
				throw new IOException(
					"Process " + processBuilder.command() + " failed with " +
						exitCode);
			}
		}

		final Map<String, List<JarDependency>> dependenciesMap = new HashMap<>();

		Files.walkFileTree(dependenciesDirPath, new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult visitFile(
						Path path, BasicFileAttributes basicFileAttributes)
					throws IOException {

					List<JarDependency> jarDependencies = new ArrayList<>();

					Properties dependencies = PropertiesUtil.loadProperties(
						path);

					String[] compileProperties = StringUtil.split(
						dependencies.getProperty("compile"), ':');

					for (String jar : compileProperties) {
						jarDependencies.add(
							new JarDependency(Paths.get(jar), false));
					}

					String[] compileTestProperties = StringUtil.split(
						dependencies.getProperty("compileTest"), ':');

					for (String jar : compileTestProperties) {
						jarDependencies.add(
							new JarDependency(Paths.get(jar), true));
					}

					Path moduleName = path.getFileName();

					dependenciesMap.put(moduleName.toString(), jarDependencies);

					Files.delete(path);

					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(
						Path path, IOException ioe)
					throws IOException {

					Files.delete(path);

					return FileVisitResult.CONTINUE;
				}
			});

		return dependenciesMap;
	}
}