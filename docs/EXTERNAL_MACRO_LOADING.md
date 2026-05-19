# External Macros Loading Guide

This document describes how to package and deploy macros as external JARs using LazerWiki's SPI (Service Provider Interface) approach.

## Overview

LazerWiki now supports loading macros from external JAR files without requiring any Tomcat configuration changes. This uses Java's standard `ServiceLoader` mechanism, which avoids the classloader security issues that occur when trying to scan external JARs directly.

## How It Works

1. When LazerWiki starts, `MacroService.registerMacros()` is called
2. It first loads built-in macros via annotation scanning (existing behavior)
3. Then it calls `ServiceLoader.load(MacroProvider.class)` 
4. ServiceLoader automatically discovers any JAR on the classpath that declares a `MacroProvider` implementation
5. Each provider's `getMacros()` method is called to retrieve its list of macros
6. All macros are registered and available for use

## Creating External Macros

### Step 1: Create a Maven Project

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <groupId>com.example</groupId>
    <artifactId>my-custom-macros</artifactId>
    <version>1.0.0</version>
    
    <dependencies>
        <!-- Dependency on LazerWiki API (scope: provided) -->
        <dependency>
            <groupId>us.calubrecht</groupId>
            <artifactId>lazerwiki</artifactId>
            <version>1.0.0</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
</project>
```

**Important:** Use `scope: provided` because LazerWiki classes will already be available via Tomcat's classloader.

### Step 2: Create Your Macros

Extend the `Macro` abstract class:

```java
package com.example.macros;

import us.calubrecht.lazerwiki.macro.Macro;
import java.util.Optional;

public class MyCustomMacro extends Macro {
    
    @Override
    public String getName() {
        return "mymacro";  // Name used in wiki syntax
    }
    
    @Override
    public String render(MacroContext context, String macroArgs) {
        // Your macro implementation
        return "<div>Rendered output</div>";
    }
    
    @Override
    public Optional<String> getCSS() {
        return Optional.empty();  // Optional CSS styling
    }
    
    @Override
    public boolean allowCache(MacroContext context, String macroArgs) {
        return true;  // Whether this macro's output can be cached
    }
}
```

### Step 3: Create the MacroProvider Implementation

```java
package com.example.macros;

import us.calubrecht.lazerwiki.macro.Macro;
import us.calubrecht.lazerwiki.macro.MacroProvider;
import java.util.List;

public class MyMacroProvider implements MacroProvider {
    
    @Override
    public List<Macro> getMacros() {
        return List.of(
            new MyCustomMacro(),
            new AnotherMacro(),
            // Add all your macros here
        );
    }
    
    @Override
    public String getName() {
        return "My Custom Macros";  // For logging
    }
}
```

### Step 4: Register via SPI

Create the file: `src/main/resources/META-INF/services/us.calubrecht.lazerwiki.macro.MacroProvider`

Contents:
```
com.example.macros.MyMacroProvider
```

**Important:** This file contains ONLY the fully-qualified class name, no blank lines.

### Step 5: Build

```bash
mvn clean package
```

This produces `target/my-custom-macros-1.0.0.jar`

## Deploying to Tomcat

### Option 1: Deploy with WAR

If developing LazerWiki itself, include your external macros JAR in the WAR file's `WEB-INF/lib/` directory.

### Option 2: Deploy to Tomcat lib (Recommended for Production)

```bash
cp target/my-custom-macros-1.0.0.jar $CATALINA_HOME/lib/
```

Then restart Tomcat:
```bash
$CATALINA_HOME/bin/shutdown.sh
sleep 2
$CATALINA_HOME/bin/startup.sh
```

**Advantages:**
- No need to rebuild the WAR file
- Separate versioning for macros and core application
- Easy to enable/disable macros (just add/remove the JAR)
- No Tomcat configuration needed

## Troubleshooting

### Macros Not Loading

Check the Tomcat logs:

```bash
tail -f $CATALINA_HOME/logs/catalina.out
```

Look for messages like:
```
INFO - Scanning for external macros via ServiceLoader
INFO - Loading macros from provider: My Custom Macros
INFO - Registering macro mymacro as com.example.macros.MyCustomMacro
```

### Common Issues

**Issue:** `ServiceConfigurationError: META-INF/services file not found`
- **Cause:** Incorrect SPI service file path or name
- **Solution:** Ensure file is at exactly: `META-INF/services/us.calubrecht.lazerwiki.macro.MacroProvider`

**Issue:** `ClassNotFoundException` for macro class
- **Cause:** JAR not on classpath
- **Solution:** Verify JAR is in `$CATALINA_HOME/lib/` and Tomcat has been restarted

**Issue:** Macro not appearing in wiki
- **Cause:** Macro name might be conflicting with a built-in macro
- **Solution:** Check logs for warnings, choose a unique macro name

**Issue:** `NoClassDefFoundError` for LazerWiki classes
- **Cause:** Dependency not marked as `provided` scope
- **Solution:** Update `pom.xml` to use `<scope>provided</scope>` for LazerWiki dependency

## Example

See `examples/external-macros-sample/` for a complete working example with two sample macros.

To test it:

```bash
cd examples/external-macros-sample
mvn clean package
cp target/external-macros-sample-1.0.0.jar $CATALINA_HOME/lib/
$CATALINA_HOME/bin/shutdown.sh
$CATALINA_HOME/bin/startup.sh
```

Then use the macros in your wiki:
```
~~MACRO~~colorbox:red:Red text~~MACRO~~
~~MACRO~~warning:This is important!~~MACRO~~
```

## Best Practices

1. **Use a consistent naming convention** for macro names to avoid conflicts
2. **Document the macro format** in comments and README
3. **Test caching behavior** - set `allowCache()` to `false` if the output can change between renders
4. **Include CSS styling** via `getCSS()` for better visual presentation
5. **Handle errors gracefully** in `render()` to avoid breaking page rendering
6. **Version your JAR** to track changes and facilitate rollbacks
7. **Include dependency information** in your JAR's `META-INF/MANIFEST.MF`

## Example Macro Implementations

### Simple Text Replacement

```java
@Override
public String render(MacroContext context, String macroArgs) {
    return "<strong>" + context.sanitize(macroArgs) + "</strong>";
}
```

### Rendering Wiki Markup

```java
@Override
public String render(MacroContext context, String macroArgs) {
    MacroContext.RenderOutput output = context.renderMarkup(macroArgs);
    return "<div class='highlighted'>" + output.getHtml() + "</div>";
}
```

### Including Other Pages

```java
@Override
public String render(MacroContext context, String macroArgs) {
    MacroContext.RenderOutput output = context.renderPage(macroArgs);
    return output.getHtml();
}
```

### Accessing Wiki Data

```java
@Override
public String render(MacroContext context, String macroArgs) {
    List<String> allPages = context.getAllPages();
    List<String> linkedPages = context.getLinksOnPage(macroArgs);
    // Build output from wiki data...
    return result;
}
```

## FAQ

**Q: Do I need to modify Tomcat configuration?**
A: No. Just drop the JAR in `$CATALINA_HOME/lib/` and restart.

**Q: Can I have multiple MacroProvider implementations in one JAR?**
A: Yes, just add multiple lines to the `META-INF/services/` file.

**Q: What if two macros have the same name?**
A: The first one registered wins. Check the logs to see the order.

**Q: Can external macros use external dependencies?**
A: Yes, include them as regular dependencies in your JAR (not as `provided` scope). They'll be available on the classpath.

**Q: How do I update my macros without restarting Tomcat?**
A: You can't with the current approach. Dynamic reloading would require complex classloader manipulation. Restart is recommended.

**Q: Can I use Spring beans in my macros?**
A: External macros are instantiated directly, not via Spring. However, they can use the `MacroContext` to access LazerWiki services indirectly.
