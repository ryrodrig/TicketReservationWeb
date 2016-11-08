package com.walmart.ticketing.exception;

public class TicketUpdateException extends RuntimeException{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String message;
	
	public TicketUpdateException(String msg) {
		// TODO Auto-generated constructor stub
		this.message=msg;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	
	
}
