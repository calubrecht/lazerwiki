package us.calubrecht.lazerwiki.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class ResourceService {
    Logger logger = LogManager.getLogger(getClass());
    @Value("${lazerwiki.static.file.root}")
    String staticFileRoot;

    @Autowired
    SiteService siteService;

    void ensureDir(String site) throws IOException {
        Files.createDirectories(Paths.get(String.join("/", staticFileRoot, site, "resources")));
    }
    public byte[] getBinaryFile(String host, String fileName) throws IOException {
        String site = siteService.getSiteForHostname(host);
        ensureDir(site);
        File f = new File(String.join("/", staticFileRoot, site, "resources", fileName));
        if (!f.exists()) {
            logger.info("Reading file from resources " + String.join("/", "static", fileName));
            InputStream s = getClass().getClassLoader().getResourceAsStream(String.join("/", "static", fileName));
            if (s == null ) {
                throw new IOException("Error reading " + fileName);
            }
            return s.readAllBytes();
        }
        logger.info("Reading file " + f.getAbsoluteFile());
        return Files.readAllBytes(f.toPath());
    }
}
