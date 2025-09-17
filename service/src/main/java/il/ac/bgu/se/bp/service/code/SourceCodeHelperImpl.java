package il.ac.bgu.se.bp.service.code;

import il.ac.bgu.se.bp.utils.logger.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class SourceCodeHelperImpl implements SourceCodeHelper {

    private static final Logger logger = new Logger(SourceCodeHelperImpl.class);
    private static final String PROGRAMS_DIRECTORY = "programs/";
    private static final String FILENAME_PREFIX = PROGRAMS_DIRECTORY + "programFile_";
    private static final String FILENAME_SUFFIX = ".js";
    private static final AtomicInteger filenameCounter = new AtomicInteger(0);

    @Autowired
    private FileHelper fileHelper;

    @PostConstruct
    public void onInit() {
        try {
            fileHelper.removeDirectory(getBasePath() + PROGRAMS_DIRECTORY);
        } catch (Exception e) {
            logger.error("onInit failed removing programs folder, error: {0}", e.getMessage());
        }
        try {
            fileHelper.createDirectory(getBasePath() + PROGRAMS_DIRECTORY);
        } catch (Exception e) {
            logger.error("onInit failed creating programs folder, error: {0}", e.getMessage());
        }
    }

    @Override
    public String createCodeFile(String sourceCode) {
        String filename = generateFilename();
        try {
            String filepath = getBasePath() + filename;
            fileHelper.createFile(filepath);
            fileHelper.writeTextToFile(sourceCode, filepath);
            return filename;
        } catch (Exception e) {
            logger.error("failed creating or writing to file with name: {0}, error: {1}", e, filename, e.getMessage());
            return null;
        }
    }

    @Override
    public void removeCodeFile(String filename) {
        try {
            fileHelper.removeFile(getBasePath() + filename);
        } catch (Exception e) {
            logger.error("failed removing file: {0}, error: {1}", e, filename, e.getMessage());
        }
    }

    @Override
    public String readCodeFile(String filename) {
        try {
            return fileHelper.readFile(getBasePath() + filename);
        } catch (Exception e) {
            logger.error("failed reading file: {0}, error: {1}", e, filename, e.getMessage());
            return null;
        }
    }

    private String generateFilename() {
        return FILENAME_PREFIX + filenameCounter.incrementAndGet() + FILENAME_SUFFIX;
    }

    private String getBasePath() {
        ClassLoader classLoader = getClass().getClassLoader();
        return classLoader.getResource(".").getFile();
    }
}
