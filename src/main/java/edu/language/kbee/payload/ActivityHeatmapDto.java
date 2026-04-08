package edu.language.kbee.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityHeatmapDto {
    private LocalDate date;
    private int count;
    private String dayName;
    private String monthName;
}
