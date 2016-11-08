package com.walmart.ticketing.webservice;

import java.util.Map;

import org.assertj.core.api.BDDAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.walmart.ticketing.conf.Bootstrapper;
import com.walmart.ticketing.pojo.SeatAvailable;
import com.walmart.ticketing.pojo.SeatHold;

@RunWith(SpringRunner.class)
@SpringBootTest(classes=Bootstrapper.class ,webEnvironment=WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
public class TicketingServiceTest {

	@LocalServerPort
	private int port;
	
	@Autowired
	private TestRestTemplate testRestTemplate;
	
	@Value("${sessionExpireTime}")
	private long expireTime;
	
	@Before
	public void setup() {
		testRestTemplate.put("http://localhost:"+port+"/ticket-reservation/resetReservation",null);
	}
	
	@Test
	public void numSeatsAvailableWhenAllSeatsAvailableExpectOKResponseFreeSeats30() {
		ResponseEntity<SeatAvailable> seatsAvailResponse = testRestTemplate.getForEntity("http://localhost:"+port+"/ticket-reservation/seatsAvailable", SeatAvailable.class);
		BDDAssertions.then(seatsAvailResponse.getStatusCode().value()).isEqualTo(200);
		BDDAssertions.then(seatsAvailResponse.getBody().getFreeSeats()).isEqualTo(30);
	}
	
	@Test
	public void findAndHoldSeatsWhenSeatsAvailableExpectOKResponseAndSeatOnHOLD() {
		ResponseEntity<SeatHold> findAndHoldResp = testRestTemplate.postForEntity("http://localhost:"+port+"/ticket-reservation/find-and-hold?numSeats=5&customerEmail=ryan.rodrigues@walmart.com", null, SeatHold.class);
		BDDAssertions.then(findAndHoldResp.getStatusCode().value()).isEqualTo(201);
		BDDAssertions.then(findAndHoldResp.getBody().getSeatHoldId()).isGreaterThan(0);
		
	}
	
	@Test
	public void findAndHoldSeatsWhenSeatsNotAvailableExpectCONFLICTResponseAndSeatOnHOLD() {
		
		ResponseEntity<SeatHold> findAndHoldResp = testRestTemplate.postForEntity("http://localhost:"+port+"/ticket-reservation/find-and-hold?numSeats=30&customerEmail=ryan.rodrigues@walmart.com", null, SeatHold.class);
		BDDAssertions.then(findAndHoldResp.getStatusCode().value()).isEqualTo(201);
		BDDAssertions.then(findAndHoldResp.getBody().getSeatHoldId()).isGreaterThan(0);
		ResponseEntity<SeatHold> findAndHoldRespUnsuccessful = testRestTemplate.postForEntity("http://localhost:"+port+"/ticket-reservation/find-and-hold?numSeats=30&customerEmail=ryan.rodrigues@walmart.com", null, SeatHold.class);
		BDDAssertions.then(findAndHoldRespUnsuccessful.getStatusCode().value()).isEqualTo(HttpStatus.CONFLICT.value());
		BDDAssertions.then(findAndHoldRespUnsuccessful.getBody().getSeatHoldId()).isEqualTo(0);
		
	}
	
	@Test
	public void reserveSeatsWhenWithRightSeatIdAndEmailAddressExpectOKResponseAndSeatReserved() {
		ResponseEntity<SeatHold> findAndHoldResp = testRestTemplate.postForEntity("http://localhost:"+port+"/ticket-reservation/find-and-hold?numSeats=30&customerEmail=ryan.rodrigues@walmart.com", null, SeatHold.class);
		BDDAssertions.then(findAndHoldResp.getStatusCode().value()).isEqualTo(201);
		BDDAssertions.then(findAndHoldResp.getBody().getSeatHoldId()).isGreaterThan(0);
		ResponseEntity<Map> reserveTicketsResponse = testRestTemplate.exchange("http://localhost:"+port+"/ticket-reservation/reserveSeats?seatHoldId="+findAndHoldResp.getBody().getSeatHoldId()+"&customerEmail=ryan.rodrigues@walmart.com",HttpMethod.PUT,null,Map.class);
		BDDAssertions.then(reserveTicketsResponse.getStatusCode().value()).isEqualTo(HttpStatus.OK.value());
		BDDAssertions.then(Integer.parseInt(reserveTicketsResponse.getBody().get("reservationId").toString())).isEqualTo(findAndHoldResp.getBody().getSeatHoldId());
	}

	// reserve same seat again
	@Test
	public void reserveSeatsWhenWithAlreadyReservedSeatsExpectNOFOUNDResponse() {
		ResponseEntity<SeatHold> findAndHoldResp = testRestTemplate.postForEntity("http://localhost:"+port+"/ticket-reservation/find-and-hold?numSeats=30&customerEmail=ryan.rodrigues@walmart.com", null, SeatHold.class);
		BDDAssertions.then(findAndHoldResp.getStatusCode().value()).isEqualTo(201);
		BDDAssertions.then(findAndHoldResp.getBody().getSeatHoldId()).isGreaterThan(0);
		ResponseEntity<Map> reserveTicketsResponse = testRestTemplate.exchange("http://localhost:"+port+"/ticket-reservation/reserveSeats?seatHoldId="+findAndHoldResp.getBody().getSeatHoldId()+"&customerEmail=ryan.rodrigues@walmart.com",HttpMethod.PUT,null,Map.class);
		BDDAssertions.then(reserveTicketsResponse.getStatusCode().value()).isEqualTo(HttpStatus.OK.value());
		BDDAssertions.then(Integer.parseInt(reserveTicketsResponse.getBody().get("reservationId").toString())).isEqualTo(findAndHoldResp.getBody().getSeatHoldId());
		ResponseEntity<Map> reserveTicketsFailureResponse = testRestTemplate.exchange("http://localhost:"+port+"/ticket-reservation/reserveSeats?seatHoldId="+findAndHoldResp.getBody().getSeatHoldId()+"&customerEmail=ryan.rodrigues@walmart.com",HttpMethod.PUT,null,Map.class);
		BDDAssertions.then(reserveTicketsFailureResponse.getStatusCode().value()).isEqualTo(HttpStatus.NOT_FOUND.value());
	}
	
	// reserve with a different seatId
	@Test
	public void reserveSeatsWhenInvalidSeatIdExpectNOFOUNDResponse() {
		ResponseEntity<SeatHold> findAndHoldResp = testRestTemplate.postForEntity("http://localhost:"+port+"/ticket-reservation/find-and-hold?numSeats=30&customerEmail=ryan.rodrigues@walmart.com", null, SeatHold.class);
		BDDAssertions.then(findAndHoldResp.getStatusCode().value()).isEqualTo(201);
		BDDAssertions.then(findAndHoldResp.getBody().getSeatHoldId()).isGreaterThan(0);
		ResponseEntity<Map> reserveTicketsResponse = testRestTemplate.exchange("http://localhost:"+port+"/ticket-reservation/reserveSeats?seatHoldId="+(findAndHoldResp.getBody().getSeatHoldId()+1)+"&customerEmail=ryan.rodrigues@walmart.com",HttpMethod.PUT,null,Map.class);
		BDDAssertions.then(reserveTicketsResponse.getStatusCode().value()).isEqualTo(HttpStatus.BAD_REQUEST.value());
	}
	
	// reserve after time expire
	@Test
	public void reserveSeatsWhenSessionTimedOutExpectREQUEST_TIMEOUTResponse() throws InterruptedException {
		ResponseEntity<SeatHold> findAndHoldResp = testRestTemplate.postForEntity("http://localhost:"+port+"/ticket-reservation/find-and-hold?numSeats=30&customerEmail=ryan.rodrigues@walmart.com", null, SeatHold.class);
		BDDAssertions.then(findAndHoldResp.getStatusCode().value()).isEqualTo(201);
		BDDAssertions.then(findAndHoldResp.getBody().getSeatHoldId()).isGreaterThan(0);
		Thread.sleep(expireTime*1000);
		ResponseEntity<Map> reserveTicketsResponse = testRestTemplate.exchange("http://localhost:"+port+"/ticket-reservation/reserveSeats?seatHoldId="+findAndHoldResp.getBody().getSeatHoldId()+"&customerEmail=ryan.rodrigues@walmart.com",HttpMethod.PUT,null,Map.class);
		BDDAssertions.then(reserveTicketsResponse.getStatusCode().value()).isEqualTo(HttpStatus.REQUEST_TIMEOUT.value());
	}

	
// Seats available after session expiry	
	@Test
	public void numSeatsAvailableWhenRequestedAfterSessionExpiryTimeExpectOKResponseAndSeatOnHOLD() throws InterruptedException {
		ResponseEntity<SeatAvailable> seatsAvailResponse = testRestTemplate.getForEntity("http://localhost:"+port+"/ticket-reservation/seatsAvailable", SeatAvailable.class);
		BDDAssertions.then(seatsAvailResponse.getStatusCode().value()).isEqualTo(200);
		BDDAssertions.then(seatsAvailResponse.getBody().getFreeSeats()).isEqualTo(30);
		// hold 5 seats
		ResponseEntity<SeatHold> findAndHoldResp = testRestTemplate.postForEntity("http://localhost:"+port+"/ticket-reservation/find-and-hold?numSeats=5&customerEmail=ryan.rodrigues@walmart.com", null, SeatHold.class);
		BDDAssertions.then(findAndHoldResp.getStatusCode().value()).isEqualTo(201);
		BDDAssertions.then(findAndHoldResp.getBody().getSeatHoldId()).isGreaterThan(0);
		ResponseEntity<SeatAvailable> seatsAvailAfterFindAndHoldResponse = testRestTemplate.getForEntity("http://localhost:"+port+"/ticket-reservation/seatsAvailable", SeatAvailable.class);
		BDDAssertions.then(seatsAvailAfterFindAndHoldResponse.getStatusCode().value()).isEqualTo(200);
		BDDAssertions.then(seatsAvailAfterFindAndHoldResponse.getBody().getFreeSeats()).isEqualTo(25);
		// sleep for expiryTimeout
		Thread.sleep(expireTime*1000);
		ResponseEntity<SeatAvailable> seatsAvailAfterExpiryTimeResponse = testRestTemplate.getForEntity("http://localhost:"+port+"/ticket-reservation/seatsAvailable", SeatAvailable.class);
		BDDAssertions.then(seatsAvailAfterExpiryTimeResponse.getStatusCode().value()).isEqualTo(200);
		BDDAssertions.then(seatsAvailAfterExpiryTimeResponse.getBody().getFreeSeats()).isEqualTo(30);
	}

	
}
