package us.calubrecht.lazerwiki.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import us.calubrecht.lazerwiki.model.*;
import us.calubrecht.lazerwiki.repository.EntityManagerProxy;
import us.calubrecht.lazerwiki.repository.MediaRecordRepository;
import us.calubrecht.lazerwiki.repository.NamespaceRepository;
import us.calubrecht.lazerwiki.repository.PageRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NamespaceService {
    @Autowired
    NamespaceRepository namespaceRepository;

    @Autowired
    PageRepository pageRepository;

    @Autowired
    MediaRecordRepository mediaRecordRepository;

    @Autowired
    UserService userService;

    final Logger logger = LogManager.getLogger(getClass());

    public String parentNamespace(String namespace) {
        if (!namespace.contains(":"))
        {
            return namespace.isBlank() ? null : "";
        }
        return namespace.substring(0, namespace.lastIndexOf(":"));
    }

    public boolean canReadNamespace(String site, String namespace, String userName) {
        Namespace nsObj = namespaceRepository.findBySiteAndNamespace(site, namespace);
        if (nsObj == null ) {
            String parentNS = parentNamespace(namespace);
            return parentNS == null || canReadNamespace(site, parentNS, userName);
        }

        Set<Namespace.RESTRICTION_TYPE> readable = Set.of(Namespace.RESTRICTION_TYPE.OPEN, Namespace.RESTRICTION_TYPE.WRITE_RESTRICTED);
        if (readable.contains(nsObj.restriction_type)) {
            return true;
        }

        if (User.isGuest(userName)) {
            return false;
        }

        User user = userService.getUser(userName);
        List<String> roles = user.roles.stream().map(role -> role.role).toList();
        List<String> necessaryRoles = List.of("ROLE_ADMIN", "ROLE_ADMIN:" + site, "ROLE_WRITE:" + site + ":" + namespace, "ROLE_READ:" + site + ":" + namespace);
        List<String> intersection = new ArrayList<>(roles);
        intersection.retainAll(necessaryRoles);

        return !intersection.isEmpty();
    }

    public boolean canWriteNamespace(String site, String namespace, String userName) {
        if (User.isGuest(userName)) {
            return false;
        }
        Namespace nsObj = namespaceRepository.findBySiteAndNamespace(site, namespace);
        if (nsObj == null ) {
            String parentNS = parentNamespace(namespace);
            return parentNS == null || canWriteNamespace(site, parentNS, userName);
        }

        if (nsObj.restriction_type == Namespace.RESTRICTION_TYPE.OPEN) {
            return true;
        }

        User user = userService.getUser(userName);
        List<String> roles = user.roles.stream().map(role -> role.role).toList();
        List<String> necessaryRoles = List.of("ROLE_ADMIN", "ROLE_ADMIN:" + site, "ROLE_WRITE:" + site + ":" + namespace);
        List<String> intersection = new ArrayList<>(roles);
        intersection.retainAll(necessaryRoles);

        return !intersection.isEmpty();
    }

    public boolean canDeleteInNamespace(String site, String namespace, String userName) {
        if (User.isGuest(userName)) {
            return false;
        }
        User user = userService.getUser(userName);
        List<String> roles = user.roles.stream().map(role -> role.role).toList();
        List<String> necessaryRoles = List.of("ROLE_ADMIN", "ROLE_ADMIN:" + site, "ROLE_DELETE:" + site);
        List<String> intersection = new ArrayList<>(roles);
        intersection.retainAll(necessaryRoles);

        return !intersection.isEmpty() && canWriteNamespace(site, namespace, userName);
    }

    public boolean canUploadInNamespace(String site, String namespace, String userName) {
        if (User.isGuest(userName)) {
            return false;
        }
        User user = userService.getUser(userName);
        List<String> roles = user.roles.stream().map(role -> role.role).toList();
        List<String> necessaryRoles = List.of("ROLE_ADMIN", "ROLE_ADMIN:" + site, "ROLE_UPLOAD:" + site);
        List<String> intersection = new ArrayList<>(roles);
        intersection.retainAll(necessaryRoles);

        return !intersection.isEmpty() && canWriteNamespace(site, namespace, userName);
    }

    public List<PageDesc>  filterReadablePages(List<PageDesc> allValid, String site, String userName) {
        Set<String> unreadableNamespaces = allValid.stream().map(PageDesc::getNamespace).distinct().
                filter(ns -> !canReadNamespace(site, ns, userName)).collect(Collectors.toSet());
        return allValid.stream().filter(p -> !unreadableNamespaces.contains(p.getNamespace())).toList();
    }

    public List<PageDescriptor>  filterReadablePageDescriptors(List<PageDescriptor> allValid, String site, String userName) {
        Set<String> unreadableNamespaces = allValid.stream().map(PageDescriptor::namespace).distinct().
                filter(ns -> !canReadNamespace(site, ns, userName)).collect(Collectors.toSet());
        return allValid.stream().filter(p -> !unreadableNamespaces.contains(p.namespace())).toList();
    }

    public List<MediaRecord> filterReadableMedia(List<MediaRecord> allValid, String site, String userName) {
        var unreadableNamespaces = allValid.stream().map(MediaRecord::getNamespace).distinct().
                filter(ns -> !canReadNamespace(site, ns, userName)).collect(Collectors.toSet());
        return allValid.stream().filter(m -> !unreadableNamespaces.contains(m.getNamespace())).toList();
    }

    public List<String> getReadableNamespaces(String site, String userName) {
        List<String> allPageNS = pageRepository.getAllNamespaces(site);
        List<String> allMediaNS = mediaRecordRepository.getAllNamespaces(site);
        Set<String> allNS = new LinkedHashSet<>(allPageNS);
        allNS.addAll(allMediaNS);
        return allNS.stream().filter(ns -> canReadNamespace(site, ns, userName)).toList();
    }

    public Namespace.RESTRICTION_TYPE getNSRestriction(String site, String namespace) {
        Namespace nsObj = namespaceRepository.findBySiteAndNamespace(site, namespace);
        if (nsObj == null) {
            return Namespace.RESTRICTION_TYPE.OPEN;
        }
        return nsObj.restriction_type;
    }

    @Transactional
    @CacheEvict(value = "FindBySiteAndNamespace", allEntries = true)
    public void setNSRestriction(String site, String namespace, Namespace.RESTRICTION_TYPE restrictionType) {
        Namespace nsObj = namespaceRepository.findBySiteAndNamespace(site, namespace);
        if (nsObj == null) {
            nsObj = new Namespace();
            nsObj.site = site;
            nsObj.namespace = namespace;
        }
        nsObj.restriction_type = restrictionType;
        namespaceRepository.save(nsObj);
    }

    public String joinNS(String rootNS, String newNS) {
        if (rootNS.isEmpty()) {
            return newNS;
        }
        return rootNS + ":" + newNS;
    }
}
