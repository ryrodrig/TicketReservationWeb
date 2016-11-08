package com.walmart.ticketing.webservice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.walmart.ticketing.dto.SeatDetails;
import com.walmart.ticketing.pojo.SeatAvailable;
import com.walmart.ticketing.pojo.SeatHold;
import com.walmart.ticketing.service.TicketService;

@Controller
@RequestMapping("ticket-reservation")
public class TicketReservationWebService {
	
	@Autowired
	TicketService ticketService;
	
	@RequestMapping(path="seatsAvailable",method=RequestMethod.GET,produces="application/json")
	public @ResponseBody SeatAvailable numSeatsAvailable() {
		SeatAvailable seatAvailable = new SeatAvailable();
		seatAvailable.setFreeSeats(ticketService.numSeatsAvailable());
		return seatAvailable;
	}
	
	@RequestMapping(path="find-and-hold",method=RequestMethod.POST,produces="application/json")
	public @ResponseBody SeatHold findAndHoldSeats(@RequestParam(value="numSeats",required=true)Integer numSeats,@RequestParam(value="customerEmail",required=true)String customerEmail,HttpServletResponse response) throws InterruptedException{
		SeatHold seatHold = ticketService.findAndHoldSeats(numSeats, customerEmail);
		if(seatHold.getSeatHoldId()>0) {
			response.setStatus(HttpStatus.CREATED.value());
		} else {
			response.setStatus(HttpStatus.CONFLICT.value());
		}
		return seatHold;
	}
	
	@RequestMapping(path="reserveSeats",method=RequestMethod.PUT,produces="application/json")
	public @ResponseBody Map<String,String> reserveSeats(@RequestParam(value="seatHoldId",required=true)int seatHoldId,@RequestParam(value="customerEmail",required=true)String customerEmail,HttpServletResponse response) {
		String responseStr = ticketService.reserveSeats(seatHoldId, customerEmail);
		Map<String,String> responseMap = new HashMap<>();
		
		if(responseStr.contains("BAD_REQUEST")) {
			response.setStatus(HttpStatus.BAD_REQUEST.value());
			responseMap.put("msg", responseStr);
		} else if(responseStr.contains("NOT_FOUND")) {
			response.setStatus(HttpStatus.NOT_FOUND.value());
			responseMap.put("msg", responseStr);
		}else if(responseStr.contains("REQUEST_TIMEOUT")) {
			response.setStatus(HttpStatus.REQUEST_TIMEOUT.value());
			responseMap.put("msg", responseStr);
		} else if(!responseStr.isEmpty()) {
			responseMap.put("reservationId", responseStr);
			responseMap.put("msg", "Reservation successful. Reservation ID : " + responseStr);
			response.setStatus(HttpStatus.OK.value());
		} else {
			responseMap.put("msg", "Reservation unsuccessful. Check Reservation Number and the emailAddress provided. Call Customer care for futher assistance");
			response.setStatus(HttpStatus.NOT_FOUND.value());
		}
		return responseMap;
	}
	
	@RequestMapping(path="resetReservation",method=RequestMethod.PUT)
	public @ResponseBody boolean cancelReservation() {
		return ticketService.resetReservation();
	}
	
	@RequestMapping(path="showSeatStatus",method=RequestMethod.GET)
	public @ResponseBody List<SeatDetails> showSeatsStatus() {
		return ticketService.showSeatsStatus();
	}

}
