package us.calubrecht.lazerwiki.exampleMacros;

import org.apache.commons.lang3.tuple.Pair;
import us.calubrecht.lazerwiki.macro.CustomMacro;
import us.calubrecht.lazerwiki.macro.Macro;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@CustomMacro
public class LinkCheckMacro extends Macro{

    final String css = """
            table.linksTable, table.linksTable td, table.linksTable th {
              border: 1px solid;
            }
            table.linksTable {
              border-collapse:collapse;
              empty-cells:show;
              border-spacing:0;
            }
             table.linksTable th {
             background: lightgrey;
             padding: 0 .5em 0 .5em;
             }
             table.linksTable td {
             padding: 0 .5em 0 .5em;
             }

            table.linksTable tr:hover {
             background: #ffe0a0;
             }
            """;

    @Override
    public String getName() {
        return "linkCheck";
    }

    public Optional<String> getCSS() {
        return Optional.of(css);
    }

    @Override
    public String render(Macro.MacroContext context, String macroArgs) {

        Map<String, String> caseInsensitiveMapping = context.getAllPages().stream().collect(Collectors.toMap(String::toLowerCase, Function.identity()));
        Map<String, String> brokenLinksMapping = new HashMap<>();
        Set<String> allPages = caseInsensitiveMapping.keySet();
        Set<String> linkedTo = new HashSet<>();
        Map<String, List<String>> brokenLinks = new HashMap<>();
        allPages.stream().sorted().forEach(page -> {
            List<String> links = context.getLinksOnPage(page);
            linkedTo.addAll(links.stream().map(String::toLowerCase).filter(l -> allPages.contains(l)).collect(Collectors.toList()));
            links.stream().filter(l -> !allPages.contains(l.toLowerCase())).forEach(l -> {
                brokenLinks.computeIfAbsent(l.toLowerCase(), (k)-> new ArrayList<>()).add(page);
                // Record case of first existence of link
                if (!brokenLinksMapping.containsKey(l.toLowerCase())) {
                    brokenLinksMapping.put(l.toLowerCase(), l);
                }
            });
        });
        Set<String> orphanedPages = new HashSet<>(allPages);
        orphanedPages.removeAll(linkedTo);
        orphanedPages.remove("");
        context.setPageDontCache();
        return """
                <h2>Broken Links</h2>
                <table class="brokenLinks linksTable">
                <tbody>
                <tr><th>#</th><th>Page Name</th><th>Linking Pages</th></tr>
                %s
                </tbody></table>
                <h2>Orphaned Pages</h2>
                <table class="orphanPages linksTable">
                <tbody>
                  <tr><th>Pages Name</th></tr>
                  %s
                  </tbody></table>
                """.formatted(renderBrokenLinks(brokenLinks, caseInsensitiveMapping, brokenLinksMapping), renderOrphanedPages(orphanedPages, caseInsensitiveMapping));
    }

    String renderBrokenLinks(Map<String, List<String>> brokenLinks, Map<String, String> caseInsensitiveMapping, Map<String, String> brokenLinksMapping) {
        return brokenLinks.entrySet().stream().map(
                entry ->"<tr><td>%s</td><td>%s</td><td>%s</td></tr>".formatted(entry.getValue().size(), renderLink(brokenLinksMapping.get(entry.getKey())), renderLinkList(entry.getValue(), caseInsensitiveMapping))).collect(Collectors.joining("\n"));
    }

    String renderOrphanedPages( Set<String> orphanedPages, Map<String, String> caseInsensitiveMapping) {
        return orphanedPages.stream().map(
                page ->"<tr><td>%s</td></tr>".formatted(renderLink(caseInsensitiveMapping.get(page)))).collect(Collectors.joining("\n"));
    }

    String renderLink(String page) {
        if (page.equals("")) {
            return "<a href=\"/\">HOME</a>";
        }
        return "<a href=\"/page/" + page + "\">"+ page + "</a>";
    }

    String renderLinkList(List<String> pages, Map<String, String> caseInsensitiveMapping) {
        return "[%s]".formatted(pages.stream().map(p -> renderLink(caseInsensitiveMapping.get(p))).collect(Collectors.joining(",")));
    }
}
