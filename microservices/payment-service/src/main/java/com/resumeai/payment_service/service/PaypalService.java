package com.resumeai.payment_service.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.paypal.api.payments.Amount;
import com.paypal.api.payments.Payer;
import com.paypal.api.payments.Payment;
import com.paypal.api.payments.PaymentExecution;
import com.paypal.api.payments.RedirectUrls;
import com.paypal.api.payments.Transaction;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaypalService {

    private final APIContext apiContext;

    public Payment createPayment(Double total, String currency, String method, String intent, String description, String cancelUrl, String successUrl) throws PayPalRESTException {
        /*
         * Here Amount contains - [Currency Type | 2 Decimal Format]
         */
        Amount amount = new Amount();
        amount.setCurrency(currency);
        // Ensure 2 decimal places (e.g., 10.00)
        String formattedTotal = String.format(Locale.forLanguageTag("en-US"), "%.2f", total);
        amount.setTotal(formattedTotal);

        /*
         * Transaction contains - [Description | Amount]
         */
        Transaction transaction = new Transaction();
        transaction.setDescription(description);
        transaction.setAmount(amount);

        /*
         * I use list of Transaction to store transaction
         */
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(transaction);
        
        /*
         * Here we set the payment method - usually it paypal
         */
        Payer payer = new Payer();
        payer.setPaymentMethod(method);

        /*
         * Here in Payment object we set the intent- [sale], payer object, and transaction list
         */
        Payment payment = new Payment();
        payment.setIntent(intent);
        payment.setPayer(payer);
        payment.setTransactions(transactions);
        
        /*
         * Here we set the redirect URL - 
         * it defines where PayPal redirect after user payment approves
         */
        RedirectUrls redirectUrls = new RedirectUrls();
        redirectUrls.setCancelUrl(cancelUrl);
        redirectUrls.setReturnUrl(successUrl);
        payment.setRedirectUrls(redirectUrls);

        /*
         * SDK Calls PayPal API
         * Generate OAuth Token
         * Create Payment Session
         * Return Payment Object with approval_url link
         */
        return payment.create(apiContext);
    }
	/*
	*If this method not called after payment then payment remains pending, money NOT transferred
	*/
    public Payment executePayment(String paymentId, String payerId) throws PayPalRESTException {
        Payment payment = new Payment();
        payment.setId(paymentId);
        PaymentExecution paymentExecute = new PaymentExecution();
        paymentExecute.setPayerId(payerId);
        
        /*
         *Here Payment get capture and we return the status 
         */
        return payment.execute(apiContext, paymentExecute);
    }
}