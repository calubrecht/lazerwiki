# LazerWiki Macros and Plugins

LazerWiki has two extension points:

- **Macros** — render arbitrary HTML in place of a `~~MACRO~~…~~/MACRO~~` tag in wiki content

- **Plugins** — contribute buttons to the wiki editor toolbar

Both are loaded at startup by scanning Java packages listed in the `lazerwiki.plugin.scan.packages` property.


## Distribution options

### Option A — localMacros jar (bundled)

Put your classes in `src/main/java/localMacros/` under any package. The `localMacroJar` Gradle task packages them into a separate `localMacros.jar` that is placed on the classpath at deploy time. The package `localMacros` is already included in the default `lazerwiki.plugin.scan.packages`.

### Option B — external jar (recommended for reuse)

Depend on the `lazerwiki-macro-api` artifact, which contains only the `Macro`, `CustomMacro`, `Plugin`, `WikiPlugin`, and `EditToolbarDef` types:

```
// build.gradle.kts  
dependencies \{  
    implementation("us.calubrecht:lazerwiki-macro-api:\<version\>")  
\}
```

Published to: `https://repo.repsy.io/mvn/ca\_lazerdwarf/default`

Because of Tomcat ClassLoader security model, the Lazerwiki cannot find the macros in the external jar. Suggest extracting both jars, copy the macro sources into the lazerwiki directory and repackage.


## Writing a Macro

1. Extend `us.calubrecht.lazerwiki.macro.Macro`

2. Annotate the class with `@us.calubrecht.lazerwiki.macro.CustomMacro`

3. Implement `getName()` and `render(MacroContext, String)`

The class must have a public no-arg constructor (the default if you declare none).

### Minimal example

```
package localMacros;  
  
import us.calubrecht.lazerwiki.macro.CustomMacro;  
import us.calubrecht.lazerwiki.macro.Macro;  
  
@CustomMacro  
public class HelloMacro extends Macro \{  
  
    @Override  
    public String getName() \{  
        return "hello";  
    \}  
  
    @Override  
    public String render(MacroContext context, String macroArgs) \{  
        String name = macroArgs.isBlank() ? "world" : context.sanitize(macroArgs.trim());  
        return "\<strong\>Hello, " + name + "!\</strong\>";  
    \}  
\}
```

Usage in a wiki page:

```
~~MACRO~~hello:Alice~~/MACRO~~
```

### Macro syntax

```
~~MACRO~~macroName:arguments~~/MACRO~~
```

Everything after the first `:` is passed verbatim as `macroArgs`. Multi-line arguments are supported:

```
~~MACRO~~myMacro:line one  
line two  
~~/MACRO~~
```

### Macro API

#### `String getName()`

The macro name as used in `~~MACRO~~name:…~~`. Case-sensitive.

#### `String render(MacroContext context, String macroArgs)`

Returns an HTML fragment. Never return `null` — return an empty string if there is nothing to render. Always sanitize any user-visible content with `context.sanitize()`.

#### `Optional\<String\> getCSS()`

Return CSS that should be injected into the page `\<head\>` whenever this macro is present. The CSS is collected once at startup. Default: empty.

#### `boolean allowCache(MacroContext context, String macroArgs)`

Return `false` to prevent the host page from being cached. Use this for macros whose output changes independently of the page text (e.g. macros that list recent changes or include other pages). Default: `true`.

#### `Map\<String, String\> toArgsMap(String macroArgs)`

Convenience helper. Parses `key1=val1&key2=val2` into a `Map\<String, String\>`. Use it when your macro accepts named parameters:

```
Map\<String, String\> args = toArgsMap(macroArgs);  
String ns = args.getOrDefault("ns", "");
```


## MacroContext API

The `MacroContext` object is passed to every `render()` call. It provides access to wiki data without coupling macros to internal services.

### Sanitization

```
String sanitize(String input)
```

HTML-escapes a string. Always call this before inserting user-controlled strings into your HTML output.

### Page queries

```
List\<String\> getAllPages()  
List\<String\> getPagesByNSAndTag(String ns, String tag)  
boolean isReadable(String pageDescriptor)  
List\<String\> getLinksOnPage(String page)
```

`pageDescriptor` format: `namespace:pagename` (e.g. `"docs:intro"`). The root namespace is `""`.

### Rendering

```
RenderOutput renderPage(String pageDescriptor)  
RenderOutput getCachedRender(String pageDescriptor)  
Map\<String, RenderOutput\> getCachedRenders(List\<String\> pageDescriptors)  
RenderOutput renderMarkup(String markup)
```

`renderPage` renders the named page through the full pipeline. `renderMarkup` renders an arbitrary markup string in the context of the current page. Both return a `RenderOutput` with:

```
abstract class RenderOutput \{  
    public abstract String getHtml();  
    public abstract Map\<String, Object\> getState();  
\}
```

### Cache control

```
void setPageDontCache()
```

Call this inside `render()` to mark the current host page as uncacheable. Use when your macro output depends on state that changes independently of the page (e.g. when including another page, so that changes to the included page surface immediately).

### Link tracking

```
void addLinks(Collection\<String\> newLinks)
```

Register page names that this macro logically links to. This ensures the link graph stays accurate so that tools like `linkCheck` count links produced by macros.

### Plaintext mode

```
boolean isPlaintextRender()
```

Returns `true` when the page is being rendered for search indexing rather than display. In plaintext mode you should return plain text or an empty string (no HTML).


## Built-in example macros

These ship with LazerWiki and are good reference implementations.

### `include`

Embeds another wiki page inline.

```
~~MACRO~~include:namespace:pagename~~/MACRO~~
```

Marks the host page as uncacheable and adds an "Edit" link for users with write permission.

### `wrap`

Wraps content in a `\<div\>` with a CSS class.

```
~~MACRO~~wrap:myClass:some \*\*formatted\*\* text~~/MACRO~~
```

Multi-line:

```
~~MACRO~~wrap:myClass:  
= Heading =  
Some text.  
~~/MACRO~~
```

### `linkCheck`

Generates a report of broken links and orphaned pages across the wiki. Accepts optional named arguments:

| Argument | Effect |
| - | - |
| `ns=foo,bar` | Only report pages in namespaces `foo` and `bar` |
| `filterNS=secret` | Exclude pages in the `secret` namespace |
| `filterOrphanNS=archive` | Exclude `archive` pages from the orphan list |


```
~~MACRO~~linkCheck:ns=docs,tutorials~~/MACRO~~
```


## Writing a Plugin

Plugins add buttons to the editor toolbar. Each button has a name, an optional icon, and a JavaScript function that is called when the user clicks it.

1. Extend `us.calubrecht.lazerwiki.plugin.Plugin`

2. Annotate the class with `@us.calubrecht.lazerwiki.plugin.WikiPlugin`

3. Implement `getName()` and `getEditToolbarDefinitions()`

### Example

```
package localMacros;  
  
import us.calubrecht.lazerwiki.plugin.EditToolbarDef;  
import us.calubrecht.lazerwiki.plugin.Plugin;  
import us.calubrecht.lazerwiki.plugin.WikiPlugin;  
  
import java.util.List;  
  
@WikiPlugin  
public class MyPlugin extends Plugin \{  
  
    @Override  
    public String getName() \{  
        return "myPlugin";  
    \}  
  
    @Override  
    public List\<EditToolbarDef\> getEditToolbarDefinitions() \{  
        String script = """  
            (currentText, selectStart, selectEnd) =\> \{  
                const selected = currentText.substring(selectStart, selectEnd);  
                return \{action: "replace", value: "\*\*" + selected + "\*\*"\};  
            \}  
            """;  
        return List.of(new EditToolbarDef("Bold", "bold.png", script));  
    \}  
\}
```

### `EditToolbarDef(String name, String icon, String script)`

| Field | Description |
| - | - |
| `name` | Label shown on the toolbar button |
| `icon` | Filename of the button icon (relative to the site's static directory). Use `null` for the default icon. |
| `script` | JavaScript arrow function `(currentText, selectStart, selectEnd) =\> \{ … \}` |


### Toolbar script return values

The script function receives the full editor text and the current selection range, and must return an action object:

```
// Insert text at the cursor position  
\{ action: "insert", atCursor: true, value: "~~MACRO~~foo:bar~~/MACRO~~" \}  
  
// Replace the selected text  
\{ action: "replace", value: "\*\*" + selected + "\*\*" \}
```

### Disabling plugins per site

A site can blocklist plugins by name via the `pluginBlacklist` site setting (a JSON array). Blocklisted plugins are excluded from the toolbar definition served to that site's UI.


## Configuration

Add your package(s) to `lazerwiki.plugin.scan.packages` in `application.properties`:

```
lazerwiki.plugin.scan.packages=us.calubrecht.lazerwiki.exampleMacros,us.calubrecht.lazerwiki.examplePlugins,localMacros,com.example.mymacros
```

The scanner finds all classes annotated with `@CustomMacro` or `@WikiPlugin` in the listed packages and instantiates them using their no-arg constructors. Instantiation failures are logged and skipped — they do not prevent startup.

