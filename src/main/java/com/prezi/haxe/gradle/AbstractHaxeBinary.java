package com.prezi.haxe.gradle;

import com.prezi.haxe.gradle.incubating.AbstractBuildableModelElement;
import com.prezi.haxe.gradle.incubating.BinaryInternal;
import com.prezi.haxe.gradle.incubating.BinaryNamingScheme;
import com.prezi.haxe.gradle.incubating.LanguageSourceSet;
import org.gradle.api.DomainObjectSet;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.DefaultDomainObjectSet;

public abstract class AbstractHaxeBinary<T extends HaxeCompile> extends AbstractBuildableModelElement implements HaxeBinaryBase<T>, BinaryInternal {
	private final DomainObjectSet<LanguageSourceSet> source = new DefaultDomainObjectSet<LanguageSourceSet>(LanguageSourceSet.class);
	private final String name;
	private final Configuration configuration;
	private final BinaryNamingScheme namingScheme;
	private final TargetPlatform targetPlatform;
	private final Flavor flavor;
	private T compileTask;
	private Har sourceHarTask;

	protected AbstractHaxeBinary(String parentName, Configuration configuration, TargetPlatform targetPlatform, Flavor flavor) {
		this.namingScheme = new HaxeBinaryNamingScheme(parentName);
		this.name = namingScheme.getLifecycleTaskName();
		this.configuration = configuration;
		this.targetPlatform = targetPlatform;
		this.flavor = flavor;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDisplayName() {
		return namingScheme.getDescription();
	}

	@Override
	public BinaryNamingScheme getNamingScheme() {
		return namingScheme;
	}

	@Override
	public String toString() {
		return namingScheme.getDescription();
	}

	@Override
	public Configuration getConfiguration() {
		return configuration;
	}

	@Override
	public DomainObjectSet<LanguageSourceSet> getSource() {
		return source;
	}

	@Override
	public TargetPlatform getTargetPlatform() {
		return targetPlatform;
	}

	@Override
	public Flavor getFlavor() {
		return flavor;
	}

	@Override
	public T getCompileTask() {
		return compileTask;
	}

	@Override
	public void setCompileTask(T compileTask) {
		this.compileTask = compileTask;
	}

	@Override
	public Har getSourceHarTask() {
		return sourceHarTask;
	}

	@Override
	public void setSourceHarTask(Har sourceHarTask) {
		this.sourceHarTask = sourceHarTask;
	}

}
