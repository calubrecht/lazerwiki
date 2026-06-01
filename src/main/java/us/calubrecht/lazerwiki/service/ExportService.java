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
import us.calubrecht.lazerwiki.model.MediaRecord;
import us.calubrecht.lazerwiki.model.PageDesc;
import us.calubrecht.lazerwiki.responses.MediaListResponse;
import us.calubrecht.lazerwiki.responses.PageData;
import us.calubrecht.lazerwiki.responses.PageListResponse;
import us.calubrecht.lazerwiki.service.exception.MediaReadException;
import us.calubrecht.lazerwiki.service.exception.MediaWriteException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.NoSuchObjectException;
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

    String printMetaData(PageDesc page, List<String> tags) {
        return
                "Name: " + page.getPagename() + "\n" +
                "NameSpace: " + page.getNamespace() + "\n" +
                "Tags: " + String.join(",", tags) + "\n" +
                "Format: Doku\n";
    }

    public void createExportBundle(String site, String user, OutputStream out) throws IOException {
        String hostName = siteService.getHostForSitename(site);
        PageListResponse pageList = pageService.getAllPages(hostName, user);
        logger.info("Creating export file for {}", site);
        try (             GzipCompressorOutputStream gos = new GzipCompressorOutputStream(out);
             TarArchiveOutputStream taos = new TarArchiveOutputStream(gos)) {
            for (String ns : pageList.pages().keySet().stream().sorted().toList()) {
                List<PageDesc> pages = pageList.pages().get(ns);
                for (PageDesc page : pages) {
                    String descriptor = page.getDescriptor();
                    Path filePath = toPath("pages", page.getNamespace(), page.getPagename() + ".txt");
                    PageData data = pageService.getPageData(hostName, descriptor, user);
                    String pageText = data.source();
                    TarArchiveEntry entry = new TarArchiveEntry(filePath.toString());

                    byte[] contentBytes = pageText.getBytes(StandardCharsets.UTF_8);
                    entry.setSize(contentBytes.length);

                    taos.putArchiveEntry(entry);
                    taos.write(contentBytes);
                    taos.closeArchiveEntry();

                    Path metaPath = toPath("pages", page.getNamespace(), page.getPagename() + ".meta");
                    TarArchiveEntry metaData = new TarArchiveEntry(metaPath.toString());
                    byte[] metaDataBytes = printMetaData(page, data.tags()).getBytes(StandardCharsets.UTF_8);
                    metaData.setSize(metaDataBytes.length);
                    taos.putArchiveEntry(metaData);
                    taos.write(metaDataBytes);
                    taos.closeArchiveEntry();
                }
            }
            MediaListResponse mediaList = mediaService.getAllFiles(hostName, null);
            for (String ns : mediaList.media().keySet().stream().sorted().toList()) {
                List<MediaRecord> mediaItems = mediaList.media().get(ns);
                for (MediaRecord media : mediaItems) {
                    Path filePath = toPath("media", media.getNamespace(), media.getFileName());
                    try {
                        byte[] data = mediaService.getBinaryFile(hostName, user, media.getPath(), null);

                        TarArchiveEntry entry = new TarArchiveEntry(filePath.toString());
                        entry.setSize(data.length);

                        taos.putArchiveEntry(entry);
                        taos.write(data);
                        taos.closeArchiveEntry();
                    }
                    catch (NoSuchFileException nsfe) {
                        logger.error("Missing file during export: {}", nsfe.getFile());
                    }
                }
            }
            taos.finish();
            logger.info("Export for {} created successfully", site);
        } catch (IOException | MediaReadException | MediaWriteException e) {
            throw new RuntimeException(e);
        }

    }

    private Path toPath(String root, String namespace, String pageName) {
        String[] ns = namespace.split(":");
        List<String> path = new ArrayList<>();
        path.add(root);
        path.addAll(Arrays.asList(ns));
        path.add(pageName);
        return Path.of(Strings.join(path, '/'));
    }
}
