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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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

		DocumentBuilderFactory documentBuilderFactory =
			DocumentBuilderFactory.newInstance();

		DocumentBuilder documentBuilder =
			documentBuilderFactory.newDocumentBuilder();

		_document = documentBuilder.newDocument();

		_createProjectElement(referenceProjects, properties);

		TransformerFactory transformerFactory =
			TransformerFactory.newInstance();

		Transformer transformer = transformerFactory.newTransformer();

		Path projectXMLPath = Paths.get(
			projectPath.toString(), "nbproject", "project.xml");

		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(
			"{http://xml.apache.org/xslt}indent-amount", "4");

		transformer.transform(
			new DOMSource(_document),
			new StreamResult(projectXMLPath.toFile()));
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

	private static void _createConfiguration(
		Element projectElement, List<Path> referenceProjects,
		Properties properties) {

		Element configurationElement = _document.createElement("configuration");

		projectElement.appendChild(configurationElement);

		_createData(configurationElement, properties);

		_createReferences(configurationElement, referenceProjects);
	}

	private static void _createData(
		Element configurationElement, Properties properties) {

		Element dataElement = _document.createElement("data");

		dataElement.setAttribute(
			"xmlns", "http://www.netbeans.org/ns/j2se-project/3");

		configurationElement.appendChild(dataElement);

		Element nameElement = _document.createElement("name");

		nameElement.appendChild(
			_document.createTextNode(properties.getProperty("project.name")));

		dataElement.appendChild(nameElement);

		Element sourceRootsElement = _document.createElement("source-roots");

		dataElement.appendChild(sourceRootsElement);

		for (
			String module :
			StringUtil.split(
				properties.getProperty("umbrella.source.list"), ',')) {

			_createRoots(sourceRootsElement, "src." + module + ".dir");
		}

		Element testRootsElement = _document.createElement("test-roots");

		dataElement.appendChild(testRootsElement);
	}

	private static void _createProjectElement(
		List<Path> referenceProjects, Properties properties) {

		Element projectElement = _document.createElement("project");

		projectElement.setAttribute(
			"xmlns", "http://www.netbeans.org/ns/project/1");

		_document.appendChild(projectElement);

		Element typeElement = _document.createElement("type");

		typeElement.appendChild(
			_document.createTextNode("org.netbeans.modules.java.j2seproject"));

		projectElement.appendChild(typeElement);

		_createConfiguration(
			projectElement, referenceProjects, properties);
	}

	private static void _createReference(
		Element referencesElement, String moduleName) {

		Element referenceElement = _document.createElement("reference");

		referencesElement.appendChild(referenceElement);

		Element foreignProjectElement = _document.createElement(
			"foreign-project");

		foreignProjectElement.appendChild(_document.createTextNode(moduleName));

		referenceElement.appendChild(foreignProjectElement);

		Element artifactTypeElement = _document.createElement("artifact-type");

		artifactTypeElement.appendChild(_document.createTextNode("jar"));

		referenceElement.appendChild(artifactTypeElement);

		Element scriptElement = _document.createElement("script");

		scriptElement.appendChild(_document.createTextNode("build.xml"));

		referenceElement.appendChild(scriptElement);

		Element targetElement = _document.createElement("target");

		targetElement.appendChild(_document.createTextNode("jar"));

		referenceElement.appendChild(targetElement);

		Element cleanTargetElement = _document.createElement("clean-target");

		cleanTargetElement.appendChild(_document.createTextNode("clean"));

		referenceElement.appendChild(cleanTargetElement);

		Element idElement = _document.createElement("id");

		idElement.appendChild(_document.createTextNode("jar"));

		referenceElement.appendChild(idElement);
	}

	private static void _createReferences(
		Element configurationElement, List<Path> referenceProjects) {

		Element referencesElement = _document.createElement("references");

		referencesElement.setAttribute(
			"xmlns", "http://www.netbeans.org/ns/ant-project-references/1");

		configurationElement.appendChild(referencesElement);

		for (Path groupPathName : referenceProjects) {
			_createReference(referencesElement, groupPathName.toString());
		}
	}

	private static void _createRoots(
		Element sourceRootsElement, String moduleName) {

		Element rootElement = _document.createElement("root");

		rootElement.setAttribute("id", moduleName);

		sourceRootsElement.appendChild(rootElement);
	}

	private static void _replaceProjectName(Path modulesDir)
		throws IOException {

		Path buildXmlPath = modulesDir.resolve("build.xml");

		String content = new String(Files.readAllBytes(buildXmlPath));

		content = StringUtil.replace(
			content, "%placeholder%", "Portal-Umbrella");

		Files.write(buildXmlPath, content.getBytes());
	}

	private static Document _document;
}
