package edu.language.kbee.payload;

import edu.language.kbee.enums.BookStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBookRequest {
    private String title;
    private String level; 
    private String duration;
    private String image;
    private BookStatus status;
}
