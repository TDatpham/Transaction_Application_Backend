package com.webapp.bankingportal.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.text.SimpleDateFormat;

import com.webapp.bankingportal.repository.AccountRepository;
import org.springframework.stereotype.Service;

import com.webapp.bankingportal.dto.TransactionDTO;
import com.webapp.bankingportal.dto.ChartDataDTO;
import com.webapp.bankingportal.dto.ExpenseStatisticsDTO;
import com.webapp.bankingportal.entity.Account;
import com.webapp.bankingportal.entity.Transaction;
import com.webapp.bankingportal.entity.TransactionType;
import com.webapp.bankingportal.mapper.TransactionMapper;
import com.webapp.bankingportal.repository.TransactionRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class TransactionServiceImpl implements TransactionService {

	private static final Logger log = LoggerFactory.getLogger(TransactionServiceImpl.class);

	private final TransactionRepository transactionRepository;
	private final TransactionMapper transactionMapper = new TransactionMapper();
	private final EmailService emailService;
	private final AccountRepository accountRepository;

	public TransactionServiceImpl(TransactionRepository transactionRepository,
			EmailService emailService,
			AccountRepository accountRepository) {
		this.transactionRepository = transactionRepository;
		this.emailService = emailService;
		this.accountRepository = accountRepository;
	}

	@Override
	public List<TransactionDTO> getAllTransactionsByAccountNumber(String accountNumber) {
		log.debug("Getting all transactions for account: {}", accountNumber);
		List<Transaction> transactions = transactionRepository
				.findBySourceAccount_AccountNumberOrTargetAccount_AccountNumber(accountNumber, accountNumber);

		List<TransactionDTO> transactionDTOs = transactions.parallelStream()
				.map(transactionMapper::toDto)
				.sorted((t1, t2) -> t2.getTransactionDate().compareTo(t1.getTransactionDate()))
				.collect(Collectors.toList());

		log.debug("Found {} transactions for account: {}", transactionDTOs.size(), accountNumber);
		return transactionDTOs;
	}

	public void sendBankStatementByEmail(String accountNumber) {
		log.info("Sending bank statement by email for account: {}", accountNumber);
		if (accountNumber == null || accountNumber.trim().isEmpty()) {
			log.warn("Account number is null or empty");
			throw new IllegalArgumentException("Account number must not be null or empty");
		}
		List<TransactionDTO> transactions = getAllTransactionsByAccountNumber(accountNumber);

		StringBuilder sb = new StringBuilder();
		sb.append("Bank Statement for Account: ").append(accountNumber).append("\n\n");

		for (TransactionDTO txn : transactions) {
			sb.append("Date: ").append(txn.getTransactionDate())
					.append(", Type: ").append(txn.getTransactionType())
					.append(", Amount: ").append(txn.getAmount())
					.append("\n");
		}

		Account account = accountRepository.findByAccountNumber(accountNumber);
		if (account == null || account.getUser() == null) {
			log.warn("Account or user not found for account: {}", accountNumber);
			return;
		}
		String email = account.getUser().getEmail();
		emailService.sendEmail(email, "Your Bank Statement", sb.toString());
		log.info("Bank statement sent successfully to: {}", email);
	}

	@Override
	public Map<String, Object> getTransactionChartData(String accountNumber, Integer year, String month) {
		log.debug("Getting chart data for account: {}, year: {}, month: {}", accountNumber, year, month);

		// Get all transactions for the account
		List<Transaction> transactions = transactionRepository
				.findBySourceAccount_AccountNumberOrTargetAccount_AccountNumber(accountNumber, accountNumber);

		// Filter transactions by year and month if provided
		List<Transaction> filteredTransactions = transactions.stream()
				.filter(txn -> {
					if (txn.getTransactionDate() == null)
						return false;

					Calendar cal = Calendar.getInstance();
					cal.setTime(txn.getTransactionDate());

					// Filter by year if provided
					if (year != null && cal.get(Calendar.YEAR) != year) {
						return false;
					}

					// Filter by month if provided
					if (month != null && !month.trim().isEmpty()) {
						String monthName = new SimpleDateFormat("MMMM", Locale.ENGLISH)
								.format(txn.getTransactionDate());
						if (!monthName.equalsIgnoreCase(month.trim())) {
							return false;
						}
					}

					return true;
				})
				.collect(Collectors.toList());

		// Group transactions by date
		Map<String, ChartDataDTO> chartDataMap = new HashMap<>();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

		for (Transaction txn : filteredTransactions) {
			if (txn.getTransactionDate() == null || txn.getTransactionType() == null) {
				continue;
			}

			String dateKey = dateFormat.format(txn.getTransactionDate());
			ChartDataDTO chartData = chartDataMap.getOrDefault(dateKey,
					new ChartDataDTO(dateKey, 0.0, 0.0, 0.0, 0.0));

			// Determine if this transaction is for the account (incoming or outgoing)
			boolean isSourceAccount = txn.getSourceAccount() != null &&
					accountNumber.equals(txn.getSourceAccount().getAccountNumber());

			double amount = txn.getAmount();

			// Add amounts based on transaction type
			switch (txn.getTransactionType()) {
				case CASH_DEPOSIT:
					chartData.setDeposit(chartData.getDeposit() + amount);
					break;
				case CASH_WITHDRAWAL:
					chartData.setWithdrawal(chartData.getWithdrawal() + amount);
					break;
				case CASH_TRANSFER:
					// Count transfer for both incoming and outgoing
					chartData.setTransfer(chartData.getTransfer() + amount);
					break;
				case CASH_CREDIT:
					chartData.setCredit(chartData.getCredit() + amount);
					break;
			}

			chartDataMap.put(dateKey, chartData);
		}

		// Convert to sorted list
		List<ChartDataDTO> chartDataList = new ArrayList<>(chartDataMap.values());
		chartDataList.sort((a, b) -> a.getDate().compareTo(b.getDate()));

		// Build response
		Map<String, Object> response = new HashMap<>();
		response.put("data", chartDataList);
		response.put("labels", chartDataList.stream().map(ChartDataDTO::getDate).collect(Collectors.toList()));

		log.debug("Returning {} chart data points", chartDataList.size());
		return response;
	}

	@Override
	public List<Integer> getAvailableYears(String accountNumber) {
		log.debug("Getting available years for account: {}", accountNumber);

		List<Transaction> transactions = transactionRepository
				.findBySourceAccount_AccountNumberOrTargetAccount_AccountNumber(accountNumber, accountNumber);

		List<Integer> years = transactions.stream()
				.filter(txn -> txn.getTransactionDate() != null)
				.map(txn -> {
					Calendar cal = Calendar.getInstance();
					cal.setTime(txn.getTransactionDate());
					return cal.get(Calendar.YEAR);
				})
				.distinct()
				.sorted((a, b) -> b.compareTo(a)) // Sort descending (most recent first)
				.collect(Collectors.toList());

		log.debug("Found {} distinct years", years.size());
		return years;
	}

	@Override
	public Map<String, Object> getMonthlySummary(String accountNumber, Integer year) {
		log.debug("Getting monthly summary for account: {}, year: {}", accountNumber, year);

		// Default to current year if not provided - use final variable for lambda
		final int targetYear = (year == null) ? Calendar.getInstance().get(Calendar.YEAR) : year;

		// Get all transactions for the account
		List<Transaction> transactions = transactionRepository
				.findBySourceAccount_AccountNumberOrTargetAccount_AccountNumber(accountNumber, accountNumber);

		// Filter by year
		List<Transaction> filteredTransactions = transactions.stream()
				.filter(txn -> {
					if (txn.getTransactionDate() == null)
						return false;
					Calendar cal = Calendar.getInstance();
					cal.setTime(txn.getTransactionDate());
					return cal.get(Calendar.YEAR) == targetYear;
				})
				.collect(Collectors.toList());

		// Initialize monthly totals (12 months)
		String[] monthNames = { "January", "February", "March", "April", "May", "June",
				"July", "August", "September", "October", "November", "December" };

		Map<String, Double> monthlyTotals = new HashMap<>();
		for (String monthName : monthNames) {
			monthlyTotals.put(monthName, 0.0);
		}

		// Calculate totals per month
		for (Transaction txn : filteredTransactions) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(txn.getTransactionDate());
			int monthIndex = cal.get(Calendar.MONTH);
			String monthName = monthNames[monthIndex];

			double amount = txn.getAmount();
			monthlyTotals.put(monthName, monthlyTotals.get(monthName) + amount);
		}

		// Build response
		Map<String, Object> response = new HashMap<>();
		response.put("year", targetYear);
		response.put("labels", monthNames);

		List<Double> amounts = new ArrayList<>();
		for (String monthName : monthNames) {
			amounts.add(monthlyTotals.get(monthName));
		}
		response.put("amounts", amounts);

		log.debug("Monthly summary calculated for year: {}", targetYear);
		return response;
	}

	@Override
	public ExpenseStatisticsDTO getExpenseStatistics(String accountNumber, Integer year) {
		log.debug("Getting expense statistics for account: {}, year: {}", accountNumber, year);

		// Default to current year if not provided
		final int targetYear = (year == null) ? Calendar.getInstance().get(Calendar.YEAR) : year;

		// Get all transactions for the account
		List<Transaction> transactions = transactionRepository
				.findBySourceAccount_AccountNumberOrTargetAccount_AccountNumber(accountNumber, accountNumber);

		// Filter by year and include only transactions with categories
		List<Transaction> filteredTransactions = transactions.stream()
				.filter(txn -> {
					if (txn.getTransactionDate() == null || txn.getTransactionType() == null) {
						return false;
					}

					Calendar cal = Calendar.getInstance();
					cal.setTime(txn.getTransactionDate());

					// Filter by year
					if (cal.get(Calendar.YEAR) != targetYear) {
						return false;
					}

					// Only include transactions with categories
					if (txn.getCategory() == null || txn.getCategory().trim().isEmpty()) {
						return false;
					}

					// Include deposits with category
					if (txn.getTransactionType() == TransactionType.CASH_DEPOSIT) {
						return true;
					}

					// Include withdrawals with category
					if (txn.getTransactionType() == TransactionType.CASH_WITHDRAWAL) {
						return true;
					}

					// Include outgoing transfers (where account is source) with category
					if (txn.getTransactionType() == TransactionType.CASH_TRANSFER &&
							txn.getSourceAccount() != null &&
							txn.getSourceAccount().getAccountNumber().equals(accountNumber)) {
						return true;
					}

					return false;
				})
				.collect(Collectors.toList());

		// Calculate totals based on transactions with categories
		double totalDeposits = 0.0;
		double totalWithdrawals = 0.0;

		for (Transaction txn : filteredTransactions) {
			if (txn.getTransactionType() == TransactionType.CASH_DEPOSIT) {
				totalDeposits += txn.getAmount();
			} else if (txn.getTransactionType() == TransactionType.CASH_WITHDRAWAL) {
				totalWithdrawals += txn.getAmount();
			} else if (txn.getTransactionType() == TransactionType.CASH_TRANSFER &&
					txn.getSourceAccount() != null &&
					txn.getSourceAccount().getAccountNumber().equals(accountNumber)) {
				// Outgoing transfers count as expenses
				totalWithdrawals += txn.getAmount();
			}
		}

		double netBalance = totalDeposits - totalWithdrawals;

		// Build monthly data
		String[] monthNames = { "January", "February", "March", "April", "May", "June",
				"July", "August", "September", "October", "November", "December" };

		Map<String, ExpenseStatisticsDTO.MonthlyExpenseData> monthlyDataMap = new HashMap<>();
		for (String monthName : monthNames) {
			monthlyDataMap.put(monthName, new ExpenseStatisticsDTO.MonthlyExpenseData(monthName, targetYear, 0.0, 0.0));
		}

		for (Transaction txn : filteredTransactions) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(txn.getTransactionDate());
			int monthIndex = cal.get(Calendar.MONTH);
			String monthName = monthNames[monthIndex];

			ExpenseStatisticsDTO.MonthlyExpenseData monthlyData = monthlyDataMap.get(monthName);

			// Add to category data for this month
			if (monthlyData.getCategoryData() == null) {
				monthlyData.setCategoryData(new HashMap<>());
			}
			String category = txn.getCategory();

			if (txn.getTransactionType() == TransactionType.CASH_DEPOSIT) {
				monthlyData.setDeposits(monthlyData.getDeposits() + txn.getAmount());
				monthlyData.getCategoryData().put(category,
						monthlyData.getCategoryData().getOrDefault(category, 0.0) + txn.getAmount());
			} else if (txn.getTransactionType() == TransactionType.CASH_WITHDRAWAL) {
				monthlyData.setWithdrawals(monthlyData.getWithdrawals() + txn.getAmount());
				monthlyData.getCategoryData().put(category,
						monthlyData.getCategoryData().getOrDefault(category, 0.0) + txn.getAmount());
			} else if (txn.getTransactionType() == TransactionType.CASH_TRANSFER &&
					txn.getSourceAccount() != null &&
					txn.getSourceAccount().getAccountNumber().equals(accountNumber)) {
				monthlyData.setWithdrawals(monthlyData.getWithdrawals() + txn.getAmount());
				monthlyData.getCategoryData().put(category,
						monthlyData.getCategoryData().getOrDefault(category, 0.0) + txn.getAmount());
			}
		}

		List<ExpenseStatisticsDTO.MonthlyExpenseData> monthlyDataList = new ArrayList<>(monthlyDataMap.values());

		// Build daily data
		Map<String, ExpenseStatisticsDTO.DailyExpenseData> dailyDataMap = new HashMap<>();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

		for (Transaction txn : filteredTransactions) {
			String dateKey = dateFormat.format(txn.getTransactionDate());
			ExpenseStatisticsDTO.DailyExpenseData dailyData = dailyDataMap.getOrDefault(dateKey,
					new ExpenseStatisticsDTO.DailyExpenseData(dateKey, 0.0, 0.0));

			// Add to category data for this day
			if (dailyData.getCategoryData() == null) {
				dailyData.setCategoryData(new HashMap<>());
			}
			String category = txn.getCategory();

			if (txn.getTransactionType() == TransactionType.CASH_DEPOSIT) {
				dailyData.setDeposits(dailyData.getDeposits() + txn.getAmount());
				dailyData.getCategoryData().put(category,
						dailyData.getCategoryData().getOrDefault(category, 0.0) + txn.getAmount());
			} else if (txn.getTransactionType() == TransactionType.CASH_WITHDRAWAL) {
				dailyData.setWithdrawals(dailyData.getWithdrawals() + txn.getAmount());
				dailyData.getCategoryData().put(category,
						dailyData.getCategoryData().getOrDefault(category, 0.0) + txn.getAmount());
			} else if (txn.getTransactionType() == TransactionType.CASH_TRANSFER &&
					txn.getSourceAccount() != null &&
					txn.getSourceAccount().getAccountNumber().equals(accountNumber)) {
				dailyData.setWithdrawals(dailyData.getWithdrawals() + txn.getAmount());
				dailyData.getCategoryData().put(category,
						dailyData.getCategoryData().getOrDefault(category, 0.0) + txn.getAmount());
			}

			dailyDataMap.put(dateKey, dailyData);
		}

		List<ExpenseStatisticsDTO.DailyExpenseData> dailyDataList = new ArrayList<>(dailyDataMap.values());
		dailyDataList.sort((a, b) -> a.getDate().compareTo(b.getDate()));

		// Build weekly data
		Map<String, ExpenseStatisticsDTO.WeeklyExpenseData> weeklyDataMap = new HashMap<>();

		for (Transaction txn : filteredTransactions) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(txn.getTransactionDate());
			int weekOfYear = cal.get(Calendar.WEEK_OF_YEAR);
			String monthName = monthNames[cal.get(Calendar.MONTH)];
			String weekKey = "Week " + weekOfYear + ", " + monthName + " " + targetYear;

			ExpenseStatisticsDTO.WeeklyExpenseData weeklyData = weeklyDataMap.getOrDefault(weekKey,
					new ExpenseStatisticsDTO.WeeklyExpenseData(weekKey, 0.0, 0.0));

			// Add to category data for this week
			if (weeklyData.getCategoryData() == null) {
				weeklyData.setCategoryData(new HashMap<>());
			}
			String category = txn.getCategory();

			if (txn.getTransactionType() == TransactionType.CASH_DEPOSIT) {
				weeklyData.setDeposits(weeklyData.getDeposits() + txn.getAmount());
				weeklyData.getCategoryData().put(category,
						weeklyData.getCategoryData().getOrDefault(category, 0.0) + txn.getAmount());
			} else if (txn.getTransactionType() == TransactionType.CASH_WITHDRAWAL) {
				weeklyData.setWithdrawals(weeklyData.getWithdrawals() + txn.getAmount());
				weeklyData.getCategoryData().put(category,
						weeklyData.getCategoryData().getOrDefault(category, 0.0) + txn.getAmount());
			} else if (txn.getTransactionType() == TransactionType.CASH_TRANSFER &&
					txn.getSourceAccount() != null &&
					txn.getSourceAccount().getAccountNumber().equals(accountNumber)) {
				weeklyData.setWithdrawals(weeklyData.getWithdrawals() + txn.getAmount());
				weeklyData.getCategoryData().put(category,
						weeklyData.getCategoryData().getOrDefault(category, 0.0) + txn.getAmount());
			}

			weeklyDataMap.put(weekKey, weeklyData);
		}

		List<ExpenseStatisticsDTO.WeeklyExpenseData> weeklyDataList = new ArrayList<>(weeklyDataMap.values());
		weeklyDataList.sort((a, b) -> a.getWeek().compareTo(b.getWeek()));

		// Build category data (all transactions with categories)
		Map<String, Double> categoryDataMap = new HashMap<>();
		for (Transaction txn : filteredTransactions) {
			String category = txn.getCategory();
			categoryDataMap.put(category, categoryDataMap.getOrDefault(category, 0.0) + txn.getAmount());
		}

		// Build response
		ExpenseStatisticsDTO statistics = new ExpenseStatisticsDTO(totalDeposits, totalWithdrawals, netBalance);
		statistics.setMonthlyData(monthlyDataList);
		statistics.setDailyData(dailyDataList);
		statistics.setWeeklyData(weeklyDataList);
		statistics.setCategoryData(categoryDataMap);

		log.debug("Expense statistics calculated: deposits={}, withdrawals={}, netBalance={}, categories={}",
				totalDeposits, totalWithdrawals, netBalance, categoryDataMap.size());

		return statistics;
	}

}