import com.yohan.event_planner.exception.ErrorCode;
import com.yohan.event_planner.exception.GlobalExceptionHandler;
import com.yohan.event_planner.exception.PasswordException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class TestPasswordException {
    public static void main(String[] args) {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        
        // Test INVALID_RESET_TOKEN
        PasswordException invalidTokenException = new PasswordException(ErrorCode.INVALID_RESET_TOKEN);
        ResponseEntity<?> response = handler.handlePasswordException(invalidTokenException);
        
        System.out.println("INVALID_RESET_TOKEN status: " + response.getStatusCode());
        System.out.println("Expected: " + HttpStatus.UNAUTHORIZED);
        System.out.println("Match: " + (response.getStatusCode() == HttpStatus.UNAUTHORIZED));
        
        // Test WEAK_PASSWORD  
        PasswordException weakPasswordException = new PasswordException(ErrorCode.WEAK_PASSWORD);
        ResponseEntity<?> response2 = handler.handlePasswordException(weakPasswordException);
        
        System.out.println("WEAK_PASSWORD status: " + response2.getStatusCode());
        System.out.println("Expected: " + HttpStatus.BAD_REQUEST);
        System.out.println("Match: " + (response2.getStatusCode() == HttpStatus.BAD_REQUEST));
    }
}