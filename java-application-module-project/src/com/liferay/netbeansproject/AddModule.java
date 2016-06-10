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
import com.liferay.netbeansproject.util.PathUtil;
import com.liferay.netbeansproject.util.PropertiesUtil;
import com.liferay.netbeansproject.util.ZipUtil;

import java.io.IOException;

import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author Tom Wang
 */
public class AddModule {

	public static void main(String[] args) throws IOException {
		AddModule addModule = new AddModule();

		Map<String, String> arguments = ArgumentsUtil.parseArguments(args);

		addModule.addModule(Paths.get(arguments.get("portal.dir")));
	}

	public void addModule(final Path portalPath) throws IOException {
		Properties properties = PropertiesUtil.loadProperties(
			Paths.get("build.properties"));

		Path portalName = portalPath.getFileName();

		final Path projectRootPath = Paths.get(
			properties.getProperty("project.dir"), portalName.toString());

		final Map<String, Module> currentProjectMap = _getExistingProjects(
			projectRootPath);

		final String ignoredDirs = properties.getProperty("ignored.dirs");

		Files.walkFileTree(
			portalPath, EnumSet.allOf(FileVisitOption.class), Integer.MAX_VALUE,
			new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult preVisitDirectory(
						Path path, BasicFileAttributes basicFileAttributes)
					throws IOException {

					Path pathFileName = path.getFileName();

					if (ignoredDirs.contains(pathFileName.toString())) {
						return FileVisitResult.SKIP_SUBTREE;
					}

					if (!Files.exists(path.resolve("src"))) {
						return FileVisitResult.CONTINUE;
					}

					String moduleName = ModuleUtil.getModuleName(path);

					try {
						Path moduleProjectPath = projectRootPath.resolve(
							Paths.get("modules", moduleName));

						if (currentProjectMap.containsKey(moduleName) &&
							_isProjectUpToDate(
								currentProjectMap.get(moduleName),
								moduleProjectPath, path)) {

							return FileVisitResult.SKIP_SUBTREE;
						}

						PathUtil.delete(moduleProjectPath);

						Map<String, List<JarDependency>> jarDependenciesMap =
							new HashMap<>();

						if (Files.exists(path.resolve("build.gradle"))) {
							jarDependenciesMap = ProcessGradle.processGradle(
								portalPath, projectRootPath, path);
						}

						ZipUtil.unZip(
							projectRootPath.resolve(
								Paths.get("modules", moduleName)));

						Module.createModule(
							projectRootPath.resolve(
								Paths.get(
									"modules", ModuleUtil.getModuleName(path))),
							path, jarDependenciesMap.get(moduleName));

						CreateModule.createModule(
							projectRootPath, path, portalPath,
							new ArrayList<>(currentProjectMap.keySet()));
					}
					catch (IOException ioe) {
						throw ioe;
					}
					catch (Exception e) {
						throw new IOException(e);
					}

					return FileVisitResult.SKIP_SUBTREE;
				}

			});
	}

	private Path _checkIfNullPath(Path path) {
		if (path == null) {
			return Paths.get("");
		}

		return path;
	}

	private Map<String, Module> _getExistingProjects(Path projectRootPath)
		throws IOException {

		Map<String, Module> map = new HashMap<>();

		for (Path path :
				Files.newDirectoryStream(projectRootPath.resolve("modules"))) {

			Module module = Module.load(path);

			if (module != null) {
				map.put(module.getModuleName(), module);
			}
		}

		return map;
	}

	private boolean _isProjectUpToDate(
			Module currentModule, Path projectPath, Path scannedPath)
		throws IOException {

		Module module = Module.createModule(projectPath, scannedPath, null);

		Path modulePath = _checkIfNullPath(module.getModulePath());
		Path sourcePath = _checkIfNullPath(module.getSourcePath());
		Path sourceResourcePath = _checkIfNullPath(
			module.getSourceResourcePath());
		Path testUnitPath = _checkIfNullPath(module.getTestUnitPath());
		Path testUnitResourcePath = _checkIfNullPath(
			module.getTestUnitResourcePath());
		Path testIntegrationPath = _checkIfNullPath(
			module.getTestIntegrationPath());
		Path testIntegrationResourcePath = _checkIfNullPath(
			module.getTestIntegrationResourcePath());

		String checksum = module.getChecksum();

		if (!modulePath.equals(currentModule.getModulePath()) ||
			!sourcePath.equals(currentModule.getSourcePath()) ||
			!sourceResourcePath.equals(currentModule.getSourceResourcePath()) ||
			!testUnitPath.equals(currentModule.getTestUnitPath()) ||
			!testUnitResourcePath.equals(
				currentModule.getTestUnitResourcePath()) ||
			!testIntegrationPath.equals(
				currentModule.getTestIntegrationPath()) ||
			!testIntegrationResourcePath.equals(
				currentModule.getTestIntegrationResourcePath()) ||
			!checksum.equals(currentModule.getChecksum())) {

			return false;
		}

		return true;
	}

}