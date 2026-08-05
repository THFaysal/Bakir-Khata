package com.example.bakir_khata.dto;


import com.example.bakir_khata.model.Loan;
import com.example.bakir_khata.model.Payment;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class DashboardSummaryDTO {
    private BigDecimal totalBorrowed;
    private BigDecimal totalPaid;
    private BigDecimal remainingBalance;
    private long overdueCount;
    private long dueTodayCount;
    private long dueThisWeekCount;
    private List<Loan> upcomingDueLoans;
    private List<Payment> recentPayments;
}
