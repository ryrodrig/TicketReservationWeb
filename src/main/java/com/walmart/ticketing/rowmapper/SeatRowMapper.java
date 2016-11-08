package com.walmart.ticketing.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import com.walmart.ticketing.dto.SeatDetails;

public class SeatRowMapper implements RowMapper<SeatDetails>{

	@Override
	public SeatDetails mapRow(ResultSet rs, int rowNum) throws SQLException {
		SeatDetails seatDetails = new SeatDetails();
		seatDetails.setRowId(rs.getString("ROW_ID"));
		seatDetails.setSeatNumber(rs.getInt("SEAT_NO"));
		seatDetails.setSeatID(rs.getString("SEAT_ID"));
		seatDetails.setSeatStatus(rs.getString("CUST_TXN_STATUS"));
		return seatDetails;
	}

}
