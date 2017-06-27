package org.eclipse.titan.runtime.core;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * "record of" type for runtime 2
 * Originally Basetype.hh/Record_Of_Type, Basetype2.cc/Record_Of_Type
 * @author Arpad Lovassy
 */
public class TitanRecordOf extends Base_Type {

	/**
	 * Indexed sequence of elements of the same type
	 * Originally value_elements
	 */
	List<Base_Type> valueElements;

	/** type of the element of "record of" */
	private final Class<? extends Base_Type> ofType;

	public TitanRecordOf(final Class<? extends Base_Type> aOfType) {
		this.ofType = aOfType;
	}

	public TitanRecordOf( final TitanRecordOf other_value ) {
		other_value.mustBound("Copying an unbound record of/set of value.");
		ofType = other_value.ofType;
		valueElements = copyList( other_value.valueElements );
	}

	@Override
	public boolean isPresent() {
		return isBound();
	}

	@Override
	public boolean isBound() {
		return valueElements != null;
	}

	public void mustBound( final String aErrorMessage ) {
		if ( isBound() ) {
			throw new TtcnError( aErrorMessage );
		}
	}

	//originally clean_up
	public void cleanUp() {
		valueElements = null;
	}

	/**
	 * @return true if and only if otherValue is the same record of type as this
	 */
	private boolean isSameType( Base_Type otherValue ) {
		return otherValue instanceof TitanRecordOf && ofType == ((TitanRecordOf)otherValue).ofType;
	}

	@Override
	public TitanBoolean operatorEquals(Base_Type otherValue) {
		if ( !isSameType( otherValue ) ) {
			throw new TtcnError(MessageFormat.format("Internal Error: value `{0}'' can not be cast to record of {1}", otherValue, getOfTypeName() ));
		}

		//compiler warning is incorrect, it is not unchecked
		return operatorEquals( ( TitanRecordOf ) otherValue );
	}

	//originally operator==
	public TitanBoolean operatorEquals( final TitanRecordOf otherValue ) {
		mustBound("The left operand of comparison is an unbound value of type record of " + getOfTypeName() + ".");
		otherValue.mustBound("The right operand of comparison is an unbound value of type" + otherValue.getOfTypeName() + ".");

		final int size = valueElements.size(); 
		if ( size != otherValue.valueElements.size() ) {
			return new TitanBoolean( false );
		}

		for ( int i = 0; i < size; i++ ) {
			final Base_Type leftElem = valueElements.get( i );
			final Base_Type rightElem = otherValue.valueElements.get( i );
			if ( leftElem.operatorEquals( rightElem ).not().getValue() ) {
				return new TitanBoolean( false );
			}
		}

		return new TitanBoolean( true );
	}

	@Override
	public Base_Type assign(Base_Type otherValue) {
		if ( !isSameType( otherValue ) ) {
			throw new TtcnError(MessageFormat.format("Internal Error: value `{0}'' can not be cast to record of {1}", otherValue, getOfTypeName() ));
		}

		//compiler warning is incorrect, it is not unchecked
		return assign( ( TitanRecordOf ) otherValue );
	}

	//originally operator=
	public TitanRecordOf assign( final TitanRecordOf aOtherValue ) {
		aOtherValue.mustBound( "Assignment of an unbound record of value." );

		valueElements = aOtherValue.valueElements;
		return this;
	}

	public final List<Base_Type> copyList( final List<Base_Type> srcList ) {
		if ( srcList == null ) {
			return null;
		}

		final List<Base_Type> newList = new ArrayList<Base_Type>( srcList.size() );
		for (Base_Type srcElem : srcList) {
			Base_Type newElem = getUnboundElem();
			newElem.assign( srcElem );
			newList.add( ( newElem ) );
		}
		return newList;
	}

	//originally get_at(int)
	public Base_Type getAt( final int index_value ) {
		if (index_value < 0) {
			throw new TtcnError( MessageFormat.format("Accessing an element of type record of {0} using a negative index: {1}.", getOfTypeName(), index_value));
		}

		if (index_value >= valueElements.size() ) {
			//increase list size
			for ( int i = valueElements.size(); i <= index_value; i++ ) {
				valueElements.set( index_value, null );
			}
		}

		if ( valueElements.get( index_value ) == null ) {
			Base_Type newElem = getUnboundElem();
			valueElements.set( index_value, newElem );
		}
		return valueElements.get( index_value );
	}

	//originally get_at(const INTEGER&)
	public Base_Type getAt(final TitanInteger index_value) {
		index_value.mustBound( MessageFormat.format( "Using an unbound integer value for indexing a value of type {0}.", getOfTypeName() ) );
		return getAt( index_value.getInt() );
	}

	//originally get_at(int) const
	public Base_Type constGetAt( final int index_value ) {
		if ( !isBound() ) {
			throw new TtcnError( MessageFormat.format("Accessing an element in an unbound value of type record of {0}.", getOfTypeName()));
		}
		if (index_value < 0) {
			throw new TtcnError( MessageFormat.format("Accessing an element of type record of {0} using a negative index: {1}.", getOfTypeName(), index_value));
		}
		final int nofElements = getNofElements();
		if ( index_value >= nofElements ) {
			throw new TtcnError( MessageFormat.format( "Index overflow in a value of type record of {0}: The index is {1}, but the value has only {2} elements.",
					getOfTypeName(), index_value, nofElements ) );
		}

		final Base_Type elem = valueElements.get( index_value );
		return ( elem != null ) ? elem : getUnboundElem();
	}

	//originally get_at(const INTEGER&) const
	public Base_Type constGetAt(final TitanInteger index_value) {
		index_value.mustBound( MessageFormat.format( "Using an unbound integer value for indexing a value of type {0}.", getOfTypeName() ) );

		return constGetAt( index_value.getInt() );
	}

	private Base_Type getUnboundElem() {
		try {
			return ofType.newInstance();
		} catch (Exception e) {
			throw new TtcnError(MessageFormat.format("Internal Error: exception `{0}'' thrown while instantiating class of `{1}'' type", e.getMessage(), getOfTypeName()));
		}
	}

	private static String getOfTypeName( final Class<? extends Base_Type> aOfType ) {
		//TODO: get type name
		if ( aOfType == TitanBoolean.class ) {
			return "boolean";
		} else if ( aOfType == TitanBitString.class ) {
			return "bitstring";
		} else {
			return aOfType.toString();
		}
	}

	private String getOfTypeName() {
		return getOfTypeName( ofType );
	}

	public int getNofElements() {
		if ( valueElements == null ) {
			return 0;
		}
		return valueElements.size();
	}

	public void add( final Base_Type aElement ) {
		if ( aElement.getClass() != ofType ) {
			throw new TtcnError(MessageFormat.format("Adding a {0} type variable to a record of {1}", getOfTypeName( aElement.getClass() ), getOfTypeName()));
		}
		if ( valueElements == null ) {
			valueElements = new ArrayList<Base_Type>();
		}
		valueElements.add( aElement );
	}

}