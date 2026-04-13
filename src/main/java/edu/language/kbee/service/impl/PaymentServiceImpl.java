package edu.language.kbee.service.impl;

import edu.language.kbee.config.VNPayConfig;
import edu.language.kbee.model.SubscriptionPackage;
import edu.language.kbee.model.Role;
import edu.language.kbee.model.User;
import edu.language.kbee.enums.RoleName;
import edu.language.kbee.repository.RoleRepository;
import edu.language.kbee.repository.SubscriptionPackageRepository;
import edu.language.kbee.repository.UserRepository;
import edu.language.kbee.model.PaymentTransaction;
import edu.language.kbee.repository.PaymentTransactionRepository;
import edu.language.kbee.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    @Value("${vnp_TmnCode}")
    private String vnp_TmnCode;

    @Value("${vnp_HashSecret}")
    private String vnp_HashSecret;

    @Value("${vnp_Url}")
    private String vnp_Url;

    @Value("${vnp_ReturnUrl}")
    private String vnp_ReturnUrl;

    private final SubscriptionPackageRepository packageRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PaymentTransactionRepository transactionRepository;

    @Override
    public String createPaymentUrl(Long packageId, HttpServletRequest request) throws UnsupportedEncodingException {
        SubscriptionPackage pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new RuntimeException("Package not found"));

        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String orderType = "other";
        long amount = pkg.getPrice().multiply(new BigDecimal(100)).longValue();
        String bankCode = request.getParameter("bankCode");

        String vnp_TxnRef = VNPayConfig.getRandomNumber(8);
        String vnp_IpAddr = VNPayConfig.getIpAddress(request);

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");

        if (bankCode != null && !bankCode.isEmpty()) {
            vnp_Params.put("vnp_BankCode", bankCode);
        }
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        // The packageId is sent via orderInfo to be used in the return callback
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        vnp_Params.put("vnp_OrderInfo", "User_" + currentUsername + "_PackageId_" + packageId);
        vnp_Params.put("vnp_OrderType", orderType);
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                // Build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        String queryUrl = query.toString();
        String vnp_SecureHash = VNPayConfig.hmacSHA512(vnp_HashSecret, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        return vnp_Url + "?" + queryUrl;
    }

    @Override
    public boolean handleVnpayReturn(HttpServletRequest request) {
        Map fields = new HashMap();
        for (Enumeration params = request.getParameterNames(); params.hasMoreElements();) {
            String fieldName = (String) params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                fields.put(fieldName, fieldValue);
            }
        }

        String vnp_SecureHash = request.getParameter("vnp_SecureHash");
        if (fields.containsKey("vnp_SecureHashType")) {
            fields.remove("vnp_SecureHashType");
        }
        if (fields.containsKey("vnp_SecureHash")) {
            fields.remove("vnp_SecureHash");
        }
        String signValue = VNPayConfig.hmacSHA512(vnp_HashSecret, VNPayConfig.buildHashData(fields));

        if (signValue.equals(vnp_SecureHash)) {
            if ("00".equals(request.getParameter("vnp_ResponseCode"))) {
                // Payment successful!
                String orderInfo = request.getParameter("vnp_OrderInfo");
                String txnRef = request.getParameter("vnp_TxnRef");
                
                if (transactionRepository.existsByTxnRef(txnRef)) {
                    return true;
                }

                if (orderInfo != null && orderInfo.contains("User_") && orderInfo.contains("PackageId_")) {
                    try {
                        String[] parts = orderInfo.split("_");
                        String username = parts[1];
                        Long packageId = Long.parseLong(parts[3]);

                        User user = userRepository.findByUsername(username).orElse(null);
                        SubscriptionPackage pkg = packageRepository.findById(packageId).orElse(null);

                        if (user != null && pkg != null) {
                            user.setIsPremium((byte) 1);
                            LocalDate currentExpiry = user.getPremiumExpiryDate();
                            if (currentExpiry == null || currentExpiry.isBefore(LocalDate.now())) {
                                currentExpiry = LocalDate.now();
                            }
                            user.setPremiumExpiryDate(currentExpiry.plusDays(pkg.getDurationDays()));
                            
                            Role premiumRole = roleRepository.findByRoleName(RoleName.PREMIUM).orElse(null);
                            if (premiumRole != null && !user.getRoles().contains(premiumRole)) {
                                user.getRoles().add(premiumRole);
                            }
                            userRepository.save(user);
                            
                            PaymentTransaction transaction = PaymentTransaction.builder()
                                    .txnRef(txnRef)
                                    .packageId(packageId)
                                    .username(username)
                                    .createdAt(java.time.LocalDateTime.now())
                                    .build();
                            transactionRepository.save(transaction);
                            
                            return true;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                }
            }
        }
        return false;
    }

}
