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

    String parentNamespace(String namespace) {
        if (!namespace.contains(":"))
        {
            return null;
        }
        return namespace.substring(0, namespace.lastIndexOf(":"));
    }

    public boolean canReadNamespace(String site, String namespace, String userName) {
        Namespace nsObj = namespaceRepository.findBySiteAndNamespace(site, namespace);
        if (nsObj == null ) {
            String parentNS = parentNamespace(namespace);
            return parentNS == null ? true : canReadNamespace(site, parentNS, userName);
        }

        Set<Namespace.RESTRICTION_TYPE> readable = Set.of(Namespace.RESTRICTION_TYPE.OPEN, Namespace.RESTRICTION_TYPE.WRITE_RESTRICTED);
        if (readable.contains(nsObj.restriction_type)) {
            return true;
        }

        if (User.isGuest(userName)) {
            return false;
        }

        User user = userService.getUser(userName);
        List<String> roles = user.roles.stream().map(role -> role.role).collect(Collectors.toList());
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
            return parentNS == null ? true : canWriteNamespace(site, parentNS, userName);
        }

        if (nsObj.restriction_type == Namespace.RESTRICTION_TYPE.OPEN) {
            return true;
        }

        User user = userService.getUser(userName);
        List<String> roles = user.roles.stream().map(role -> role.role).collect(Collectors.toList());
        List<String> necessaryRoles = List.of("ROLE_ADMIN", "ROLE_ADMIN:" + site, "ROLE_WRITE:" + site + ":" + namespace);
        List<String> intersection = new ArrayList<>(roles);
        intersection.retainAll(necessaryRoles);

        return !intersection.isEmpty();
    }

    public List<PageDesc>  filterReadablePages(List<PageDesc> allValid, String site, String userName) {
        Set<String> unreadableNamespaces = allValid.stream().map(p -> p.getNamespace()).distinct().
                filter(ns -> !canReadNamespace(site, ns, userName)).collect(Collectors.toSet());
        return allValid.stream().filter(p -> !unreadableNamespaces.contains(p.getNamespace())).toList();
    }

    public List<MediaRecord> filterReadableMedia(List<MediaRecord> allValid, String site, String userName) {
        List vv2 = allValid;
        Set<String> unreadableNamespaces = allValid.stream().map(m -> m.getNamespace()).distinct().
                filter(ns -> !canReadNamespace(site, ns, userName)).collect(Collectors.toSet());
        return allValid.stream().filter(m -> !unreadableNamespaces.contains(m.getNamespace())).toList();
    }
}
