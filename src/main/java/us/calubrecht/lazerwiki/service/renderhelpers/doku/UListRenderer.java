package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;

@Component
public class UListRenderer extends ListRenderer{
    public UListRenderer() {
        super("ul");
    }

    @Override
    public Class getTarget() {
        return DokuwikiParser.Ulist_itemContext.class;
    }
}
