package us.calubrecht.lazerwiki.macro;

import java.util.List;

/**
 * Service Provider Interface (SPI) for loading macros from external JARs.
 * 
 * Implementations should be registered in META-INF/services/us.calubrecht.lazerwiki.macro.MacroProvider
 * 
 * Example META-INF/services file content:
 * com.example.macros.ExternalMacroProvider
 * 
 * This approach avoids ClassLoader security issues in Tomcat by:
 * - Not requiring annotation scanning across classloader boundaries
 * - Using Java's standard ServiceLoader mechanism
 * - Allowing external JARs to be dropped in $CATALINA_HOME/lib/ without configuration changes
 */
public interface MacroProvider {
    /**
     * Returns a list of Macro implementations provided by this provider.
     * 
     * @return List of Macro instances to be registered
     */
    List<Macro> getMacros();
    
    /**
     * Human-readable name for this provider (used for logging).
     * 
     * @return Name of the provider
     */
    String getName();
}
