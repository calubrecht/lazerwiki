package us.calubrecht.lazerwiki.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import us.calubrecht.lazerwiki.responses.PageListResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class ExportService {
    final Logger logger = LogManager.getLogger(getClass());
    @Autowired
    SiteService siteService;

    @Autowired
    PageService pageService;

    @Autowired
    MediaService mediaService;

    @Value("${lazerwiki.static.file.root}")
    String staticFileRoot;

    void ensureDir() throws IOException {
        Files.createDirectories(Paths.get(String.join("/", staticFileRoot, "tmp", "exports")));
    }

    public void createExportBundle(String hostName, String user) {
        PageListResponse pageList = pageService.getAllPages(hostName, user);
        String site = siteService.getSiteForHostname(hostName);
        logger.info("Creating export file for {}", site);
        Path exportFile = Paths.get(String.join("/", staticFileRoot, "tmp", "exports", site + "zip"));


    }

    private Path toPath(String namespace, String pagename) {
        return null;
    }
}
