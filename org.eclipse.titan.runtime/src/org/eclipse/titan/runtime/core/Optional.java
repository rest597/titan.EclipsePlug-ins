/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titan.runtime.core;

import java.text.MessageFormat;

import org.eclipse.titan.runtime.core.Base_Template.template_sel;

/**
 * TTCN-3 boolean
 * @author Kristof Szabados
 */
public class Optional<TYPE extends Base_Type> extends Base_Type {
	public enum optional_sel { OPTIONAL_UNBOUND, OPTIONAL_OMIT, OPTIONAL_PRESENT };

	private TYPE optionalValue;

	private optional_sel optionalSelection;

	private final Class<TYPE> clazz;

	public Optional(final Class<TYPE> clazz) {
		optionalValue = null;
		optionalSelection = optional_sel.OPTIONAL_UNBOUND;
		this.clazz = clazz;
	}

	public Optional(final Class<TYPE> clazz, final template_sel otherValue) {
		if (otherValue != template_sel.OMIT_VALUE) {
			throw new TtcnError("Setting an optional field to an invalid value.");
		}
		optionalValue = null;
		optionalSelection = optional_sel.OPTIONAL_OMIT;
		this.clazz = clazz;
	}

	public Optional(final Optional<TYPE> otherValue) {
		//super(otherValue);
		optionalValue = null;
		optionalSelection = otherValue.optionalSelection;
		clazz = otherValue.clazz;
		if (optional_sel.OPTIONAL_PRESENT.equals(otherValue.optionalSelection)) {
			try {
				optionalValue = clazz.newInstance();
			} catch (Exception e) {
				throw new TtcnError(MessageFormat.format("Internal Error: exception `{0}'' thrown while instantiating class of `{1}'' type", e.getMessage(), clazz.getName()));
			}

			optionalValue.assign(otherValue.optionalValue);
		}
	}

	//originally clean_up
	public void cleanUp() {
		if (optional_sel.OPTIONAL_PRESENT.equals(optionalSelection)) {
			optionalValue = null;
		}
		optionalSelection = optional_sel.OPTIONAL_UNBOUND;
	}

	//originally operator=
	public Optional<TYPE> assign(final template_sel otherValue) {
		if (!template_sel.OMIT_VALUE.equals(otherValue)) {
			throw new TtcnError("Internal error: Setting an optional field to an invalid value.");
		}
		setToOmit();
		return this;
	}

	//originally operator=
	public Optional<TYPE> assign(final Optional<TYPE> otherValue) {
		switch (otherValue.optionalSelection) {
		case OPTIONAL_PRESENT:
			if (optional_sel.OPTIONAL_PRESENT.equals(optionalSelection)) {
				optionalValue.assign(otherValue.optionalValue);
			} else {
				try {
					optionalValue = clazz.newInstance();
				} catch (Exception e) {
					throw new TtcnError(MessageFormat.format("Internal Error: exception `{0}'' thrown while instantiating class of `{1}'' type", e.getMessage(), clazz.getName()));
				}
				optionalValue.assign(otherValue.optionalValue);
				optionalSelection = optional_sel.OPTIONAL_PRESENT;
			}
			break;
		case OPTIONAL_OMIT:
			if (otherValue != this) {
				setToOmit();
			}
			break;
		default:
			cleanUp();
			break;
		}

		return this;
	}

	@Override
	public Optional<TYPE> assign(final Base_Type otherValue) {
		if (!(otherValue instanceof Optional<?>)) {
			if (optional_sel.OPTIONAL_PRESENT.equals(optionalSelection)) {
				optionalValue.assign(otherValue);
			} else {
				try {
					optionalValue = clazz.newInstance();
				} catch (Exception e) {
					throw new TtcnError(MessageFormat.format("Internal Error: exception `{0}'' thrown while instantiating class of `{1}'' type", e.getMessage(), clazz.getName()));
				}
				optionalValue.assign(otherValue);
				optionalSelection = optional_sel.OPTIONAL_PRESENT;
			}
			return this;
		}

		final Optional<?> optionalOther = (Optional<?>)otherValue;
		switch (optionalOther.optionalSelection) {
		case OPTIONAL_PRESENT:
			if (optional_sel.OPTIONAL_PRESENT.equals(optionalSelection)) {
				optionalValue.assign(optionalOther.optionalValue);
			} else {
				try {
					optionalValue = clazz.newInstance();
				} catch (Exception e) {
					throw new TtcnError(MessageFormat.format("Internal Error: exception `{0}'' thrown while instantiating class of `{1}'' type", e.getMessage(), clazz.getName()));
				}
				optionalValue.assign(optionalOther.optionalValue);
				optionalSelection = optional_sel.OPTIONAL_PRESENT;
			}
			break;
		case OPTIONAL_OMIT:
			if (optionalOther != this) {
				setToOmit();
			}
			break;
		default:
			cleanUp();
			break;
		}

		return this;
	}

	public void setToPresent() {
		if (!optional_sel.OPTIONAL_PRESENT.equals(optionalSelection)) {
			optionalSelection = optional_sel.OPTIONAL_PRESENT;
			try {
				optionalValue = clazz.newInstance();
			} catch (Exception e) {
				throw new TtcnError(MessageFormat.format("Internal Error: exception `{0}'' thrown while instantiating class of `{1}'' type", e.getMessage(), clazz.getName()));
			}
		}
	}

	public void setToOmit() {
		if (optional_sel.OPTIONAL_PRESENT.equals(optionalSelection)) {
			optionalValue = null;
		}
		optionalSelection = optional_sel.OPTIONAL_OMIT;
	}

	public optional_sel get_selection() {
		return optionalSelection;
	}

	public void log() {
		switch (optionalSelection) {
		case OPTIONAL_PRESENT:
			optionalValue.log();
			break;
		case OPTIONAL_OMIT:
			TtcnLogger.log_event_str("omit");
			break;
		case OPTIONAL_UNBOUND:
			TtcnLogger.log_event_unbound();
			break;
		}
	}

	@Override
	/** {@inheritDoc} */
	public void encode_text(final Text_Buf text_buf) {
		switch (optionalSelection) {
		case OPTIONAL_OMIT:
			text_buf.push_int(0);
			break;
		case OPTIONAL_PRESENT:
			text_buf.push_int(1);
			optionalValue.encode_text(text_buf);
			break;
		case OPTIONAL_UNBOUND:
			throw new TtcnError("Text encoder: Encoding an unbound optional value.");
		}
	}

	@Override
	/** {@inheritDoc} */
	public void decode_text(final Text_Buf text_buf) {
		cleanUp();

		final int temp = text_buf.pull_int().getInt();
		if (temp == 1) {
			setToPresent();
			optionalValue.decode_text(text_buf);
		} else {
			setToOmit();
		}
	}

	public boolean isBound() {
		switch (optionalSelection) {
		case OPTIONAL_PRESENT:
		case OPTIONAL_OMIT:
			return true;
		default:
			if (null != optionalValue) {
				return optionalValue.isBound();
			}
			return false;
		}
	}

	//originally is_present
	public boolean isPresent() {
		return optional_sel.OPTIONAL_PRESENT.equals(optionalSelection);
	}

	/**
	 * Note: this is not the TTCN-3 ispresent(), kept for backward compatibility
	 *       with the runtime and existing testports which use this version where
	 *       unbound errors are caught before causing more trouble
	 *
	 * originally ispresent
	 * */
	public boolean isPresentOld() {
		switch (optionalSelection) {
		case OPTIONAL_PRESENT:
			return true;
		case OPTIONAL_OMIT:
			return false;
		default:
			throw new TtcnError("Using an unbound optional field.");
		}
	}

	public boolean isValue() {
		return optional_sel.OPTIONAL_PRESENT.equals(optionalSelection)
				&& optionalValue.isValue();
	}

	public boolean isOptional() {
		return true;
	}

	//originally operator()
	public TYPE get() {
		setToPresent();
		return optionalValue;
	}

	// originally const operator()
	public TYPE constGet() {
		switch (optionalSelection) {
		case OPTIONAL_UNBOUND:
			throw new TtcnError("Using the value of an unbound optional field ");
		case OPTIONAL_OMIT:
			throw new TtcnError("Using the value of an optional field containing omit.");
		default:
			return optionalValue;
		}
	}

	// originally operator==
	public boolean operatorEquals(final template_sel otherValue) {
		if (optional_sel.OPTIONAL_UNBOUND.equals(optionalSelection)) {
			if (template_sel.UNINITIALIZED_TEMPLATE.equals(otherValue)) {
				return true;
			}
			throw new TtcnError("The left operand of comparison is an unbound optional value.");
		}

		if (!template_sel.OMIT_VALUE.equals(otherValue)) {
			throw new TtcnError("Internal error: The right operand of comparison is an invalid value.");
		}

		return optional_sel.OPTIONAL_OMIT.equals(optionalSelection);
	}

	// originally operator==
	public boolean operatorEquals(final Optional<TYPE> otherValue) {
		if (optional_sel.OPTIONAL_UNBOUND.equals(optionalSelection)) {
			if (optional_sel.OPTIONAL_UNBOUND.equals(otherValue.optionalSelection)) {
				return true;
			} else {
				throw new TtcnError("The left operand of comparison is an unbound optional value.");
			}
		} else {
			if (optional_sel.OPTIONAL_UNBOUND.equals(otherValue.optionalSelection)) {
				throw new TtcnError("The right operand of comparison is an unbound optional value.");
			} else {
				if (optionalSelection != otherValue.optionalSelection) {
					return false;
				} else if (optional_sel.OPTIONAL_PRESENT.equals(optionalSelection)) {
					return optionalValue.operatorEquals(otherValue.optionalValue);
				} else {
					return true;
				}
			}
		}
	}

	@Override
	public boolean operatorEquals(final Base_Type otherValue) {
		if (!(otherValue instanceof Optional<?>)) {
			if (optional_sel.OPTIONAL_UNBOUND.equals(optionalSelection)) {
				if (!otherValue.isBound()) {
					return true;
				} else {
					throw new TtcnError("The left operand of comparison is an unbound optional value.");
				}
			} else {
				if (!otherValue.isBound()) {
					throw new TtcnError("The right operand of comparison is an unbound optional value.");
				} else {
					if (optional_sel.OPTIONAL_PRESENT.equals(optionalSelection)) {
						return optionalValue.operatorEquals(otherValue);
					} else {
						return false;
					}
				}
			}
		}

		final Optional<?> optionalOther = (Optional<?>) otherValue;
		if (optional_sel.OPTIONAL_UNBOUND.equals(optionalSelection)) {
			if (optional_sel.OPTIONAL_UNBOUND.equals(optionalOther.optionalSelection)) {
				return true;
			} else {
				throw new TtcnError("The left operand of comparison is an unbound optional value.");
			}
		} else {
			if (optional_sel.OPTIONAL_UNBOUND.equals(optionalOther.optionalSelection)) {
				throw new TtcnError("The right operand of comparison is an unbound optional value.");
			} else {
				if (optionalSelection != optionalOther.optionalSelection) {
					return false;
				} else if (optional_sel.OPTIONAL_PRESENT.equals(optionalSelection)) {
					return optionalValue.operatorEquals(optionalOther.optionalValue);
				} else {
					return true;
				}
			}
		}
	}

	// originally operator!=
	public boolean operatorNotEquals(final template_sel otherValue) {
		return !operatorEquals(otherValue);
	}

	// originally operator!=
	public boolean operatorNotEquals(final Optional<TYPE> otherValue) {
		return !operatorEquals(otherValue);
	}
}
