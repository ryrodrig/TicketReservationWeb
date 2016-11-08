package com.walmart.ticketing.dao;

import java.util.List;

import com.walmart.ticketing.dto.SeatDetails;

public interface TicketServiceDAO {
	
	/**
	 * Returns the number of seats available
	 * @return
	 */
	int findNumberOfSeats();
	
	/**
	 * Returns SeatDetail object for seats available.
	 * @return
	 */
	List<SeatDetails> findAvailableSeats();
	
	/**
	 * DAO method to assign seat to the emailAddress provided
	 * @param seatIds list of seat IDs to be set to HOLD
	 * @param emailAddress email address of the customer
	 * @return number of seats alloted
	 */
	int holdSeats(List<String> seatIds , String emailAddress);

	/**
	 * Reserve seats to the customer
	 * @param seatHoldID
	 * @param clientEmail
	 * @return
	 */
	boolean reserveSeats(int seatHoldID, String clientEmail);
	
	/**
	 * Resets the reservation
	 * @return
	 */
	boolean resetReservation();

	/**
	 * Retrieve the status for all the seats
	 * @return
	 */
	List<SeatDetails> retrieveSeatStatus();

	/**
	 * Validate if reservation details are valid
	 * @param seatHoldID
	 * @param clientEmail
	 * @return
	 */
	boolean isReservationDetailsValid(int seatHoldID, String clientEmail);

	/**
	 * Validate if request to reserve the ticket is within the ExpireSession time window.
	 * @param seatHoldID
	 * @param clientEmail
	 * @return
	 */
	boolean isReservationWithinExpireTime(int seatHoldID, String clientEmail);

	/**
	 * Check if the reservation is already booked by the user.
	 * @param seatHoldID
	 * @param clientEmail
	 * @return
	 */
	boolean isReservationBooked(int seatHoldID, String clientEmail);

}
