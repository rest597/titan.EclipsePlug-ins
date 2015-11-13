/******************************************************************************
 * Copyright (c) 2000-2015 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titanium.metrics.implementation;

import org.eclipse.titan.designer.AST.IVisitableNode;
import org.eclipse.titan.designer.AST.LargeLocation;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Testcase;
import org.eclipse.titan.designer.AST.TTCN3.statements.StatementBlock;
import org.eclipse.titanium.metrics.MetricData;
import org.eclipse.titanium.metrics.TestcaseMetric;
import org.eclipse.titanium.metrics.visitors.Counter;
import org.eclipse.titanium.metrics.visitors.CounterVisitor;

public class TMLinesOfCode extends BaseTestcaseMetric {
	public TMLinesOfCode() {
		super(TestcaseMetric.LINES_OF_CODE);
	}

	@Override
	public Number measure(final MetricData data, final Def_Testcase testcase) {
		final Counter c = new Counter(0);
		testcase.accept(new CounterVisitor(c) {
			@Override
			public int visit(final IVisitableNode node) {
				if (node instanceof Def_Testcase) {
					return V_CONTINUE;
				}
				if (node instanceof StatementBlock) {
					count.set(((LargeLocation) ((StatementBlock) node).getLocation()).getEndLine());
				}
				return V_SKIP;
			}
		});
		return c.val() - testcase.getLocation().getLine() + 1;
	}
}
