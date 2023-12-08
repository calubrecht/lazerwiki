package us.calubrecht.lazerwiki.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import us.calubrecht.lazerwiki.model.MediaRecord;
import us.calubrecht.lazerwiki.model.Namespace;
import us.calubrecht.lazerwiki.model.PageDesc;
import us.calubrecht.lazerwiki.model.User;
import us.calubrecht.lazerwiki.repository.NamespaceRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NamespaceService {
    @Autowired
    NamespaceRepository namespaceRepository;

    @Autowired
    UserService userService;

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

    public List<MediaRecord> filterReadableMedia(List<MediaRecord> allValid, String site, String userName) {
        var unreadableNamespaces = allValid.stream().map(MediaRecord::getNamespace).distinct().
                filter(ns -> !canReadNamespace(site, ns, userName)).collect(Collectors.toSet());
        return allValid.stream().filter(m -> !unreadableNamespaces.contains(m.getNamespace())).toList();
    }
}
