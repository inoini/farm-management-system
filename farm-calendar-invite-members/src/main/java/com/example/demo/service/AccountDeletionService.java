package com.example.demo.service;

import java.util.List;
import java.util.Objects;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Farm;
import com.example.demo.entity.UserAccount;
import com.example.demo.repository.AppSettingRepository;
import com.example.demo.repository.ContactMessageRepository;
import com.example.demo.repository.CropRepository;
import com.example.demo.repository.ExpenseRepository;
import com.example.demo.repository.FarmInviteRepository;
import com.example.demo.repository.FarmRepository;
import com.example.demo.repository.FieldRepository;
import com.example.demo.repository.HarvestShipmentRepository;
import com.example.demo.repository.MonthlyBudgetRepository;
import com.example.demo.repository.PasswordResetTokenRepository;
import com.example.demo.repository.SalesRepository;
import com.example.demo.repository.ScheduleRepository;
import com.example.demo.repository.StockRepository;
import com.example.demo.repository.UserAccountRepository;
import com.example.demo.repository.WorkerRepository;

@Service
public class AccountDeletionService {

    private final CurrentUserService currentUser;
    private final UserAccountRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final FarmRepository farmRepository;
    private final FarmInviteRepository inviteRepository;
    private final ScheduleRepository scheduleRepository;
    private final WorkerRepository workerRepository;
    private final HarvestShipmentRepository harvestRepository;
    private final AppSettingRepository settingRepository;
    private final CropRepository cropRepository;
    private final FieldRepository fieldRepository;
    private final SalesRepository salesRepository;
    private final ExpenseRepository expenseRepository;
    private final StockRepository stockRepository;
    private final MonthlyBudgetRepository budgetRepository;
    private final ContactMessageRepository contactRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountDeletionService(
            CurrentUserService currentUser,
            UserAccountRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            FarmRepository farmRepository,
            FarmInviteRepository inviteRepository,
            ScheduleRepository scheduleRepository,
            WorkerRepository workerRepository,
            HarvestShipmentRepository harvestRepository,
            AppSettingRepository settingRepository,
            CropRepository cropRepository,
            FieldRepository fieldRepository,
            SalesRepository salesRepository,
            ExpenseRepository expenseRepository,
            StockRepository stockRepository,
            MonthlyBudgetRepository budgetRepository,
            ContactMessageRepository contactRepository,
            PasswordEncoder passwordEncoder) {
        this.currentUser = currentUser;
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.farmRepository = farmRepository;
        this.inviteRepository = inviteRepository;
        this.scheduleRepository = scheduleRepository;
        this.workerRepository = workerRepository;
        this.harvestRepository = harvestRepository;
        this.settingRepository = settingRepository;
        this.cropRepository = cropRepository;
        this.fieldRepository = fieldRepository;
        this.salesRepository = salesRepository;
        this.expenseRepository = expenseRepository;
        this.stockRepository = stockRepository;
        this.budgetRepository = budgetRepository;
        this.contactRepository = contactRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserAccount account(Long accountId) {
        return userRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("アカウントが見つかりません。もう一度ログインしてください。"));
    }

    @Transactional(readOnly = true)
    public List<UserAccount> successorCandidates() {
        return successorCandidates(currentUser.account().getId());
    }

    @Transactional(readOnly = true)
    public List<UserAccount> successorCandidates(Long accountId) {
        UserAccount actor = account(accountId);
        if (!"OWNER".equalsIgnoreCase(actor.getFarmRole()) || actor.getFarm() == null) {
            return List.of();
        }
        return userRepository.findAllByFarmIdOrderByCreatedAtAsc(actor.getFarm().getId()).stream()
                .filter(member -> !Objects.equals(member.getId(), actor.getId()))
                .filter(member -> Boolean.TRUE.equals(member.getEnabled()))
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean hasOtherMembers() {
        return hasOtherMembers(currentUser.account().getId());
    }

    @Transactional(readOnly = true)
    public boolean hasOtherMembers(Long accountId) {
        UserAccount actor = account(accountId);
        if (actor.getFarm() == null) return false;
        return userRepository.findAllByFarmIdOrderByCreatedAtAsc(actor.getFarm().getId()).stream()
                .anyMatch(member -> !Objects.equals(member.getId(), actor.getId()));
    }

    @Transactional
    public DeletionResult deleteCurrentAccount(String email, String password, Long successorMemberId) {
        return deleteAccount(currentUser.account().getId(), email, password, successorMemberId);
    }

    @Transactional
    public DeletionResult deleteAccount(Long accountId, String email, String password, Long successorMemberId) {
        UserAccount actor = account(accountId);
        String normalizedInputEmail = CurrentUserService.normalize(email);
        String normalizedStoredEmail = CurrentUserService.normalize(actor.getEmail());

        if (normalizedInputEmail.isBlank() || !normalizedStoredEmail.equals(normalizedInputEmail)) {
            throw new IllegalArgumentException("登録されているメールアドレスを正しく入力してください。");
        }
        if (password == null || !passwordEncoder.matches(password, actor.getPasswordHash())) {
            throw new IllegalArgumentException("農業管理システムのパスワードが正しくありません。");
        }

        Farm farm = actor.getFarm();
        boolean owner = "OWNER".equalsIgnoreCase(actor.getFarmRole());
        if (!owner || farm == null) {
            deleteUserOnly(actor);
            userRepository.flush();
            return new DeletionResult(false, null);
        }

        List<UserAccount> farmMembers = userRepository.findAllByFarmIdOrderByCreatedAtAsc(farm.getId());
        List<UserAccount> others = farmMembers.stream()
                .filter(member -> !Objects.equals(member.getId(), actor.getId()))
                .toList();

        if (!others.isEmpty()) {
            if (successorMemberId == null) {
                throw new IllegalArgumentException("次の管理者を選択してください。管理者権限を引き継いでから退会します。");
            }
            UserAccount successor = others.stream()
                    .filter(member -> Objects.equals(member.getId(), successorMemberId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("選択したメンバーが見つかりません。"));
            if (!Boolean.TRUE.equals(successor.getEnabled())) {
                throw new IllegalArgumentException("利用停止中のメンバーには管理者権限を引き継げません。先にBANを解除してください。");
            }

            successor.setFarmRole("OWNER");
            userRepository.saveAndFlush(successor);
            deleteUserOnly(actor);
            userRepository.flush();
            return new DeletionResult(true, successor.getDisplayName());
        }

        deleteFarmData(farm);
        tokenRepository.deleteAllByUserId(actor.getId());
        inviteRepository.deleteAll(inviteRepository.findAllByFarmId(farm.getId()));
        inviteRepository.flush();
        userRepository.delete(actor);
        userRepository.flush();
        farmRepository.delete(farm);
        farmRepository.flush();
        return new DeletionResult(false, null);
    }

    private void deleteUserOnly(UserAccount actor) {
        tokenRepository.deleteAllByUserId(actor.getId());
        userRepository.delete(actor);
    }

    private void deleteFarmData(Farm farm) {
        String ownerKey = farm.getDataKey();
        if (ownerKey == null || ownerKey.isBlank()) return;

        scheduleRepository.deleteAll(scheduleRepository.findAllByOwnerEmail(ownerKey));
        workerRepository.deleteAll(workerRepository.findAllByOwnerEmailOrderByNameAsc(ownerKey));
        harvestRepository.deleteAll(harvestRepository.findAllByOwnerEmailOrderByWorkDateDescIdDesc(ownerKey));
        settingRepository.findFirstByOwnerEmail(ownerKey).ifPresent(settingRepository::delete);
        cropRepository.deleteAll(cropRepository.findAllByOwnerEmailOrderByIdDesc(ownerKey));
        fieldRepository.deleteAll(fieldRepository.findAllByOwnerEmail(ownerKey));
        salesRepository.deleteAll(salesRepository.findAllByOwnerEmailOrderByDateDescIdDesc(ownerKey));
        expenseRepository.deleteAll(expenseRepository.findAllByOwnerEmailOrderByDateDescIdDesc(ownerKey));
        stockRepository.deleteAll(stockRepository.findAllByOwnerEmailOrderByItemNameAsc(ownerKey));
        budgetRepository.deleteAll(budgetRepository.findAllByOwnerEmail(ownerKey));
        contactRepository.deleteAll(contactRepository.findAllByFarmDataKey(ownerKey));
    }

    public record DeletionResult(boolean ownershipTransferred, String successorName) {}
}
