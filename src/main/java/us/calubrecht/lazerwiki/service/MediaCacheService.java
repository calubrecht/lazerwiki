package us.calubrecht.lazerwiki.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import us.calubrecht.lazerwiki.model.MediaRecord;
import us.calubrecht.lazerwiki.service.exception.MediaReadException;
import us.calubrecht.lazerwiki.util.IOSupplier;
import us.calubrecht.lazerwiki.util.ImageUtil;

import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;


@Service
public class MediaCacheService {
    Logger logger = LogManager.getLogger(getClass());

    @Value("${lazerwiki.static.file.root}")
    String staticFileRoot;

    @Autowired
    ImageUtil imageUtil;

    void ensureDir(String site, String nsPath) throws IOException {
        if (nsPath.isBlank()) {
            Files.createDirectories(Paths.get(String.join("/", staticFileRoot, site, "media-cache")));
            return;
        }
        Files.createDirectories(Paths.get(String.join("/", staticFileRoot, site, "media-cache", nsPath)));
    }


    public byte[] getBinaryFile(String site, MediaRecord record, IOSupplier<byte[]> fileLoad, int width, int height) throws IOException {
        StopWatch sw = new StopWatch();
        sw.start();
        Path cacheLocation = Paths.get(staticFileRoot, site, "media-cache");
        // refuse to scale up
        if (record.getWidth() < width || record.getHeight() < height) {
            return null;
        }
        File cachedFile = record.getNamespace().isBlank() ? new File(Paths.get(cacheLocation.toString(), record.getFileName() + "-%sx%s".formatted(width, height)).toString())
                : new File(Paths.get(cacheLocation.toString(), record.getNamespace(),record.getFileName() + "-%sx%s".formatted(width, height)).toString());
        String nsPath = record.getNamespace().replaceAll(":", "/");
        ensureDir(site, nsPath);
        if (cachedFile.exists()) {
            logger.info("Reading scaled image " + record.getFileName() + " from cache");
            try (FileInputStream fin = new FileInputStream(cachedFile)) {
                return fin.readAllBytes();
            }
        }
        byte[] originalFile = fileLoad.get();
        byte[] scaledFile = imageUtil.scaleImage(new ByteArrayInputStream(originalFile), FilenameUtils.getExtension(record.getFileName()),width, height);
        try (FileOutputStream fout = new FileOutputStream(cachedFile)) {
            fout.write(scaledFile);
        }
        sw.stop();
        logger.info("Scaled image " + record.getFileName() + " and wrote " + cachedFile + " elapsed " + sw.getTotalTimeMillis() + "ms");
        return scaledFile;
    }
}
