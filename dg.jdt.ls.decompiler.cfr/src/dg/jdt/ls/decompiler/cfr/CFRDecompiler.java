/*******************************************************************************
 * Copyright (c) 2017 David Gileadi.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     David Gileadi - initial API and implementation
 *******************************************************************************/
package dg.jdt.ls.decompiler.cfr;

import static org.eclipse.jdt.ls.core.internal.handlers.MapFlattener.getValue;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dg.jdt.ls.decompiler.common.CachingDecompiler;
import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.ClassFileSource;
import org.benf.cfr.reader.apiunreleased.ClassFileSource2;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.state.ClassFileSourceImpl;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.state.TypeUsageCollectorImpl;
import org.benf.cfr.reader.util.getopt.GetOptParser;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.output.IllegalIdentifierDump;
import org.benf.cfr.reader.util.output.StreamDumper;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.ls.core.internal.IDecompiler;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.jdt.ls.core.internal.preferences.Preferences;

public class CFRDecompiler extends CachingDecompiler {

	public static final String OPTIONS_KEY = "java.decompiler.cfr";

	Map<String, String> options = new HashMap<>();

	@SuppressWarnings("unchecked")
	@Override
	public void setPreferences(Preferences preferences) {
		this.options = new HashMap<>();
		Object options = getValue(preferences.asMap(), OPTIONS_KEY);
		if (options instanceof Map) {
			// convert all values to strings
			Map<String, Object> optionsMap = (Map<String, Object>) options;
			for (String key : optionsMap.keySet()) {
				Object value = optionsMap.get(key);
				if (value != null && !(value instanceof String)) {
					optionsMap.put(key, value.toString());
				}
			}

			this.options = (Map<String, String>) options;
		}
	}

	@Override
	protected String decompileContent(URI uri, IProgressMonitor monitor) throws CoreException {
		IClassFile classFile = JDTUtils.resolveClassFile(uri);
		if (classFile != null) {
			return decompileContent(classFile, monitor);
		}
		Options options = OptionsImpl.getFactory().create(this.options);
		return getContent(new ClassFileSourceImpl(options), options, monitor,classFile.getPath());
	}

	@Override
	protected String decompileContent(IClassFile classFile, IProgressMonitor monitor) throws CoreException {
		Options options = OptionsImpl.getFactory().create(this.options);;
		return getContent(new JDTClassFileSource(classFile, options), options, monitor,classFile.getPath());
	}

	private String getContent(ClassFileSource2 classFileSource, Options options, IProgressMonitor monitor,IPath path) throws CoreException {
		try {
			DCCommonState commonState = new DCCommonState(options, classFileSource);
			
			ClassFile classFile = commonState.getClassFileMaybePath(path.toFile().getAbsolutePath());
			commonState.configureWith(classFile);

			if (((Boolean) options.getOption(OptionsImpl.DECOMPILE_INNER_CLASSES)).booleanValue()) {
				classFile.loadInnerClasses(commonState);
			}

			classFile.analyseTop(commonState);

			TypeUsageCollector collectingDumper = new TypeUsageCollectorImpl(classFile);
			classFile.collectTypeUsages(collectingDumper);

			IllegalIdentifierDump illegalIdentifierDump = IllegalIdentifierDump.Factory.get(options);

			final StringBuilder source = new StringBuilder();
			StreamDumper dumper = new StreamDumper(collectingDumper.getTypeUsageInformation(), options,
					illegalIdentifierDump) {
				@Override
				public void close() {
				}

				@Override
				public void addSummaryError(Method method, String error) {
					JavaLanguageServerPlugin.logInfo("Error decompiling method " + method.getName() + ": " + error);
				}

				@Override
				protected void write(String content) {
					source.append(content);
				}
			};
			classFile.dump(dumper);

			return source.toString();
		} catch (Throwable t) {
			throw new CoreException(new Status(Status.ERROR, "dg.jdt.ls.decompiler.cfr", "Error decompiling", t));
		}
	}
}
