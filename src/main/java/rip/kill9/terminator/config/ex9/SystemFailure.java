package rip.kill9.terminator.config.ex9;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SystemFailure {
    private String errorId;
    private LocalDateTime errorDateTime;
    private String severity;
    private String processId;
    private String errorMessage;
}
