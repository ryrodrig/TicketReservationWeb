package com.walmart.ticketing.pojo;

import java.util.List;

/**
 * @author ryanr_000
 * SeatHold POJO Object
 */
public class SeatHold {
	
	private int seatHoldId;
	
	private List<String> seatNums;
	
	private String msg;
	
	private String url;

	public String getUrl() {
		return url;
	}

	/**
	 * @param url
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * @return
	 */
	public List<String> getSeatNums() {
		return seatNums;
	}

	/**
	 * @param seatNums
	 */
	public void setSeatNums(List<String> seatNums) {
		this.seatNums = seatNums;
	}

	/**
	 * @return
	 */
	public int getSeatHoldId() {
		return seatHoldId;
	}

	/**
	 * @param seatHoldId
	 */
	public void setSeatHoldId(int seatHoldId) {
		this.seatHoldId = seatHoldId;
	}

	/**
	 * @return
	 */
	public String getMsg() {
		return msg;
	}

	/**
	 * @param msg
	 */
	public void setMsg(String msg) {
		this.msg = msg;
	}


}
