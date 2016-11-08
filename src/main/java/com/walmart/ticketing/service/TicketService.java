package com.walmart.ticketing.service;

import java.util.List;
import java.util.Map;

import com.walmart.ticketing.dto.SeatDetails;
import com.walmart.ticketing.pojo.SeatHold;

public interface TicketService {
/**
* The number of seats in the venue that are neither held nor reserved
*
* @return the number of tickets available in the venue
*/
 int numSeatsAvailable();
/**
* Find and hold the best available seats for a customer
*
* @param numSeats the number of seats to find and hold
* @param customerEmail unique identifier for the customer
* @return a SeatHold object identifying the specific seats and related
information
 * @throws InterruptedException 
*/
 SeatHold findAndHoldSeats(int numSeats, String customerEmail) throws InterruptedException;
/**
* Commit seats held for a specific customer
*
* @param seatHoldId the seat hold identifier
* @param customerEmail the email address of the customer to which the
seat hold is assigned
* @return a reservation confirmation code
*/
String reserveSeats(int seatHoldId, String customerEmail);
/**
 * Reset Reservations
 * @return
 */
boolean resetReservation();

/**
 * Show status of the seats
 * @return
 */
List<SeatDetails> showSeatsStatus();
}