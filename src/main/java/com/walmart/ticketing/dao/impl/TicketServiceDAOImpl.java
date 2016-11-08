package com.walmart.ticketing.dao.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.walmart.ticketing.dao.TicketServiceDAO;
import com.walmart.ticketing.dto.SeatDetails;
import com.walmart.ticketing.exception.TicketUpdateException;
import com.walmart.ticketing.rowmapper.SeatRowMapper;

@Repository("ticketServiceDAOImpl")
public class TicketServiceDAOImpl implements TicketServiceDAO {
	
	@Autowired
	@Qualifier("jdbctemplate")
	private NamedParameterJdbcTemplate jdbcTemplate;
	
	@Value("${sessionExpireTime}")
	private long sessionExpireTime;

	public long getSessionExpireTime() {
		return sessionExpireTime;
	}

	public void setSessionExpireTime(long sessionExpireTime) {
		this.sessionExpireTime = sessionExpireTime;
	}

	public NamedParameterJdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

	public void setJdbcTemplate(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public int findNumberOfSeats() {
		return jdbcTemplate.queryForObject(
				"select count(*) from SEAT_ALLOCATION SA where "
				+ "SA.CUST_TXN_STATUS in ('AVAILABLE','HOLD') AND  SA.ALLOTMENT_TIME < TIMESTAMPADD (SQL_TSI_SECOND,-"+sessionExpireTime+", NOW())",
				new HashMap<String, String>(), Integer.class);
	}
	
	//
	
	@SuppressWarnings("unchecked")
	public List<SeatDetails> findAvailableSeats() {
		return (List<SeatDetails>) jdbcTemplate.query("select SD.*,SA.CUST_TXN_STATUS from SEAT_ALLOCATION SA ,SEAT_DET SD where "
				+ "SA.CUST_TXN_STATUS in ('AVAILABLE','HOLD') AND  SA.ALLOTMENT_TIME < TIMESTAMPADD (SQL_TSI_SECOND,-"+sessionExpireTime+", NOW()) and SD.SEAT_ID=SA.SEAT_ID", new SeatRowMapper());
	}

	/* (non-Javadoc)
	 * @see com.walmart.ticketing.dao.TicketServiceDAO#holdSeats(java.util.List, java.lang.String)
	 */
	@Override
	@Transactional(propagation=Propagation.REQUIRED)
	public int holdSeats(List<String> seatIds, String emailAddress) {
		Map<String,Object> namedParam = new HashMap<>();
		namedParam.put("custEmail", emailAddress.toLowerCase());
		namedParam.put("seatIds", seatIds);
		namedParam.put("txnStatus", "HOLD");
		
		int txnID = jdbcTemplate.queryForObject("call NEXT VALUE FOR TXN_ID_SEQ", new HashMap<>(), Integer.class);
		namedParam.put("custId", txnID);
		jdbcTemplate.update("insert into CUST_TXN_DETAILS(CUST_TXN_ID,CUST_EMAIL) values(:custId,:custEmail)", namedParam);
		// insert record into CUST_TXN_DETAILS table.
		int updatedRecords = jdbcTemplate.update("Update SEAT_ALLOCATION SA set CUST_TXN_ID=:custId,CUST_TXN_STATUS='HOLD',ALLOTMENT_TIME=now() where SEAT_ID in (:seatIds) and SA.ALLOTMENT_TIME < TIMESTAMPADD (SQL_TSI_SECOND,-"+sessionExpireTime+", NOW())",namedParam);
		
		if(updatedRecords!=seatIds.size()) {
			throw new TicketUpdateException("Seat Reservation Collision. Rolling Back Transaction.. Retry Again to resolve conflict...");
		}
		
		System.out.println(updatedRecords);
		
		return txnID;
	}
	
	/**
	 * Reserve seats for the clientEmailId and the seatHoldId provided
	 * @param seatHoldID Customer transaction id 
	 * @param clientEmail email address of the client
	 * @return true if any of the rows are updated and false other wise
	 */
	@Override
	@Transactional(propagation=Propagation.REQUIRED)
	public boolean reserveSeats(int seatHoldID, String clientEmail) {
		
		Map<String, String> namedParam = new HashMap<>();
		namedParam.put("emailAddress", clientEmail);
		namedParam.put("seatHoldID", String.valueOf(seatHoldID));
		int updateCount = jdbcTemplate.update("Update SEAT_ALLOCATION SA set CUST_TXN_STATUS='RESERVED',ALLOTMENT_TIME=now() where CUST_TXN_ID=:seatHoldID", namedParam);
		return updateCount>0?true:false;
	}
	
	@Override
	public boolean isReservationDetailsValid(int seatHoldID,String clientEmail) {
		Map<String, String> namedParam = new HashMap<>();
		namedParam.put("emailAddress", clientEmail);
		namedParam.put("seatHoldID", String.valueOf(seatHoldID));
		int queryCount = jdbcTemplate.queryForObject("select count(*) from CUST_TXN_DETAILS CD where CUST_EMAIL =:emailAddress and CUST_TXN_ID =:seatHoldID", namedParam,Integer.class);
		return queryCount>0?true:false;
	}
	
	@Override
	public boolean isReservationBooked(int seatHoldID,String clientEmail) {
		Map<String, String> namedParam = new HashMap<>();
		namedParam.put("emailAddress", clientEmail);
		namedParam.put("seatHoldID", String.valueOf(seatHoldID));
		int queryCount = jdbcTemplate.queryForObject("select count(*) from CUST_TXN_DETAILS CD , SEAT_ALLOCATION SA where CUST_EMAIL =:emailAddress and CUST_TXN_ID =:seatHoldID AND CUST_TXN_STATUS='RESERVED' AND CD.CUST_TXN_ID = SA.CUST_TXN_ID", namedParam,Integer.class);
		return queryCount>0?true:false;
	}
	
	@Override
	public boolean isReservationWithinExpireTime(int seatHoldID,String clientEmail) {
		Map<String, String> namedParam = new HashMap<>();
		namedParam.put("emailAddress", clientEmail);
		namedParam.put("seatHoldID", String.valueOf(seatHoldID));
		int queryCount = jdbcTemplate.queryForObject("select count(*) from CUST_TXN_DETAILS CD , SEAT_ALLOCATION SA where CUST_EMAIL =:emailAddress and CUST_TXN_ID =:seatHoldID AND CUST_TXN_STATUS='HOLD' AND CD.CUST_TXN_ID = SA.CUST_TXN_ID AND SA.ALLOTMENT_TIME > TIMESTAMPADD (SQL_TSI_SECOND,-"+sessionExpireTime+", NOW())", namedParam,Integer.class);
		return queryCount>0?true:false;
	}
	
	
	@Override
	@Transactional
	public boolean resetReservation() {
		jdbcTemplate.update("update SEAT_ALLOCATION SA set CUST_TXN_STATUS=DEFAULT,ALLOTMENT_TIME=DEFAULT,CUST_TXN_ID=NULL ", new HashMap<>());
		return true;
	}
	
	@Override
	public List<SeatDetails> retrieveSeatStatus(){
		return jdbcTemplate.query("select SA.CUST_TXN_STATUS,SD.* from SEAT_ALLOCATION SA,SEAT_DET SD where SD.SEAT_ID=SA.SEAT_ID",new SeatRowMapper());
	}

}
