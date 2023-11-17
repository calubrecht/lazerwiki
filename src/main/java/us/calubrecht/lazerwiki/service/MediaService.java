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

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
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

    Pair<Integer, Integer> getImageDimension(InputStream is) throws IOException {
        try (ImageInputStream in = ImageIO.createImageInputStream(is)) {
            final Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    reader.setInput(in);
                    return Pair.of(reader.getWidth(0), reader.getHeight(0));
                } finally {
                    reader.dispose();
                }
            }
        }
        return Pair.of(0,0);
    }

    @Transactional
    public void saveFile(String host, String userName, MultipartFile mfile) throws IOException {
        String site = siteService.getSiteForHostname(host);
        ensureDir(site);
        String fileName = mfile.getOriginalFilename();
        byte[] fileBytes = mfile.getBytes();
        ByteArrayInputStream bis = new ByteArrayInputStream(fileBytes);
        Pair<Integer, Integer> imageDimension = getImageDimension(bis);
        MediaRecord newRecord = new MediaRecord(fileName, site, userName, mfile.getSize(), imageDimension.getLeft(), imageDimension.getRight());
        mediaRecordRepository.save(newRecord);
        File f = new File(String.join("/", staticFileRoot, site, "media", fileName));
        logger.info("Writing file " + f.getAbsoluteFile());
        FileOutputStream fos = new FileOutputStream(f);
        IOUtils.copy(mfile.getInputStream(), fos);
    }

    public List<MediaRecord> getAllFiles(String host, String userName) {
        String site = siteService.getSiteForHostname(host);
        return mediaRecordRepository.findAllBySiteOrderByFileName(site);
        // If we have ACL, filter by user permissions
    }
}
