package com.open.spring.mvc.cryptoMining;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.open.spring.mvc.bank.Bank;
import com.open.spring.mvc.bank.BankJpaRepository;
import com.open.spring.mvc.person.Person;
import com.open.spring.mvc.person.PersonJpaRepository;
import com.open.spring.mvc.userStocks.UserStocksRepository;
import com.open.spring.mvc.userStocks.userStocksTable;

import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.transaction.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import java.util.Map;
import java.util.Date;

@Service
@EnableScheduling
public class MiningService {
    @Autowired
    private MiningUserRepository miningUserRepository;
    
    @Autowired
    private GPURepository gpuRepository;

    @Autowired
    private UserStocksRepository userStocksRepo;

    @Autowired
    private PersonJpaRepository personRepository;

    @Autowired
    private BankJpaRepository bankRepository;

    // Fine-tune constants
    public static final double HASH_TO_BTC_RATE = 0.0001; // Current rate
    public static final double DIFFICULTY_FACTOR = 1.0;
    public static final int MINING_INTERVAL = 900000; // 15 minutes
    public static final int BALANCE_TRANSFER_INTERVAL = 900000; // 15 minutes
    public static final double ELECTRICITY_RATE = 0.12; // USD per kWh
    public static final double BTC_PRICE = 45000.0;

    @Scheduled(fixedRate = MINING_INTERVAL, initialDelay = 60000)
    @Transactional
    public void processMining() {
        try {
            System.out.println("\n=== Mining Process Started ===");
            
            List<MiningUser> activeMiners = miningUserRepository.findAll().stream()
                .filter(user -> user.isMining() && !user.getActiveGPUs().isEmpty())
                .collect(Collectors.toList());

            activeMiners.forEach(miner -> {
                try {
                    double hashrate = miner.getCurrentHashrate();
                    
                    if (hashrate > 0) {
                        // Calculate mining reward
                        double btcMined = hashrate * HASH_TO_BTC_RATE / 60.0;
                        
                        // Convert BTC to USD
                        double usdMined = btcMined * BTC_PRICE;
                        
                        // Update BTC balance
                        miner.setPendingBalance(miner.getPendingBalance() + btcMined);
                        
                        // Update Person's USD balance
                        Person person = miner.getPerson();
                        String uid = person.getUid();
                        Bank bank = bankRepository.findByUid(uid);
                        if (bank != null) {
                            double currentBalance = bank.getBalance();
                            bank.setBalance(currentBalance + usdMined, "cryptomining");
                            bankRepository.save(bank);
                        }
                        
                        // Update mining stats
                        miner.setTotalBtcEarned(miner.getTotalBtcEarned() + btcMined);
                        miner.setShares(miner.getShares() + 1);
                        miner.setTotalSharesMined(miner.getTotalSharesMined() + 1);
                        
                        System.out.println(String.format("Miner %s earned %.8f BTC (%.2f USD)", 
                            miner.getPerson().getEmail(), btcMined, usdMined));
                    }
                    
                    miningUserRepository.save(miner);
                } catch (Exception e) {
                    System.err.println("Error processing miner " + miner.getPerson().getEmail() + ": " + e.getMessage());
                }
            });
        } catch (Exception e) {
            System.err.println("Mining process failed: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("Cause: " + e.getCause().getMessage());
            }
        }
    }

    @Scheduled(fixedRate = BALANCE_TRANSFER_INTERVAL, initialDelay = 60000)
    @Transactional
    public void processPendingBalances() {
        try {
            System.out.println("\n=== Processing Pending Balances ===");
            
            List<MiningUser> miners = miningUserRepository.findAll();
            System.out.println("Found " + miners.size() + " total miners");
            
            miners.forEach(miner -> {
                double pending = miner.getPendingBalance();
                if (pending > 0) {
                    try {
                    // First, transfer BTC from pending to confirmed
                    double currentConfirmed = miner.getBtcBalance();
                    double newConfirmed = currentConfirmed + pending;
                    miner.setBtcBalance(newConfirmed);
                    
                    // Calculate USD value
                    double pendingUSD = pending * BTC_PRICE;
                    
                    // Get user's stocks/balance record for USD update
                    userStocksTable userStocks = userStocksRepo.findByEmail(miner.getPerson().getEmail());
                    if (userStocks != null) {
                        double oldBalance = userStocks.getPerson().getBanks().getBalance();
                        double newBalance = oldBalance + pendingUSD;
                        
                        // Update USD balance
                        userStocks.getPerson().getBanks().setBalance(newBalance);
                        userStocksRepo.save(userStocks);
                    }
                    
                    // Clear pending BTC
                    miner.setPendingBalance(0.0);
                    miningUserRepository.save(miner);
                    
                    System.out.println("\nTransfer Details for " + miner.getPerson().getEmail());
                    System.out.println("BTC Transferred: " + String.format("%.8f", pending));
                    System.out.println("New Confirmed BTC: " + String.format("%.8f", newConfirmed));
                    System.out.println("USD Value: $" + String.format("%.2f", pendingUSD));
                    
                } catch (Exception e) {
                    System.out.println("Error processing pending balance: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
        } catch (Exception e) {
            System.err.println("Process pending balances failed: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("Cause: " + e.getCause().getMessage());
            }
        }
    }

    public Map<String, Object> buyGPU(MiningUser user, Long gpuId) {
        GPU gpu = gpuRepository.findById(gpuId).orElse(null);
        if (gpu == null) {
            return Map.of("success", false, "message", "GPU not found");
        }

        user.addGPU(gpu);
        miningUserRepository.save(user);

        return Map.of(
            "success", true,
            "message", "Successfully purchased " + gpu.getName(),
            "newBalance", user.getBtcBalance()
        );
    }

    public void calculateProfitability(MiningUser miner) {
        double hashrate = miner.getCurrentHashrate();
        
        // Calculate daily earnings (24 hours)
        double dailyBTC = hashrate * HASH_TO_BTC_RATE * 24.0;
        double dailyUSD = dailyBTC * BTC_PRICE;
        
        // Calculate power costs
        double dailyPowerKWH = miner.getActiveGPUs().stream()
            .mapToDouble(gpu -> gpu.getPowerConsumption() * 24.0 / 1000.0)
            .sum();
        double dailyPowerCost = dailyPowerKWH * ELECTRICITY_RATE;
        
        // Set values
        miner.setDailyRevenue(dailyUSD);
        miner.setPowerCost(dailyPowerCost);
        
        // Log profitability details
        System.out.println("\nDaily Mining Projections:");
        System.out.println("GPU: " + miner.getActiveGPUs().get(0).getName());
        System.out.println("Hashrate: " + String.format("%.2f", hashrate) + " MH/s");
        System.out.println("BTC Revenue: " + String.format("%.8f", dailyBTC) + " BTC");
        System.out.println("USD Revenue: $" + String.format("%.2f", dailyUSD));
        System.out.println("Power Cost: $" + String.format("%.2f", dailyPowerCost));
        System.out.println("Net Profit: $" + String.format("%.2f", dailyUSD - dailyPowerCost));
        System.out.println("ROI (days): " + String.format("%.1f", 
            (miner.getActiveGPUs().get(0).getPrice() / (dailyUSD - dailyPowerCost))));
    }
}