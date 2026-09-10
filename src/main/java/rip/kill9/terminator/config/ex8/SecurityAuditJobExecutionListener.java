package rip.kill9.terminator.config.ex8;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SecurityAuditJobExecutionListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("[JOB SECURITY AUDIT] Job 침투 시작. Job Name: {}", jobExecution.getJobInstance().getJobName());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        log.info("[JOB SECURITY AUDIT] Job 침투 종료. 흔적 수집 완료. Job Status: {}", jobExecution.getStatus());
    }
}
