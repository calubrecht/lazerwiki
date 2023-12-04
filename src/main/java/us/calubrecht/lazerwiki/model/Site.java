package us.calubrecht.lazerwiki.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity(name = "sites")
public class Site {

    @Id
    public String name;

    public String hostname;

    public String siteName;
}
