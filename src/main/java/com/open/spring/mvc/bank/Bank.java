package com.open.spring.mvc.bank;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.LinkedHashMap;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.open.spring.mvc.person.Person;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.text.SimpleDateFormat;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Bank {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String uid;

    @OneToOne
    @JoinColumn(name = "person_id", referencedColumnName = "id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private Person person; // One-to-One relationship with the Person entity

    private double balance;
    private double loanAmount;
    
    // Add a field for personalized interest rate
    private double dailyInterestRate = 5.0; // Default 3%
    
    // Risk category (0=low, 1=medium, 2=high)
    private int riskCategory = 1;
    
    // Track transaction history for ML features
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, List<List<Object>>> profitMap = new HashMap<>();
    
    // Store ML feature importance for explainability
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Double> featureImportance = new HashMap<>();

    // Track NPC progress
    @JdbcTypeCode(SqlTypes.JSON)
    private LinkedHashMap<String, Boolean> npcProgress = new LinkedHashMap<>();

    public Bank(Person person) {
        this.person = person;
        this.person.setBanks(this);
        this.uid = person.getUid();
        this.loanAmount = 0.0; // Default to 0
        this.balance = 100000.0;
        this.profitMap = new HashMap<>();
        this.featureImportance = new HashMap<>();
        this.npcProgress = new LinkedHashMap<>();
        initializeNpcProgress();
        initializeFeatureImportance();
    }

    public String getUsername() {
        return person != null ? person.getName() : null;
    }
    
    private void initializeNpcProgress() {
        this.npcProgress.put("Stock-NPC", true);
        this.npcProgress.put("Casino-NPC", false);
        this.npcProgress.put("Fidelity", false);
        this.npcProgress.put("Schwab", false);
        this.npcProgress.put("Mining-NPC", false);
        this.npcProgress.put("Crypto-NPC", false);
        this.npcProgress.put("Bank-NPC", false);
    }
    
    private void initializeFeatureImportance() {
        this.featureImportance.put("casino_frequency", 0.42);
        this.featureImportance.put("profit_loss_ratio", 0.38);
        this.featureImportance.put("recent_activity", 0.35);
        this.featureImportance.put("loan_balance_ratio", 0.25);
        this.featureImportance.put("loan_history", 0.20);
        this.featureImportance.put("stock_activity", -0.30);
        this.featureImportance.put("crypto_activity", -0.28);
        this.featureImportance.put("volatility", 0.15);
        this.featureImportance.put("balance_trend", 0.22);
    }
    
    public double setBalance(double updatedBalance, String source) {
         // Update the balance as a String
        Double profit = updatedBalance - this.balance;
        this.balance = updatedBalance;
        System.out.println("Profit: " + profit);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp = dateFormat.format(new Date());
        updateProfitMap(source, timestamp, profit);
        return this.balance; // Return the updated balance as a String
    }
    
    public void updateProfitMap(String category, String time, double profit) {
        if (this.profitMap == null) {
            this.profitMap = new HashMap<>();
        }

        List<Object> transaction = Arrays.asList(time, profit);

        this.profitMap.computeIfAbsent(category, k -> new ArrayList<>()).add(transaction);
    }

    public List<List<Object>> getProfitByCategory(String category) {
        return this.profitMap.getOrDefault(category, new ArrayList<>());
    }

    public void requestLoan(double loanAmount) {
        this.loanAmount += loanAmount;  // Increase the loan amount
        balance += loanAmount;   // Add the loan amount to the balance
        
        // Re-assess risk using ML model
        assessRiskUsingML();
    }
    
    public void repayLoan(double repaymentAmount) {
        // Validate the repayment amount
        if (repaymentAmount <= 0) {
            throw new IllegalArgumentException("Repayment amount must be positive");
        }
        
        // Check if the user has enough balance
        if (balance < repaymentAmount) {
            throw new IllegalArgumentException("Insufficient balance for this repayment");
        }
        
        // Check if the repayment is more than the loan
        if (repaymentAmount > loanAmount) {
            throw new IllegalArgumentException("Repayment amount exceeds the loan balance");
        }
        
        // Process the repayment
        balance -= repaymentAmount;
        loanAmount -= repaymentAmount;
        
        // Record transaction
        String timestamp = Instant.now().toString();
        this.updateProfitMap("loan_repayment", timestamp, -repaymentAmount);
        
        // Re-assess risk after repayment
        assessRiskUsingML();
    }

    // Updated method to calculate daily interest using personalized rate
    public double dailyInterestCalculation() {
        return loanAmount * (dailyInterestRate / 100);  // Convert percentage to decimal
    }
    
    // Method to assess risk using machine learning
    public void assessRiskUsingML() {
        // Use ML model to calculate interest rate
        double baseRate = LoanRiskCalculator.calculateDailyInterestRate(this);
        
        // Apply ensemble method for more robust prediction
        double ensembleRate = LoanRiskCalculator.ensembleInterestRate(this);
        
        // Use 70% ensemble and 30% base rate
        double finalRate = (ensembleRate * 0.7) + (baseRate * 0.3);
        
        // Update the interest rate
        this.dailyInterestRate = finalRate;
        
        // Update risk category
        this.riskCategory = LoanRiskCalculator.classifyRiskCategory(this);
        
        // Update feature importance if new activities were added
        updateFeatureImportance();
    }
    
    // Method to update feature importance with slight random variations
    // to simulate ML model re-training
    private void updateFeatureImportance() {
        Random random = new Random();
        
        // Get current activities
        boolean hasCasino = false;
        boolean hasStocks = false;
        boolean hasCrypto = false;
        
        for (String key : profitMap.keySet()) {
            if (key.startsWith("casino_")) hasCasino = true;
            if (key.equals("stocks")) hasStocks = true;
            if (key.equals("cryptomining")) hasCrypto = true;
        }
        
        // Adjust weights based on activity
        if (hasCasino) {
            double variation = (random.nextDouble() * 0.1) - 0.05; // -5% to +5%
            featureImportance.put("casino_frequency", 
                    Math.max(0.3, Math.min(0.5, featureImportance.get("casino_frequency") + variation)));
        }
        
        if (hasStocks) {
            double variation = (random.nextDouble() * 0.08) - 0.04; // -4% to +4%
            featureImportance.put("stock_activity", 
                    Math.min(-0.2, Math.max(-0.4, featureImportance.get("stock_activity") + variation)));
        }
        
        if (hasCrypto) {
            double variation = (random.nextDouble() * 0.08) - 0.04; // -4% to +4%
            featureImportance.put("crypto_activity", 
                    Math.min(-0.2, Math.max(-0.4, featureImportance.get("crypto_activity") + variation)));
        }
        
        // Adjust loan importance based on loan amount
        if (loanAmount > 0) {
            double loanToBalanceRatio = balance > 0 ? loanAmount / balance : 2.0;
            if (loanToBalanceRatio > 0.8) {
                // High loan ratio increases importance of loan history
                featureImportance.put("loan_history", 
                        Math.min(0.3, featureImportance.get("loan_history") + 0.05));
            }
        }
    }
    
    // Method to simulate a casino game activity and record it
    public double playCasinoGame(String gameType, double betAmount) {
        // Validate inputs
        if (betAmount <= 0 || balance < betAmount) {
            return 0.0;
        }
        
        if (!gameType.startsWith("casino_")) {
            gameType = "casino_" + gameType;
        }
        
        // Simple RNG for game outcomes with appropriate house edges
        double winChance;
        double payoutMultiplier;
        
        switch (gameType) {
            case "casino_dice":
                winChance = 0.48;  // Slightly below 50%
                payoutMultiplier = 2.0;
                break;
            case "casino_poker":
                winChance = 0.45;
                payoutMultiplier = 2.2;
                break;
            case "casino_mines":
                winChance = 0.40;  // Highest risk
                payoutMultiplier = 2.5;
                break;
            case "casino_blackjack":
                winChance = 0.47;
                payoutMultiplier = 2.1;
                break;
            default:
                winChance = 0.48;
                payoutMultiplier = 2.0;
        }
        
        // Subtract bet from balance
        this.balance -= betAmount;
        
        // Determine outcome
        double profit;
        if (Math.random() < winChance) {
            // Win
            profit = betAmount * payoutMultiplier;
            this.balance += profit;
            profit -= betAmount; // Net profit
        } else {
            // Loss
            profit = -betAmount;
        }
        
        // Record transaction
        String timestamp = Instant.now().toString();
        this.updateProfitMap(gameType, timestamp, profit);
        
        // Re-assess risk using ML model
        assessRiskUsingML();
        
        return profit;
    }
    
    // Method to simulate stock market investment
    public double investInStocks(double investmentAmount) {
        // Validate inputs
        if (investmentAmount <= 0 || balance < investmentAmount) {
            return 0.0;
        }
        
        // Subtract investment from balance
        this.balance -= investmentAmount;
        
        // Stock market has better odds but lower returns than casino
        double winChance = 0.55;
        double returnRange = 0.25; // +/- 25% 
        
        // Calculate return
        double returnMultiplier = 1.0 + (Math.random() * returnRange * 2) - returnRange;
        double returns = investmentAmount * returnMultiplier;
        
        // Add returns to balance
        this.balance += returns;
        
        // Calculate profit
        double profit = returns - investmentAmount;
        
        // Record transaction
        String timestamp = Instant.now().toString();
        this.updateProfitMap("stocks", timestamp, profit);
        
        // Re-assess risk using ML model
        assessRiskUsingML();
        
        return profit;
    }
    
    // Method to simulate crypto mining
    public double mineCrypto(double electricityCost) {
        // Validate inputs
        if (electricityCost <= 0 || balance < electricityCost) {
            return 0.0;
        }
        
        // Subtract electricity cost from balance
        this.balance -= electricityCost;
        
        // Mining has steady returns with occasional bonuses
        double baseReturn = electricityCost * 1.1; // 10% base profit
        double bonusChance = 0.15; // 15% chance of bonus
        
        double returns = baseReturn;
        if (Math.random() < bonusChance) {
            returns += electricityCost * Math.random(); // Bonus up to 100% of cost
        }
        
        // Add returns to balance
        this.balance += returns;
        
        // Calculate profit
        double profit = returns - electricityCost;
        
        // Record transaction
        String timestamp = Instant.now().toString();
        this.updateProfitMap("cryptomining", timestamp, profit);
        
        // Re-assess risk using ML model
        assessRiskUsingML();
        
        return profit;
    }
    
    // Get risk category as string
    public String getRiskCategoryString() {
        switch (riskCategory) {
            case 0: return "Low Risk";
            case 1: return "Medium Risk";
            case 2: return "High Risk";
            default: return "Unknown Risk";
        }
    }
    
    // Get feature importance explanations
    public List<String> getFeatureImportanceExplanations() {
        List<String> explanations = new ArrayList<>();
        
        for (Map.Entry<String, Double> feature : featureImportance.entrySet()) {
            String impact = feature.getValue() > 0 ? "increases" : "decreases";
            String magnitude = Math.abs(feature.getValue()) > 0.3 ? "significantly" : "slightly";
            
            explanations.add(String.format("Your %s %s %s your interest rate", 
                    formatFeatureName(feature.getKey()), magnitude, impact));
        }
        
        return explanations;
    }
    
    private String formatFeatureName(String featureName) {
        return featureName
            .replace("_", " ")
            .replace("casino frequency", "casino gaming activity")
            .replace("profit loss ratio", "gambling win/loss record")
            .replace("recent activity", "recent gambling activity")
            .replace("loan balance ratio", "loan-to-balance ratio")
            .replace("loan history", "loan repayment history")
            .replace("stock activity", "stock market investments")
            .replace("crypto activity", "cryptocurrency mining")
            .replace("balance trend", "account balance history");
    }

    public static Bank[] init(Person[] persons) {
        ArrayList<Bank> bankList = new ArrayList<>();

        for (Person person : persons) {
            Bank bank = new Bank(person);
            bank.assessRiskUsingML(); // Set initial rate based on ML model
            bankList.add(bank);
        }

        return bankList.toArray(new Bank[0]);
    }
}