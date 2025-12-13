package com.webapp.bankingportal.dto;

import lombok.val;

public record PinRequest(String accountNumber, String pin, String password) {


	public PinRequest(String accountNumber, String pin, String password) {
	    this.accountNumber = accountNumber;
	    this.pin = pin;
	    this.password = password;
	}
	public val get(val string) {
		// TODO Auto-generated method stub
		return string;
	}
}
