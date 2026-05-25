package us.calubrecht.lazerwiki.macro;

import java.util.List;

/**
 * Service Provider Interface for loading macros from external JARs.
 * 
 * Implementations of this interface allow macros to be packaged and distributed
 * separately from the main LazerWiki application. This solves ClassLoader
 * security issues in Tomcat by allowing external macro JARs to be loaded via
 * Java's ServiceLoader mechanism.
 * 
 * To use this interface:
 * 1. Create a JAR file with macro implementations
 * 2. Implement this interface in a provider class
 * 3. Register the implementation in META-INF/services/us.calubrecht.lazerwiki.macro.MacroProvider
 * 4. Add the JAR to the Tomcat classpath (e.g., CATALINA_BASE/lib)
 */
public interface MacroProvider {
    
    /**
     * Returns the name of this macro provider.
     * 
     * @return A descriptive name for logging and debugging purposes
     */
    String getName();
    
    /**
     * Returns a list of macro instances provided by this provider.
     * 
     * Each macro returned should be a new instance with no shared state
     * between calls, as the MacroService will create instances during
     * the registration phase.
     * 
     * @return A list of Macro implementations provided by this provider
     */
    List<Macro> getMacros();
}
