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

import com.liferay.netbeansproject.container.JarDependency;
import com.liferay.netbeansproject.container.Module;
import com.liferay.netbeansproject.util.ArgumentsUtil;
import com.liferay.netbeansproject.util.ModuleUtil;
import com.liferay.netbeansproject.util.PropertiesUtil;

import java.io.BufferedWriter;
import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author Tom Wang
 */
public class PortalScanner {

	public static void main(String[] args) throws Exception {
		Map<String, String> arguments = ArgumentsUtil.parseArguments(args);

		PortalScanner portalScanner = new PortalScanner();

		portalScanner.scanPortal(Paths.get(arguments.get("portal.dir")));
	}

	public void scanPortal(Path portalPath) throws Exception {
		Properties buildProperties = PropertiesUtil.loadProperties(
			Paths.get("build.properties"));

		Path portalNamePath = portalPath.getFileName();

		final Path projectPath = Paths.get(
			buildProperties.getProperty("project.dir"),
			portalNamePath.toString());

		final String ignoredDirs = buildProperties.getProperty("ignored.dirs");

		final Map<String, List<JarDependency>> jarDependenciesMap =
			ProcessGradle.processGradle(
				portalPath, projectPath, portalPath.resolve("modules"));

		final Map<Path, Map<String, Module>> projectMap = new HashMap<>();

		Files.walkFileTree(
			portalPath, EnumSet.allOf(FileVisitOption.class), Integer.MAX_VALUE,
			new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult preVisitDirectory(
						Path path, BasicFileAttributes basicFileAttributes)
					throws IOException {

					Path fileNamePath = path.getFileName();

					String fileName = fileNamePath.toString();

					if (ignoredDirs.contains(fileName)) {
						return FileVisitResult.SKIP_SUBTREE;
					}

					if (!Files.exists(path.resolve("src"))) {
						return FileVisitResult.CONTINUE;
					}

					Module module = Module.createModule(
						projectPath.resolve(
							Paths.get(
								"modules", ModuleUtil.getModuleName(path))),
						path, jarDependenciesMap.get(fileName));

					Path modulePath = module.getModulePath();

					Path modulesGroupPath = modulePath.getParent();

					Map<String, Module> modulesMap = projectMap.get(
						modulesGroupPath);

					if (modulesMap == null) {
						modulesMap = new HashMap<>();

						projectMap.put(modulesGroupPath, modulesMap);
					}

					modulesMap.put(module.getModuleName(), module);

					return FileVisitResult.SKIP_SUBTREE;
				}

			});

		_generateModuleList(
			projectMap.values(), projectPath.resolve("moduleList"));

		CreateUmbrella createUmbrella = new CreateUmbrella();

		createUmbrella.createUmbrella(projectMap, portalPath, buildProperties);
	}

	private void _generateModuleList(
			Collection<Map<String, Module>> moduleCollection, Path filePath)
		throws IOException {

		try (BufferedWriter bufferedWriter = Files.newBufferedWriter(
				filePath, StandardCharsets.UTF_8, StandardOpenOption.CREATE)) {

			StringBuilder sb = new StringBuilder();

			for (Map<String, Module> map : moduleCollection) {
				for (String name : map.keySet()) {
					sb.append(name);
					sb.append(',');
				}
			}

			bufferedWriter.append(sb);
		}
	}

}