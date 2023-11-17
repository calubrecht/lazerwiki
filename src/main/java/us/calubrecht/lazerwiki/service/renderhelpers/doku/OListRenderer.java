package us.calubrecht.lazerwiki.service.renderhelpers.doku;

import org.springframework.stereotype.Component;
import us.calubrecht.lazerwiki.service.parser.doku.DokuwikiParser;

import java.util.List;

@Component
public class OListRenderer extends ListRenderer{
    public OListRenderer() {
        super("ol");
    }

    @Override
    public List<Class> getTargets() {
        return List.of(DokuwikiParser.Olist_itemContext.class);
    }
}