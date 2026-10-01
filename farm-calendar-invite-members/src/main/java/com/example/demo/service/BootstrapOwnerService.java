package com.example.demo.service;

import java.time.LocalDateTime;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Farm;
import com.example.demo.entity.UserAccount;
import com.example.demo.repository.AppSettingRepository;
import com.example.demo.repository.CropRepository;
import com.example.demo.repository.ExpenseRepository;
import com.example.demo.repository.FarmRepository;
import com.example.demo.repository.FieldRepository;
import com.example.demo.repository.HarvestShipmentRepository;
import com.example.demo.repository.SalesRepository;
import com.example.demo.repository.ScheduleRepository;
import com.example.demo.repository.StockRepository;
import com.example.demo.repository.UserAccountRepository;
import com.example.demo.repository.WorkerRepository;

@Component
public class BootstrapOwnerService implements ApplicationRunner {

    private final Environment env;
    private final UserAccountRepository userRepository;
    private final FarmRepository farmRepository;
    private final PasswordEncoder passwordEncoder;
    private final CropRepository cropRepository;
    private final ExpenseRepository expenseRepository;
    private final FieldRepository fieldRepository;
    private final HarvestShipmentRepository harvestRepository;
    private final SalesRepository salesRepository;
    private final ScheduleRepository scheduleRepository;
    private final StockRepository stockRepository;
    private final WorkerRepository workerRepository;
    private final AppSettingRepository settingRepository;

    public BootstrapOwnerService(Environment env, UserAccountRepository userRepository,
            FarmRepository farmRepository, PasswordEncoder passwordEncoder,
            CropRepository cropRepository, ExpenseRepository expenseRepository,
            FieldRepository fieldRepository, HarvestShipmentRepository harvestRepository,
            SalesRepository salesRepository, ScheduleRepository scheduleRepository,
            StockRepository stockRepository, WorkerRepository workerRepository,
            AppSettingRepository settingRepository) {
        this.env = env;
        this.userRepository = userRepository;
        this.farmRepository = farmRepository;
        this.passwordEncoder = passwordEncoder;
        this.cropRepository = cropRepository;
        this.expenseRepository = expenseRepository;
        this.fieldRepository = fieldRepository;
        this.harvestRepository = harvestRepository;
        this.salesRepository = salesRepository;
        this.scheduleRepository = scheduleRepository;
        this.stockRepository = stockRepository;
        this.workerRepository = workerRepository;
        this.settingRepository = settingRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = CurrentUserService.normalize(env.getProperty("BOOTSTRAP_OWNER_EMAIL"));
        String username = CurrentUserService.normalizeUsername(env.getProperty("BOOTSTRAP_OWNER_USERNAME", "admin"));
        String password = env.getProperty("BOOTSTRAP_OWNER_PASSWORD");
        String displayName = env.getProperty("BOOTSTRAP_OWNER_NAME", "管理者").strip();
        String farmName = env.getProperty("BOOTSTRAP_FARM_NAME", displayName + "の農場").strip();

        if (!email.isBlank() && password != null && password.length() >= 8 && password.length() <= 64
                && !userRepository.existsByEmailIgnoreCaseAndFarmRoleIgnoreCase(email, "OWNER")) {
            UserAccount user = new UserAccount();
            user.setUsername(uniqueUsername(username.isBlank() ? "admin" : username));
            user.setEmail(email);
            user.setDisplayName(displayName.isBlank() ? "管理者" : displayName);
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setRole("USER");
            user.setEnabled(true);
            user.setFarmRole("OWNER");
            user.setCreatedAt(LocalDateTime.now());
            Farm farm = ensureFarm(email, farmName.isBlank() ? "農場" : farmName);
            user.setFarm(farm);
            userRepository.save(user);
        }

        fillMissingUsernames();
        ensureFarmAssignments();

        boolean migrate = Boolean.parseBoolean(env.getProperty("MIGRATE_LEGACY_DATA", "false"));
        if (!migrate || email.isBlank()) return;

        UserAccount owner = userRepository.findFirstByEmailIgnoreCaseAndFarmRoleIgnoreCase(email, "OWNER").orElse(null);
        if (owner == null || owner.getFarm() == null) return;
        String dataKey = owner.getFarm().getDataKey();

        cropRepository.findAll().stream().filter(v -> blank(v.getOwnerEmail())).forEach(v -> v.setOwnerEmail(dataKey));
        expenseRepository.findAll().stream().filter(v -> blank(v.getOwnerEmail())).forEach(v -> v.setOwnerEmail(dataKey));
        fieldRepository.findAll().stream().filter(v -> blank(v.getOwnerEmail())).forEach(v -> v.setOwnerEmail(dataKey));
        harvestRepository.findAll().stream().filter(v -> blank(v.getOwnerEmail())).forEach(v -> v.setOwnerEmail(dataKey));
        salesRepository.findAll().stream().filter(v -> blank(v.getOwnerEmail())).forEach(v -> v.setOwnerEmail(dataKey));
        scheduleRepository.findAll().stream().filter(v -> blank(v.getOwnerEmail())).forEach(v -> v.setOwnerEmail(dataKey));
        stockRepository.findAll().stream().filter(v -> blank(v.getOwnerEmail())).forEach(v -> v.setOwnerEmail(dataKey));
        workerRepository.findAll().stream().filter(v -> blank(v.getOwnerEmail())).forEach(v -> v.setOwnerEmail(dataKey));
        settingRepository.findAll().stream().filter(v -> blank(v.getOwnerEmail())).forEach(v -> v.setOwnerEmail(dataKey));

        cropRepository.flush();
        expenseRepository.flush();
        fieldRepository.flush();
        harvestRepository.flush();
        salesRepository.flush();
        scheduleRepository.flush();
        stockRepository.flush();
        workerRepository.flush();
        settingRepository.flush();
    }

    private void ensureFarmAssignments() {
        for (UserAccount user : userRepository.findAll()) {
            if (user.getFarm() != null) continue;
            String emailKey = CurrentUserService.normalize(user.getEmail());
            String name = user.getDisplayName() == null || user.getDisplayName().isBlank()
                    ? "農場" : user.getDisplayName() + "の農場";
            Farm farm = ensureFarm(emailKey, name);
            user.setFarm(farm);
            user.setFarmRole("OWNER");
            userRepository.save(user);
        }
    }

    private Farm ensureFarm(String dataKey, String farmName) {
        return farmRepository.findByDataKey(dataKey).orElseGet(() -> {
            Farm farm = new Farm();
            farm.setName(farmName);
            farm.setDataKey(dataKey);
            farm.setCreatedAt(LocalDateTime.now());
            return farmRepository.save(farm);
        });
    }

    /**
     * 旧データでユーザー名が空だった場合でも、メールアドレスからユーザー名を作らない。
     * 過去の処理でメールの @ より前がユーザー名に入ったレコードも、表示名を基準に修復する。
     */
    private void fillMissingUsernames() {
        for (UserAccount user : userRepository.findAll()) {
            boolean missingUsername = blank(user.getUsername());
            boolean legacyEmailDerivedUsername = isLegacyEmailDerivedUsername(user);

            if (missingUsername || legacyEmailDerivedUsername) {
                String base = usernameBaseFromDisplayName(user.getDisplayName());
                String normalizedBase = CurrentUserService.normalizeUsername(base);

                if (missingUsername || !normalizedBase.equalsIgnoreCase(user.getUsername())) {
                    user.setUsername(uniqueUsername(normalizedBase));
                    userRepository.save(user);
                }
            }

            if (blank(user.getFarmRole())) {
                user.setFarmRole("OWNER");
                userRepository.save(user);
            }
        }
    }

    private boolean isLegacyEmailDerivedUsername(UserAccount user) {
        if (blank(user.getUsername()) || blank(user.getEmail())) return false;
        return user.getUsername().equalsIgnoreCase(legacyEmailUsernameBase(user.getEmail()));
    }

    private String legacyEmailUsernameBase(String email) {
        String local = email == null ? "farm" : email.split("@", 2)[0];
        String base = local.replaceAll("[^\\p{L}\\p{N}._-]", "-");
        if (base.length() < 3) base = "farm-" + base;
        if (base.length() > 32) base = base.substring(0, 32);
        return CurrentUserService.normalizeUsername(base);
    }

    private String usernameBaseFromDisplayName(String displayName) {
        String base = displayName == null ? "" : displayName.strip();
        base = base.replaceAll("[^\\p{L}\\p{N}._-]", "-");
        base = base.replaceAll("-+", "-");
        base = base.replaceAll("^-+|-+$", "");
        if (base.length() < 3) base = "farm-" + base;
        if (base.length() > 32) base = base.substring(0, 32);
        if (base.isBlank()) base = "farm-user";
        return base;
    }

    private String uniqueUsername(String requested) {
        String base = requested == null || requested.isBlank() ? "farm" : requested;
        String candidate = base;
        int suffix = 2;
        while (userRepository.existsByUsernameIgnoreCase(candidate)) {
            String tail = "-" + suffix++;
            int maxBase = Math.max(1, 40 - tail.length());
            candidate = (base.length() > maxBase ? base.substring(0, maxBase) : base) + tail;
        }
        return candidate;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
