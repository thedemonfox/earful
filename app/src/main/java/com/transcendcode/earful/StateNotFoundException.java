package com.transcendcode.earful;

public class StateNotFoundException extends Exception
{

  /**
	 * 
	 */
	private static final long serialVersionUID = -1468272340099382266L;

	public StateNotFoundException(String message) {
      super(message);
  }

  public StateNotFoundException(String message, Throwable throwable) {
      super(message, throwable);
  }
}
