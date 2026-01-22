package in.gov.chennaicorporation.mobileservice.gccparks.service;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;

@ControllerAdvice
public class GlobalExceptionHandler  {
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<String> handlePageNotFoundException(NoHandlerFoundException ex) {
        // Log the exception
        ex.printStackTrace();

        // Get the requested URL from the exception
        String requestedUrl = ex.getRequestURL();

        // Create an error message with the requested URL
        String errorMessage = "Page not found: " + requestedUrl;

        // Return a ResponseEntity with a 404 Not Found status code and error message
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        // Log the exception
        ex.printStackTrace();

        // Create an error response object
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "An error occurred: " + ex.getMessage());

        // Return a ResponseEntity with an appropriate HTTP status code and error response object
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    @ExceptionHandler(NoDataException.class)
    public ResponseEntity<ErrorResponse> noDataException(NoDataException ex){
    	
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.NOT_FOUND.value(),ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);

    	
    }
}
