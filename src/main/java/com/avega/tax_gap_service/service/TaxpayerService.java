package com.avega.tax_gap_service.service;

import com.avega.tax_gap_service.domain.Taxpayer;
import com.avega.tax_gap_service.dto.CreateTaxpayerRequest;
import com.avega.tax_gap_service.dto.TaxpayerResponse;
import com.avega.tax_gap_service.repository.TaxpayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaxpayerService {

    private final TaxpayerRepository taxpayerRepository;

    public TaxpayerService(TaxpayerRepository taxpayerRepository) {
        this.taxpayerRepository = taxpayerRepository;
    }

    @Transactional
    public TaxpayerResponse create(CreateTaxpayerRequest request) {
        Taxpayer taxpayer = new Taxpayer(
                request.taxpayerReference(),
                request.legalName(),
                request.taxIdentifier(),
                request.taxYear(),
                request.declaredIncome(),
                request.declaredTax()
        );

        return toResponse(taxpayerRepository.saveAndFlush(taxpayer));
    }

    @Transactional(readOnly = true)
    public List<TaxpayerResponse> findAll() {
        return taxpayerRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private TaxpayerResponse toResponse(Taxpayer taxpayer) {
        return new TaxpayerResponse(
                taxpayer.getId(),
                taxpayer.getTaxpayerReference(),
                taxpayer.getLegalName(),
                taxpayer.getTaxIdentifier(),
                taxpayer.getTaxYear(),
                taxpayer.getDeclaredIncome(),
                taxpayer.getDeclaredTax(),
                taxpayer.getCreatedAt()
        );
    }
}