package rip.kill9.terminator.config.ex6;

import lombok.Data;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@StepScope
@Component
public class SystemInfiltrationParameters {

    // 필드주입
    @Value("#{jobParameters['missionName']}")
    private String missionName;

    private AttackMethod attackMethod;
    private final String operationCommander;

    // 생성자 주입
    public SystemInfiltrationParameters(
        @Value("#{jobParameters['operationCommander']}") String operationCommander
    ) {
        this.operationCommander = operationCommander;
    }

    // 세터 메서드 주입
    @Value("#{jobParameters['attackMethod']}")
    public void setAttackMethod(AttackMethod attackMethod) {
        this.attackMethod = attackMethod;
    }

    public enum AttackMethod {
        UNPLUG_CABLE,
        HAMMER_SMASH,
        COFFEE_SPILL,
        CTRL_ALT_DELETE
    }
}
