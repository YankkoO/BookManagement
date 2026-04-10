package kayanko.util.parameter.validation;

/**
 * This class simulates the UNIOVI MP's Argument checks class
 */

public final class ParamCheck {
	
	private static final String MESSAGE = " is not a valid argument.";

	/**
	 * Validate that the argument condition is {@code true}; otherwise throwing an
	 * exception with the specified message. This method is useful when validating
	 * according to an arbitrary boolean expression, such as validating a primitive
	 * number or using your own custom validation expression. This method throws an
	 * IllegalArgumentException with the given message.
	 * 
	 * @param expression the boolean expression to check.
	 * @param message    the message to throw within the exception.
	 * @throws IllegalArgumentException with the given message if the expression is
	 *                                  not true.
	 */
	public static void isTrue(final boolean expression, String message) {
		if (expression == false) {
			throw new IllegalArgumentException(message);
		}
	}

	public static void isTrue(final boolean expression) {
		isTrue(expression, "False" + MESSAGE);
	}

	/**
	 * Validate that the argument condition is false, otherwise it throws an
	 * exception with a message passed as parameter. The exception is thrown as
	 * IllegalArgumentException.
	 * 
	 * @param expression the boolean expression to check.
	 * @param message    the message to throw within the exception.
	 * @throws IllegalArgumentException with the given message if the expression is
	 *                                  true.
	 */
	public static void isFalse(final boolean expression, String message) {
		if (expression == true) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * Validate that the argument condition is false, otherwise it throws an
	 * IllegalArgumentException with a default message. * @param expression the
	 * boolean expression to check.
	 * 
	 * @throws IllegalArgumentException if the expression is true.
	 */
	public static void isFalse(final boolean expression) {
		isFalse(expression, "True" + MESSAGE);
	}

	/**
	 * This method overloads the isTrue method to check that the value of the object
	 * is not null. In case the value is null, an IllegalArgumentException is thrown
	 * with the message given as parameter.
	 * 
	 * @param object  to check whether is null or not.
	 * @param message is the message to throw within the exception if the object is
	 *                null.
	 * @throws IllegalArgumentException if the validation is not fulfilled.
	 */
	public static void isNotNull(final Object object, String messsage) {
		isTrue(object != null, messsage);
	}

	public static void isNotNull(final Object object) {
		isNotNull(object, "null" + MESSAGE);
	}

	/**
	 * This method overloads the isTrue method to check that the value of the given
	 * string is not empty (any kind of characters, including blanks and escape
	 * characters, count). In case the value is empty, an IllegalArgumentException
	 * is thrown with the message given as parameter.
	 * 
	 * @param object  to check whether is empty or not.
	 * @param message is the message to throw within the exception if the object is
	 *                empty.
	 * @throws IllegalArgumentException if the validation is not fulfilled.
	 */
	public static void isNotEmpty(final String string, String message) {
		isTrue(string != null && string.isEmpty() == false, message);
	}

	public static void isNotEmpty(final String string) {
		isNotEmpty(string, "Null or empty string" + MESSAGE);
	}

	/**
	 * This method overloads the isTrue method to check that the value of the given
	 * string is not blank. In case the value is blank, an IllegalArgumentException
	 * is thrown with the message given as parameter.
	 * 
	 * @param object  to check whether is blank or not.
	 * @param message is the message to throw within the exception if the object is
	 *                blank.
	 * @throws IllegalArgumentException if the validation is not fulfilled.
	 */
	public static void isNotBlank(final String string, String message) {
		isTrue(string != null && string.isBlank() == false, message);
	}

	public static void isNotBlank(final String string) {
		isNotBlank(string, "Blank string" + MESSAGE);
	}
	
	
	/**
	 * This method validates whether an String is both null or empty. If any
	 * condition is fulfilled, IllegalArgumentException is thrown.
	 * 
	 * @param str String to check both conditions
	 * @param msg Message to be passed as parameter to the 
	 * 		IllegalArgumentException 
	 * @throws IllegalArgumentException if string passed is null or empty. 
	 */
	public static void notNullEmpty(final String str, String msg) {
		isNotNull(str, msg);
		isNotEmpty(str, msg);
	}
	
	public static void notNullEmpty(final String str) {
		isNotNull(str);
		isNotEmpty(str);
	}

}
