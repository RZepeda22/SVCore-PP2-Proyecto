package com.svcore.api_demo.service.implementations;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.svcore.api_demo.exceptions.BusinessException;
import com.svcore.api_demo.models.dtos.SaveCustomerDTO;
import com.svcore.api_demo.models.entities.Customer;
import com.svcore.api_demo.models.enums.AuditAction;
import com.svcore.api_demo.models.enums.TaxpayerType;
import com.svcore.api_demo.repositories.CustomerRepository;
import com.svcore.api_demo.service.AuditService;
import com.svcore.api_demo.service.CustomerService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

	private final CustomerRepository customerRepository;
	private final AuditService auditService;

	@Override
	@Transactional(readOnly = true)
	public List<Customer> search(String query) {
		if (query == null || query.isBlank()) {
			return customerRepository.findTop50ByOrderByNameAsc();
		}
		String value = query.trim();
		return customerRepository.findTop20ByNameContainingIgnoreCaseOrNrcContainingOrderByNameAsc(value, value);
	}

	@Override
	@Transactional(readOnly = true)
	public Customer findById(UUID customerId) {
		return customerRepository.findById(customerId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
	}

	@Override
	@Transactional
	public Customer saveCustomer(SaveCustomerDTO info) {
		Customer customer = new Customer();
		apply(customer, info);
		customerRepository.save(customer);
		auditService.record(AuditAction.CLIENTE_CREADO, "customer", customer.getId(), customer.getName());
		return customer;
	}

	@Override
	@Transactional
	public Customer editCustomer(UUID customerId, SaveCustomerDTO info) {
		Customer customer = findById(customerId);
		String previousNrc = customer.getNrc();
		apply(customer, info);
		customerRepository.save(customer);
		auditService.record(AuditAction.CLIENTE_EDITADO, "customer", customer.getId(), customer.getName()
				+ (java.util.Objects.equals(previousNrc, customer.getNrc()) ? "" : "; NRC " + previousNrc + " -> " + customer.getNrc()));
		return customer;
	}

	private void apply(Customer customer, SaveCustomerDTO info) {
		boolean contribuyente = info.getTaxpayerType() == TaxpayerType.CONTRIBUYENTE;
		String nrc = contribuyente ? blankToNull(info.getNrc()) : null;
		String nit = blankToNull(info.getNit());

		if (contribuyente && (nrc == null || nit == null)) {
			throw new BusinessException(HttpStatus.BAD_REQUEST, "Un cliente contribuyente requiere NRC y NIT");
		}
		if (nrc != null) {
			customerRepository.findByNrc(nrc)
					.filter(existing -> !existing.getId().equals(customer.getId()))
					.ifPresent(existing -> {
						throw new BusinessException(HttpStatus.CONFLICT, "Ya existe un cliente con el NRC " + nrc);
					});
		}

		customer.setName(info.getName().trim());
		customer.setTaxpayerType(info.getTaxpayerType());
		customer.setNrc(nrc);
		customer.setNit(nit);
		customer.setEmail(blankToNull(info.getEmail()));
		customer.setAddress(blankToNull(info.getAddress()));
		// Si el formulario no envía los datos fiscales adicionales (POS), se conservan los existentes
		if (info.getEconomicActivityCode() != null) customer.setEconomicActivityCode(blankToNull(info.getEconomicActivityCode()));
		if (info.getEconomicActivity() != null) customer.setEconomicActivity(blankToNull(info.getEconomicActivity()));
		if (info.getPhone() != null) customer.setPhone(blankToNull(info.getPhone()));
		if (info.getDepartmentCode() != null) customer.setDepartmentCode(blankToNull(info.getDepartmentCode()));
		if (info.getMunicipalityCode() != null) customer.setMunicipalityCode(blankToNull(info.getMunicipalityCode()));
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
