package com.hellfire.team.service;

import com.hellfire.model.PlatformSettings;
import com.hellfire.repository.PlatformSettingsRepository;
import com.hellfire.team.dto.PlatformSettingsDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class PlatformSettingsService {

    private static final Logger log = LoggerFactory.getLogger(PlatformSettingsService.class);

    private final PlatformSettingsRepository repository;
    private final BigDecimal defaultCommission;
    private final String currency;

    public PlatformSettingsService(PlatformSettingsRepository repository,
                                   @Value("${platform.default-commission-percentage:10}") BigDecimal defaultCommission,
                                   @Value("${platform.currency:INR}") String currency) {
        this.repository = repository;
        this.defaultCommission = defaultCommission;
        this.currency = currency;
    }

    /** Returns the singleton settings row, creating it with configured defaults on first use. */
    @Transactional
    public PlatformSettings get() {
        return repository.findById(PlatformSettings.SINGLETON_ID).orElseGet(() -> {
            PlatformSettings settings = new PlatformSettings();
            settings.setId(PlatformSettings.SINGLETON_ID);
            settings.setCommissionPercentage(defaultCommission);
            settings.setCurrency(currency);
            settings.setUpdatedAt(LocalDateTime.now());
            settings.setUpdatedBy("system");
            return repository.save(settings);
        });
    }

    @Transactional
    public PlatformSettingsDto getDto() {
        return toDto(get());
    }

    @Transactional
    public PlatformSettingsDto updateCommission(BigDecimal commissionPercentage, String actor) {
        PlatformSettings settings = get();
        BigDecimal before = settings.getCommissionPercentage();
        settings.setCommissionPercentage(commissionPercentage);
        settings.setUpdatedAt(LocalDateTime.now());
        settings.setUpdatedBy(actor);
        repository.save(settings);
        log.info("AUDIT commission changed: {} -> {} by {}", before, commissionPercentage, actor);
        return toDto(settings);
    }

    private static PlatformSettingsDto toDto(PlatformSettings s) {
        return new PlatformSettingsDto(s.getCommissionPercentage(), s.getCurrency(), s.getUpdatedAt(), s.getUpdatedBy());
    }
}
