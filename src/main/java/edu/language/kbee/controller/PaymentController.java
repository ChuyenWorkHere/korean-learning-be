package edu.language.kbee.controller;

import edu.language.kbee.payload.PaymentResDto;
import edu.language.kbee.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/create_payment")
    public ResponseEntity<PaymentResDto> createPayment(
            @RequestParam("packageId") Long packageId,
            HttpServletRequest request
    ) {
        try {
            String paymentUrl = paymentService.createPaymentUrl(packageId, request);
            PaymentResDto res = new PaymentResDto("OK", "Successfully created payment url", paymentUrl);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new PaymentResDto("FAILED", e.getMessage(), null));
        }
    }

    @GetMapping("/vnpay_return")
    public ResponseEntity<?> vnpayReturn(HttpServletRequest request) {
        boolean success = paymentService.handleVnpayReturn(request);
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        if (success) {
            response.put("message", "Payment completed successfully");
            return ResponseEntity.ok(response);
        } else {
            response.put("message", "Payment failed or verification error");
            return ResponseEntity.badRequest().body(response);
        }
    }
}
