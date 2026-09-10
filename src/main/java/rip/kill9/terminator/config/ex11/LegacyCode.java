package rip.kill9.terminator.config.ex11;

import lombok.Data;

@Data
public class LegacyCode implements CursedCode {

    private String type;
    private String filename;
    private String writtenDate;
    private String curseLevel;
    private String description;
    private String warning;


    @Override
    public String type() {
        return type;
    }

    @Override
    public String filename() {
        return filename;
    }
}
