/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.statements;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.IReferencingType;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.Module;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.ReferenceChain;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.Value;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.TemplateRestriction.Restriction_type;
import org.eclipse.titan.designer.AST.TTCN3.templates.TemplateInstance;
import org.eclipse.titan.designer.AST.TTCN3.types.PortGenerator;
import org.eclipse.titan.designer.AST.TTCN3.types.Referenced_Type;
import org.eclipse.titan.designer.AST.TTCN3.values.expressions.ExpressionStruct;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;
import org.eclipse.titan.designer.parsers.ttcn3parser.Ttcn3Lexer;

/**
 * @author Kristof Szabados
 * */
public final class Done_Statement extends Statement {
	private static final String FULLNAMEPART1 = "componentreference";
	private static final String FULLNAMEPART2 = "donematch";
	private static final String FULLNAMEPART3 = "redirection";
	private static final String STATEMENT_NAME = "done";

	private final Value componentreference;
	private final TemplateInstance doneMatch;
	private final Reference redirect;

	//when componentReference is null, this show if the killed was called with any component or all component
	private final boolean isAny;

	public Done_Statement(final Value componentreference, final TemplateInstance doneMatch, final Reference redirect, final boolean isAny) {
		this.componentreference = componentreference;
		this.doneMatch = doneMatch;
		this.redirect = redirect;
		this.isAny = isAny;

		if (componentreference != null) {
			componentreference.setFullNameParent(this);
		}
		if (doneMatch != null) {
			doneMatch.setFullNameParent(this);
		}
		if (redirect != null) {
			redirect.setFullNameParent(this);
		}
	}

	@Override
	/** {@inheritDoc} */
	public Statement_type getType() {
		return Statement_type.S_DONE;
	}

	@Override
	/** {@inheritDoc} */
	public String getStatementName() {
		return STATEMENT_NAME;
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getFullName(final INamedNode child) {
		final StringBuilder builder = super.getFullName(child);

		if (componentreference == child) {
			return builder.append(FULLNAMEPART1);
		} else if (doneMatch == child) {
			return builder.append(FULLNAMEPART2);
		} else if (redirect == child) {
			return builder.append(FULLNAMEPART3);
		}

		return builder;
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		if (componentreference != null) {
			componentreference.setMyScope(scope);
		}
		if (doneMatch != null) {
			doneMatch.setMyScope(scope);
		}
		if (redirect != null) {
			redirect.setMyScope(scope);
		}
	}

	@Override
	/** {@inheritDoc} */
	public boolean hasReceivingStatement() {
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public boolean canRepeat() {
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public void check(final CompilationTimeStamp timestamp) {
		if (lastTimeChecked != null && !lastTimeChecked.isLess(timestamp)) {
			return;
		}

		Port_Utility.checkComponentReference(timestamp, this, componentreference, false, false);

		if (componentreference == null) {
			lastTimeChecked = timestamp;
			return;
		}

		if (doneMatch != null) {
			final boolean[] valueRedirectChecked = new boolean[] { false };
			final IType returnType = Port_Utility.getIncomingType(timestamp, doneMatch, redirect, valueRedirectChecked);
			if (returnType == null) {
				doneMatch.getLocation().reportSemanticError("Cannot determine the return type for value returning done");
			} else {
				IType lastType = returnType;
				boolean returnTypeCorrect = false;
				while (!returnTypeCorrect) {
					if (lastType.hasDoneAttribute()) {
						returnTypeCorrect = true;
						break;
					}
					if (lastType instanceof IReferencingType) {
						final IReferenceChain refChain = ReferenceChain.getInstance(IReferenceChain.CIRCULARREFERENCE, true);
						final IType refd = ((IReferencingType) lastType).getTypeRefd(timestamp, refChain);
						refChain.release();
						if (lastType != refd) {
							lastType = refd;
						} else {
							break;
						}
					} else {
						break;
					}
				}

				if (!returnTypeCorrect) {
					location.reportSemanticError(MessageFormat.format(
							"Return type `{0}'' does not have `done'' extension attibute", returnType.getTypename()));
					returnType.setIsErroneous(true);
				}

				doneMatch.check(timestamp, returnType);
				if (!valueRedirectChecked[0]) {
					Port_Utility.checkValueRedirect(timestamp, redirect, returnType);
				}
			}

		} else if (redirect != null) {
			redirect.getLocation().reportSemanticError("Redirect cannot be used for the return value without a matching template");
			Port_Utility.checkValueRedirect(timestamp, redirect, null);
			redirect.setUsedOnLeftHandSide();
		}

		lastTimeChecked = timestamp;
	}

	@Override
	/** {@inheritDoc} */
	public List<Integer> getPossibleExtensionStarterTokens() {
		if (redirect != null) {
			return null;
		}

		final List<Integer> result = new ArrayList<Integer>();
		result.add(Ttcn3Lexer.PORTREDIRECTSYMBOL);

		if (doneMatch != null) {
			return result;
		}

		result.add(Ttcn3Lexer.LPAREN);

		return result;
	}

	@Override
	/** {@inheritDoc} */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (isDamaged) {
			throw new ReParseException();
		}

		if (componentreference != null) {
			componentreference.updateSyntax(reparser, false);
			reparser.updateLocation(componentreference.getLocation());
		}

		if (doneMatch != null) {
			doneMatch.updateSyntax(reparser, false);
			reparser.updateLocation(doneMatch.getLocation());
		}

		if (redirect != null) {
			redirect.updateSyntax(reparser, false);
			reparser.updateLocation(redirect.getLocation());
		}
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (componentreference != null) {
			componentreference.findReferences(referenceFinder, foundIdentifiers);
		}
		if (doneMatch != null) {
			doneMatch.findReferences(referenceFinder, foundIdentifiers);
		}
		if (redirect != null) {
			redirect.findReferences(referenceFinder, foundIdentifiers);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (componentreference != null && !componentreference.accept(v)) {
			return false;
		}
		if (doneMatch != null && !doneMatch.accept(v)) {
			return false;
		}
		if (redirect != null && !redirect.accept(v)) {
			return false;
		}
		return true;
	}
	@Override
	/** {@inheritDoc} */
	public void generateCode(final JavaGenData aData, final StringBuilder source) {
		final ExpressionStruct expression = new ExpressionStruct();
		generateCodeExpression(aData, expression, null);

		PortGenerator.generateCodeStandalone(aData, source, expression.expression.toString(), getStatementName(), canRepeat(), getLocation());
	}

	@Override
	/** {@inheritDoc} */
	public void generateCodeExpression(final JavaGenData aData, final ExpressionStruct expression, final String callTimer) {
		aData.addCommonLibraryImport("TTCN_Runtime");
		aData.addBuiltinTypeImport("TitanComponent");

		if (componentreference != null) {
			if (doneMatch != null) {
				// value returning done
				// figure out what type the done() function belongs to
				IType t = doneMatch.getExpressionGovernor(CompilationTimeStamp.getBaseTimestamp(), Expected_Value_type.EXPECTED_TEMPLATE);
				if (t == null) {
					ErrorReporter.INTERNAL_ERROR("Encountered a done with unknown governor `" + getFullName() + "''");
					return;
				}
				while (t instanceof Referenced_Type && !t.hasDoneAttribute()) {
					final IReferenceChain refChain = ReferenceChain.getInstance(IReferenceChain.CIRCULARREFERENCE, true);
					t = ((IReferencingType) t).getTypeRefd(CompilationTimeStamp.getBaseTimestamp(), refChain);
					refChain.release();
				}
				if (!t.hasDoneAttribute()) {
					ErrorReporter.INTERNAL_ERROR("Encountered a done return type without done attribute `" + getFullName() + "''");
					return;
				}

				// determine whether the done() function is in the same module
				final Module t_module = t.getMyScope().getModuleScope();
				if (t_module != myStatementBlock.getModuleScope()) {
					expression.expression.append(MessageFormat.format("{0}.", t_module.getIdentifier().getName()));
				}
				expression.expression.append("done(");
				componentreference.generateCodeExpression(aData, expression, true);
				expression.expression.append(", ");
				//FIXME handle decoded match
				doneMatch.generateCode(aData, expression, Restriction_type.TR_NONE);
				expression.expression.append(", ");
			} else {
				// simple done
				componentreference.generateCodeExpressionMandatory(aData, expression, true);
				expression.expression.append(".done(");
			}

			if (redirect == null) {
				expression.expression.append("null");
			} else {
				//FIXME handle value redirection
				redirect.generateCode(aData, expression);
			}

			//FIXME handle index redirection
			expression.expression.append(", null");
			expression.expression.append(')');
		} else if (isAny) {
			// any component.done
			expression.expression.append("TTCN_Runtime.component_done(TitanComponent.ANY_COMPREF, null)");
		} else {
			// all component.done
			expression.expression.append("TTCN_Runtime.component_done(TitanComponent.ALL_COMPREF, null)");
		}
	}
}
