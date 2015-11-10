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
import com.liferay.netbeansproject.util.StringUtil;
import com.liferay.netbeansproject.util.ZipUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

	private static void _createModuleProject(
			Module module, Properties properties, String moduleFolderName)
		throws IOException {

		Path projectDirPath = Paths.get(properties.getProperty("project.dir"));

		Path modulesDirPath = projectDirPath.resolve(moduleFolderName);

		String moduleName = module.getModuleName();

		ZipUtil.unZip(
			Paths.get("CleanProject.zip"), modulesDirPath.resolve(moduleName));

		_replaceProjectName(module.getModuleName(), modulesDirPath);
	}

	private static void _replaceProjectName(String moduleName, Path modulesDir)
		throws IOException {

		Path modulePath = modulesDir.resolve(moduleName);

		Path buildXmlPath = modulePath.resolve("build.xml");

		String content = new String(Files.readAllBytes(buildXmlPath));

		content = StringUtil.replace(content, "%placeholder%", moduleName);

		Files.write(buildXmlPath, content.getBytes());
	}

}