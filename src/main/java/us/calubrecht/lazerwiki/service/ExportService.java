package us.calubrecht.lazerwiki.service;

import com.redfin.sitemapgenerator.WebSitemapUrl;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import us.calubrecht.lazerwiki.model.PageDesc;
import us.calubrecht.lazerwiki.responses.PageData;
import us.calubrecht.lazerwiki.responses.PageListResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
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
        try (OutputStream fos = Files.newOutputStream(exportFile);
             GzipCompressorOutputStream gos = new GzipCompressorOutputStream(fos);
             TarArchiveOutputStream taos = new TarArchiveOutputStream(gos)) {
             for (String ns : pageList.pages.keySet().stream().sorted().toList()) {
                List<PageDesc> pages = pageList.pages.get(ns);
                for (PageDesc page : pages) {
                    String descriptor = page.getDescriptor();
                    Path filePath = toPath(page.getNamespace(), page.getPagename());
                    PageData data = pageService.getPageData(hostName, descriptor, user);
                    String pageText = data.source();
                    TarArchiveEntry entry = new TarArchiveEntry(filePath);

                    byte[] contentBytes = pageText.getBytes(StandardCharsets.UTF_8);
                    entry.setSize(contentBytes.length);

                    taos.putArchiveEntry(entry);
                    taos.write(contentBytes);
                    taos.closeArchiveEntry();
                }
             }
            taos.finish();
            logger.info("Export file created successfully: {}", exportFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private Path toPath(String namespace, String pageName) {
        String[] ns = namespace.split(":");
        List<String> path = new ArrayList<>(Arrays.asList(ns));
        path.add(pageName);
        return Path.of(Strings.join(path, '/'));
    }
}
