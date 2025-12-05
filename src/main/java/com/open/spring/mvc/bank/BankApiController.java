package com.open.spring.mvc.bank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.open.spring.mvc.person.Person;
import com.open.spring.mvc.person.PersonJpaRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@RestController
@RequestMapping("/bank")
public class BankApiController {

    @Autowired
    private BankService bankService;
    
    @Autowired
    private BankJpaRepository bankJpaRepository;
    

    @Autowired
    private PersonJpaRepository personJpaRepository;

    /**
     * Helper method to find or create a Bank for a given personId
     */
    private Bank findOrCreateBankByPersonId(Long personId) {
        Bank bank = bankJpaRepository.findByPersonId(personId);
        if (bank == null) {
            // Find the person
            Person person = personJpaRepository.findById(personId).orElse(null);
            if (person == null) {
                throw new RuntimeException("Person not found with ID: " + personId);
            }
            
            // Create new bank account
            bank = new Bank(person);
            bank.assessRiskUsingML();
            bank = bankJpaRepository.save(bank);
        }
        return bank;
    }

    // Get top 10 leaderboard
    @GetMapping("/leaderboard")
    public ResponseEntity<Map<String, Object>> getLeaderboard() {
        try {
            List<Bank> topBanks = bankJpaRepository.findTop10ByOrderByBalanceDesc();
            List<LeaderboardEntry> leaderboard = new ArrayList<>();
            
            for (int i = 0; i < topBanks.size(); i++) {
                Bank bank = topBanks.get(i);
                leaderboard.add(new LeaderboardEntry(
                    i + 1,
                    bank.getId(),
                    bank.getUsername() != null ? bank.getUsername() : "User " + bank.getId(),
                    bank.getBalance()
                ));
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", leaderboard
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "error", "Error fetching leaderboard: " + e.getMessage()
            ));
        }
    }
    
    // Search leaderboard
    @GetMapping("/leaderboard/search")
    public ResponseEntity<Map<String, Object>> searchLeaderboard(@RequestParam String query) {
        try {
            List<Bank> matchedBanks = bankJpaRepository.findByUidContainingIgnoreCase(query);
            List<LeaderboardEntry> leaderboard = new ArrayList<>();
            
            for (int i = 0; i < matchedBanks.size(); i++) {
                Bank bank = matchedBanks.get(i);
                leaderboard.add(new LeaderboardEntry(
                    i + 1,
                    bank.getId(),
                    bank.getUsername() != null ? bank.getUsername() : "User " + bank.getId(),
                    bank.getBalance()
                ));
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", leaderboard
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "error", "Error searching leaderboard: " + e.getMessage()
            ));
        }
    }

    // Get individual user analytics data
    @GetMapping("/analytics/{userId}")
    public ResponseEntity<Map<String, Object>> getUserAnalytics(@PathVariable Long userId) {
        try {
            Bank bank = bankJpaRepository.findById(userId).orElse(null);
            if (bank == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "success", false,
                    "error", "User not found"
                ));
            }

            // Prepare analytics data
            Map<String, Object> analyticsData = new HashMap<>();
            analyticsData.put("userId", bank.getId());
            analyticsData.put("username", bank.getUsername() != null ? bank.getUsername() : "User " + bank.getId());
            analyticsData.put("balance", bank.getBalance());
            analyticsData.put("loanAmount", bank.getLoanAmount());
            analyticsData.put("dailyInterestRate", bank.getDailyInterestRate());
            analyticsData.put("riskCategory", bank.getRiskCategory());
            analyticsData.put("riskCategoryString", bank.getRiskCategoryString());
            analyticsData.put("profitMap", bank.getProfitMap());
            analyticsData.put("featureImportance", bank.getFeatureImportance());
            analyticsData.put("featureExplanations", bank.getFeatureImportanceExplanations());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", analyticsData
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "error", "Error fetching user analytics: " + e.getMessage()
            ));
        }
    }

    // Get user analytics by person ID (alternative endpoint) - MODIFIED
    @GetMapping("/analytics/person/{personId}")
    public ResponseEntity<Map<String, Object>> getUserAnalyticsByPersonId(@PathVariable Long personId) {
        try {
            Bank bank = findOrCreateBankByPersonId(personId);

            // Prepare analytics data
            Map<String, Object> analyticsData = new HashMap<>();
            analyticsData.put("userId", bank.getId());
            analyticsData.put("personId", bank.getPerson().getId());
            analyticsData.put("username", bank.getUsername() != null ? bank.getUsername() : "User " + bank.getId());
            analyticsData.put("balance", bank.getBalance());
            analyticsData.put("loanAmount", bank.getLoanAmount());
            analyticsData.put("dailyInterestRate", bank.getDailyInterestRate());
            analyticsData.put("riskCategory", bank.getRiskCategory());
            analyticsData.put("riskCategoryString", bank.getRiskCategoryString());
            analyticsData.put("profitMap", bank.getProfitMap());
            analyticsData.put("featureImportance", bank.getFeatureImportance());
            analyticsData.put("featureExplanations", bank.getFeatureImportanceExplanations());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", analyticsData
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "error", "Error fetching user analytics: " + e.getMessage()
            ));
        }
    }

    // MODIFIED - Now creates bank if not found
    @GetMapping("/{id}/profitmap/{category}")
    public ResponseEntity<List<List<Object>>> getProfitByCategory(@PathVariable Long id, @PathVariable String category) {
        try {
            Bank bank = findOrCreateBankByPersonId(id);
            return ResponseEntity.ok(bank.getProfitByCategory(category));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // MODIFIED - Now creates bank if not found
    @GetMapping("/{id}/interestRate")
    public ResponseEntity<Double> getInterestRate(@PathVariable Long id) {
        try {
            Bank bank = findOrCreateBankByPersonId(id);
            return ResponseEntity.ok(bank.getDailyInterestRate());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/requestLoan")
    public ResponseEntity<String> requestLoan(@RequestBody LoanRequest request) {
        try {
            Bank bank = bankService.requestLoan(request.getPersonId(), request.getLoanAmount());
            return ResponseEntity.ok("Loan of amount " + request.getLoanAmount() + " granted to user with Person ID: " + request.getPersonId());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Loan request failed: " + e.getMessage());
        }
    }
    
    @PostMapping("/repayLoan")
    public ResponseEntity<String> repayLoan(@RequestBody RepaymentRequest request) {
        try {
            Bank bank = bankService.repayLoan(request.getPersonId(), request.getRepaymentAmount());
            return ResponseEntity.ok("Loan repayment of amount " + request.getRepaymentAmount() + 
                    " processed for user with Person ID: " + request.getPersonId() + 
                    ". Remaining loan amount: " + bank.getLoanAmount());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Loan repayment failed: " + e.getMessage());
        }
    }

    // MODIFIED - Now creates bank if not found
    @GetMapping("/{personId}/loanAmount")
    public ResponseEntity<Double> getLoanAmount(@PathVariable Long personId) {
        try {
            Bank bank = findOrCreateBankByPersonId(personId);
            return ResponseEntity.ok(bank.getLoanAmount());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    @Scheduled(fixedRate = 86400000, initialDelay = 60000)
    public void scheduledInterestApplication() {
        try {
            applyInterestToAllLoans();
        } catch (Exception e) {
            System.err.println("Scheduled interest application failed: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("Cause: " + e.getCause().getMessage());
            }
        }
    }
    
    @PostMapping("/newLoanAmountInterest")
    public String applyInterestToAllLoans() {
        try {
            List<Bank> allBanks = bankJpaRepository.findAll();
            if (allBanks.isEmpty()) {
                return "No banks found to apply interest to.";
            }
            for (Bank bank : allBanks) {
                bank.setLoanAmount(bank.getLoanAmount() * 1.05);
            }
            bankJpaRepository.saveAll(allBanks);
            return "Applied 5% interest to all loan amounts.";
        } catch (Exception e) {
            System.err.println("Error applying interest: " + e.getMessage());
            throw e;
        }
    }

    // MODIFIED - Now creates bank if not found
    @GetMapping("/{personId}/npcProgress")
    public ResponseEntity<LinkedHashMap<String, Boolean>> getNpcProgress(@PathVariable Long personId) {
        try {
            Bank bank = findOrCreateBankByPersonId(personId);
            return ResponseEntity.ok((LinkedHashMap<String, Boolean>) bank.getNpcProgress());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/updateNpcProgress")
    public ResponseEntity<LinkedHashMap<String, Boolean>> updateNpcProgress(@RequestBody npcProgress request) {
        try {
        String justCompletedNpc = request.getNpcId();
        if (justCompletedNpc == null || justCompletedNpc.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Bank bank = findOrCreateBankByPersonId(request.getPersonId());

        LinkedHashMap<String, Boolean> progressMap = bank.getNpcProgress();

        /*
          Iterate over the LinkedHashMap entries in insertion order.
          Once we find the entry whose key == justCompletedNpc, we set its value to false,
          then break out of that loop iteration and immediately set the VERY NEXT entry’s value to true.
        */
        boolean found = false;
        Iterator<Map.Entry<String, Boolean>> iter = progressMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Boolean> entry = iter.next();

            if (!found) {
                if (entry.getKey().equals(justCompletedNpc)) {
                    found = true;
                }
            }
            else {
                entry.setValue(true);
                break; 
            }
        }

        bank.setNpcProgress(progressMap);
        bankJpaRepository.save(bank);

        @SuppressWarnings("unchecked")
        LinkedHashMap<String, Boolean> result = (LinkedHashMap<String, Boolean>) bank.getNpcProgress();
        return ResponseEntity.ok(result);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    // Extract all bank accounts data into DTOs
    @GetMapping("/bulk/extract")
    public ResponseEntity<List<BankDto>> bulkExtract() {
        try {
            // Fetch all Bank entries from the database
            List<Bank> bankList = bankJpaRepository.findAll();
            
            // Map Bank entities to BankDto objects
            List<BankDto> bankDtos = new ArrayList<>();
            for (Bank bank : bankList) {
                BankDto bankDto = new BankDto();
                bankDto.setId(bank.getId());
                bankDto.setUsername(bank.getUsername());
                bankDto.setUid(bank.getUid());
                bankDto.setBalance(bank.getBalance());
                bankDto.setLoanAmount(bank.getLoanAmount());
                bankDto.setDailyInterestRate(bank.getDailyInterestRate());
                bankDto.setRiskCategory(bank.getRiskCategory());
                
                // Add person ID if available
                if (bank.getPerson() != null) {
                    bankDto.setPersonId(bank.getPerson().getId());
                }
                
                bankDtos.add(bankDto);
            }
            
            // Return the list of BankDto objects
            return new ResponseEntity<>(bankDtos, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @DeleteMapping("/bulk/clear")
    public ResponseEntity<?> clearTable(HttpServletRequest request) {
        try {
            // Get initial count
            long initialCount = bankJpaRepository.count();
            
            if (initialCount == 0) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "No bank records to clear");
                response.put("initialCount", 0);
                response.put("deletedCount", 0);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            
            // Attempt to clear
            bankService.clearAllBanks();
            
            // Verify deletion
            long finalCount = bankJpaRepository.count();
            long deletedCount = initialCount - finalCount;
            
            Map<String, Object> response = new HashMap<>();
            if (finalCount == 0) {
                response.put("status", "success");
                response.put("message", "All bank records have been cleared successfully");
            } else {
                response.put("status", "partial_success");
                response.put("message", String.format("Partially cleared: %d out of %d records deleted", deletedCount, initialCount));
            }
            
            response.put("initialCount", initialCount);
            response.put("finalCount", finalCount);
            response.put("deletedCount", deletedCount);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to clear table: " + e.getMessage());
            errorResponse.put("exception", e.getClass().getSimpleName());
            
            // Include current count for debugging
            try {
                errorResponse.put("currentCount", bankJpaRepository.count());
            } catch (Exception countException) {
                errorResponse.put("currentCount", "unable_to_count");
            }
            
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    // Alternative force clear endpoint
    @DeleteMapping("/bulk/clear/force")
    public ResponseEntity<?> forceClearTable(HttpServletRequest request) {
        try {
            long initialCount = bankJpaRepository.count();
            
            if (initialCount == 0) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "No bank records to clear");
                response.put("initialCount", 0);
                response.put("deletedCount", 0);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            
            // Use force clear method
            bankService.clearAllBanksForce();
            
            // Verify deletion
            long finalCount = bankJpaRepository.count();
            long deletedCount = initialCount - finalCount;
            
            Map<String, Object> response = new HashMap<>();
            if (finalCount == 0) {
                response.put("status", "success");
                response.put("message", "All bank records have been force cleared successfully");
            } else {
                response.put("status", "partial_success");
                response.put("message", String.format("Force clear partially successful: %d out of %d records deleted", deletedCount, initialCount));
            }
            
            response.put("initialCount", initialCount);
            response.put("finalCount", finalCount);
            response.put("deletedCount", deletedCount);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to force clear table: " + e.getMessage());
            errorResponse.put("exception", e.getClass().getSimpleName());
            
            try {
                errorResponse.put("currentCount", bankJpaRepository.count());
            } catch (Exception countException) {
                errorResponse.put("currentCount", "unable_to_count");
            }
            
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Bulk create/update bank accounts
    @PostMapping("/bulk/create")
    public ResponseEntity<Object> bulkCreateBanks(@RequestBody List<BankDto> bankDtos) {
        List<String> createdBanks = new ArrayList<>();
        List<String> updatedBanks = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (BankDto bankDto : bankDtos) {
            try {
                // If ID is provided, try to find existing bank
                Bank bank = null;
                
                if (bankDto.getId() != null) {
                    bank = bankJpaRepository.findById(bankDto.getId()).orElse(null);
                } else if (bankDto.getPersonId() != null) {
                    // Otherwise try to find by person ID
                    bank = bankJpaRepository.findByPersonId(bankDto.getPersonId());
                } else if (bankDto.getUid() != null) {
                    // Or by username
                    bank = bankJpaRepository.findByUid(bankDto.getUid());
                }
                
                if (bank != null) {
                    // Update existing bank
                    if (bankDto.getBalance() > 0) {
                        bank.setBalance(bankDto.getBalance());
                    }
                    
                    if (bankDto.getLoanAmount() >= 0) {
                        bank.setLoanAmount(bankDto.getLoanAmount());
                    }
                    
                    if (bankDto.getDailyInterestRate() > 0) {
                        bank.setDailyInterestRate(bankDto.getDailyInterestRate());
                    }
                    
                    bankJpaRepository.save(bank);
                    updatedBanks.add(bank.getUsername() != null ? bank.getUsername() : "Bank ID: " + bank.getId());
                } else if (bankDto.getUid() != null) {
                    // Create new bank account if person exists
                    Person person = personJpaRepository.findById(bankDto.getPersonId()).get();
                    
                    if (person != null) {
                        // Create new bank account using the modified constructor
                        bank = new Bank(person);
                        
                        // Set loan amount if provided
                        if (bankDto.getLoanAmount() >= 0) {
                            bank.setLoanAmount(bankDto.getLoanAmount());
                        }
                        
                        if (bankDto.getBalance() > 0) {
                            bank.setBalance(bankDto.getBalance());
                        }
                        
                        if (bankDto.getDailyInterestRate() > 0) {
                            bank.setDailyInterestRate(bankDto.getDailyInterestRate());
                        }
                        
                        bank.assessRiskUsingML();
                        bankJpaRepository.save(bank);
                        createdBanks.add(bank.getUsername());
                    } else {
                        errors.add("Person not found with ID: " + bankDto.getUid());
                    }
                } else if (bankDto.getUsername() != null) {
                    // Try to find person by username (name)
                    Person person = personJpaRepository.findByName(bankDto.getUsername());
                    
                    if (person != null) {
                        // Create new bank account
                        bank = new Bank(person);
                        
                        // Set properties if provided
                        if (bankDto.getLoanAmount() >= 0) {
                            bank.setLoanAmount(bankDto.getLoanAmount());
                        }
                        
                        if (bankDto.getBalance() > 0) {
                            bank.setBalance(bankDto.getBalance());
                        }
                        
                        if (bankDto.getDailyInterestRate() > 0) {
                            bank.setDailyInterestRate(bankDto.getDailyInterestRate());
                        }
                        
                        bank.assessRiskUsingML();
                        bankJpaRepository.save(bank);
                        createdBanks.add(bank.getUsername());
                    } else {
                        errors.add("Person not found with username: " + bankDto.getUsername());
                    }
                } else if (bankDto.getUid() != null) {
                    // Try to find person by UID
                    Person person = personJpaRepository.findByUid(bankDto.getUid());
                    
                    if (person != null) {
                        // Create new bank account
                        bank = new Bank(person);
                        
                        // Set properties if provided
                        if (bankDto.getLoanAmount() >= 0) {
                            bank.setLoanAmount(bankDto.getLoanAmount());
                        }
                        
                        if (bankDto.getBalance() > 0) {
                            bank.setBalance(bankDto.getBalance());
                        }
                        
                        if (bankDto.getDailyInterestRate() > 0) {
                            bank.setDailyInterestRate(bankDto.getDailyInterestRate());
                        }
                        
                        bank.assessRiskUsingML();
                        bankJpaRepository.save(bank);
                        createdBanks.add(bank.getUsername());
                    } else {
                        errors.add("Person not found with UID: " + bankDto.getUid());
                    }
                } else {
                    errors.add("Cannot create bank account: missing identification information (personId, username, or uid)");
                }
            } catch (Exception e) {
                errors.add("Exception for bank: " + 
                          (bankDto.getUsername() != null ? bankDto.getUsername() : "ID: " + bankDto.getId()) + 
                          " - " + e.getMessage());
            }
        }

        // Prepare the response
        Map<String, Object> response = new HashMap<>();
        response.put("created", createdBanks);
        response.put("updated", updatedBanks);
        response.put("errors", errors);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class LoanRequest {
    private Long personId;
    private double loanAmount;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class RepaymentRequest {
    private Long personId;
    private double repaymentAmount;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class LeaderboardEntry {
    private int rank;
    private Long userId;
    private String username;
    private double balance;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class BankDto {
    private Long id;
    private Long personId;
    private String username;
    private String uid;
    private double balance;
    private double loanAmount;
    private double dailyInterestRate;
    private int riskCategory;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class npcProgress {
    private Long personId;
    private String npcId;
}