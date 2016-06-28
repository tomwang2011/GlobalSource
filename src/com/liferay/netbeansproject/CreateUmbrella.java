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

import com.liferay.netbeansproject.template.FreeMarkerUtil;
import com.liferay.netbeansproject.util.FileUtil;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author Tom Wang
 */
public class CreateUmbrella {

	public static void createUmbrella(
			Path portalPath, String projectName,
			Map<String, String> umbrellaSourceMap, String excludeTypes,
			Set<String> moduleNames, Path projectPath)
		throws Exception {

		FileUtil.delete(projectPath);

		FileUtil.unZip(projectPath);

		try (Writer writer = Files.newBufferedWriter(
				projectPath.resolve("build.xml"))) {

			FreeMarkerUtil.process(
				"resources/build_xml.ftl",
				Collections.singletonMap(
					"projectName", "IndividualModuleUmbrella"),
				writer);
		}

		_appendProjectProperties(
			portalPath, excludeTypes, umbrellaSourceMap, moduleNames,
			projectPath);

		Map<String, Object> data = new HashMap<>();

		data.put("portalName", portalPath.getFileName());
		data.put("moduleNames", moduleNames);

		try (Writer writer = Files.newBufferedWriter(
				projectPath.resolve("nbproject/project.xml"))) {

			FreeMarkerUtil.process(
				"resources/umbrella_project_xml.ftl", data, writer);
		}
	}

	private static void _appendProjectProperties(
			Path portalPath, String excludeTypes,
			Map<String, String> umbrellaSourceMap, Set<String> moduleNames,
			Path projectPath)
		throws IOException {

		StringBuilder sb = new StringBuilder();

		sb.append("excludes=");

		if (excludeTypes != null) {
			sb.append(excludeTypes);
		}

		sb.append('\n');

		for (Entry<String, String> source : umbrellaSourceMap.entrySet()) {
			String key = source.getKey();

			sb.append("file.reference.");
			sb.append(key);
			sb.append(".src=");
			sb.append(portalPath.resolve(source.getValue()));
			sb.append('\n');
			sb.append("src.");
			sb.append(key);
			sb.append(".dir=${file.reference.");
			sb.append(key);
			sb.append(".src}");
			sb.append('\n');
		}

		Path projectRootPath = projectPath.getParent();

		Path projectModulesPath = projectRootPath.resolve("modules");

		StringBuilder javacSB = new StringBuilder("javac.classpath=\\\n");

		for (String moduleName : moduleNames) {
			sb.append("project.");
			sb.append(moduleName);
			sb.append('=');
			sb.append(projectModulesPath.resolve(moduleName));
			sb.append('\n');
			sb.append("reference.");
			sb.append(moduleName);
			sb.append(".jar=${project.");
			sb.append(moduleName);
			sb.append("}/dist/");
			sb.append(moduleName);
			sb.append(".jar\n");

			javacSB.append("\t${reference.");
			javacSB.append(moduleName);
			javacSB.append(".jar}:\\\n");
		}

		try (BufferedWriter bufferedWriter = Files.newBufferedWriter(
				projectPath.resolve("nbproject/project.properties"),
				StandardOpenOption.APPEND)) {

			bufferedWriter.append(sb);
			bufferedWriter.newLine();

			if (!moduleNames.isEmpty()) {
				javacSB.setLength(javacSB.length() - 3);
			}

			bufferedWriter.append(javacSB);
			bufferedWriter.newLine();
		}
	}

}