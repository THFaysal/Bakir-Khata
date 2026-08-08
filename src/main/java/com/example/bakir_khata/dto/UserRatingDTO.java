package com.example.bakir_khata.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserRatingDTO {
    private double score;
    private String label;
    private int onTimeCount;
    private int lateCount;
    private int overdueCount;
    private boolean hasHistory;
    private int filledStars;
}
