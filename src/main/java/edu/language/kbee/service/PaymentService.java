package edu.language.kbee.service;

import jakarta.servlet.http.HttpServletRequest;

import java.io.UnsupportedEncodingException;

public interface PaymentService {
    String createPaymentUrl(Long packageId, HttpServletRequest request) throws UnsupportedEncodingException;
    boolean handleVnpayReturn(HttpServletRequest request);
}
