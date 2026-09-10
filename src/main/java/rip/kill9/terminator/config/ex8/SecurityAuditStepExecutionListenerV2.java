package rip.kill9.terminator.config.ex8;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeJob;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;

@Slf4j
public class SecurityAuditStepExecutionListenerV2 {

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        log.info("[STEP SECURITY SCAN] Step 영역 스캔 시작: {}", stepExecution.getStepName());
    }

    @AfterStep
    public @Nullable ExitStatus afterStep(StepExecution stepExecution) {
        log.info("[STEP SECURITY SCAN] Step 영역 스캔 완료. Step Status: {}", stepExecution.getStatus());
        return stepExecution.getExitStatus();
    }
}
