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

import com.liferay.netbeansproject.container.Module;
import com.liferay.netbeansproject.util.PropertiesUtil;
import com.liferay.netbeansproject.util.ZipUtil;

import java.io.BufferedWriter;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import java.util.Map;
import java.util.Map.Entry;
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
public class CreateUmbrella {

	public static void createUmbrella(
			Map<Path, Map<String, Module>> projectMap, Path portalPath,
			Properties buildProperties)
		throws Exception {

		Path portalNamePath = portalPath.getFileName();

		Path projectPath = Paths.get(
			buildProperties.getProperty("project.dir"),
			portalNamePath.toString());

		ZipUtil.unZip(projectPath);

		Map<String, String> umbrellaSourceMap = PropertiesUtil.getProperties(
			buildProperties, "umbrella.source.list");

		_appendProjectProperties(
			projectMap, umbrellaSourceMap, portalPath, projectPath,
			buildProperties);

		DocumentBuilderFactory documentBuilderFactory =
			DocumentBuilderFactory.newInstance();

		DocumentBuilder documentBuilder =
			documentBuilderFactory.newDocumentBuilder();

		_document = documentBuilder.newDocument();

		_createProjectElement(projectMap, buildProperties);

		TransformerFactory transformerFactory =
			TransformerFactory.newInstance();

		Transformer transformer = transformerFactory.newTransformer();

		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(
			"{http://xml.apache.org/xslt}indent-amount", "4");

		Path projectXMLPath = projectPath.resolve("nbproject/project.xml");

		transformer.transform(
			new DOMSource(_document),
			new StreamResult(projectXMLPath.toFile()));
	}

	private static void _appendProjectProperties(
			Map<Path, Map<String, Module>> projectMap,
			Map<String, String> umbrellaSourceMap, Path portalPath,
			Path projectPath, Properties buildProperties)
		throws IOException {

		StringBuilder sb = new StringBuilder();

		sb.append("excludes=");
		sb.append(buildProperties.getProperty("exclude.types"));
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

		Path projectModulesPath = projectPath.resolve("modules");

		StringBuilder javacSB = new StringBuilder("javac.classpath=\\\n");

		for (Map<String, Module> map : projectMap.values()) {
			for (String name : map.keySet()) {
				sb.append("project.");
				sb.append(name);
				sb.append('=');
				sb.append(projectModulesPath.resolve(name));
				sb.append('\n');
				sb.append("reference.");
				sb.append(name);
				sb.append(".jar=${project.");
				sb.append(name);
				sb.append("}/dist/");
				sb.append(name);
				sb.append(".jar\n");

				javacSB.append("\t${reference.");
				javacSB.append(name);
				javacSB.append(".jar}:\\\n");
			}
		}

		try (BufferedWriter bufferedWriter = Files.newBufferedWriter(
				projectPath.resolve("nbproject/project.properties"),
				StandardOpenOption.APPEND)) {

			bufferedWriter.append(sb);
			bufferedWriter.newLine();

			javacSB.setLength(javacSB.length() - 3);

			bufferedWriter.append(javacSB);
			bufferedWriter.newLine();
		}
	}

	private static void _createConfiguration(
		Element projectElement, Map<Path, Map<String, Module>> projectMap,
		Properties buildProperties) {

		Element configurationElement = _document.createElement("configuration");

		projectElement.appendChild(configurationElement);

		_createData(configurationElement, buildProperties);

		_createReferences(configurationElement, projectMap);
	}

	private static void _createData(
		Element configurationElement, Properties buildProperties) {

		Element dataElement = _document.createElement("data");

		dataElement.setAttribute(
			"xmlns", "http://www.netbeans.org/ns/j2se-project/3");

		configurationElement.appendChild(dataElement);

		Element nameElement = _document.createElement("name");

		nameElement.appendChild(
			_document.createTextNode(
				buildProperties.getProperty("project.name")));

		dataElement.appendChild(nameElement);

		Element sourceRootsElement = _document.createElement("source-roots");

		dataElement.appendChild(sourceRootsElement);

		Map<String, String> umbrellaSourceMap = PropertiesUtil.getProperties(
			buildProperties, "umbrella.source.list");

		for (String module : umbrellaSourceMap.keySet()) {
			_createRoots(sourceRootsElement, "src." + module + ".dir");
		}

		Element testRootsElement = _document.createElement("test-roots");

		dataElement.appendChild(testRootsElement);
	}

	private static void _createProjectElement(
		Map<Path, Map<String, Module>> projectMap, Properties buildProperties) {

		Element projectElement = _document.createElement("project");

		projectElement.setAttribute(
			"xmlns", "http://www.netbeans.org/ns/project/1");

		_document.appendChild(projectElement);

		Element typeElement = _document.createElement("type");

		typeElement.appendChild(
			_document.createTextNode("org.netbeans.modules.java.j2seproject"));

		projectElement.appendChild(typeElement);

		_createConfiguration(projectElement, projectMap, buildProperties);
	}

	private static void _createReference(
		Element referencesElement, String module) {

		Element referenceElement = _document.createElement("reference");

		referencesElement.appendChild(referenceElement);

		Element foreignProjectElement = _document.createElement(
			"foreign-project");

		foreignProjectElement.appendChild(_document.createTextNode(module));

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
		Element configurationElement,
		Map<Path, Map<String, Module>> projectMap) {

		Element referencesElement = _document.createElement("references");

		referencesElement.setAttribute(
			"xmlns", "http://www.netbeans.org/ns/ant-project-references/1");

		configurationElement.appendChild(referencesElement);

		for (Map<String, Module> map : projectMap.values()) {
			for (String name : map.keySet()) {
				_createReference(referencesElement, name);
			}
		}
	}

	private static void _createRoots(
		Element sourceRootsElement, String module) {

		Element rootElement = _document.createElement("root");

		rootElement.setAttribute("id", module);

		sourceRootsElement.appendChild(rootElement);
	}

	private static Document _document;

}