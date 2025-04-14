package us.calubrecht.lazerwiki.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Service
@ConditionalOnProperty("lazerwiki.standalone.ui.warfile")
public class WarResourceService {

    @Value("${lazerwiki.standalone.ui.warfile}")
    String warFileLocation;

    JarFile file;

    synchronized JarFile getJarfile() throws IOException {
        if (file != null) {
            return file;
        }
        file = new JarFile(warFileLocation);
        return file;
    }

    public byte[] getBinaryFile(String fileName) throws IOException {
        // Whitelist files...

        JarFile jarFile = getJarfile();
        JarEntry entry =jarFile.getJarEntry(fileName);

        try (InputStream is = jarFile.getInputStream(entry)) {
            return is.readAllBytes();
        }
    }

    public long getFileLastModified(String fileName) throws IOException {
        JarFile jarFile = getJarfile();
        JarEntry entry =jarFile.getJarEntry(fileName);
        return entry.getLastModifiedTime().toMillis();
    }
}
