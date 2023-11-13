package us.calubrecht.lazerwiki.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class MediaService {
    Logger logger = LogManager.getLogger(getClass());
    @Autowired
    SiteService siteService;

    @Value("${lazerwiki.static.file.root}")
    String staticFileRoot;

    void ensureDir(String site) throws IOException {
        Files.createDirectories(Paths.get(String.join("/", staticFileRoot, site, "media")));
    }

    public byte[] getBinaryFile(String host, String userName, String fileName) throws IOException {
        String site = siteService.getSiteForHostname(host);
        ensureDir(site);
        File f = new File(String.join("/", staticFileRoot, site, "media", fileName));
        logger.info("Reading file " + f.getAbsoluteFile());
        return Files.readAllBytes(f.toPath());
    }

    public void saveFile(String host, String userName, MultipartFile mfile) throws IOException {
        String site = siteService.getSiteForHostname(host);
        ensureDir(site);
        String fileName = mfile.getOriginalFilename();
        File f = new File(String.join("/", staticFileRoot, site, "media", fileName));
        logger.info("Writing file " + f.getAbsoluteFile());
        FileOutputStream fos = new FileOutputStream(f);
        IOUtils.copy(mfile.getInputStream(), fos);
    }
}
