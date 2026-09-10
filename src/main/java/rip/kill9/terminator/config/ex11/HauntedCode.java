package rip.kill9.terminator.config.ex11;

public record HauntedCode(
    String type,
    String filename,
    String authorId,
    String lastCommit,
    String docStatus
) implements CursedCode {
}
