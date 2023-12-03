package us.calubrecht.lazerwiki.service;

import jakarta.transaction.Transactional;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import us.calubrecht.lazerwiki.model.*;
import us.calubrecht.lazerwiki.repository.LinkRepository;
import us.calubrecht.lazerwiki.repository.PageRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class LinkService {
    @Autowired
    LinkRepository linkRepository;

    public PageDescriptor decodeDescriptor(String pageDescriptor) {
        List<String> tokens = new ArrayList<>(Arrays.asList(pageDescriptor.split(":")));
        String pageName = tokens.remove(tokens.size() -1);
        return new PageDescriptor(String.join(":", tokens), pageName);
    }

    @Transactional
    public void setLinksFromPage(String site, String pageNS, String pageName, Collection<String> targets) {
        linkRepository.deleteBySiteAndSourcePageNSAndSourcePageName(site, pageNS, pageName);
        List<Link> links = targets.stream().map(t -> {
            PageDescriptor pd = decodeDescriptor(t);
            return new Link(site, pageNS, pageName, pd.namespace(), pd.pageName());
        }).collect(Collectors.toList());
        linkRepository.saveAll(links);
    }
}
