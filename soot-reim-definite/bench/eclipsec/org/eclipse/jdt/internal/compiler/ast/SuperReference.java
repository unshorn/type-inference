/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;
//import checkers.inference.ownership.quals.*;

import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;

public class SuperReference extends ThisReference {
	
	public SuperReference(int sourceStart, int sourceEnd) {

		super(sourceStart, sourceEnd);
	}

	public static ExplicitConstructorCall implicitSuperConstructorCall() {

		return new ExplicitConstructorCall(ExplicitConstructorCall.ImplicitSuper);
	}

	public boolean isImplicitThis() {
		
		return false;
	}

	public boolean isSuper() {
		
		return true;
	}

	public boolean isThis() {
		
		return false ;
	}

	public StringBuffer printExpression(int indent, StringBuffer output){
	
		return output.append("super"); //$NON-NLS-1$
		
	}

	public TypeBinding resolveType(BlockScope scope) {

		constant = Constant.NotAConstant;
		if (!checkAccess(scope.methodScope()))
			return null;
		ReferenceBinding enclosingReceiverType = scope.enclosingReceiverType();
		if (enclosingReceiverType.id == T_JavaLangObject) {
			scope.problemReporter().cannotUseSuperInJavaLangObject((/*@OwnPar*/ /*@NoRep*/ SuperReference)this);
			return null;
		}
		return this.resolvedType = enclosingReceiverType.superclass();
	}

	public void traverse(ASTVisitor visitor, BlockScope blockScope) {
		visitor.visit((/*@OwnPar*/ /*@NoRep*/ SuperReference)this, blockScope);
		visitor.endVisit((/*@OwnPar*/ /*@NoRep*/ SuperReference)this, blockScope);
	}
}
