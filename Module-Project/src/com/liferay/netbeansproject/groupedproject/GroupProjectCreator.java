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
import com.liferay.netbeansproject.util.StringUtil;
import com.liferay.netbeansproject.util.ZipUtil;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Properties;

/**
 * @author tom
 */
public class GroupProjectCreator {

	public static void createGroupProject(
			Map<Path, Map<String, Module>> projectMap, Properties properties)
		throws IOException {

		for (Path groupPath : projectMap.keySet()) {
			if (!groupPath.equals(Paths.get(properties.getProperty(
				"portal.dir")))) {

				_createGroupModule(groupPath, properties);
			}
		}
	}

	private static void _createGroupModule(
			Path groupPath, Properties properties)
		throws IOException {

		Path projectDirPath = Paths.get(properties.getProperty("project.dir"));

		Path modulesDirPath = projectDirPath.resolve("group-modules");

		Path groupName = groupPath.getFileName();

		ZipUtil.unZip(
			Paths.get("CleanProject.zip"), modulesDirPath.resolve(groupName));

		_replaceProjectName(groupName, modulesDirPath);

		_prepareProjectPropertyFile(
			groupName, groupPath, modulesDirPath, properties);
	}

	private static void _prepareProjectPropertyFile(
			Path groupName, Path groupPath, Path modulesDirPath,
			Properties properties)
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

		try (BufferedWriter bufferedWriter = Files.newBufferedWriter(
			projectPropertiesPath, Charset.defaultCharset(),
			StandardOpenOption.APPEND)) {

			bufferedWriter.append(projectSB);
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

}