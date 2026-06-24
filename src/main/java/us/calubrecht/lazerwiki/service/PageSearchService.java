package us.calubrecht.lazerwiki.service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import us.calubrecht.lazerwiki.model.PageCache;
import us.calubrecht.lazerwiki.repository.PageCacheRepository;
import us.calubrecht.lazerwiki.repository.PageRepository;
import us.calubrecht.lazerwiki.responses.SearchResult;

@Service
public class PageSearchService {
  final Logger logger = LogManager.getLogger(getClass());

  @Autowired PageRepository pageRepository;

  @Autowired SiteService siteService;

  @Autowired NamespaceService namespaceService;

  @Autowired PageCacheRepository pageCacheRepository;

  @Value("${lazerwiki.db.engine:mysql}")
  String dbEngine;

  @Transactional(readOnly = true)
  public Map<String, List<SearchResult>> searchPages(
      String host, String userName, String searchTerm) {
    String[] searchValues = searchTerm.split(":");
    return searchPages(host, userName, Map.of(searchValues[0], searchValues[1]));
  }

  @Transactional(readOnly = true)
  public Map<String, List<SearchResult>> searchPages(
      String host, String userName, Map<String, String> searchTerms) {
    String site = siteService.getSiteForHostname(host);
    if (searchTerms.containsKey("tag")) {
      String tagName = searchTerms.get("tag");
      List<SearchResult> tagPages =
          namespaceService
              .filterReadablePages(
                  pageRepository.getByTagname(dbEngine, site, tagName), site, userName)
              .stream()
              .sorted(Comparator.comparing(p -> p.getNamespace() + ":" + p.getPagename()))
              .map(pd -> new SearchResult(pd.getNamespace(), pd.getPagename(), pd.getTitle(), null))
              .toList();
      if (!searchTerms.getOrDefault("ns", "*").equals("*")) {
        Pattern nsPattern = Pattern.compile(searchTerms.get("ns").replaceAll("\\*", ".*"));
        return Map.of(
            "tag",
            tagPages.stream().filter(pd -> nsPattern.matcher(pd.namespace()).matches()).toList());
      }
      return Map.of("tag", tagPages);
    } else if (searchTerms.containsKey("text")) {
      String searchTerm = prepareSearchTerm(searchTerms.get("text"));
      String titleSearchTerm = prepareTitleSearchTerm(searchTerms.get("text"));
      String searchLower = searchTerms.get("text").toLowerCase();
      List<SearchResult> titlePages =
          namespaceService
              .filterReadablePages(
                  new ArrayList<>(
                      pageCacheRepository.searchByTitle(dbEngine, site, titleSearchTerm)),
                  site,
                  userName)
              .stream()
              .map(pd -> new SearchResult(pd.getNamespace(), pd.getPagename(), pd.getTitle(), null))
              .collect(Collectors.toList());
      List<SearchResult> textPages =
          namespaceService
              .filterReadablePages(
                  new ArrayList<>(pageCacheRepository.searchByText(dbEngine, site, searchTerm)),
                  site,
                  userName)
              .stream()
              .map(pc -> searchResultFromPlaintext((PageCache) pc, searchLower))
              .collect(Collectors.toList());
      if (!searchTerms.getOrDefault("ns", "*").equals("*")) {
        Pattern nsPattern = Pattern.compile(searchTerms.get("ns").replaceAll("\\*", ".*"));
        titlePages =
            titlePages.stream().filter(pd -> nsPattern.matcher(pd.namespace()).matches()).toList();
        textPages =
            textPages.stream().filter(pd -> nsPattern.matcher(pd.namespace()).matches()).toList();
      }
      return Map.of("title", titlePages, "text", textPages);
    }
    return Collections.emptyMap();
  }

  String prepareSearchTerm(String terms) {
    return Stream.of(terms.split(" "))
        .map(term -> !term.endsWith("*") ? term + "*" : term)
        .collect(Collectors.joining(" "));
  }

  String prepareTitleSearchTerm(String terms) {
    List<String> rawTerms = Stream.of(terms.split(" ")).toList();
    List<String> searchTerms =
        rawTerms.stream()
            .filter(term -> !(List.of("a", "the").contains(term.toLowerCase())))
            .toList();
    searchTerms = searchTerms.isEmpty() ? rawTerms : searchTerms;
    return searchTerms.stream()
        .map(term -> !term.endsWith("*") ? term + "*" : term)
        .collect(Collectors.joining(" "));
  }

  List<List<String>> prioritizeSearchTerms(List<String> originalSearchTerms) {
    // Could be configurable, what terms to deprioritize.
    List<String> priorityTerms =
        originalSearchTerms.stream()
            .filter(term -> !(List.of("a", "the").contains(term.toLowerCase())))
            .toList();
    return List.of(priorityTerms, originalSearchTerms);
  }

  SearchResult searchResultFromPlaintext(PageCache pc, String search) {
    // Check full term first
    Optional<String> searchLine =
        Stream.of(pc.plaintextCache.split("\n"))
            .filter(
                line -> {
                  // Can do something smarter? make prefer if text is a word of its own?
                  String lowerLine = line.toLowerCase();
                  return lowerLine.contains(search);
                })
            .findFirst();
    if (searchLine.isEmpty()) {
      List<String> searchTerms = List.of(search.split(" "));
      List<List<String>> priorityList = prioritizeSearchTerms(searchTerms);
      for (List<String> terms : priorityList) {
        searchLine =
            Stream.of(pc.plaintextCache.split("\n"))
                .filter(
                    line -> {
                      // Can do something smarter? make prefer if text is a word of its own?
                      String lowerLine = line.toLowerCase();
                      for (String term : terms) {
                        if (lowerLine.contains(term)) {
                          return true;
                        }
                      }
                      return false;
                    })
                .findFirst();
        if (searchLine.isPresent()) {
          break;
        }
      }
    }
    return new SearchResult(
        pc.getNamespace(), pc.getPagename(), pc.getTitle(), searchLine.orElse(null));
  }
}
