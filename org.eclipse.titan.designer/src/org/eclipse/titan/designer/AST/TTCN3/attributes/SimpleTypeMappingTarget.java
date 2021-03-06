/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.attributes;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Type;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;

/**
 * Represents an simple type mapping target (source == target).
 *
 * @author Kristof Szabados
 * */
public final class SimpleTypeMappingTarget extends TypeMappingTarget {

	private final Type targetType;

	public SimpleTypeMappingTarget(final Type targetType) {
		this.targetType = targetType;
	}

	@Override
	/** {@inheritDoc} */
	public TypeMapping_type getTypeMappingType() {
		return TypeMapping_type.SIMPLE;
	}

	@Override
	/** {@inheritDoc} */
	public String getMappingName() {
		return "simple";
	}

	@Override
	/** {@inheritDoc} */
	public Type getTargetType() {
		return targetType;
	}

	@Override
	/** {@inheritDoc} */
	public void check(final CompilationTimeStamp timestamp, final Type source) {
		if (lastTimeChecked != null && !lastTimeChecked.isLess(timestamp)) {
			return;
		}

		lastTimeChecked = timestamp;

		if (source != null && !source.isIdentical(timestamp, targetType)) {
			targetType.getLocation().reportSemanticError(
					MessageFormat.format("The source and target types must be the same: `{0}'' was expected instead of `{1}''",
							source.getTypename(), targetType.getTypename()));
		}
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (targetType != null) {
			targetType.findReferences(referenceFinder, foundIdentifiers);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (targetType != null && !targetType.accept(v)) {
			return false;
		}
		return true;
	}
}
