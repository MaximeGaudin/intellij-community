/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.types;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.resolve.ResolveCache;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Function;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.NullableFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.GroovyElementVisitor;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrSafeCastExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.types.GrTypeElement;
import org.jetbrains.plugins.groovy.lang.psi.impl.GroovyPsiManager;
import org.jetbrains.plugins.groovy.lang.psi.impl.PsiImplUtil;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.GrExpressionImpl;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil;

import java.util.HashMap;

import static com.intellij.psi.CommonClassNames.JAVA_UTIL_COLLECTION;
import static org.jetbrains.plugins.groovy.lang.lexer.GroovyTokenTypes.kAS;

/**
 * @author ven
 */
public class GrSafeCastExpressionImpl extends GrExpressionImpl implements GrSafeCastExpression, PsiPolyVariantReference {

  private static final Function<GrSafeCastExpressionImpl, PsiType> TYPE_CALCULATOR =
    new NullableFunction<GrSafeCastExpressionImpl, PsiType>() {
      @Override
      public PsiType fun(GrSafeCastExpressionImpl cast) {
        GrTypeElement typeElement = cast.getCastTypeElement();
        if (typeElement == null) return null;

        final PsiType opType = cast.getOperand().getType();
        final PsiType castType = typeElement.getType();

        if (isCastToRawCollectionFromArray(opType, castType)) {
          final PsiClass resolved = ((PsiClassType)castType).resolve();
          final PsiTypeParameter typeParameter = resolved.getTypeParameters()[0];
          final HashMap<PsiTypeParameter, PsiType> substitutionMap = new HashMap<PsiTypeParameter, PsiType>();
          substitutionMap.put(typeParameter, TypesUtil.getItemType(opType));
          final PsiSubstitutor substitutor = JavaPsiFacade.getElementFactory(cast.getProject()).createSubstitutor(substitutionMap);
          return JavaPsiFacade.getElementFactory(cast.getProject()).createType(resolved, substitutor);
        }

        return TypesUtil.boxPrimitiveType(castType, cast.getManager(), cast.getResolveScope());
      }
    };


  /**
   * It is assumed that collection class should have only one type param and this param defines collection's item type.
   */
  private static boolean isCastToRawCollectionFromArray(PsiType opType, PsiType castType) {
    return castType instanceof PsiClassType &&
           InheritanceUtil.isInheritor(castType, JAVA_UTIL_COLLECTION) &&
           PsiUtil.extractIterableTypeParameter(castType, false) == null &&
           ((PsiClassType)castType).resolve().getTypeParameters().length == 1 &&
           TypesUtil.getItemType(opType) != null;
  }


  private static final class OurResolver implements ResolveCache.PolyVariantResolver<GrSafeCastExpressionImpl> {
    @NotNull
    @Override
    public ResolveResult[] resolve(@NotNull GrSafeCastExpressionImpl cast, boolean incompleteCode) {
      PsiType type = cast.getOperand().getType();
      if (type == null) {
        return GroovyResolveResult.EMPTY_ARRAY;
      }

      final GrTypeElement typeElement = cast.getCastTypeElement();
      final PsiType toCast = typeElement == null ? null : typeElement.getType();
      final PsiType classType = TypesUtil.createJavaLangClassType(toCast, cast.getProject(), cast.getResolveScope());
      return TypesUtil.getOverloadedOperatorCandidates(type, kAS, cast, new PsiType[]{classType});
    }
  }

  private static final OurResolver OUR_RESOLVER = new OurResolver();

  public GrSafeCastExpressionImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(GroovyElementVisitor visitor) {
    visitor.visitSafeCastExpression(this);
  }

  public String toString() {
    return "Safe cast expression";
  }

  public PsiType getType() {
    return GroovyPsiManager.getInstance(getProject()).getType(this, TYPE_CALCULATOR);
  }

  @Nullable
  public GrTypeElement getCastTypeElement() {
    return findChildByClass(GrTypeElement.class);
  }

  @NotNull
  public GrExpression getOperand() {
    return findNotNullChildByClass(GrExpression.class);
  }

  @Override
  public PsiReference getReference() {
    return this;
  }

  @Override
  public PsiElement getElement() {
    return this;
  }

  @Override
  public TextRange getRangeInElement() {
    final PsiElement as = findNotNullChildByType(kAS);
    final int offset = as.getStartOffsetInParent();
    return new TextRange(offset, offset + 2);
  }

  @Override
  public PsiElement resolve() {
    return PsiImplUtil.extractUniqueResult(multiResolve(false)).getElement();
  }

  @NotNull
  @Override
  public String getCanonicalText() {
    return getText();
  }

  @Override
  public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
    throw new UnsupportedOperationException("safe cast cannot be renamed");
  }

  @Override
  public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
    throw new UnsupportedOperationException("safe cast can be bounded to nothing");
  }

  @Override
  public boolean isReferenceTo(PsiElement element) {
    return getManager().areElementsEquivalent(resolve(), element);
  }

  @NotNull
  @Override
  public Object[] getVariants() {
    return ArrayUtil.EMPTY_OBJECT_ARRAY;
  }

  @Override
  public boolean isSoft() {
    return false;
  }

  @NotNull
  @Override
  public GroovyResolveResult[] multiResolve(boolean incompleteCode) {
    return (GroovyResolveResult[])ResolveCache.getInstance(getProject()).resolveWithCaching(this, OUR_RESOLVER, false, incompleteCode);
  }
}