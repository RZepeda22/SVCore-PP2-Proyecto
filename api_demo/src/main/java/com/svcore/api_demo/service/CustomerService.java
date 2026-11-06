package com.svcore.api_demo.service;

import java.util.List;
import java.util.UUID;

import com.svcore.api_demo.models.dtos.SaveCustomerDTO;
import com.svcore.api_demo.models.entities.Customer;

/** CU-08: gestión de clientes. */
public interface CustomerService {

	List<Customer> search(String query);

	Customer findById(UUID customerId);

	Customer saveCustomer(SaveCustomerDTO info);

	Customer editCustomer(UUID customerId, SaveCustomerDTO info);
}
