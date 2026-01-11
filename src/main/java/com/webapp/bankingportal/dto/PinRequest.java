package com.webapp.bankingportal.dto;

public record PinRequest(String accountNumber, String pin, String password) {

	public PinRequest(String accountNumber, String pin, String password) {
		this.accountNumber = accountNumber;
		this.pin = pin;
		this.password = password;
	}

}
