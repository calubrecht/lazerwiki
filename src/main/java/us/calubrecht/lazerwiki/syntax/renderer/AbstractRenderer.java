package us.calubrecht.lazerwiki.syntax.renderer;

import us.calubrecht.lazerwiki.syntax.framework.ITreeRenderer;
import us.calubrecht.lazerwiki.syntax.framework.ParserRegistrar;

public abstract class AbstractRenderer implements ITreeRenderer {
    protected ParserRegistrar parserRegistrar;

    @Override
    public void setRegistrar(ParserRegistrar registrar) {
        parserRegistrar = registrar;
    }
}
