package org.jnbt;


/**
 * The <code>TAG_End</code> tag.
 * @author Graham Edgecombe
 *
 */
public final class EndTag extends Tag {

	/**
	 * Creates the tag.
	 */
	public EndTag() {
		super("");
	}

	@Override
	public Object getValue() {
		return null;
	}
	
	@Override
	public String toString() {
		return "TAG_End";
	}

}