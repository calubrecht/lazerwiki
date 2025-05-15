package us.calubrecht.lazerwiki.service;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import us.calubrecht.lazerwiki.model.ActivityType;
import us.calubrecht.lazerwiki.model.MediaHistoryRecord;
import us.calubrecht.lazerwiki.model.User;
import us.calubrecht.lazerwiki.repository.MediaHistoryRepository;
import us.calubrecht.lazerwiki.responses.MediaListResponse;
import us.calubrecht.lazerwiki.model.MediaRecord;
import us.calubrecht.lazerwiki.responses.NsNode;
import us.calubrecht.lazerwiki.repository.MediaRecordRepository;
import us.calubrecht.lazerwiki.service.exception.MediaReadException;
import us.calubrecht.lazerwiki.service.exception.MediaWriteException;
import us.calubrecht.lazerwiki.util.IOSupplier;
import us.calubrecht.lazerwiki.util.ImageUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MediaService {
    final Logger logger = LogManager.getLogger(getClass());
    @Autowired
    SiteService siteService;

    @Autowired
    MediaRecordRepository mediaRecordRepository;

    @Autowired
    NamespaceService namespaceService;

    @Autowired
    MediaCacheService mediaCacheService;

    @Autowired
    MediaHistoryRepository mediaHistoryRepository;

    @Autowired
    UserService userService;

    @Autowired
    ActivityLogService activityLogService;

    @Value("${lazerwiki.static.file.root}")
    String staticFileRoot;

    @Autowired
    ImageUtil imageUtil;

    void ensureDir(String site) throws IOException {
        Files.createDirectories(Paths.get(String.join("/", staticFileRoot, site, "media")));
    }

    void ensureDir(String site, String nsPath) throws IOException {
        if (nsPath.isBlank()) {
            ensureDir(site);
            return;
        }
        Files.createDirectories(Paths.get(String.join("/", staticFileRoot, site, "media", nsPath)));
    }

    Pair<String, String> getNamespace(String fileName) {
        List<String> parts = new ArrayList<>(List.of(fileName.split(":")));
        String baseFile = parts.remove(parts.size() -1);
        if (parts.size() == 0) {
            return Pair.of("", baseFile);
        }
        return Pair.of(String.join(":", parts), baseFile);

    }

    boolean sizeMismatch(MediaRecord record, int width, int height) {
        if (width != 0 && record.getWidth() != width) {
            return true;
        }
        return height != 0 && record.getHeight() != height;
    }


    public byte[] getBinaryFile(String host, String userName, String fileName, String size) throws IOException, MediaReadException {
        String site = siteService.getSiteForHostname(host);
        Pair<String, String> splitFile = getNamespace(fileName);
        if (!namespaceService.canReadNamespace(site, splitFile.getLeft(), userName)) {
            throw new MediaReadException("Not permissioned to read this file");
        }
        IOSupplier<byte[]> byteReader = () -> {
                String nsPath = splitFile.getLeft().replaceAll(":", "/");
                ensureDir(site, nsPath);
                File f = nsPath.isBlank() ?
                        new File(String.join("/", staticFileRoot, site, "media", splitFile.getRight())) :
                        new File(String.join("/", staticFileRoot, site, "media", nsPath, splitFile.getRight()));
                logger.info("Reading file " + f.getAbsoluteFile());
                return Files.readAllBytes(f.toPath());
            };
        if (size != null) {
            MediaRecord record = mediaRecordRepository.findBySiteAndNamespaceAndFileName(site, splitFile.getLeft(), splitFile.getRight());
            if (record == null) {
                return byteReader.get();
            }
            String[] dimensions = size.split("x");

            int width = Integer.parseInt(dimensions[0]);
            int height = dimensions.length > 1 ? Integer.parseInt(dimensions[1]) : 0;
            if (sizeMismatch(record, width, height)) {
                return mediaCacheService.getBinaryFile(site, record, byteReader, width, height);
            }
        }
        return byteReader.get();
    }

    @Transactional
    public void saveFile(String host, String userName, MultipartFile mfile, String namespace) throws IOException, MediaWriteException {
        String site = siteService.getSiteForHostname(host);
        if (!namespaceService.canUploadInNamespace(site, namespace, userName)) {
            throw new MediaWriteException("Not permissioned to write this file");
        }
        String nsPath = namespace.replaceAll(":", "/");
        ensureDir(site, nsPath);
        String fileName = mfile.getOriginalFilename();
        MediaRecord oldRecord = mediaRecordRepository.findBySiteAndNamespaceAndFileName(site, namespace, fileName);
        Long id = null;
        ActivityType action = ActivityType.ACTIVITY_PROTO_UPLOAD_MEDIA;
        if (oldRecord != null){
            if (!namespaceService.canDeleteInNamespace(site, namespace, userName)) {
                throw new MediaWriteException("Not permissioned to overwrite existing file");
            }
            id = oldRecord.getId();
            action = ActivityType.ACTIVITY_PROTO_REPLACE_MEDIA;
        }
        byte[] fileBytes = mfile.getBytes();
        ByteArrayInputStream bis = new ByteArrayInputStream(fileBytes);
        Pair<Integer, Integer> imageDimension = imageUtil.getImageDimension(bis);
        User user = userService.getUser(userName);
        MediaRecord newRecord = new MediaRecord(fileName, site, namespace, user, mfile.getSize(), imageDimension.getLeft(), imageDimension.getRight());
        newRecord.setId(id);
        mediaRecordRepository.save(newRecord);
        MediaHistoryRecord historyRecord = new MediaHistoryRecord(fileName, site, namespace, user, action);
        mediaHistoryRepository.save(historyRecord);
        activityLogService.log(action, user, namespaceService.joinNS(namespace, fileName));
        mediaCacheService.clearCache(site, newRecord);
        File f = nsPath.isBlank() ?
                new File(String.join("/", staticFileRoot, site, "media", fileName)):
                new File(String.join("/", staticFileRoot, site, "media", nsPath, fileName));
        logger.info("Writing file " + f.getAbsoluteFile());
        try (FileOutputStream fos = new FileOutputStream(f)) {
            IOUtils.copy(mfile.getInputStream(), fos);
        }
        // XXX: Delete scaled images if exist
    }

    List<String> getNamespaces(String rootNS, List<MediaRecord> mediaRecords) {
        return mediaRecords.stream().map(p -> p.getNamespace()).distinct().flatMap(ns -> {
                    List<String> parts = List.of(ns.split(":"));
                    List<String> namespaces = new ArrayList<>();
                    if (parts.size() == 1) {
                        return List.of(ns).stream();
                    }
                    for ( int i = 0 ; i <= parts.size(); i++) {
                        String namespace = parts.subList(0, i).stream().collect(Collectors.joining(":"));
                        namespaces.add(namespace);
                    }
                    return namespaces.stream();
                }).distinct().filter(ns -> ns.startsWith(rootNS) && !ns.equals(rootNS)).
                filter(ns -> ns.substring(rootNS.length() + 1).indexOf(":") == -1).
                sorted().toList();
    }
    NsNode getNsNode(String site, String rootNS, List<MediaRecord> mediaRecords, String userName) {
        List<String> namespaces = getNamespaces(rootNS, mediaRecords);
        List<NsNode> nodes = new ArrayList<>();
        namespaces.forEach(ns ->
                nodes.add(getNsNode(site, ns, mediaRecords, userName)));
        NsNode node = new NsNode(rootNS, namespaceService.canUploadInNamespace(site, rootNS, userName));
        node.setChildren(nodes);
        return node;

    }

    public MediaListResponse getAllFiles(String host, String userName) {
        String site = siteService.getSiteForHostname(host);
        List<MediaRecord> mediaRecords= namespaceService.filterReadableMedia(mediaRecordRepository.findAllBySiteOrderByFileName(site), site, userName);
        NsNode namespaces = getNsNode(site,"", mediaRecords, userName);


        return new MediaListResponse(mediaRecords.stream().collect(Collectors.groupingBy(MediaRecord::getNamespace)), namespaces);
        // If we have ACL, filter by user permissions
    }

    @Transactional
    public void deleteFile(String host, String fileName, String userName) throws IOException, MediaWriteException {
        String site = siteService.getSiteForHostname(host);
        Pair<String, String> splitFile = getNamespace(fileName);
        String nsPath = splitFile.getLeft().replaceAll(":", "/");
        if (!namespaceService.canDeleteInNamespace(site, splitFile.getLeft(), userName)) {
            throw new MediaWriteException("Not permissioned to delete this file");
        }

        ensureDir(site, nsPath);
        File f = nsPath.isBlank() ?
                new File(String.join("/", staticFileRoot, site, "media", splitFile.getRight())):
                new File(String.join("/", staticFileRoot, site, "media", nsPath, splitFile.getRight()));
        logger.info("Deleting file " + f.getAbsoluteFile());
        User user = userService.getUser(userName);
        mediaRecordRepository.deleteBySiteAndFilenameAndNamespace(site, splitFile.getRight(), splitFile.getLeft());
        MediaHistoryRecord historyRecord = new MediaHistoryRecord(fileName, site, splitFile.getLeft(), user,  ActivityType.ACTIVITY_PROTO_DELETE_MEDIA);
        mediaHistoryRepository.save(historyRecord);
        activityLogService.log(ActivityType.ACTIVITY_PROTO_DELETE_MEDIA, user, fileName);
        Files.delete(f.toPath());
        // XXX: Delete scaled images if exist
    }

    public long getFileLastModified(String host, String fileName) throws IOException {
        String site = siteService.getSiteForHostname(host);
        Pair<String, String> splitFile = getNamespace(fileName);
        String nsPath = splitFile.getLeft().replaceAll(":", "/");
        ensureDir(site, nsPath);
        File f = nsPath.isBlank() ?
                new File(String.join("/", staticFileRoot, site, "media", splitFile.getRight())) :
                new File(String.join("/", staticFileRoot, site, "media", nsPath, splitFile.getRight()));
        return f.lastModified();
    }

    public List<MediaHistoryRecord> getRecentChanges(String host, String user) {
        String site = siteService.getSiteForHostname(host);
        List<String> namespaces = namespaceService.getReadableNamespaces(site, user);
        return mediaHistoryRepository.findAllBySiteAndNamespaceInOrderByTsDesc(Limit.of(10), site, namespaces);
    }
}
