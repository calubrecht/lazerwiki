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
import us.calubrecht.lazerwiki.responses.MediaListResponse;
import us.calubrecht.lazerwiki.model.MediaRecord;
import us.calubrecht.lazerwiki.responses.NsNode;
import us.calubrecht.lazerwiki.repository.MediaRecordRepository;
import us.calubrecht.lazerwiki.service.exception.MediaReadException;
import us.calubrecht.lazerwiki.service.exception.MediaWriteException;
import us.calubrecht.lazerwiki.util.ImageUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MediaService {
    Logger logger = LogManager.getLogger(getClass());
    @Autowired
    SiteService siteService;

    @Autowired
    MediaRecordRepository mediaRecordRepository;

    @Autowired
    NamespaceService namespaceService;

    @Value("${lazerwiki.static.file.root}")
    String staticFileRoot;

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

    public byte[] getBinaryFile(String host, String userName, String fileName) throws IOException, MediaReadException {
        String site = siteService.getSiteForHostname(host);
        Pair<String, String> splitFile = getNamespace(fileName);
        if (!namespaceService.canReadNamespace(site, splitFile.getLeft(), userName)) {
            throw new MediaReadException("Not permissioned to read this file");
        }
        String nsPath = splitFile.getLeft().replaceAll(":", "/");
        ensureDir(site, nsPath);
        File f = nsPath.isBlank() ?
                new File(String.join("/", staticFileRoot, site, "media", splitFile.getRight())):
                new File(String.join("/", staticFileRoot, site, "media", nsPath, splitFile.getRight()));
        logger.info("Reading file " + f.getAbsoluteFile());
        return Files.readAllBytes(f.toPath());
    }

    @Transactional
    public void saveFile(String host, String userName, MultipartFile mfile, String namespace) throws IOException, MediaWriteException {
        String site = siteService.getSiteForHostname(host);
        if (!namespaceService.canWriteNamespace(site, namespace, userName)) {
            throw new MediaWriteException("Not permissioned to write this file");
        }
        String nsPath = namespace.replaceAll(":", "/");
        ensureDir(site, nsPath);
        String fileName = mfile.getOriginalFilename();
        byte[] fileBytes = mfile.getBytes();
        ByteArrayInputStream bis = new ByteArrayInputStream(fileBytes);
        Pair<Integer, Integer> imageDimension = ImageUtil.getImageDimension(bis);
        MediaRecord newRecord = new MediaRecord(fileName, site, namespace, userName, mfile.getSize(), imageDimension.getLeft(), imageDimension.getRight());
        mediaRecordRepository.save(newRecord);
        File f = nsPath.isBlank() ?
                new File(String.join("/", staticFileRoot, site, "media", fileName)):
                new File(String.join("/", staticFileRoot, site, "media", nsPath, fileName));
        logger.info("Writing file " + f.getAbsoluteFile());
        try (FileOutputStream fos = new FileOutputStream(f)) {
            IOUtils.copy(mfile.getInputStream(), fos);
        }
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
        List<NsNode> nodes = new ArrayList();
        namespaces.forEach(ns ->
                nodes.add(getNsNode(site, ns, mediaRecords, userName)));
        NsNode node = new NsNode(rootNS, namespaceService.canWriteNamespace(site, rootNS, userName));
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
    public void deleteFile(String host, String fileName, String user) throws IOException, MediaWriteException {
        String site = siteService.getSiteForHostname(host);
        Pair<String, String> splitFile = getNamespace(fileName);
        String nsPath = splitFile.getLeft().replaceAll(":", "/");
        if (!namespaceService.canWriteNamespace(site, splitFile.getLeft(), user)) {
            throw new MediaWriteException("Not permissioned to delete this file");
        }
        ensureDir(site, nsPath);
        File f = nsPath.isBlank() ?
                new File(String.join("/", staticFileRoot, site, "media", splitFile.getRight())):
                new File(String.join("/", staticFileRoot, site, "media", nsPath, splitFile.getRight()));
        logger.info("Deleting file " + f.getAbsoluteFile());
        mediaRecordRepository.deleteBySiteAndFilenameAndNamespace(site, splitFile.getRight(), splitFile.getLeft());
        Files.delete(f.toPath());
    }
}
