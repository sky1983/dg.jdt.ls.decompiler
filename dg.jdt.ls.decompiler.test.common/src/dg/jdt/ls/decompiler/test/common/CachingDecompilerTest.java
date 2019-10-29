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
package dg.jdt.ls.decompiler.test.common;

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.JavaModelException;
import org.junit.Before;
import org.junit.Test;
import dg.jdt.ls.decompiler.common.CachingDecompiler;

public class CachingDecompilerTest {

    public static String HELLO_CLASS_PATH = "testclasses/HelloWorld.class";
	protected IProgressMonitor monitor = new NullProgressMonitor();
	Path path;
	
	@Before
    public void getTestClassPath() throws IOException, JavaModelException {
        path = Paths.get(HELLO_CLASS_PATH);
    }

	@Test
	public void testGetContentCaching() throws CoreException, URISyntaxException {
		FakeDecompiler decompiler = new FakeDecompiler();
		decompiler.getContent(new URI("just://test"), monitor);
		assertEquals(decompiler.uriCalls, 1);
		decompiler.getContent(new URI("just://test"), monitor);
		assertEquals(decompiler.uriCalls, 1);
	}

	@Test
	public void testGetSourceCaching() throws CoreException, IOException {
		FakeDecompiler decompiler = new FakeDecompiler();
		decompiler.getSource(new FakeClassFile(new byte[0], "test",path.toFile().getPath()), monitor);
		assertEquals(decompiler.classFileCalls, 1);
		decompiler.getSource(new FakeClassFile(new byte[0], "test",path.toFile().getPath()), monitor);
		assertEquals(decompiler.classFileCalls, 1);
	}

	private class FakeDecompiler extends CachingDecompiler {
		public int uriCalls = 0;
		public int classFileCalls = 0;

		@Override
		protected String decompileContent(URI uri, IProgressMonitor monitor) {
			++uriCalls;
			return "";
		}

		@Override
		protected String decompileContent(IClassFile classFile, IProgressMonitor monitor) {
			++classFileCalls;
			return "";
		}
	}
}
