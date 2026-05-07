package com.resumeai.auth.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.resumeai.auth.dtos.AuthResponse;
import com.resumeai.auth.dtos.CurrentUserResponseDTO;
import com.resumeai.auth.dtos.EmailEvent;
import com.resumeai.auth.dtos.LoginRequest;
import com.resumeai.auth.dtos.RegisterRequest;
import com.resumeai.auth.dtos.SubscriptionUpdateRequest;
import com.resumeai.auth.dtos.UpdateProfileRequest;
import com.resumeai.auth.dtos.UserResponseDTO;
import com.resumeai.auth.entity.User;
import com.resumeai.auth.exception.BadRequestException;
import com.resumeai.auth.exception.UnauthorizedException;
import com.resumeai.auth.messaging.EmailEventProducer;
import com.resumeai.auth.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class UserServiceImp implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImp.class);
    private static final SecureRandom random = new SecureRandom();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailEventProducer emailEventProducer;

    @Transactional
    @Override
    public String registerRequest(RegisterRequest registerRequest) {
        String normalizedEmail = normalizeEmail(registerRequest.getEmail());
        Optional<User> userOptional = userRepository.findByEmail(normalizedEmail);

        User user;
        if (userOptional.isPresent()) {
            user = userOptional.get();

            if (user.isActive()) {
                throw new RuntimeException("User already registered with this email!");
            }

            updateUserDetails(user, registerRequest);
        } else {
            user = new User();
            user.setFullName(registerRequest.getFullName());
            user.setEmail(normalizedEmail);
            user.setRole("USER");
            user.setSubscriptionPlan("FREE");
            user.setPremiumActive(false);
            user.setSubscriptionStatus("FREE");
            user.setPaymentStatus("UNPAID");
            user.setActive(false);
            updateUserDetails(user, registerRequest);
        }

        String otp = generateOtp();
        user.setOtpCode(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));

        userRepository.save(user);

        EmailEvent event = EmailEvent.builder()
                .to(user.getEmail())
                .otp(otp)
                .subject("Registration OTP")
                .purpose("REGISTER")
                .build();
        emailEventProducer.publishEmailEvent(event);
        log.info("[AUTH] Registration OTP event published for email={}", user.getEmail());

        return "OTP sent to your email for verification.";
    }

    private void updateUserDetails(User user, RegisterRequest request) {
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    }

    @Override
    public AuthResponse registerUser(String email, String otp) {
        User userdb = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new BadRequestException("We couldn't find a pending registration for that email. Please request a new OTP and try again."));
        String normalizedOtp = normalizeOtp(otp);

        if (userdb.getOtpCode() == null || !userdb.getOtpCode().equals(normalizedOtp)) {
            throw new BadRequestException("Invalid OTP. Please check the latest code from your email or request a new OTP.");
        }

        if (userdb.getOtpExpiry() == null || userdb.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP expired. Please request a new OTP and try again.");
        }

        userdb.setActive(true);
        userdb.setOtpCode(null);
        userdb.setOtpExpiry(null);
        ensureFreeDefaults(userdb);
        userRepository.save(userdb);
        return buildAuthResponse(userdb, "User Register success");
    }

    @Override
    public AuthResponse loginUser(LoginRequest loginRequest) {
        User userdb = userRepository.findByEmail(normalizeEmail(loginRequest.getEmail()))
                .orElseThrow(() -> new RuntimeException("User not found!"));

        if (!passwordEncoder.matches(loginRequest.getPassword(), userdb.getPasswordHash())) {
            throw new RuntimeException("Invalid Password!");
        }

        if (!userdb.isActive()) {
            throw new RuntimeException("Account is not verified. Please verify your email using the OTP sent during registration.");
        }

        validateAndUpdateSubscription(userdb);
        return buildAuthResponse(userdb, "Login Success");
    }

    @Override
    public AuthResponse refreshToken(String email) {
        User userdb = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new RuntimeException("User not found!"));

        if (!userdb.isActive()) {
            throw new UnauthorizedException("Your account has been deactivated. Please contact support.");
        }

        validateAndUpdateSubscription(userdb);
        return buildAuthResponse(userdb, "Token refreshed successfully");
    }

    @Override
    public String initiateForgetPassword(String email) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new RuntimeException("User not found!"));

        String otp = generateOtp();
        user.setOtpCode(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        EmailEvent event = EmailEvent.builder()
                .to(email)
                .otp(otp)
                .subject("Forgot Password OTP")
                .purpose("FORGOT_PASSWORD")
                .build();
        emailEventProducer.publishEmailEvent(event);
        log.info("[AUTH] Forgot-password OTP event published for email={}", email);

        return "Verification code sent to your email.";
    }

    @Override
    public String verifyOtp(String email, String otp) {
        User userdb = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new RuntimeException("User not found!"));

        if (userdb.getOtpCode() == null || !userdb.getOtpCode().equals(normalizeOtp(otp))) {
            throw new RuntimeException("OTP Invalid! please try again.");
        }

        if (userdb.getOtpExpiry() == null || !userdb.getOtpExpiry().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("OTP Expired! please try again.");
        }
        return "OTP Verified. You may now reset your password.";
    }

    @Override
    public String resetPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new RuntimeException("User not found!"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setOtpCode(null);
        userRepository.save(user);

        return "Password updated successfully.";
    }

    @Override
    @Transactional
    public AuthResponse updateSubscription(SubscriptionUpdateRequest request) {
        if (request == null || request.getUserId() == null) {
            throw new BadRequestException("User ID is required to update subscription.");
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found!"));

        String normalizedPlan = normalizePlan(request.getPlanType());
        if (request.getPaymentId() != null
                && request.getPaymentId().equals(user.getLastPaymentId())
                && normalizedPlan.equalsIgnoreCase(user.getSubscriptionPlan())
                && Boolean.TRUE.equals(user.getPremiumActive())) {
            validateAndUpdateSubscription(user);
            return buildAuthResponse(user, "Subscription already active.");
        }

        LocalDateTime now = LocalDateTime.now();

        user.setSubscriptionPlan(normalizedPlan);
        user.setPremiumActive(!"FREE".equals(normalizedPlan));
        user.setSubscriptionStatus("FREE".equals(normalizedPlan) ? "FREE" : "ACTIVE");
        user.setSubscriptionStartDate("FREE".equals(normalizedPlan) ? null : now);
        user.setSubscriptionEndDate(resolveSubscriptionEndDate(normalizedPlan, now));
        user.setPaymentStatus(request.getPaymentStatus() == null ? "COMPLETED" : request.getPaymentStatus().toUpperCase());
        user.setLastPaymentId(request.getPaymentId());

        validateAndUpdateSubscription(user);
        userRepository.save(user);
        return buildAuthResponse(user, "Subscription updated to " + normalizedPlan);
    }

    @Override
    @Transactional
    public AuthResponse updateProfile(String email, UpdateProfileRequest updateUser) {
        User userdb = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new RuntimeException("User not found!"));

        boolean isEmailChanging = !userdb.getEmail().equalsIgnoreCase(updateUser.getEmail());

        userdb.setFullName(updateUser.getFullName());
        userdb.setPhone(updateUser.getPhone());
        validateAndUpdateSubscription(userdb);

        if (isEmailChanging) {
            if (userRepository.findByEmail(normalizeEmail(updateUser.getEmail())).isPresent()) {
                throw new RuntimeException("Email already in use by another account!");
            }

            userdb.setPendingEmail(normalizeEmail(updateUser.getEmail()));

            String otp = generateOtp();
            userdb.setOtpCode(otp);
            userdb.setOtpExpiry(LocalDateTime.now().plusMinutes(5));

            EmailEvent event = EmailEvent.builder()
                    .to(updateUser.getEmail())
                    .otp(otp)
                    .subject("Email Update Verification")
                    .purpose("UPDATE_EMAIL")
                    .build();

            emailEventProducer.publishEmailEvent(event);
            log.info("[AUTH] Email-update OTP event published for pendingEmail={}", updateUser.getEmail());
            userdb.setActive(false);
        }

        userRepository.save(userdb);

        String message = isEmailChanging
                ? "Email changed. Please verify your new email. You will be logged out."
                : "Profile updated successfully";

        return buildAuthResponse(userdb, message);
    }

    @Override
    @Transactional
    public AuthResponse verifyEmailUpdate(String currentEmail, String otp) {
        User user = userRepository.findByEmail(normalizeEmail(currentEmail))
                .orElseThrow(() -> new RuntimeException("User not found!"));

        if (user.getOtpCode() == null || !user.getOtpCode().equals(normalizeOtp(otp))) {
            throw new RuntimeException("Invalid OTP!");
        }

        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired!");
        }

        user.setEmail(user.getPendingEmail());
        user.setPendingEmail(null);
        user.setOtpCode(null);
        user.setOtpExpiry(null);
        user.setActive(true);
        validateAndUpdateSubscription(user);
        userRepository.save(user);

        return buildAuthResponse(user, "Email verified successfully. You can continue.");
    }

    @Override
    public UserResponseDTO getUserByEmail(String email) {
        User userdb = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new RuntimeException("User not found!"));
        validateAndUpdateSubscription(userdb);
        return mapUserResponse(userdb);
    }

    @Override
    public UserResponseDTO getUserById(Long id) {
        User userdb = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found!"));
        validateAndUpdateSubscription(userdb);
        return mapUserResponse(userdb);
    }

    @Override
    public CurrentUserResponseDTO getCurrentUser(String email) {
        User userdb = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new RuntimeException("User not found!"));

        if (!userdb.isActive()) {
            throw new UnauthorizedException("Your account has been deactivated. Please contact support.");
        }

        validateAndUpdateSubscription(userdb);
        return mapCurrentUser(userdb);
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void expireSubscriptions() {
        List<User> expiredUsers = userRepository.findByPremiumActiveTrueAndSubscriptionEndDateBefore(LocalDateTime.now());
        if (expiredUsers.isEmpty()) {
            return;
        }

        expiredUsers.forEach(this::downgradeExpiredSubscription);
        userRepository.saveAll(expiredUsers);
        log.info("Expired {} subscription(s) during scheduled validation.", expiredUsers.size());
    }

    private String generateOtp() {
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String normalizeOtp(String otp) {
        return otp == null ? "" : otp.trim();
    }

    private String normalizePlan(String plan) {
        String normalized = plan == null ? "FREE" : plan.trim().toUpperCase();
        return switch (normalized) {
            case "PRO" -> "MONTHLY";
            case "MONTHLY", "YEARLY", "FREE" -> normalized;
            default -> throw new BadRequestException("Unsupported subscription plan: " + plan);
        };
    }

    private LocalDateTime resolveSubscriptionEndDate(String planType, LocalDateTime startDate) {
        return switch (planType) {
            case "MONTHLY" -> startDate.plusMonths(1);
            case "YEARLY" -> startDate.plusYears(1);
            default -> null;
        };
    }

    private void ensureFreeDefaults(User user) {
        if (user.getSubscriptionPlan() == null) {
            user.setSubscriptionPlan("FREE");
        }
        if (user.getPremiumActive() == null) {
            user.setPremiumActive(false);
        }
        if (user.getSubscriptionStatus() == null) {
            user.setSubscriptionStatus("FREE");
        }
        if (user.getPaymentStatus() == null) {
            user.setPaymentStatus("UNPAID");
        }
    }

    private void validateAndUpdateSubscription(User user) {
        ensureFreeDefaults(user);

        if (!Boolean.TRUE.equals(user.getPremiumActive())) {
            if (!"FREE".equalsIgnoreCase(user.getSubscriptionPlan())) {
                user.setSubscriptionPlan("FREE");
            }
            if (!"EXPIRED".equalsIgnoreCase(user.getSubscriptionStatus())) {
                user.setSubscriptionStatus("FREE");
            }
            return;
        }

        LocalDateTime endDate = user.getSubscriptionEndDate();
        if (endDate == null || LocalDateTime.now().isAfter(endDate)) {
            downgradeExpiredSubscription(user);
        } else {
            user.setSubscriptionStatus("ACTIVE");
        }
    }

    private void downgradeExpiredSubscription(User user) {
        user.setSubscriptionPlan("FREE");
        user.setPremiumActive(false);
        user.setSubscriptionStatus("EXPIRED");
        user.setPaymentStatus("EXPIRED");
        user.setSubscriptionStartDate(null);
        user.setSubscriptionEndDate(null);
    }

    private AuthResponse buildAuthResponse(User user, String message) {
        String token = jwtService.generateToken(user);
        return new AuthResponse(token, message, mapCurrentUser(user));
    }

    private CurrentUserResponseDTO mapCurrentUser(User user) {
        return new CurrentUserResponseDTO(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.isActive(),
                user.getSubscriptionPlan(),
                Boolean.TRUE.equals(user.getPremiumActive()),
                user.getSubscriptionStatus(),
                user.getPaymentStatus(),
                user.getLastPaymentId(),
                formatDate(user.getSubscriptionStartDate()),
                formatDate(user.getSubscriptionEndDate())
        );
    }

    private UserResponseDTO mapUserResponse(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.isActive(),
                user.getSubscriptionPlan(),
                Boolean.TRUE.equals(user.getPremiumActive()),
                user.getSubscriptionStatus(),
                user.getPaymentStatus(),
                user.getLastPaymentId(),
                formatDate(user.getSubscriptionStartDate()),
                formatDate(user.getSubscriptionEndDate())
        );
    }

    private String formatDate(LocalDateTime value) {
        return value == null ? null : value.format(DATE_FORMATTER);
    }
}
