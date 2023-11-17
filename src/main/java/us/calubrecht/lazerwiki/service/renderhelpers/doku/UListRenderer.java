package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;

import java.util.List;

@Component
public class UListRenderer extends ListRenderer{
    public UListRenderer() {
        super("ul");
    }

    @Override
    public List<Class> getTargets() {
        return List.of(DokuwikiParser.Ulist_itemContext.class);
    }
}
