package com.resumeai.payment_service.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import com.resumeai.payment_service.dto.Order;
import com.resumeai.payment_service.service.PaypalService;
import com.paypal.api.payments.Links;
import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
@Slf4j
public class PaypalController {

	private final PaypalService service;

	// Use absolute paths for the PayPal config
	// Note: These are the URLs PayPal will call after the user clicks "Pay"
	public static final String BACKEND_SUCCESS_URL = "http://localhost:8080/api/v1/payments/pay/success";
	public static final String BACKEND_CANCEL_URL = "http://localhost:8080/api/v1/payments/pay/cancel";

	// These are your Angular Frontend Routes
	public static final String FRONTEND_SUCCESS_PAGE = "http://localhost:4200/payment-success";
	public static final String FRONTEND_CANCEL_PAGE = "http://localhost:4200/payment-failed";

	// Injecting base URLs
	@Value("${app.gateway-url}")
	private String gatewayUrl;

	@Value("${app.client-url}")
	private String clientUrl;

	// Injecting Paths
	@Value("${app.endpoints.success-path}")
	private String successPath;

	@Value("${app.endpoints.cancel-path}")
	private String cancelPath;

	@Value("${app.endpoints.frontend-success-path}")
	private String frontendSuccessPath;

	@Value("${app.endpoints.frontend-failed-path}")
	private String frontendFailedPath;

	@PostMapping("/pay")
	public String payment(@RequestBody Order order) {
		try {
			// Constructing full Backend URLs for PayPal
			String backendSuccessUrl = gatewayUrl + successPath;
			String backendCancelUrl = gatewayUrl + cancelPath;
			Payment payment = service.createPayment(
					order.getPrice(),
					order.getCurrency(),
					order.getMethod(),
					order.getIntent(),
					order.getDescription(),
					backendCancelUrl,
					backendSuccessUrl
			);
			/*
			 * Here we use loop to get URL because PayPal return multiple links: [self | execute | approval_url]
			 * We want only approval_url that is the reason we traverse over links to get approval_urls
			 */
			for (Links link : payment.getLinks()) {
				if (link.getRel().equals("approval_url")) {
					return link.getHref(); // Angular will receive this and redirect
				}
			}
		} catch (PayPalRESTException e) {
			log.error("Error creating PayPal payment", e);
		}
		throw new RuntimeException("PayPal Exception occurred");
	}

	@GetMapping("/pay/success")
	public RedirectView successPay(@RequestParam("paymentId") String paymentId, @RequestParam("PayerID") String payerId) {
		try {
			Payment payment = service.executePayment(paymentId, payerId);
			if (payment.getState().equals("approved")) {
				// Redirect browser to Angular Success Page
				return new RedirectView(clientUrl+frontendSuccessPath);
			}
		} catch (PayPalRESTException e) {
			log.error("Error executing payment", e);
			throw new RuntimeException("PalPal Exception: PayPal responding with error!");
		}
		return new RedirectView(clientUrl+frontendFailedPath);
	}

	@GetMapping("/pay/cancel")
	public RedirectView cancelPay() {
		return new RedirectView(clientUrl+frontendFailedPath);
	}
}