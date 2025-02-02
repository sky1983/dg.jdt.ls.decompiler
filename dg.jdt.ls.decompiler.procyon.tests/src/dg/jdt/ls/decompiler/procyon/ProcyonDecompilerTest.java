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
package dg.jdt.ls.decompiler.procyon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ls.core.internal.preferences.Preferences;
import org.junit.Before;
import org.junit.Test;
import dg.jdt.ls.decompiler.test.common.FakeClassFile;

public class ProcyonDecompilerTest {

	public static String HELLO_CLASS_PATH = "testclasses/HelloWorld.class";
	public static String EXPECTED_DECOMPILED_CODE = ProcyonDecompiler.DECOMPILED_HEADER + 
			"package org.eclipse.jdt.ls.decompiler.fernflowe;\n" + 
			"\n" + 
			"public class HelloWorld\n"
			+ "{\n" + 
			"    public void sayHello() {\n" + 
			"        System.out.println(\"Hello world!\");\n" + 
			"    }\n" + 
			"}\n";

	protected IProgressMonitor monitor = new NullProgressMonitor();
	Path path;
	URI uri;

	@Before
	public void getTestClassPath() throws IOException, JavaModelException {
		path = Paths.get(HELLO_CLASS_PATH);
		uri = path.toUri();
	}

	@Test
	public void testSetPreferences() {		
		ProcyonDecompiler decompiler = new ProcyonDecompiler();
		Map<String, Object> options = new HashMap<>();
		options.put("excludeNestedTypes", true);
		Map<String, Object> configuration = new HashMap<>();
		configuration.put("java.decompiler.procyon", options);
		Preferences preferences = Preferences.createFrom(configuration);

		decompiler.setPreferences(preferences);

		assertTrue(decompiler.settings.getExcludeNestedTypes());
	}

	@Test
	public void testGetContent() throws CoreException {
		ProcyonDecompiler decompiler = new ProcyonDecompiler();
		String decompiled = decompiler.getContent(uri, monitor);
		assertEquals(EXPECTED_DECOMPILED_CODE, decompiled);
	}

	@Test
	public void testGetSource() throws CoreException, IOException {
		ProcyonDecompiler decompiler = new ProcyonDecompiler();
		String decompiled = decompiler.getSource(new FakeClassFile(Files.readAllBytes(path),path.toFile().getPath()), monitor);
		assertEquals(EXPECTED_DECOMPILED_CODE, decompiled);
	}

}
