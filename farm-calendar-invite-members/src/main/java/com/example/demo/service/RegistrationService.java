package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Farm;
import com.example.demo.entity.UserAccount;
import com.example.demo.repository.FarmRepository;
import com.example.demo.repository.UserAccountRepository;

@Service
public class RegistrationService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[\\p{L}\\p{N}._-]{3,40}$");

    private final UserAccountRepository userRepository;
    private final FarmRepository farmRepository;
    private final PasswordEncoder passwordEncoder;
    private final FarmInviteService inviteService;

    public RegistrationService(UserAccountRepository userRepository, FarmRepository farmRepository,
            PasswordEncoder passwordEncoder, FarmInviteService inviteService) {
        this.userRepository = userRepository;
        this.farmRepository = farmRepository;
        this.passwordEncoder = passwordEncoder;
        this.inviteService = inviteService;
    }

    @Transactional
    public void register(String displayName, String farmName, String username, String email,
            String password, String passwordConfirm) {
        ValidatedUser input = validateUser(displayName, username, email, password, passwordConfirm);
        String normalizedFarmName = farmName == null ? "" : farmName.strip();
        if (normalizedFarmName.isBlank() || normalizedFarmName.length() > 100) {
            throw new IllegalArgumentException("農場名は1～100文字で入力してください。");
        }

        Farm farm = new Farm();
        farm.setName(normalizedFarmName);
        farm.setDataKey(UUID.randomUUID().toString());
        farm.setCreatedAt(LocalDateTime.now());
        farmRepository.save(farm);

        saveUser(input, farm, "OWNER");
    }

    @Transactional
    public void joinFarm(String inviteCode, String displayName, String username, String email,
            String password, String passwordConfirm) {
        ValidatedUser input = validateUser(displayName, username, email, password, passwordConfirm);
        Farm farm = inviteService.resolveFarmForJoin(inviteCode);
        saveUser(input, farm, "MEMBER");
    }

    private ValidatedUser validateUser(String displayName, String username, String email,
            String password, String passwordConfirm) {
        String normalizedName = displayName == null ? "" : displayName.strip();
        String normalizedUsername = CurrentUserService.normalizeUsername(username);
        String normalizedEmail = CurrentUserService.normalize(email);

        if (normalizedName.isBlank() || normalizedName.length() > 50) {
            throw new IllegalArgumentException("表示名は1～50文字で入力してください。");
        }
        if (!USERNAME_PATTERN.matcher(normalizedUsername).matches()) {
            throw new IllegalArgumentException("ユーザー名は3～40文字で、文字・数字・.・_・-を使用してください。");
        }
        if (normalizedEmail.length() > 254 || !EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new IllegalArgumentException("メールアドレスを正しく入力してください。");
        }
        if (password == null || password.length() < 8 || password.length() > 64) {
            throw new IllegalArgumentException("パスワードは8～64文字で入力してください。");
        }
        if (!password.equals(passwordConfirm)) {
            throw new IllegalArgumentException("確認用パスワードが一致しません。");
        }
        if (userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new IllegalArgumentException("このユーザー名はすでに使用されています。");
        }
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("このメールアドレスはすでに登録されています。");
        }
        return new ValidatedUser(normalizedName, normalizedUsername, normalizedEmail, password);
    }

    private void saveUser(ValidatedUser input, Farm farm, String farmRole) {
        UserAccount user = new UserAccount();
        user.setDisplayName(input.displayName());
        user.setUsername(input.username());
        user.setEmail(input.email());
        user.setPasswordHash(passwordEncoder.encode(input.password()));
        user.setRole("USER");
        user.setEnabled(true);
        user.setFarm(farm);
        user.setFarmRole(farmRole);
        user.setCreatedAt(LocalDateTime.now());
        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("ユーザー名またはメールアドレスはすでに登録されています。");
        }
    }

    private record ValidatedUser(String displayName, String username, String email, String password) {}
}
