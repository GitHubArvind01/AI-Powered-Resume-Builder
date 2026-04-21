package com.resumeai.payment_service.controller;

import org.springframework.web.bind.annotation.*;

import com.resumeai.payment_service.dto.Order;
import com.resumeai.payment_service.service.PaypalService;
import com.paypal.api.payments.Links;
import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
@Slf4j
public class PaypalController {

	private final PaypalService service;

	public static final String SUCCESS_URL = "pay/success";
	public static final String CANCEL_URL = "pay/cancel";

	@PostMapping("/pay")
	public String payment(@RequestBody Order order) {
	    try {
	        Payment payment = service.createPayment(
	                order.getPrice(),
	                order.getCurrency(),
	                order.getMethod(),
	                order.getIntent(),
	                order.getDescription(),
	                "http://localhost:8080/" + CANCEL_URL,
	                "http://localhost:8080/" + SUCCESS_URL
	        );
	        log.info(payment.toJSON());
	        /*
	         * Here we use loop to get URL because PayPal return multiple links: [self | execute | approval_url]
	         * We want only approval_url that is the reason we traverse over links to get approval_urls
	         */
	        for (Links link : payment.getLinks()) {
	            if (link.getRel().equals("approval_url")) {
	                return link.getHref(); // it will return URL
	            }
	        }
	    } catch (PayPalRESTException e) {
	        e.printStackTrace();
	    }
	    throw new RuntimeException("PalPal Exception: It may credentials wrongs or PayPal responding with error!");
	}

	@GetMapping(value = CANCEL_URL)
	public String cancelPay() {
		return "cancel";
	}

	@GetMapping(value = SUCCESS_URL)
	public String successPay(@RequestParam("paymentId") String paymentId, @RequestParam("PayerID") String payerId) {
		try {
			Payment payment = service.executePayment(paymentId, payerId);
			log.info(payment.toJSON());
			if (payment.getState().equals("approved")) {
				return "success";
			}
		} catch (PayPalRESTException e) {
			System.out.println(e.getMessage());
		}
		throw new RuntimeException("PalPal Exception: PayPal responding with error!");
	}
}