package il.ac.bgu.se.bp.service.code;

public interface SourceCodeHelper {
    String createCodeFile(String sourceCode);
    void removeCodeFile(String filename);
    String readCodeFile(String filename);
}
