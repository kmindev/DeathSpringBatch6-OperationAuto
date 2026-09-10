package rip.kill9.terminator.config.ex8;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;

@Slf4j
public class SecurityAuditStepExecutionListener implements StepExecutionListener {
    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.info("[STEP SECURITY SCAN] Step 영역 스캔 시작: {}", stepExecution.getStepName());
    }

    @Override
    public @Nullable ExitStatus afterStep(StepExecution stepExecution) {
        log.info("[STEP SECURITY SCAN] Step 영역 스캔 완료. Step Status: {}", stepExecution.getStatus());
        return stepExecution.getExitStatus();
    }
}
