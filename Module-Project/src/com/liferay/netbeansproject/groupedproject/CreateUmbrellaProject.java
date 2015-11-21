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
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author Tom Wang
 */
public class CreateUmbrellaProject {

	public static void createUmbrellaProject(
			Map<Path, Map<String, Module>> projectMap, Properties properties,
			String portalLibJars)
		throws Exception {

		Path projectPath = Paths.get(
			properties.getProperty("project.dir"), "grouped-umbrella");

		ZipUtil.unZip(Paths.get("CleanProject.zip"), projectPath);

		_replaceProjectName(projectPath);

		List<Path> referenceProjects = new ArrayList<>();

		_appendList(
			referenceProjects, projectMap, projectPath, properties,
			portalLibJars);
	}

	private static void _appendList(
			List<Path> referenceProjects,
			Map<Path, Map<String, Module>> projectMap, Path projectPath,
			Properties properties, String portalLibJars)
		throws IOException {

		Path projectPropertiesPath = projectPath.resolve("nbproject");

		projectPropertiesPath = projectPropertiesPath.resolve(
			"project.properties");

		StringBuilder sb = new StringBuilder("dist.jar=${dist.dir}");

		sb.append(File.separator);
		sb.append("portal.jar\n");

		Path portalWebPath = Paths.get(
			properties.getProperty("portal.dir"), "portal-web", "docroot");

		sb.append("file.reference.portal-web.src=");
		sb.append(portalWebPath);
		sb.append(
			"\nsrc.portal-web.dir=${file.reference.portal-web.src}\n");

		Path portalWebFunctionalPath = Paths.get(
			properties.getProperty("portal.dir"), "portal-web", "test",
			"functional");

		sb.append("file.reference.portal-web-functional.src=");
		sb.append(portalWebFunctionalPath);
		sb.append(
			"\nsrc.portal-web-functional.dir=${file.reference." +
			"portal-web-functional.src}\n");

		StringBuilder javacSB = new StringBuilder("javac.classpath=\\\n");

		for (Path groupPath : projectMap.keySet()) {
			if (!groupPath.equals(Paths.get(properties.getProperty(
				"portal.dir")))) {

				Path moduleName = groupPath.getFileName();

				_appendModuleList(moduleName, "group-modules", javacSB, sb);

				referenceProjects.add(moduleName);
			}
			else {
				Map<String, Module> moduleMap = projectMap.get(groupPath);

				for (Module module : moduleMap.values()) {
					Path moduleName = Paths.get(module.getModuleName());

					_appendModuleList(moduleName, "modules", javacSB, sb);

					referenceProjects.add(moduleName);
				}
			}
		}

		javacSB.append(portalLibJars);

		try (BufferedWriter bufferedWriter = Files.newBufferedWriter(
			projectPropertiesPath, Charset.defaultCharset(),
			StandardOpenOption.APPEND)) {

			bufferedWriter.append(sb);
			bufferedWriter.newLine();

			javacSB.setLength(javacSB.length() - 3);

			bufferedWriter.append(javacSB);
			bufferedWriter.newLine();
		}
	}

	private static void _appendModuleList(
		Path moduleName, String moduleType, StringBuilder javacSB,
		StringBuilder sb) {

		sb.append("project.");
		sb.append(moduleName);
		sb.append("=");

		Path path = Paths.get(
			"..", moduleType, moduleName.toString());

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

	private static void _replaceProjectName(Path modulesDir)
		throws IOException {

		Path buildXmlPath = modulesDir.resolve("build.xml");

		String content = new String(Files.readAllBytes(buildXmlPath));

		content = StringUtil.replace(
			content, "%placeholder%", "Portal-Umbrella");

		Files.write(buildXmlPath, content.getBytes());
	}

}
