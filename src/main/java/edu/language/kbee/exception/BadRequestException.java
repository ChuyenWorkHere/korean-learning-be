package edu.language.kbee.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class BadRequestException extends RuntimeException {
  private String message;

  private HttpStatus status;

  private String[] params;

  public BadRequestException(String message) {
    super(message);
    this.status = HttpStatus.BAD_REQUEST;
    this.message = message;
  }

  public BadRequestException(String message, HttpStatus status) {
    super(message);
    this.status = status;
    this.message = message;
  }

  public BadRequestException(String message, String[] params) {
    super(message);
    this.status = HttpStatus.BAD_REQUEST;
    this.message = message;
    this.params = params;
  }

  public BadRequestException(HttpStatus status, String message, String[] params) {
    super(message);
    this.status = status;
    this.message = message;
    this.params = params;
  }
}
