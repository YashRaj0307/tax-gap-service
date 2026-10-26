
package com.avega.tax_gap_service.controller;

import com.avega.tax_gap_service.dto.CreateTaxpayerRequest;
import com.avega.tax_gap_service.dto.TaxpayerResponse;
import com.avega.tax_gap_service.service.TaxpayerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/taxpayers")
public class TaxpayerController {

    private final TaxpayerService taxpayerService;

    public TaxpayerController(TaxpayerService taxpayerService) {
        this.taxpayerService = taxpayerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaxpayerResponse create(
            @Valid @RequestBody CreateTaxpayerRequest request) {
        return taxpayerService.create(request);
    }

    @GetMapping
    public List<TaxpayerResponse> findAll() {
        return taxpayerService.findAll();
    }

    @GetMapping("/paged")
    public Page<TaxpayerResponse> findAllPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        if (page < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Page must be zero or greater");
        }

        if (size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Size must be between 1 and 100");
        }

        if (!sortBy.equals("id")
                && !sortBy.equals("taxpayerReference")
                && !sortBy.equals("legalName")
                && !sortBy.equals("taxYear")
                && !sortBy.equals("createdAt")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Unsupported sort field");
        }

        Sort.Direction sortDirection;
        try {
            sortDirection = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Direction must be asc or desc");
        }

        Pageable pageable = PageRequest.of(
                page, size, Sort.by(sortDirection, sortBy));

        return taxpayerService.findAll(pageable);
    }
}
