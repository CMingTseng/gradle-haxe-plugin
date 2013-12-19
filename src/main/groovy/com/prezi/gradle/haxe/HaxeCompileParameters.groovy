package com.prezi.gradle.haxe

import com.prezi.gradle.DeprecationLogger
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency

class HaxeCompileParameters {

	private final Project project

	public HaxeCompileParameters(Project project)
	{
		this.project = project
	}

	Configuration configuration

	public boolean hasConfiguration()
	{
		return configuration != null
	}

	public Configuration getConfiguration()
	{
		if (configuration == null)
		{
			return project.configurations[Dependency.DEFAULT_CONFIGURATION]
		}
		return configuration
	}

	public void configuration(Configuration configuration)
	{
		this.configuration = configuration
	}

	String targetPlatform

	String main

	List<String> macros = []
	public macro(String m)
	{
		macros.add(m)
	}

	LinkedHashSet<String> includes = []

	public include(String thing)
	{
		includes.add(thing)
	}

	LinkedHashSet<String> excludes = []

	public exclude(String thing)
	{
		excludes.add(thing)
	}

	LinkedHashSet<String> flagList = []

	public void flag(String... flag)
	{
		flagList.addAll(flag)
	}

	boolean debug

	List<Object> sourcePaths = []
	LinkedHashSet<String> legacyPlatformPaths = []
	List<Object> resourcePaths = []
	LinkedHashMap<String, File> embeddedResources = [:]

	public void source(Object path)
	{
		sourcePaths.add(path)
		if (path instanceof String
				&& path.startsWith("src/"))
		{
			legacyPlatformPaths << path.substring(4)
		}
	}

	public void includeLegacyPlatform(String platform)
	{
		legacyPlatformPaths << platform
	}

	public resource(Object path)
	{
		resourcePaths.add(path)
	}

	public embed(String name, Object file)
	{
		embeddedResources.put(name, project.file(file))
	}

	public embed(Object file)
	{
		def realFile = project.file(file)
		embed(realFile.name, realFile)
	}

	public embed(Map<String, ?> resources)
	{
		resources.each { String name, Object file ->
			embed(name, file)
		}
	}

	public embedAll(Object directory)
	{
		def realDir = project.file(directory)
		if (!realDir.directory)
		{
			throw new IllegalArgumentException("embedAll requires a directory: " + directory)
		}
		realDir.eachFileRecurse { embed(it) }
	}

	String spaghetti

	public spaghetti(String output) {
		if (!(output in ["module", "application"])) {
			throw new IllegalArgumentException("spaghetti argument must be either 'module' or 'application'")
		}
		this.spaghetti = output
	}

	// Clone

	protected void copyTo(HaxeCompileParameters params)
	{
		params.configuration = configuration
		params.targetPlatform = targetPlatform
		params.main = main
		params.macros.addAll(macros)
		params.includes.addAll(includes)
		params.excludes.addAll(excludes)
		params.flagList.addAll(flagList)
		params.debug = debug
		params.sourcePaths.addAll(sourcePaths)
		params.legacyPlatformPaths.addAll(legacyPlatformPaths)
		params.resourcePaths.addAll(resourcePaths)
		params.embeddedResources.putAll(embeddedResources)
	}

	@Override
	String toString()
	{
		def s = new StringBuilder()
		def separator = "\n\t"
		s.append "Haxe compiler config"
		s.append separator
		s.append "Configuration: ${configuration ? configuration.name : null}"
		s.append separator
		s.append "Target platform: ${targetPlatform}"
		s.append separator
		s.append "Main: ${main}"
		s.append separator
		s.append "Macros: ${macros}"
		s.append separator
		s.append "Includes: ${includes}"
		s.append separator
		s.append "Excludes: ${excludes}"
		s.append separator
		s.append "Flags: ${flagList}"
		s.append separator
		s.append "Debug: ${debug}"
		s.append separator
		s.append "Sources: ${sourcePaths}"
		s.append separator
		s.append "Resources: ${resourcePaths}"
		s.append separator
		s.append "Embedded resources: ${embeddedResources}"
		s.append separator
		s.append "Legacy platforms: ${legacyPlatformPaths}"
		return s.toString()
	}

	// Deprecated properties

	@Deprecated
	public setIncludePackages(String[] pkgs)
	{
		DeprecationLogger.nagUserOfReplacedProperty("includePackages", "includes")
		includes = pkgs
	}
	@Deprecated
	public includePackage(String pkg)
	{
		DeprecationLogger.nagUserOfReplacedProperty("includePackage", "include")
		include(pkg)
	}

	@Deprecated
	public setExcludePackages(String[] pkgs)
	{
		DeprecationLogger.nagUserOfReplacedProperty("excludePackages", "excludes")
		excludes = pkgs
	}
	@Deprecated
	public excludePackage(String pkg)
	{
		DeprecationLogger.nagUserOfReplacedProperty("excludePackage", "exclude")
		exclude(pkg)
	}

	@Deprecated
	public void setFlags(String flagsToAdd)
	{
		DeprecationLogger.nagUserOfReplacedProperty("flags", "flag")
		((" " + flagsToAdd.trim()).split(" -")).each { if (it) flag("-$it") }
		this
	}

	@Deprecated
	public void legacySource(String path)
	{
		DeprecationLogger.nagUserOfReplacedProperty("legacySource", "includeLegacyPlatform")
		if (path.startsWith("src/"))
		{
			legacyPlatformPaths << path.substring(4)
		}
		else
		{
			throw new IllegalArgumentException("Invalid legacy source path (should start with 'src/'): " + path)
		}
	}


}
