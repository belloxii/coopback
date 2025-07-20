package com.gotryt.coop.controller;

import com.gotryt.coop.service.PaystackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/webhook")
public class WebhookController {

    private final PaystackService paystackService;

    @PostMapping("/paystack")
    public ResponseEntity<String> receivePaystackWebhook(@RequestBody Map<String, Object> payload) {
        log.info("🔔 Received Paystack Webhook");
        log.info("🔔 log Full Payload: " + payload);
        System.out.println("Event: " + payload.get("event"));
        System.out.println("Full Payload: " + payload);

        try {
            paystackService.handleWebhook(payload);
            return ResponseEntity.ok("Webhook processed");
        } catch (Exception e) {
            log.error("❌ Error processing Paystack webhook", e);
            return ResponseEntity.status(500).body("Webhook processing failed");
        }
    }
}
