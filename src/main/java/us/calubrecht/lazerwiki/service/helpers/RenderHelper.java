package us.calubrecht.lazerwiki.service.helpers;

public interface RenderHelper {
    public boolean matches(String line);
    public String render(String line);
}
