package com.prezi.haxe.gradle

import org.gradle.api.Action
import org.gradle.api.DomainObjectCollection
import org.gradle.api.DomainObjectSet
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.internal.DefaultDomainObjectSet
import org.gradle.api.internal.file.DefaultSourceDirectorySet
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.plugins.BasePlugin
import org.gradle.internal.reflect.Instantiator
import org.gradle.language.base.BinaryContainer
import org.gradle.language.base.FunctionalSourceSet
import org.gradle.language.base.LanguageSourceSet
import org.gradle.language.base.ProjectSourceSet
import org.gradle.language.base.internal.BinaryInternal
import org.gradle.language.base.plugins.LanguageBasePlugin
import org.gradle.language.jvm.ResourceSet
import org.gradle.language.jvm.internal.DefaultResourceSet
import org.slf4j.LoggerFactory

import javax.inject.Inject

/**
 * Created by lptr on 27/04/14.
 */
class HaxeBasePlugin implements Plugin<Project> {
	private static final logger = LoggerFactory.getLogger(HaxeBasePlugin)

	public static final String HAXE_SOURCE_SET_NAME = "haxe"
	public static final String RESOURCE_SET_NAME = "resources"
	public static final String HAXE_RESOURCE_SET_NAME = "haxeResources"

	public static final String COMPILE_TASK_NAME = "compile"
	public static final String COMPILE_TASKS_GROUP = "compile"
	public static final String TEST_TASK_NAME = "test"
	public static final String TEST_TASKS_GROUP = "test"

	private final Instantiator instantiator
	private final FileResolver fileResolver

	@Inject
	public HaxeBasePlugin(Instantiator instantiator, FileResolver fileResolver) {
		this.instantiator = instantiator
		this.fileResolver = fileResolver
	}

	@Override
	void apply(Project project) {
		project.plugins.apply(BasePlugin)
		project.plugins.apply(LanguageBasePlugin)

		def projectSourceSet = project.extensions.getByType(ProjectSourceSet)

		// Add functional source sets for main code
		def main = projectSourceSet.maybeCreate("main")
		def test = projectSourceSet.maybeCreate("test")
		logger.debug("Created ${main} and ${test} in ${project.path}")
		Configuration mainCompile = maybeCreateCompileConfigurationFor(project, "main")
		Configuration testCompile = maybeCreateCompileConfigurationFor(project, "test")
		testCompile.extendsFrom mainCompile
		logger.debug("Created ${mainCompile} and ${testCompile} in ${project.path}")

		// For each source set create a configuration and language source sets
		projectSourceSet.all(new Action<FunctionalSourceSet>() {
			@Override
			void execute(FunctionalSourceSet functionalSourceSet) {
				// Inspired by JavaBasePlugin
				// Add Haxe source set for "src/<name>/haxe"
				def compileConfiguration = project.configurations.getByName(functionalSourceSet.name)
				def haxeSourceSet = instantiator.newInstance(DefaultHaxeSourceSet, HAXE_SOURCE_SET_NAME, functionalSourceSet, compileConfiguration, fileResolver)
				haxeSourceSet.source.srcDir(String.format("src/%s/haxe", functionalSourceSet.name))
				functionalSourceSet.add(haxeSourceSet)
				HaxeBasePlugin.logger.debug("Added ${haxeSourceSet} in ${project.path}")

				// Add resources if not exists yet
				if (!functionalSourceSet.findByName(RESOURCE_SET_NAME)) {
					def resourcesDirectorySet = instantiator.newInstance(DefaultSourceDirectorySet, String.format("%s resources", functionalSourceSet.name), fileResolver)
					resourcesDirectorySet.srcDir(String.format("src/%s/resources", functionalSourceSet.name))
					def resourceSet = instantiator.newInstance(DefaultResourceSet, RESOURCE_SET_NAME, resourcesDirectorySet, functionalSourceSet)
					functionalSourceSet.add(resourceSet)
					HaxeBasePlugin.logger.debug("Added ${resourceSet} in ${project.path}")
				}

				// Add Haxe resource set to be used for embedded resources
				def haxeResourceSet = instantiator.newInstance(DefaultHaxeResourceSet, HAXE_RESOURCE_SET_NAME, functionalSourceSet, fileResolver)
				functionalSourceSet.add(haxeResourceSet)
				HaxeBasePlugin.logger.debug("Added ${haxeResourceSet} in ${project.path}")
			}
		})

		// Add "haxe" extension
		def extension = project.extensions.create("haxe", HaxeExtension, project)
		def targetPlatforms = extension.targetPlatforms

		// For each target platform add functional source sets
		targetPlatforms.all(new Action<TargetPlatform>() {
			@Override
			void execute(TargetPlatform targetPlatform) {
				HaxeBasePlugin.logger.debug("Configuring ${targetPlatform} in ${project.path}")

				// Create platform configurations
				Configuration platformMainCompile = maybeCreateCompileConfigurationFor(project, targetPlatform.name)
				Configuration platformTestCompile = maybeCreateCompileConfigurationFor(project, targetPlatform.name + "Test")
				platformMainCompile.extendsFrom mainCompile
				platformTestCompile.extendsFrom testCompile
				platformTestCompile.extendsFrom platformMainCompile
				HaxeBasePlugin.logger.debug("Added ${platformMainCompile} and ${platformTestCompile} in ${project.path}")

				def platformMain = projectSourceSet.maybeCreate(targetPlatform.name)
				def platformTest = projectSourceSet.maybeCreate(targetPlatform.name + "Test")
				HaxeBasePlugin.logger.debug("Added ${platformMain} and ${platformTest} in ${project.path}")

				def mainLanguageSets = getLanguageSets(main, platformMain)
				def testLanguageSets = getLanguageSets(test, platformTest)

				createHaxeBinary(project, targetPlatform.name, targetPlatform, null,
						mainLanguageSets, testLanguageSets,
						platformMainCompile, platformTestCompile)

				// Add some flavor
				targetPlatform.flavors.all(new Action<Flavor>() {
					@Override
					void execute(Flavor flavor) {
						HaxeBasePlugin.logger.debug("Configuring ${targetPlatform} with ${flavor} in ${project.path}")

						def flavorName = targetPlatform.name + flavor.name.capitalize()

						Configuration flavorMainCompile = maybeCreateCompileConfigurationFor(project, flavorName)
						Configuration flavorTestCompile = maybeCreateCompileConfigurationFor(project, flavorName + "Test")
						flavorMainCompile.extendsFrom platformMainCompile
						flavorTestCompile.extendsFrom platformTestCompile
						flavorTestCompile.extendsFrom flavorMainCompile
						HaxeBasePlugin.logger.debug("Added ${flavorMainCompile} and ${flavorTestCompile} in ${project.path}")

						def flavorMain = projectSourceSet.maybeCreate(flavorName)
						def flavorTest = projectSourceSet.maybeCreate(flavorName + "Test")
						HaxeBasePlugin.logger.debug("Added ${flavorMain} and ${flavorTest} in ${project.path}")

						def flavorMainLanguageSets = getLanguageSets(main, platformMain, flavorMain)
						def flavorTestLanguageSets = getLanguageSets(test, platformTest, flavorTest)

						createHaxeBinary(project, flavorName, targetPlatform, flavor,
								flavorMainLanguageSets, flavorTestLanguageSets,
								flavorMainCompile, flavorTestCompile)
					}
				})
			}
		})

		// Add compile all task
		def compileTask = project.tasks.findByName(COMPILE_TASK_NAME)
		if (compileTask == null) {
			compileTask = project.tasks.create(COMPILE_TASK_NAME)
			compileTask.group = COMPILE_TASKS_GROUP
			compileTask.description = "Compile all Haxe artifacts"
		}
		project.tasks.withType(HaxeCompile).all(new Action<HaxeCompile>() {
			@Override
			void execute(HaxeCompile task) {
				task.group = COMPILE_TASKS_GROUP
				compileTask.dependsOn task
			}
		})

		// Add test all task
		def testTask = project.tasks.findByName(TEST_TASK_NAME)
		if (testTask == null) {
			testTask = project.tasks.create(TEST_TASK_NAME)
			testTask.group = TEST_TASKS_GROUP
			testTask.description = "Test built Haxe artifacts"
		}
		project.tasks.withType(MUnit).all(new Action<MUnit>() {
			@Override
			void execute(MUnit task) {
				task.group = TEST_TASKS_GROUP
				testTask.dependsOn task
			}
		})
	}

	private static HaxeBinary createHaxeBinary(
			Project project, String name, TargetPlatform targetPlatform, Flavor flavor,
			DomainObjectSet<LanguageSourceSet> mainLanguageSets, DomainObjectSet<LanguageSourceSet> testLanguageSets,
			Configuration mainConfiguration, Configuration testConfiguration) {
		def binaryContainer = project.extensions.getByType(BinaryContainer.class)

		// Add compiled binary
		def compiledHaxe = new DefaultHaxeBinary(name, mainConfiguration, testConfiguration, targetPlatform, flavor)
		mainLanguageSets.all { compiledHaxe.source.add it }
		testLanguageSets.all { compiledHaxe.testSource.add it }
		binaryContainer.add(compiledHaxe)
		logger.debug("Added compiled binary ${compiledHaxe} in ${project.path}")
		return compiledHaxe
	}

	private static DomainObjectSet<LanguageSourceSet> getLanguageSets(FunctionalSourceSet... sets) {
		def result = new DefaultDomainObjectSet<>(LanguageSourceSet);
		sets.each { set ->
			result.add set.getByName(HAXE_SOURCE_SET_NAME)
			result.add set.getByName(RESOURCE_SET_NAME)
			result.add set.getByName(HAXE_RESOURCE_SET_NAME)
		}
		return result
	}

	private static Configuration maybeCreateCompileConfigurationFor(Project project, String name) {
		def config = project.configurations.findByName(name)
		if (!config) {
			config = project.configurations.create(name)
			config.visible = false
			config.description = "Compile classpath for ${name}."
		}
		return config
	}

	public static HaxeCompile createCompileTask(Project project, HaxeBinary binary, Class<? extends HaxeCompile> compileType) {
		def namingScheme = ((BinaryInternal) binary).namingScheme
		def compileTaskName = namingScheme.getTaskName("compile")
		HaxeCompile compileTask = project.tasks.create(compileTaskName, compileType)
		compileTask.description = "Compiles $binary"
		binary.source.all { compileTask.source it }
		compileTask.conventionMapping.targetPlatform = { binary.targetPlatform }
		compileTask.conventionMapping.embeddedResources = { gatherEmbeddedResources(binary.source) }
		compileTask.conventionMapping.outputFile = { project.file("${project.buildDir}/compiled-haxe/${namingScheme.outputDirectoryBase}/${binary.name}.${binary.targetPlatform.name}") }

		HaxeCompileParameters.setConventionMapping(compileTask, getParams(project, binary.targetPlatform, binary.flavor))

		project.tasks.getByName(namingScheme.getLifecycleTaskName()).dependsOn compileTask
		// Let' depend on the input configurations
		compileTask.dependsOn binary.configuration
		compileTask.dependsOn binary.source

		binary.compileTask = compileTask
		binary.builtBy(compileTask)
		logger.debug("Created compile task ${compileTask} for ${binary} in ${project.path}")
		return compileTask
	}

	public static MUnit createMUnitTask(Project project, HaxeBinary binary, Class<? extends MUnit> munitType) {
		def namingScheme = ((BinaryInternal) binary).namingScheme
		def munitTaskName = namingScheme.getTaskName("test")
		def munitTask = project.tasks.create(munitTaskName, munitType)
		munitTask.description = "Tests ${binary}"
		binary.source.all { munitTask.source it }
		binary.testSource.all { munitTask.testSource it }
		munitTask.conventionMapping.targetPlatform = { binary.targetPlatform }
		munitTask.conventionMapping.embeddedResources = { gatherEmbeddedResources(binary.source.withType(HaxeResourceSet)) }
		munitTask.conventionMapping.embeddedTestResources = { gatherEmbeddedResources(binary.testSource.withType(HaxeResourceSet)) }
		munitTask.conventionMapping.workingDirectory = { project.file("${project.buildDir}/munit-work/" + binary.targetPlatform.name) }

		HaxeCompileParameters.setConventionMapping(munitTask, getParams(project, binary.targetPlatform, binary.flavor))

		// Let' depend on the input configurations (both from main and test)
		munitTask.dependsOn binary.testConfiguration
		munitTask.dependsOn binary.source, binary.testSource
		logger.debug("Created munit task ${munitTask} for ${binary} in ${project.path}")
		return munitTask
	}

	public static Har createSourceTask(Project project, HaxeBinary binary, Class<? extends Har> harType) {
		def namingScheme = ((BinaryInternal) binary).namingScheme

		def sourceTaskName = namingScheme.getTaskName("bundle", "source")
		Har sourceTask = project.tasks.create(sourceTaskName, harType)
		sourceTask.description = "Bundles the sources of $binary"
		sourceTask.conventionMapping.baseName = { project.name }
		sourceTask.conventionMapping.destinationDir = { project.file("${project.buildDir}/haxe-source/${namingScheme.outputDirectoryBase}") }
		sourceTask.conventionMapping.embeddedResources = { gatherEmbeddedResources(binary.source) }
		sourceTask.into "sources", {
			duplicatesStrategy = DuplicatesStrategy.EXCLUDE
			from binary.source.withType(HaxeSourceSet)*.source
		}
		sourceTask.into RESOURCE_SET_NAME, {
			duplicatesStrategy = DuplicatesStrategy.EXCLUDE
			from binary.source.withType(ResourceSet)*.source
		}
		sourceTask.into "embedded", {
			duplicatesStrategy = DuplicatesStrategy.EXCLUDE
			from binary.source.withType(HaxeResourceSet)*.embeddedResources*.values()
		}
		sourceTask.dependsOn binary.source
		project.tasks.getByName(namingScheme.getLifecycleTaskName()).dependsOn sourceTask
		binary.sourceHarTask = sourceTask
		binary.builtBy(sourceTask)

		// TODO This should state more clearly what it does
		project.artifacts.add(binary.configuration.name, sourceTask) {
			name = project.name + "-" + binary.name
			type = "har"
		}
		logger.debug("Created source source task ${sourceTask} for ${binary} in ${project.path}")
		return sourceTask
	}

	public static Set<HaxeCompileParameters> getParams(Project project, TargetPlatform targetPlatform, Flavor flavor) {
		def rootParams = project.extensions.getByType(HaxeExtension).params
		def platformParams = targetPlatform.params
		def flavorParams = flavor?.params
		return [ rootParams, platformParams, flavorParams ] - null
	}

	public static LinkedHashMap<String, File> gatherEmbeddedResources(DomainObjectCollection<LanguageSourceSet> source) {
		return source.withType(HaxeResourceSet)*.embeddedResources.flatten().inject([:]) { acc, val -> acc + val }
	}
}
