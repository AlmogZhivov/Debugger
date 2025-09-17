package il.ac.bgu.se.bp.service.code;

public interface FileHelper {
    void createFile(String filename) throws Exception;
    void removeFile(String filename) throws Exception;

    String readFile(String filename) throws Exception;
    void writeTextToFile(String sourceCode, String filename) throws Exception;

    void createDirectory(String directoryName) throws Exception;
    void removeDirectory(String directoryName) throws Exception;

}