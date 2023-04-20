package com.company.assetmanagementwebservice.service;

import org.hibernate.Transaction;
import org.springframework.data.repository.CrudRepository;

public interface TransactionRepository extends CrudRepository<Transaction, Long> {
}
