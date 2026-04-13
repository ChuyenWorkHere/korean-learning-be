package edu.language.kbee.service.impl;

import edu.language.kbee.enums.RoleName;
import edu.language.kbee.exception.BadRequestException;
import edu.language.kbee.exception.InternalServerException;
import edu.language.kbee.exception.ResourceNotFoundException;
import edu.language.kbee.model.Role;
import edu.language.kbee.model.User;
import edu.language.kbee.payload.UserDto;
import edu.language.kbee.payload.request.LoginRequest;
import edu.language.kbee.payload.request.SignUpRequest;
import edu.language.kbee.payload.response.LoginResponse;
import edu.language.kbee.repository.RoleRepository;
import edu.language.kbee.repository.UserRepository;
import edu.language.kbee.security.CustomUserDetails;
import edu.language.kbee.security.JwtTokenUtil;
import edu.language.kbee.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper mapper;

    @Value("${google.client.id}")
    private String googleClientId;

    public static Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Override
    public LoginResponse login(LoginRequest request) {

        Authentication authentication;

        try {
            authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (AuthenticationException ex) {
            logger.error("Username: " + request.getUsername() + ", password " + request.getPassword());
            throw new BadRequestException("Tài khoản hoặc mật khẩu không chính xác");
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        String token = this.jwtTokenUtil.generateToken(userDetails);
        List<String> roles = userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return LoginResponse.builder()
                .username(userDetails.getUsername())
                .token(token)
                .roles(roles)
                .build();
    }

    @Override
    public String signup(SignUpRequest signUpRequest) {
        if(userRepository.existsByEmail(signUpRequest.getEmail())){
            throw new BadRequestException("Email đã tồn tại");
        }
        if(userRepository.existsByUsername(signUpRequest.getEmail())){
            throw new BadRequestException("Username đã tồn tại");
        }

        User user = User.builder()
                .username(signUpRequest.getEmail())
                .email(signUpRequest.getEmail())
                .password(passwordEncoder.encode(signUpRequest.getPassword()))
                .fullName(signUpRequest.getFullName())
                .build();

        Role role = roleRepository.findByRoleName(RoleName.USER)
                .orElseThrow(() -> new InternalServerException("Lỗi khi tạo mới người dùng, vui lòng thử lại sau!"));

        user.getRoles().add(role);
        userRepository.save(user);
        return "success";
    }

    @Override
    public UserDto getLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();

        User loggedInUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));
        Set<Role> roleSet = loggedInUser.getRoles();
        List<String> roleName = roleSet.stream().map( r -> String.valueOf(r.getRoleName())).toList();

        UserDto userDto = mapper.map(loggedInUser, UserDto.class);
        userDto.setRoles(roleName);
        userDto.setIsPremium(loggedInUser.getIsPremium());
        userDto.setPremiumExpiryDate(loggedInUser.getPremiumExpiryDate());

        return userDto;
    }

    @Override
    public LoginResponse loginWithGoogle(String accessToken) throws Exception {
        // Validate access_token with Google UserInfo endpoint
        String userInfoUrl = "https://www.googleapis.com/oauth2/v3/userinfo";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>("", headers);
        
        ResponseEntity<String> response = restTemplate.exchange(userInfoUrl, HttpMethod.GET, entity, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new BadRequestException("Token Google không hợp lệ");
        }

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode payload = objectMapper.readTree(response.getBody());

        String email = payload.has("email") ? payload.get("email").asText() : null;
        String name = payload.has("name") ? payload.get("name").asText() : null;
        String pictureUrl = payload.has("picture") ? payload.get("picture").asText() : null;

        if (email == null) {
            throw new BadRequestException("Không thể lấy email từ tài khoản Google");
        }

        User user = userRepository.findByUsername(email).orElse(null);
        if (user == null) {
            user = User.builder()
                    .username(email)
                    .email(email)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .fullName(name)
                    .userAvatar(pictureUrl)
                    .build();

            Role role = roleRepository.findByRoleName(RoleName.USER)
                    .orElseThrow(() -> new InternalServerException("Lỗi khi tạo mới người dùng, vui lòng thử lại sau!"));
            user.getRoles().add(role);
            userRepository.save(user);
        }

        // Tạo thủ công UserDetails và đăng nhập
        UserDetails userDetails = CustomUserDetails.builder()
                .user(user)
                .build();
        
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = this.jwtTokenUtil.generateToken(userDetails);
        List<String> roles = userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return LoginResponse.builder()
                .username(userDetails.getUsername())
                .token(token)
                .roles(roles)
                .build();
    }
}
