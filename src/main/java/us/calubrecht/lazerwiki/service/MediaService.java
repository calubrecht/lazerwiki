package us.calubrecht.lazerwiki.service;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import us.calubrecht.lazerwiki.model.MediaRecord;
import us.calubrecht.lazerwiki.repository.MediaRecordRepository;
import us.calubrecht.lazerwiki.util.ImageUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.List;

@Service
public class MediaService {
    Logger logger = LogManager.getLogger(getClass());
    @Autowired
    SiteService siteService;

    @Autowired
    MediaRecordRepository mediaRecordRepository;

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

    @Transactional
    public void saveFile(String host, String userName, MultipartFile mfile) throws IOException {
        String site = siteService.getSiteForHostname(host);
        ensureDir(site);
        String fileName = mfile.getOriginalFilename();
        byte[] fileBytes = mfile.getBytes();
        ByteArrayInputStream bis = new ByteArrayInputStream(fileBytes);
        Pair<Integer, Integer> imageDimension = ImageUtil.getImageDimension(bis);
        MediaRecord newRecord = new MediaRecord(fileName, site, userName, mfile.getSize(), imageDimension.getLeft(), imageDimension.getRight());
        mediaRecordRepository.save(newRecord);
        File f = new File(String.join("/", staticFileRoot, site, "media", fileName));
        logger.info("Writing file " + f.getAbsoluteFile());
        try (FileOutputStream fos = new FileOutputStream(f)) {
            IOUtils.copy(mfile.getInputStream(), fos);
        }
    }

    public List<MediaRecord> getAllFiles(String host, String userName) {
        String site = siteService.getSiteForHostname(host);
        return mediaRecordRepository.findAllBySiteOrderByFileName(site);
        // If we have ACL, filter by user permissions
    }

    @Transactional
    public void deleteFile(String host, String fileName, String user) throws IOException {
        String site = siteService.getSiteForHostname(host);
        ensureDir(site);
        File f = new File(String.join("/", staticFileRoot, site, "media", fileName));
        logger.info("Deleting file " + f.getAbsoluteFile());
        mediaRecordRepository.deleteBySiteAndFilename(site, fileName);
        Files.delete(f.toPath());
    }
}
