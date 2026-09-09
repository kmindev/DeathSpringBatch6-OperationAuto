package rip.kill9.terminator.config.ex7;

import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersValidator;
import org.springframework.stereotype.Component;

@Component
public class SystemDestructionValidator implements JobParametersValidator {
    @Override
    public void validate(JobParameters parameters) throws InvalidJobParametersException {
        if (parameters == null) {
            throw new InvalidJobParametersException("파라미터가 NULL입니다.");
        }

        Long destructionPower = parameters.getLong("destructionPower");
        if (destructionPower == null) {
            throw new InvalidJobParametersException("destructionPower 파라미터는 필수값입니다.");
        }

        if (destructionPower > 9) {
            throw new InvalidJobParametersException("파괴력 수준이 허용치를 초과했습니다: " + destructionPower + " (최대 허용치:9)");
        }
    }
}
