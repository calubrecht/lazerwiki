package us.calubrecht.lazerwiki.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Service
public class MediaService {
    Logger logger = LogManager.getLogger(getClass());
    @Autowired
    SiteService siteService;

    @Value("${lazerwiki.static.file.root}")
    String staticFileRoot;

    public byte[] getBinaryFile(String host, String userName, String fileName) throws IOException {
        String site = siteService.getSiteForHostname(host);
        File f = new File(String.join("/", staticFileRoot, site, "media", fileName));
        logger.info("Reading file " + f.getAbsoluteFile());
        return Files.readAllBytes(f.toPath());
    }
}
