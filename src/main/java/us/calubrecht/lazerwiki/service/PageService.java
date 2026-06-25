package us.calubrecht.lazerwiki.service;

import static us.calubrecht.lazerwiki.model.RenderResult.RenderStateKeys.OVERRIDE_STATS;

import com.github.difflib.text.DiffRow;
import com.github.difflib.text.DiffRowGenerator;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import us.calubrecht.lazerwiki.model.*;
import us.calubrecht.lazerwiki.repository.*;
import us.calubrecht.lazerwiki.responses.*;
import us.calubrecht.lazerwiki.responses.PageData.PageFlags;
import us.calubrecht.lazerwiki.service.exception.PageReadException;
import us.calubrecht.lazerwiki.service.exception.PageWriteException;
import us.calubrecht.lazerwiki.util.DbSupport;

@Service
@Transactional(rollbackFor = PageWriteException.class)
public class PageService {
  final Logger logger = LogManager.getLogger(getClass());

  @Autowired PageRepository pageRepository;

  @Autowired SiteService siteService;

  @Autowired NamespaceService namespaceService;

  @Autowired LinkService linkService;

  @Autowired TagRepository tagRepository;

  @Autowired PageCacheRepository pageCacheRepository;

  @Autowired LinkOverrideService linkOverrideService;

  @Value("${lazerwiki.db.engine:mysql}")
  String dbEngine;

  @Transactional(readOnly = true)
  public boolean exists(String host, String pageName) {
    String site = siteService.getSiteForHostname(host);
    PageDescriptor pageDescriptor = decodeDescriptor(pageName);
    Page p =
        pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted(
            site, pageDescriptor.namespace(), pageDescriptor.pageName(), false);
    return p != null;
  }

  @Transactional(readOnly = true)
  public String getTitle(String host, String pageName) {
    String site = siteService.getSiteForHostname(host);
    PageDescriptor pageDescriptor = decodeDescriptor(pageName);
    Page p =
        pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted(
            site, pageDescriptor.namespace(), pageDescriptor.pageName(), false);
    return getTitle(pageDescriptor, p);
  }

  public static String getTitle(PageDescriptor pd, Page p) {
    return p == null
        ? pd.renderedName()
        : (p.getTitle() == null ? pd.renderedName() : p.getTitle());
  }

  @Transactional(readOnly = true)
  public PageData getPageData(String host, String sPageDescriptor, String userName) {
    logger.info(
        "fetch page: host="
            + host
            + " sPageDescriptor="
            + sPageDescriptor
            + " userName="
            + userName);
    String site = siteService.getSiteForHostname(host);
    PageDescriptor pageDescriptor = decodeDescriptor(sPageDescriptor);
    boolean canWrite =
        namespaceService.canWriteNamespace(site, pageDescriptor.namespace(), userName);
    boolean canRead = namespaceService.canReadNamespace(site, pageDescriptor.namespace(), userName);
    boolean canDelete =
        namespaceService.canDeleteInNamespace(site, pageDescriptor.namespace(), userName)
            && !pageDescriptor.isHome();
    if (!canRead) {
      return new PageData(
          "You are not permissioned to read this page",
          "",
          getTitle(host, sPageDescriptor),
          Collections.emptyList(),
          Collections.emptyList(),
          PageData.EMPTY_FLAGS);
    }
    Page p =
        pageRepository.getBySiteAndNamespaceAndPagename(
            site, pageDescriptor.namespace(), pageDescriptor.pageName());
    List<String> backlinks = linkService.getBacklinks(site, sPageDescriptor);
    List<String> overrideBacklinks =
        linkOverrideService.getOverridesForNewTargetPage(host, sPageDescriptor).stream()
            .map(LinkOverride::getSource)
            .toList();
    List<String> allBackLnks =
        Stream.concat(backlinks.stream(), overrideBacklinks.stream()).distinct().toList();
    List<String> visibleBacklinks =
        namespaceService
            .filterReadablePageDescriptors(
                allBackLnks.stream().map(PageDescriptor::fromFullName).toList(), site, userName)
            .stream()
            .map(PageDescriptor::toString)
            .toList();

    if (p == null) {
      // Add support for namespace level templates. Need templating language for
      // pageName/namespace/splitPageName
      return new PageData(
          "This page doesn't exist",
          getTemplate(site, pageDescriptor),
          getTitle(host, sPageDescriptor),
          Collections.emptyList(),
          visibleBacklinks,
          new PageFlags(false, false, true, canWrite, false, false));
    }
    if (p.isDeleted()) {
      List<LinkOverride> overrideInstances =
          linkOverrideService.getOverridesForTargetPage(host, sPageDescriptor);
      if (!overrideInstances.isEmpty()) {
        String newPD = overrideInstances.get(0).getNewTarget();
        return new PageData(
            null,
            "This page has been moved to [[" + newPD + "]] (" + newPD + ")",
            sPageDescriptor,
            Collections.emptyList(),
            visibleBacklinks,
            new PageFlags(false, true, true, false, false, true));
      }
      return new PageData(
          "This page doesn't exist",
          getTemplate(site, pageDescriptor),
          getTitle(host, sPageDescriptor),
          Collections.emptyList(),
          visibleBacklinks,
          new PageFlags(false, true, true, canWrite, false, false));
    }
    String source = p.getText();
    return new PageData(
        null,
        source,
        getTitle(pageDescriptor, p),
        p.getTags().stream().map(PageTag::getTag).toList(),
        visibleBacklinks,
        new PageFlags(true, false, true, canWrite, canDelete, false),
        p.getId(),
        p.getRevision());
  }

  /** Bulk page get, does not retrieve backlinks or tags */
  @Transactional(readOnly = true)
  public Map<PageDescriptor, PageData> getPageData(
      String host, List<String> pageDescriptors, String userName) {
    String site = siteService.getSiteForHostname(host);
    List<String> keys =
        pageDescriptors.stream()
            .map(
                desc -> {
                  PageDescriptor pageDescriptor = decodeDescriptor(desc);
                  return pageDescriptor.namespace() + ":" + pageDescriptor.pageName();
                })
            .toList();
    List<PageText> pageTexts =
        pageRepository.getAllBySiteAndNamespaceAndPagename(dbEngine, site, keys);
    return pageTexts.stream()
        .collect(
            Collectors.toMap(
                pageText -> new PageDescriptor(pageText.getNamespace(), pageText.getPagename()),
                pageText -> {
                  PageDescriptor pageDescriptor =
                      new PageDescriptor(pageText.getNamespace(), pageText.getPagename());
                  String sPageDescriptor = pageDescriptor.renderedName();
                  boolean canWrite =
                      namespaceService.canWriteNamespace(
                          site, pageDescriptor.namespace(), userName);
                  boolean canRead =
                      namespaceService.canReadNamespace(site, pageDescriptor.namespace(), userName);
                  boolean canDelete =
                      namespaceService.canDeleteInNamespace(
                              site, pageDescriptor.namespace(), userName)
                          && !pageDescriptor.isHome();
                  if (!canRead) {
                    return new PageData(
                        "You are not permissioned to read this page",
                        "",
                        sPageDescriptor,
                        Collections.emptyList(),
                        Collections.emptyList(),
                        PageData.EMPTY_FLAGS);
                  }

                  String title =
                      pageText.getTitle() != null ? pageText.getTitle() : pageText.getPagename();
                  return new PageData(
                      null,
                      pageText.getText(),
                      title,
                      Collections.emptyList(),
                      Collections.emptyList(),
                      new PageFlags(true, false, true, canWrite, canDelete, false));
                }));
  }

  @Transactional(readOnly = true)
  public PageData getHistoricalPageData(
      String host, String sPageDescriptor, long revision, String userName) {
    logger.info(
        "fetch page: host="
            + host
            + " sPageDescriptor="
            + sPageDescriptor
            + " revision= "
            + revision
            + " userName="
            + userName);
    String site = siteService.getSiteForHostname(host);
    PageDescriptor pageDescriptor = decodeDescriptor(sPageDescriptor);
    boolean canRead = namespaceService.canReadNamespace(site, pageDescriptor.namespace(), userName);
    if (!canRead) {
      return new PageData(
          "You are not permissioned to read this page",
          "",
          getTitle(host, sPageDescriptor),
          Collections.emptyList(),
          Collections.emptyList(),
          PageData.EMPTY_FLAGS);
    }
    Page p =
        pageRepository.findBySiteAndNamespaceAndPagenameAndRevision(
            site, pageDescriptor.namespace(), pageDescriptor.pageName(), revision);

    if (p == null) {
      // Add support for namespace level templates. Need templating language for
      // pageName/namespace/splitPageName
      return new PageData(
          "This page doesn't exist",
          getTemplate(site, pageDescriptor),
          getTitle(host, sPageDescriptor),
          Collections.emptyList(),
          null,
          PageData.EMPTY_FLAGS);
    }
    if (p.isDeleted()) {
      return new PageData(
          "This page doesn't exist",
          getTemplate(site, pageDescriptor),
          getTitle(host, sPageDescriptor),
          Collections.emptyList(),
          null,
          new PageFlags(false, true, true, false, false, false));
    }
    String source = p.getText();
    return new PageData(
        null,
        source,
        getTitle(pageDescriptor, p),
        p.getTags().stream().map(PageTag::getTag).toList(),
        null,
        new PageFlags(true, false, true, false, false, false));
  }

  @Transactional(readOnly = true)
  public PageCache getCachedPage(String host, String sPageDescriptor) {
    String site = siteService.getSiteForHostname(host);
    PageDescriptor pageDescriptor = decodeDescriptor(sPageDescriptor);
    PageCache.PageCacheKey key =
        new PageCache.PageCacheKey(site, pageDescriptor.namespace(), pageDescriptor.pageName());
    return pageCacheRepository.findById(key).orElse(null);
  }

  List<PageCache> getCachedPages(String host, List<String> pageDescriptors) {
    String site = siteService.getSiteForHostname(host);
    List<PageCache.PageCacheKey> keys =
        pageDescriptors.stream()
            .map(
                desc -> {
                  PageDescriptor pageDescriptor = decodeDescriptor(desc);
                  return new PageCache.PageCacheKey(
                      site, pageDescriptor.namespace(), pageDescriptor.pageName());
                })
            .toList();
    return DbSupport.toList(pageCacheRepository.findAllById(keys));
  }

  @Transactional
  public void saveCache(String host, String sPageDescriptor, String source, RenderResult rendered) {
    String site = siteService.getSiteForHostname(host);
    PageDescriptor pageDescriptor = decodeDescriptor(sPageDescriptor);
    Page p =
        pageRepository.getBySiteAndNamespaceAndPagename(
            site, pageDescriptor.namespace(), pageDescriptor.pageName());
    pageCacheRepository.deleteBySiteAndNamespaceAndPageName(
        site, pageDescriptor.namespace(), pageDescriptor.pageName());
    PageCache newCache = new PageCache();
    newCache.site = site;
    newCache.namespace = p.getNamespace();
    newCache.pageName = p.getPagename();
    newCache.renderedCache = rendered.renderedText();
    newCache.plaintextCache = rendered.plainText();
    newCache.useCache =
        !(Boolean)
            rendered
                .renderState()
                .getOrDefault(RenderResult.RenderStateKeys.DONT_CACHE.name(), Boolean.FALSE);
    newCache.source = adjustSource(source, rendered);
    newCache.title = (String) rendered.renderState().get(RenderResult.RenderStateKeys.TITLE.name());
    pageCacheRepository.save(newCache);
  }

  public String adjustSource(String source, RenderResult rendered) {
    return doAdjustSource(source, rendered);
  }

  public static String doAdjustSource(String source, RenderResult rendered) {
    if (rendered.renderState().containsKey(OVERRIDE_STATS.name())) {
      @SuppressWarnings("unchecked")
      List<LinkOverrideInstance> overrides =
          new ArrayList<>(
              (List<LinkOverrideInstance>) rendered.renderState().get(OVERRIDE_STATS.name()));
      Collections.reverse(overrides);
      StringBuilder sb = new StringBuilder(source);
      overrides.forEach(
          over -> {
            sb.replace(over.start(), over.stop(), over.override());
          });
      return sb.toString();
    }
    return source;
  }

  public static PageDescriptor decodeDescriptor(String pageDescriptor) {
    List<String> tokens = new ArrayList<>(Arrays.asList(pageDescriptor.split(":")));
    String pageName = tokens.remove(tokens.size() - 1);
    return new PageDescriptor(String.join(":", tokens), pageName);
  }

  static final Pattern PAGE_PATTERN = Pattern.compile("[A-z0-9-_:.]*");

  public static boolean validateDescriptor(String pageDescriptor) {
    Matcher m = PAGE_PATTERN.matcher(pageDescriptor);
    return m.matches();
  }

  List<String> getNamespaces(String rootNS, List<PageDesc> pages) {
    return pages.stream()
        .map(PageDesc::getNamespace)
        .distinct()
        .flatMap(
            ns -> {
              List<String> parts = List.of(ns.split(":"));
              List<String> namespaces = new ArrayList<>();
              for (int i = 0; i <= parts.size(); i++) {
                String namespace = String.join(":", parts.subList(0, i));
                namespaces.add(namespace);
              }
              return namespaces.stream();
            })
        .distinct()
        .filter(ns -> ns.startsWith(rootNS) && !ns.equals(rootNS))
        .filter(ns -> !ns.substring(rootNS.length() + 1).contains(":"))
        .sorted()
        .toList();
  }

  NsNode getNsNode(String rootNS, List<PageDesc> pages) {
    List<String> namespaces = getNamespaces(rootNS, pages);
    List<NsNode> nodes = new ArrayList<>();
    namespaces.forEach(ns -> nodes.add(getNsNode(ns, pages)));
    NsNode node = new NsNode(rootNS, false);
    node.setChildren(nodes);
    return node;
  }

  void buildNsNodeFromTree(
      NsNode root,
      List<String> nsParts,
      String site,
      Namespace.RestrictionType inheritedRestrictionType) {
    Optional<NsNode> existingChild =
        root.getChildren().stream()
            .filter(child -> child.getNamespace().equals(nsParts.get(0)))
            .findFirst();
    if (existingChild.isPresent()) {
      buildNsNodeFromTree(
          existingChild.get(),
          nsParts.subList(1, nsParts.size()),
          site,
          existingChild.get().getRestrictionTypeToPass());
      return;
    }
    List<String> buildParts = nsParts;
    while (!buildParts.isEmpty()) {
      NsNode newChild =
          new NsNode(namespaceService.joinNS(root.getFullNamespace(), buildParts.get(0)), true);
      newChild.setRestrictionType(
          namespaceService.getNSRestriction(site, newChild.getFullNamespace()));
      newChild.setInheritedRestrictionType(inheritedRestrictionType);
      List<NsNode> children = root.getChildren();
      children.add(newChild);
      root.setChildren(children);
      buildParts = buildParts.subList(1, buildParts.size());
      root = newChild;
      inheritedRestrictionType = newChild.getRestrictionTypeToPass();
    }
  }

  NsNode getNsNodeFromNS(List<String> namespaces, String site) {
    NsNode root = new NsNode("", true);
    root.setRestrictionType(namespaceService.getNSRestriction(site, ""));
    root.setInheritedRestrictionType(Namespace.RestrictionType.OPEN);
    namespaces.forEach(
        ns -> {
          if (!ns.isEmpty()) {
            List<String> nsParts = Arrays.asList(ns.split(":"));
            buildNsNodeFromTree(root, nsParts, site, root.getRestrictionTypeToPass());
          }
        });
    return root;
  }

  @Transactional(readOnly = true)
  public PageListResponse getAllPages(String host, String userName) {
    String site = siteService.getSiteForHostname(host);
    List<PageDesc> pages =
        namespaceService.filterReadablePages(pageRepository.getAllValid(site), site, userName);
    NsNode node = getNsNode("", pages);
    return new PageListResponse(
        pages.stream()
            .sorted(Comparator.comparing(p -> p.getPagename().toLowerCase()))
            .collect(Collectors.groupingBy(PageDesc::getNamespace)),
        node);
  }

  @Transactional(readOnly = true)
  public PageListResponse getAllNamespaces(String site, String userName) {
    List<String> nsList = namespaceService.getReadableNamespaces(site, userName);
    NsNode node = getNsNodeFromNS(nsList.stream().sorted().toList(), site);
    return new PageListResponse(null, node);
  }

  @Transactional(readOnly = true)
  public RecentChangesResponse recentChanges(String host, String userName) {
    String site = siteService.getSiteForHostname(host);
    List<String> namespaces = namespaceService.getReadableNamespaces(site, userName);
    List<PageDesc> pages =
        pageRepository.findAllBySiteAndNamespaceInOrderByModifiedDesc(
            Limit.of(10), site, namespaces);
    return new RecentChangesResponse(
        pages.stream().map(RecentChangesResponse::recFor).toList(), null, null);
  }

  @Transactional(readOnly = true)
  public List<String> getAllPagesFlat(String host, String userName) {
    String site = siteService.getSiteForHostname(host);
    List<PageDesc> pages =
        namespaceService.filterReadablePages(pageRepository.getAllValid(site), site, userName);
    return pages.stream().map(PageDesc::getDescriptor).toList();
  }

  @Transactional(readOnly = true)
  public boolean isReadable(String host, String pageDescriptor, String userName) {
    String site = siteService.getSiteForHostname(host);
    PageDescriptor pd = PageDescriptor.fromFullName(pageDescriptor);
    return namespaceService.canReadNamespace(site, pd.namespace(), userName);
  }

  @Transactional(readOnly = true)
  public List<String> getAllTags(String host, String userName) {
    String site = siteService.getSiteForHostname(host);
    return tagRepository.getAllActiveTags(site);
  }

  String getTemplate(String site, PageDescriptor pageDescriptor) {
    String ns = pageDescriptor.namespace();
    Page template = null;
    while (ns != null) {
      template =
          pageRepository.getBySiteAndNamespaceAndPagenameAndDeleted(site, ns, "_template", false);
      if (template != null) {
        break;
      }
      ns = namespaceService.parentNamespace(ns);
    }
    if (template == null) {
      return "======" + pageDescriptor.renderedName() + "======";
    }
    Map<String, String> replacements =
        Map.of(
            "%NAME%",
            pageDescriptor.renderedName(),
            "%NAMESPACE%",
            pageDescriptor.namespace(),
            "%RAWNAME%",
            pageDescriptor.pageName());
    String text = template.getText();
    for (Map.Entry<String, String> entry : replacements.entrySet()) {
      text = text.replace(entry.getKey(), entry.getValue());
    }
    return text;
  }

  @Transactional(readOnly = true)
  public List<PageDesc> getPageHistory(String host, String sPageDescriptor, String userName)
      throws PageReadException {
    String site = siteService.getSiteForHostname(host);
    PageDescriptor pageDescriptor = decodeDescriptor(sPageDescriptor);
    boolean canRead = namespaceService.canReadNamespace(site, pageDescriptor.namespace(), userName);
    if (!canRead) {
      throw new PageReadException("You are not permissioned to read this namespace");
    }
    return pageRepository.findAllBySiteAndNamespaceAndPagenameOrderByRevision(
        site, pageDescriptor.namespace(), pageDescriptor.pageName());
  }

  @Transactional(readOnly = true)
  public List<Pair<Integer, String>> getPageDiff(
      String host, String sPageDescriptor, Long rev1, Long rev2, String userName)
      throws PageReadException {
    String site = siteService.getSiteForHostname(host);
    PageDescriptor pageDescriptor = decodeDescriptor(sPageDescriptor);
    boolean canRead = namespaceService.canReadNamespace(site, pageDescriptor.namespace(), userName);
    if (!canRead) {
      throw new PageReadException("You are not permissioned to read this namespace");
    }
    Page latest =
        pageRepository.getBySiteAndNamespaceAndPagename(
            site, pageDescriptor.namespace(), pageDescriptor.pageName());
    Long id = latest.getId();
    PageKey key1 = new PageKey(id, rev1);
    PageKey key2 = new PageKey(id, rev2);
    Page p1 = pageRepository.findById(key1).orElse(null);
    Page p2 = pageRepository.findById(key2).orElse(null);
    if (p1 == null) {
      throw new PageReadException("Cannot read " + sPageDescriptor + " rev " + rev1);
    }
    if (p2 == null) {
      throw new PageReadException("Cannot read " + sPageDescriptor + " rev " + rev2);
    }
    return generateDiffs(p1.getText(), p2.getText());
  }

  public List<Pair<Integer, String>> generateDiffs(String text1, String text2) {
    List<String> v1 = List.of(text1.split("\n"));
    List<String> v2 = List.of(text2.split("\n"));
    DiffRowGenerator generator =
        DiffRowGenerator.create()
            .showInlineDiffs(true)
            .inlineDiffByWord(true)
            .mergeOriginalRevised(true)
            .build();
    List<DiffRow> rows = generator.generateDiffRows(v1, v2);
    List<Pair<Integer, String>> result = new ArrayList<>();
    int rowNum = 1;
    for (DiffRow row : rows) {
      Integer lineNum = row.getTag() == DiffRow.Tag.INSERT ? -1 : rowNum++;
      result.add(Pair.of(lineNum, row.getOldLine()));
    }
    return result;
  }
}
