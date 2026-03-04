package edu.language.kbee.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class InternalServerException extends RuntimeException {
  private String message;

  private HttpStatus status;

  private String[] params;

  public InternalServerException(String message) {
    super(message);
    this.status = HttpStatus.NOT_FOUND;
    this.message = message;
  }

  public InternalServerException(String message, HttpStatus status) {
    super(message);
    this.status = status;
    this.message = message;
  }

  public InternalServerException(String message, String[] params) {
    super(message);
    this.status = HttpStatus.NOT_FOUND;
    this.message = message;
    this.params = params;
  }

  public InternalServerException(HttpStatus status, String message, String[] params) {
    super(message);
    this.status = status;
    this.message = message;
    this.params = params;
  }
}
