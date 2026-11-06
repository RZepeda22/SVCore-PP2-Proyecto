package com.svcore.api_demo.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.svcore.api_demo.models.entities.Customer;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

	List<Customer> findTop50ByOrderByNameAsc();

	List<Customer> findTop20ByNameContainingIgnoreCaseOrNrcContainingOrderByNameAsc(String name, String nrc);

	Optional<Customer> findByNrc(String nrc);
}
