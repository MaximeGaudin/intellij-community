package com.intellij.codeInsight;

import com.intellij.JavaTestUtil;
import com.intellij.ide.actions.CopyReferenceAction;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NonNls
import com.intellij.openapi.util.io.FileUtil;

public class CopyReferenceTest extends LightCodeInsightFixtureTestCase {
  @NonNls private static final String BASE_PATH = "/codeInsight/copyReference";
  protected int oldSetting;

  @Override
  protected String getBasePath() {
    return JavaTestUtil.getRelativeJavaTestDataPath() + BASE_PATH;
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    CodeInsightSettings settings = CodeInsightSettings.getInstance();
    oldSetting = settings.ADD_IMPORTS_ON_PASTE;
    settings.ADD_IMPORTS_ON_PASTE = CodeInsightSettings.YES;
  }

  @Override
  protected void tearDown() throws Exception {
    CodeInsightSettings settings = CodeInsightSettings.getInstance();
    settings.ADD_IMPORTS_ON_PASTE = oldSetting;
    super.tearDown();
  }

  public void testConstructor() throws Exception { doTest(); }
  public void testDefaultConstructor() throws Exception { doTest(); }
  public void testIdentifierSeparator() throws Exception { doTest(); }
  public void testMethodFromAnonymousClass() throws Exception { doTest(); }

  public void testCopyFile() throws Exception {
    PsiFile psiFile = myFixture.addFileToProject("x/x.txt", "");
    assertTrue(CopyReferenceAction.doCopy(psiFile, getProject()));

    String name = getTestName(false);
    myFixture.configureByFile(name + "_dst.java");
    performPaste();
    myFixture.checkResultByFile(name + "_after.java");
  }

  public void testCopyLineNumber() {
    myFixture.configureByText 'a.java', '''
<caret>class Foo {
}'''
    def path = FileUtil.toSystemDependentName(myFixture.file.virtualFile.path)
    performCopy()
    myFixture.configureByText 'a.txt', ''
    performPaste()
    myFixture.checkResult "$path:2"
  }

  private void doTest() throws Exception {
    String name = getTestName(false);
    myFixture.configureByFile(name + ".java");
    performCopy();
    myFixture.configureByFile(name + "_dst.java");
    performPaste();
    myFixture.checkResultByFile(name + "_after.java");
  }

  private void performCopy() {
    myFixture.testAction(ActionManager.getInstance().getAction(IdeActions.ACTION_COPY_REFERENCE));
  }

  private void performPaste() {
    myFixture.performEditorAction(IdeActions.ACTION_EDITOR_PASTE);
  }
}
