package us.calubrecht.lazerwiki.model;

public record PageDescriptor(String namespace, String pageName) {

    @Override
    public String toString() {
        return this.namespace + ":" + this.pageName;
    }
}
