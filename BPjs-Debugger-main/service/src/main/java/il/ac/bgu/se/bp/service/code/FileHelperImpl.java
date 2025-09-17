package il.ac.bgu.se.bp.service.code;

import il.ac.bgu.se.bp.utils.logger.Logger;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;

@Component
public class FileHelperImpl implements FileHelper {

    private static final Logger logger = new Logger(FileHelperImpl.class);

    @Override
    public void createFile(String filename) throws Exception {
        File file = new File(filename);
        if (file.createNewFile()) {
            logger.info("file created: {0}", filename);
        }
        else {
            throw new RuntimeException("file " + filename + " already exists");
        }
    }

    @Override
    public void removeFile(String filename) throws Exception {
        removeFile(new File(filename));
    }

    @Override
    public String readFile(String filename) throws Exception {
        File file = new File(filename);
        if (!file.exists()) {
            return null;
        }

        StringBuilder resultStringBuilder = new StringBuilder();
        try (InputStream inputStream = new FileInputStream(file)) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = br.readLine()) != null) {
                    resultStringBuilder.append(line).append("\n");
                }
            }
        }

        return resultStringBuilder.toString();
    }

    private void removeFile(File file) throws Exception {
        if (!file.exists()) {
            return;
        }

        if (file.isDirectory()) {
            removeDirectory(file);
            return;
        }

        logger.info("file: {0} deletion result: {1}", file.getName(), file.delete());
    }


    @Override
    public void writeTextToFile(String sourceCode, String filename) throws Exception {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
        writer.write(sourceCode);
        writer.close();
        logger.info("finished writing code to file: {0}", filename);
    }

    @Override
    public void createDirectory(String directoryName) throws Exception {
        File file = new File(directoryName);
        Files.createDirectory(file.toPath());
        logger.info("created directory: {0}", directoryName);
    }

    @Override
    public void removeDirectory(String directoryName) throws Exception {
        removeDirectory(new File(directoryName));
    }

    private void removeDirectory(File file) throws Exception {
        if (!file.exists()) {
            return;
        }

        if (!file.isDirectory()) {
            removeFile(file);
            return;
        }

        File[] entries = file.listFiles();
        if (entries != null) {
            for (File entry : entries) {
                removeDirectory(entry);
            }
        }

        logger.info("directory: {0} deletion result: {1}", file.getName(), file.delete());
    }

}