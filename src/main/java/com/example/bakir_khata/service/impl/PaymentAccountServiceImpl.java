package com.example.bakir_khata.service.impl;

import com.example.bakir_khata.exception.BusinessRuleException;
import com.example.bakir_khata.model.PaymentAccount;
import com.example.bakir_khata.model.User;
import com.example.bakir_khata.model.enums.MobileProvider;
import com.example.bakir_khata.model.enums.PaymentAccountType;
import com.example.bakir_khata.repository.PaymentAccountRepository;
import com.example.bakir_khata.service.PaymentAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentAccountServiceImpl implements PaymentAccountService {
    private final PaymentAccountRepository repository;

    @Override
    public List<PaymentAccount> listForUser(User user) {
        return repository.findByUserIdAndActiveTrueOrderByPrimaryAccountDescCreatedAtAsc(user.getId());
    }

    @Override
    @Transactional
    public PaymentAccount addBank(User user, String bankName, String holder, String number,
                                  String branch, String routing) {
        if (blank(bankName) || blank(holder) || blank(number)) {
            throw new BusinessRuleException("Bank name, account holder and account number are required.");
        }
        return repository.save(PaymentAccount.builder()
                .user(user)
                .type(PaymentAccountType.BANK)
                .bankName(bankName.trim())
                .accountHolderName(holder.trim())
                .accountNumber(number.trim())
                .branchName(trim(branch))
                .routingNumber(trim(routing))
                .verified(true)
                .verifiedAt(LocalDateTime.now())
                .build());
    }

    @Override
    @Transactional
    public PaymentAccount addMobile(User user, MobileProvider provider, String mobileNumber) {
        validateMobile(provider, mobileNumber);
        return repository.save(PaymentAccount.builder()
                .user(user)
                .type(PaymentAccountType.MOBILE)
                .mobileProvider(provider)
                .mobileNumber(mobileNumber.trim())
                .verified(true)
                .verifiedAt(LocalDateTime.now())
                .build());
    }

    @Override
    @Transactional
    public PaymentAccount updateMobile(Long id, User user, MobileProvider provider, String mobileNumber) {
        validateMobile(provider, mobileNumber);
        PaymentAccount account = owned(id, user);
        if (account.getType() != PaymentAccountType.MOBILE) {
            throw new BusinessRuleException("Only mobile payment destinations can be updated here.");
        }
        account.setMobileProvider(provider);
        account.setMobileNumber(mobileNumber.trim());
        account.setVerified(true);
        account.setVerifiedAt(LocalDateTime.now());
        return repository.save(account);
    }

    @Override
    @Transactional
    public void deactivate(Long id, User user) {
        PaymentAccount account = owned(id, user);
        account.setActive(false);
        repository.save(account);
    }

    private void validateMobile(MobileProvider provider, String mobileNumber) {
        if (provider == null || blank(mobileNumber)) {
            throw new BusinessRuleException("Mobile provider and number are required.");
        }
        if (provider == MobileProvider.OTHER) {
            throw new BusinessRuleException("Choose bKash, Nagad or Rocket.");
        }
    }

    private PaymentAccount owned(Long id, User user) {
        PaymentAccount account = repository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Payment account not found."));
        if (!account.getUser().getId().equals(user.getId())) {
            throw new BusinessRuleException("Payment account not found.");
        }
        return account;
    }

    private boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private String trim(String s) {
        return blank(s) ? null : s.trim();
    }
}
