package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;

@Component
public class OListRenderer extends ListRenderer{
    public OListRenderer() {
        super("ol");
    }

    @Override
    public Class getTarget() {
        return DokuwikiParser.Olist_itemContext.class;
    }
}