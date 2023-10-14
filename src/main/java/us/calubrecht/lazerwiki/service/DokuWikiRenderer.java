package us.calubrecht.lazerwiki.service;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import us.calubrecht.lazerwiki.service.helpers.RenderHelper_Line;
import us.calubrecht.lazerwiki.service.helpers.doku.DokuHeader;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiLexer;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * An implementation of IMarkupRenderer that speaks DokuWiki's markup language.
 */
@Service
public class DokuWikiRenderer implements IMarkupRenderer {

    @Autowired
    RendererRegistrar renderers;

    @Override
    public String render(String markup) {
        DokuwikiLexer lexer = new DokuwikiLexer(CharStreams.fromString(markup + '\n'));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        DokuwikiParser parser = new DokuwikiParser(tokens);
        ParseTree tree = parser.page();
        StringBuffer outBuffer = new StringBuffer();
        for(int i = 0; i < tree.getChildCount(); i++) {
            ParseTree child = tree.getChild(i);
            outBuffer.append(renderers.getRenderer(child.getClass()).render(child));
        }
        char l = outBuffer.charAt(outBuffer.length() -1);
        if (outBuffer.charAt(outBuffer.length() -1) == 'a') {
            outBuffer.deleteCharAt(outBuffer.length()-1);
        }
        return outBuffer.toString().strip();
    }

    protected String parseLine(String line) {
        RenderHelper_Line helper = new DokuHeader();
        if (helper.matches(line)) {
            return helper.render(line);
        }
        return line;
    }
}
