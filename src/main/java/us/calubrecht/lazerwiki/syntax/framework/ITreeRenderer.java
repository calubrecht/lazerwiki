package us.calubrecht.lazerwiki.syntax.framework;

import java.util.Collection;
import java.util.List;

public interface ITreeRenderer {
    Collection<Class> getTargets();

    void setRegistrar(ParserRegistrar registrar);

    public static class DefaultRenderer implements ITreeRenderer {

        @Override
        public Collection<Class> getTargets() {
            return List.of();
        }

        @Override
        public void setRegistrar(ParserRegistrar registrar) {

        }
    }
}
