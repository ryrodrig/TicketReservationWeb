package com.walmart.ticketing.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.walmart.ticketing.dao.TicketServiceDAO;
import com.walmart.ticketing.dto.SeatDetails;
import com.walmart.ticketing.exception.TicketUpdateException;
import com.walmart.ticketing.pojo.SeatHold;
import com.walmart.ticketing.service.TicketService;

@Component("ticketServiceImpl")
public class TicketServiceImpl implements TicketService {
	
	@Value("${sessionExpireTime}")
	private long sessionExpireTime;
	
	private static int MAX_RETRY=3;
	
	private ThreadLocal<Integer> retryCount = new ThreadLocal<Integer>(){ 
		@Override 
		protected Integer initialValue() {
		return 0;
	} };
	
	@Autowired
	@Qualifier("ticketServiceDAOImpl")
	private TicketServiceDAO ticketServiceDAO;

	@Override
	public int numSeatsAvailable() {
		// TODO Auto-generated method stub
		return ticketServiceDAO.findNumberOfSeats();
	}

	/**
	 * Find and hold seats for the customer
	 * @param numSeats
	 * @param customerEmail
	 * @return
	 * @throws InterruptedException
	 */
	@Override
	@Transactional(propagation=Propagation.REQUIRED)
	public SeatHold findAndHoldSeats(int numSeats, String customerEmail) throws InterruptedException {
		SeatHold seatHold = new SeatHold();
		// Find Available seats
		List<SeatDetails> seatDetails = ticketServiceDAO.findAvailableSeats();
		seatHold.setMsg("Currently cannot allocate " + numSeats + ".Number of seats available to book : " + seatDetails.size());
		// if seats requested are lesser than available seats return
		if(seatDetails.size()<numSeats) {
			return seatHold;
		}
		// find the best seat available
		List<String> seatNums = findSeatIds(seatDetails, numSeats);
		if(seatNums.isEmpty()) {
			// allocate random seats.
			seatNums = allocateRandomSeats(seatDetails, numSeats);
			if(seatNums.isEmpty()) {
				return seatHold;
			}
		}
		int custTxnId=0;
		try {
			// hold identified seats.
			custTxnId = ticketServiceDAO.holdSeats(seatNums, customerEmail);
		} catch (TicketUpdateException e) {
			// handle exception if some other session updates the row.
			if(retryCount.get()> MAX_RETRY) {
				seatHold.setMsg("Currently cannot allocate " + numSeats + ".Please retry after sometime.");
				return seatHold;
			}
			// retry for a specified number of times to allocate the seat.
			retryCount.set(retryCount.get()+1);
			Thread.sleep(1000*retryCount.get().intValue());
			findAndHoldSeats(numSeats, customerEmail);
		}
		 
		seatHold.setSeatHoldId(custTxnId);
		seatHold.setSeatNums(seatNums);
		seatHold.setMsg("Reserve the seats by next " + sessionExpireTime + " seconds, to avoid auto cancellation.");
		seatHold.setUrl("/ticket-reservation/reserveSeats?seatHoldId="+custTxnId+"&customerEmail="+customerEmail);
		return seatHold;
	}
	
	/**
	 * @param seatDetails
	 * @param numSeats
	 * @return
	 */
	private List<String> allocateRandomSeats(List<SeatDetails> seatDetails,int numSeats){
		List<String> seatNumbers = seatDetails.stream().map(seatNum->seatNum.getRowId()+seatNum.getSeatNumber()).collect(Collectors.toList());
		if(seatNumbers.size()<numSeats) {
			return new ArrayList<>();
		}
		return seatNumbers.subList(0, numSeats);
	}
	
	
	/**
	 * Retrieves available seats and their Row details.
	 * 
	 * @param seatDetails seat details from DB
	 * @param numSeats requested number of seats
	 * @return returns the best available seats.
	 */
	private List<String> findSeatIds(List<SeatDetails> seatDetails,int numSeats) {
		Map<String, List<Integer>> rowToSeatMap = new TreeMap<String,List<Integer>>();
		for(SeatDetails seatDetail : seatDetails) {
			String rowId = seatDetail.getRowId();
			List<Integer> seatIds = rowToSeatMap.get(rowId);
			if(!rowToSeatMap.containsKey(rowId)) {
				if(seatIds == null || seatIds.isEmpty()) {
					seatIds = new ArrayList<Integer>();
				}
			}
			seatIds.add(seatDetail.getSeatNumber());
			rowToSeatMap.put(rowId, seatIds);
		}
		return checkSeatsAvailFromRow(rowToSeatMap,numSeats);
	}
	
	
	/**
	 * Returns the best available seats.
	 * method tries to retrieve consequetive seats for the user.
	 * @param rowToSeatMap
	 * @param numSeats
	 * @return returns availabe seats
	 */
	private List<String> checkSeatsAvailFromRow(Map<String, List<Integer>> rowToSeatMap,int numSeats) {
		Set<Entry<String, List<Integer>>> sortedSet = new TreeSet<Entry<String, List<Integer>>>(new Comparator<Entry<String, List<Integer>>>() {

			@Override
			public int compare(Entry<String, List<Integer>> o1, Entry<String, List<Integer>> o2) {
				// TODO Auto-generated method stub
				return o1.getValue().size() > o2.getValue().size() ? 1 : o1.getValue().size() == o2.getValue().size() ? 0 : -1;
			}
		});
		
		sortedSet.addAll(rowToSeatMap.entrySet());
		
		for(Entry<String, List<Integer>> rowToSeatId : sortedSet) {
			if(rowToSeatId.getValue().size() >= numSeats) {
				List<String> finalSeatIds = checkConsecutiveSeats(rowToSeatId.getKey(),rowToSeatId.getValue(),numSeats);
				if(finalSeatIds.isEmpty()) {
					continue;
				} else {
					return finalSeatIds;
				}
			}
		}
		
		return new ArrayList<>();
	}
	
	
	/**
	 * Method to retrieve consecutive seats in a row.
	 * @param rowId row ID
	 * @param seatIds seatIds
	 * @param numSeats requested number of seats
	 * @return seats to be allocated
	 */
	private List<String> checkConsecutiveSeats(String rowId,List<Integer> seatIds,int numSeats) {
		List<Integer> finalSeats = new ArrayList<Integer>();
		Integer [] seatArr = new Integer[seatIds.size()];
		seatIds.toArray(seatArr);
		Arrays.sort(seatArr);
		int previousVal = seatArr[0];
		finalSeats.add(previousVal);
		for(int i=1;i<seatArr.length;i++) {
			if(seatArr[i]==(previousVal+1)) {
				if(finalSeats.size() == (numSeats-1)) {
					finalSeats.add(seatArr[i]);
					break;
				}
			} else {
				finalSeats.clear();
				
			}
			finalSeats.add(seatArr[i]);
			previousVal = seatArr[i];
		}
		if(finalSeats.size() != numSeats) {
			finalSeats =  new ArrayList<Integer>();
		}
		return finalSeats.stream().map(seatId->rowId+seatId).collect(Collectors.toList());
	}

	/**
	 * Method to reserve seats for the customer.
	 * @param seatHoldId
	 * @param customerEmail
	 * @return
	 */
	@Override
	public String reserveSeats(int seatHoldId, String customerEmail) {
		
		if(!ticketServiceDAO.isReservationDetailsValid(seatHoldId, customerEmail)) {
			return "BAD_REQUEST : seatHoldId and Customer Email Address don't match";
		}
		
		if(ticketServiceDAO.isReservationBooked(seatHoldId, customerEmail)) {
			return "NOT_FOUND : Reservation already booked.";
		}
		
		if(!ticketServiceDAO.isReservationWithinExpireTime(seatHoldId, customerEmail)) {
			return "REQUEST_TIMEOUT : Session expired. Please start the booking process again.";
		}
		
		if(ticketServiceDAO.reserveSeats(seatHoldId, customerEmail)) {
			return String.valueOf(seatHoldId);
		}
		return "";
//		return 
	}
	
	
	/* (non-Javadoc)
	 * @see com.walmart.ticketing.service.TicketService#resetReservation()
	 */
	@Override
	public boolean resetReservation() {
		return ticketServiceDAO.resetReservation();
	}
	
	/* (non-Javadoc)
	 * @see com.walmart.ticketing.service.TicketService#showSeatsStatus()
	 */
	@Override
	public List<SeatDetails> showSeatsStatus() {
		return ticketServiceDAO.retrieveSeatStatus();
	}

}
