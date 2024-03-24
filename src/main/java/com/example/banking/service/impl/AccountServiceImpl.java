package com.example.banking.service.impl;

import com.example.banking.dto.AccountDto;
import com.example.banking.dto.TransferFundDto;
import com.example.banking.entity.Account;
import com.example.banking.entity.Transaction;
import com.example.banking.exception.AccountException;
import com.example.banking.mapper.AccountMapper;
import com.example.banking.repository.AccountRepository;
import com.example.banking.repository.TransactionRepository;
import com.example.banking.service.AccountService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccountServiceImpl implements AccountService {

    private AccountRepository accountRepository;
    private TransactionRepository transactionRepository;

    private static final String TRANSACTION_TYPE_DEPOSIT = "DEPOSIT";
    private static final String TRANSACTION_TYPE_WITHDRAW = "WITHDRAW";
    private static final String TRANSACTION_TYPE_TRANSFER = "TRANSFER";
    public AccountServiceImpl(AccountRepository accountRepository, TransactionRepository transactionRepository){
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }
    @Override
    public AccountDto createAccount(AccountDto accountDto) {
        Account account = AccountMapper.mapToAccount(accountDto);
        Account savedAccount = accountRepository.save(account);
        return AccountMapper.mapToAccountDto(savedAccount);
    }

    @Override
    public AccountDto getAccountById(Long id) {
        Account account = accountRepository
                .findById(id)
                .orElseThrow(()-> new AccountException("Account does not exists"));
        return AccountMapper.mapToAccountDto(account);
    }

    @Override
    public AccountDto deposit(Long id, double amount) {

        Account account = accountRepository
                .findById(id)
                .orElseThrow(()-> new AccountException("Account does not exists"));
        double total = account.getBalance() + amount;
        account.setBalance(total);
        Account savedAccount = accountRepository.save(account);

        Transaction transaction = new Transaction();
        transaction.setAccountId(id);
        transaction.setAmount(amount);
        transaction.setTransactionType(TRANSACTION_TYPE_DEPOSIT);
        transaction.setTimestamp(LocalDateTime.now());
        transactionRepository.save(transaction);
        return AccountMapper.mapToAccountDto(savedAccount);
    }

    @Override
    public AccountDto withdraw(Long id, double amount) {

        Account account = accountRepository
                .findById(id)
                .orElseThrow(()-> new AccountException("Account does not exists"));

        if(account.getBalance() < amount){
            throw new RuntimeException("Insufficient amount");
        }
        double total = account.getBalance() - amount;
        account.setBalance(total);
        Account withdrawAccount = accountRepository.save(account);

        Transaction transaction = new Transaction();
        transaction.setAccountId(id);
        transaction.setAmount(amount);
        transaction.setTransactionType(TRANSACTION_TYPE_WITHDRAW);
        transaction.setTimestamp(LocalDateTime.now());
        transactionRepository.save(transaction);
        return AccountMapper.mapToAccountDto(withdrawAccount);
    }

    @Override
    public List<AccountDto> getAllAccounts() {
       List<Account> accounts = accountRepository.findAll();
        return accounts.stream().map((account) -> AccountMapper.mapToAccountDto(account))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteAccount(Long id) {
        Account account = accountRepository
                .findById(id)
                .orElseThrow(()-> new AccountException("Account does not exists"));

        accountRepository.delete(account);


    }

    @Override
    public void transferFunds(TransferFundDto transferFundDto) {
    // Retrieve the account from which we send the amount
    Account fromAccount = accountRepository.findById(transferFundDto.fromAccountId())
                .orElseThrow(()-> new AccountException("Account does not exists"));

    // Retrieve the account to which we send the amount
    Account accountTo = accountRepository.findById(transferFundDto.toAccountId())
            .orElseThrow(()-> new AccountException("Account does not exists"));

    if(fromAccount.getBalance() < transferFundDto.amount()){
        throw new RuntimeException("Insufficient fund");
    }
    // Debit the amount from fromAccount object
    fromAccount.setBalance(fromAccount.getBalance() - transferFundDto.amount());

    // Credit the amount to toAccount object
    accountTo.setBalance(accountTo.getBalance() + transferFundDto.amount());

    Transaction transaction = new Transaction();
    transaction.setAccountId(transferFundDto.fromAccountId());
    transaction.setAmount(transferFundDto.amount());
    transaction.setTransactionType(TRANSACTION_TYPE_TRANSFER);
    transaction.setTimestamp(LocalDateTime.now());
    transactionRepository.save(transaction);


    accountRepository.save(fromAccount);
    accountRepository.save(accountTo);
}

}
