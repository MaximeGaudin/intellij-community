/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.util.xml.stubs;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.psi.impl.PsiManagerImpl;
import com.intellij.psi.stubs.ObjectStubTree;
import com.intellij.psi.stubs.StubTreeLoader;
import com.intellij.psi.xml.XmlFile;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.DomFileDescription;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomManager;
import com.intellij.util.xml.impl.DomManagerImpl;
import com.intellij.util.xml.stubs.model.Foo;

/**
 * @author Dmitry Avdeev
 *         Date: 8/8/12
 */
public abstract class DomStubTest extends LightCodeInsightFixtureTestCase {

  private static final DomFileDescription<Foo> DOM_FILE_DESCRIPTION = new DomFileDescription<Foo>(Foo.class, "foo") {
    @Override
    public boolean hasStubs() {
      return true;
    }
  };

  @Override
  public void setUp() throws Exception {
    super.setUp();
    ((DomManagerImpl)DomManager.getDomManager(getProject())).registerFileDescription(DOM_FILE_DESCRIPTION, getTestRootDisposable());
  }

  @Override
  protected String getBasePath() {
    return "/xml/dom-tests/testData/stubs";
  }

  protected ElementStub getRootStub(String filePath) {
    PsiFile psiFile = myFixture.configureByFile(filePath);

    StubTreeLoader loader = StubTreeLoader.getInstance();
    VirtualFile file = psiFile.getVirtualFile();
    assertTrue(loader.canHaveStub(file));
    ObjectStubTree stubTree = loader.readFromVFile(getProject(), file);
    assertNotNull(stubTree);
    ElementStub root = (ElementStub)stubTree.getRoot();
    assertNotNull(root);
    return root;
  }

  protected void doBuilderTest(String file, String stubText) {
    ElementStub stub = getRootStub(file);
    assertEquals(stubText, DebugUtil.stubTreeToString(stub));
  }

  protected <T extends DomElement> DomFileElement<T> prepare(String path, Class<T> domClass) {
    XmlFile file = prepareFile(path);

    DomFileElement<T> fileElement = DomManager.getDomManager(getProject()).getFileElement(file, domClass);
    assertNotNull(fileElement);
    return fileElement;
  }

  protected XmlFile prepareFile(String path) {
    XmlFile file = (XmlFile)myFixture.configureByFile(path);
    assertFalse(file.getNode().isParsed());
    VirtualFile virtualFile = file.getVirtualFile();
    assertNotNull(virtualFile);
    ObjectStubTree tree = StubTreeLoader.getInstance().readOrBuild(getProject(), virtualFile, file);
    assertNotNull(tree);

    ((PsiManagerImpl)getPsiManager()).cleanupForNextTest();

    file = (XmlFile)getPsiManager().findFile(virtualFile);
    assertNotNull(file);
    return file;
  }
}
