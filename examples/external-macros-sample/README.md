# External Macros Sample

This is a complete example of how to create external macros for LazerWiki using the SPI (Service Provider Interface) approach.

## Structure

```
external-macros-sample/
├── pom.xml                           # Maven build configuration
└── src/main/java/us/calubrecht/examples/macros/
    ├── ExternalMacroProvider.java    # SPI implementation
    ├── ColorBoxMacro.java            # Example macro 1
    └── WarningMacro.java             # Example macro 2
└── src/main/resources/
    └── META-INF/services/
        └── us.calubrecht.lazerwiki.macro.MacroProvider  # SPI declaration file
```

## Building

```bash
mvn clean package
```

This produces `target/external-macros-sample-1.0.0.jar`

## Deploying to Tomcat

```bash
cp target/external-macros-sample-1.0.0.jar $CATALINA_HOME/lib/
```

Then restart Tomcat:
```bash
$CATALINA_HOME/bin/shutdown.sh
$CATALINA_HOME/bin/startup.sh
```

## Using the Example Macros

### ColorBox Macro

Wrap content in a colored box:

```
~~MACRO~~colorbox:red:This is red text~~/MACRO~~
~~MACRO~~colorbox:blue:This is blue text~~/MACRO~~
~~MACRO~~colorbox:green:This is green text~~/MACRO~~
```

### Warning Macro

Display a warning message:

```
~~MACRO~~warning:This is an important warning!~~/MACRO~~
```

## How It Works

1. **ExternalMacroProvider** implements the `MacroProvider` interface
2. It declares itself in `META-INF/services/us.calubrecht.lazerwiki.macro.MacroProvider`
3. When LazerWiki starts, `ServiceLoader.load(MacroProvider.class)` discovers this provider
4. The provider's `getMacros()` method is called, returning the list of macros
5. All macros are registered and available for use

## Extending This Example

To add more macros:

1. Create a new class that extends `Macro`
2. Implement `getName()` and `render()` methods
3. Add an instance to the list in `ExternalMacroProvider.getMacros()`
4. Rebuild and redeploy the JAR

## Key Files Explained

### pom.xml
- Declares dependency on LazerWiki API (scope: provided)
- Uses maven-jar-plugin to ensure META-INF/services files are included

### ExternalMacroProvider.java
- Implements `MacroProvider` interface
- Instantiates and returns the list of macros
- This is the entry point discovered via ServiceLoader

### ColorBoxMacro.java / WarningMacro.java
- Extend the `Macro` abstract class
- Implement `getName()` - returns the macro name used in wiki syntax
- Implement `render()` - generates the HTML output
- Optionally override `getCSS()` to provide stylesheet

### META-INF/services/us.calubrecht.lazerwiki.macro.MacroProvider
- Plain text file containing the fully-qualified class name of the provider
- No blank lines allowed
- Discovered automatically by Java's ServiceLoader
