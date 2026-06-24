package us.calubrecht.lazerwiki.syntax.renderer;

import static us.calubrecht.lazerwiki.model.RenderResult.RenderStateKeys.ERRORS;

import java.util.ArrayList;
import java.util.List;
import us.calubrecht.lazerwiki.service.renderhelpers.RenderContext;
import us.calubrecht.lazerwiki.syntax.framework.ITreeRenderer;
import us.calubrecht.lazerwiki.syntax.framework.ParserRegistrar;

public abstract class AbstractRenderer implements ITreeRenderer {
  protected ParserRegistrar parserRegistrar;

  @Override
  public void setRegistrar(ParserRegistrar registrar) {
    parserRegistrar = registrar;
  }

  @SuppressWarnings("unchecked")
  protected void addError(RenderContext renderContext, String error) {
    ((List<String>)
            renderContext.renderState().computeIfAbsent(ERRORS.name(), (k) -> new ArrayList<>()))
        .add(error);
  }
}
