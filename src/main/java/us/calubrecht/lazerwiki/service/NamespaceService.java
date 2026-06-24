package us.calubrecht.lazerwiki.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import us.calubrecht.lazerwiki.model.*;
import us.calubrecht.lazerwiki.repository.MediaRecordRepository;
import us.calubrecht.lazerwiki.repository.NamespaceRepository;
import us.calubrecht.lazerwiki.repository.PageRepository;

@Service
public class NamespaceService {
  @Autowired NamespaceRepository namespaceRepository;

  @Autowired PageRepository pageRepository;

  @Autowired MediaRecordRepository mediaRecordRepository;

  @Autowired UserService userService;

  final Logger logger = LogManager.getLogger(getClass());

  public String parentNamespace(String namespace) {
    if (!namespace.contains(":")) {
      return namespace.isBlank() ? null : "";
    }
    return namespace.substring(0, namespace.lastIndexOf(":"));
  }

  public boolean canReadNamespace(String site, String namespace, String userName) {
    Namespace nsObj = namespaceRepository.findBySiteAndNamespace(site, namespace);
    if (nsObj == null) {
      String parentNS = parentNamespace(namespace);
      return parentNS == null || canReadNamespace(site, parentNS, userName);
    }

    Set<Namespace.RestrictionType> readable =
        Set.of(
            Namespace.RestrictionType.OPEN,
            Namespace.RestrictionType.WRITE_RESTRICTED,
            Namespace.RestrictionType.GUEST_WRITABLE);
    if (readable.contains(nsObj.restrictionType)) {
      return true;
    }

    if (User.isGuest(userName)) {
      return false;
    }

    User user = userService.getUser(userName);
    List<String> roles = user.roles.stream().map(role -> role.role).toList();
    List<String> necessaryRoles =
        List.of(
            "ROLE_ADMIN",
            "ROLE_ADMIN:" + site,
            "ROLE_WRITE:" + site + ":" + namespace,
            "ROLE_READ:" + site + ":" + namespace);
    List<String> intersection = new ArrayList<>(roles);
    intersection.retainAll(necessaryRoles);

    return !intersection.isEmpty();
  }

  public boolean canWriteNamespace(String site, String namespace, String userName) {
    Namespace nsObj = namespaceRepository.findBySiteAndNamespace(site, namespace);
    if (nsObj == null) {
      String parentNS = parentNamespace(namespace);
      if (parentNS == null) {
        return !User.isGuest(userName);
      }
      return canWriteNamespace(site, parentNS, userName);
    }
    if (User.isGuest(userName)) {
      return nsObj.restrictionType == Namespace.RestrictionType.GUEST_WRITABLE;
    }

    if (nsObj.restrictionType == Namespace.RestrictionType.OPEN) {
      return true;
    }

    User user = userService.getUser(userName);
    List<String> roles = user.roles.stream().map(role -> role.role).toList();
    List<String> necessaryRoles =
        List.of("ROLE_ADMIN", "ROLE_ADMIN:" + site, "ROLE_WRITE:" + site + ":" + namespace);
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
    List<String> necessaryRoles =
        List.of(
            "ROLE_ADMIN",
            "ROLE_ADMIN:" + site,
            "ROLE_DELETE:" + site,
            "ROLE_DELETE:" + site + ":" + namespace);
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
    List<String> necessaryRoles =
        List.of(
            "ROLE_ADMIN",
            "ROLE_ADMIN:" + site,
            "ROLE_UPLOAD:" + site,
            "ROLE_UPLOAD:" + site + ":" + namespace);
    List<String> intersection = new ArrayList<>(roles);
    intersection.retainAll(necessaryRoles);

    return !intersection.isEmpty() && canWriteNamespace(site, namespace, userName);
  }

  public List<PageDesc> filterReadablePages(List<PageDesc> allValid, String site, String userName) {
    Set<String> unreadableNamespaces =
        allValid.stream()
            .map(PageDesc::getNamespace)
            .distinct()
            .filter(ns -> !canReadNamespace(site, ns, userName))
            .collect(Collectors.toSet());
    return allValid.stream().filter(p -> !unreadableNamespaces.contains(p.getNamespace())).toList();
  }

  public List<PageDescriptor> filterReadablePageDescriptors(
      List<PageDescriptor> allValid, String site, String userName) {
    Set<String> unreadableNamespaces =
        allValid.stream()
            .map(PageDescriptor::namespace)
            .distinct()
            .filter(ns -> !canReadNamespace(site, ns, userName))
            .collect(Collectors.toSet());
    return allValid.stream().filter(p -> !unreadableNamespaces.contains(p.namespace())).toList();
  }

  public List<MediaRecord> filterReadableMedia(
      List<MediaRecord> allValid, String site, String userName) {
    var unreadableNamespaces =
        allValid.stream()
            .map(MediaRecord::getNamespace)
            .distinct()
            .filter(ns -> !canReadNamespace(site, ns, userName))
            .collect(Collectors.toSet());
    return allValid.stream().filter(m -> !unreadableNamespaces.contains(m.getNamespace())).toList();
  }

  public List<String> getReadableNamespaces(String site, String userName) {
    List<String> allPageNS = pageRepository.getAllNamespaces(site);
    List<String> allMediaNS = mediaRecordRepository.getAllNamespaces(site);
    Set<String> allNS = new LinkedHashSet<>(allPageNS);
    allNS.addAll(allMediaNS);
    return allNS.stream().filter(ns -> canReadNamespace(site, ns, userName)).toList();
  }

  public Namespace.RestrictionType getNSRestriction(String site, String namespace) {
    Namespace nsObj = namespaceRepository.findBySiteAndNamespace(site, namespace);
    if (nsObj == null) {
      return Namespace.RestrictionType.INHERIT;
    }
    return nsObj.restrictionType;
  }

  @Transactional
  @CacheEvict(value = "FindBySiteAndNamespace", allEntries = true)
  public void setNSRestriction(
      String site, String namespace, Namespace.RestrictionType restrictionType) {
    Namespace nsObj = namespaceRepository.findBySiteAndNamespace(site, namespace);
    if (restrictionType == Namespace.RestrictionType.INHERIT) {
      if (nsObj != null) {
        namespaceRepository.delete(nsObj);
      }
      return;
    }
    if (nsObj == null) {
      nsObj = new Namespace();
      nsObj.site = site;
      nsObj.namespace = namespace;
    }
    nsObj.restrictionType = restrictionType;
    namespaceRepository.save(nsObj);
  }

  public String joinNS(String rootNS, String newNS) {
    if (rootNS.isEmpty()) {
      return newNS;
    }
    return rootNS + ":" + newNS;
  }
}
