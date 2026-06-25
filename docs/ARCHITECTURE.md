# LazerWiki Architecture

## Overview

LazerWiki is a Spring Boot web application deployed as a WAR on Tomcat. The React front end (lazerwiki-ui) communicates with the backend through a REST API. All API endpoints are prefixed with `api/` or `app/api/`.

```
lazerwiki-ui (React)
      |
      | REST (JSON)
      ↓
  Controllers  ─→  Services  ─→  Repositories (JPA)  ─→  MySQL / SQLite
                       |
                  Syntax Engine (parser → AST → renderer)
                       |
                  Macro / Plugin system
```

---

## Package Structure

```
us.calubrecht.lazerwiki
├── controller/        REST controllers — one per feature area
├── service/           Business logic; injected into controllers
│   ├── renderhelpers/ RenderContext (site, page, user, renderer, state) passed through the rendering pipeline
│   └── exception/     Checked exceptions thrown by services
├── repository/        Spring Data JPA repositories
├── model/             JPA entities and value types
├── requests/          Inbound JSON request bodies
├── responses/         Outbound JSON response bodies
├── syntax/            Wiki markup parser and renderer
│   ├── framework/     Parser/renderer interfaces and registration
│   ├── nodes/         AST node types
│   ├── parser/        Per-construct parsers
│   └── renderer/      Per-construct HTML renderers
├── macro/             Macro base class and annotation
├── plugin/            Plugin base class and annotation
├── exampleMacros/     Built-in example macros (include, wrap, linkCheck)
├── examplePlugins/    Built-in example plugins (clearFloats)
├── config/            Spring configuration classes
├── adminCommandLine/  CLI entry point for admin tasks
└── util/              Shared utilities
```

---

## Controllers

Each controller handles one feature area:

| Controller | Responsibility |
|---|---|
| `PageController` | Fetch, save, move, and search pages |
| `MediaController` | Upload, list, move, and serve media files |
| `AdminController` | Site/namespace management, ACLs, cache regeneration |
| `AdminUserController` | User management |
| `HistoryController` | Page revision history and diff |
| `ImportExportController` | Wiki import/export |
| `SessionsController` | Login/logout |
| `SiteController` | Per-site settings |
| `ResourceController` | Static template resources (CSS, JS) |
| `PluginController` | Serve plugin toolbar definitions to the UI |
| `SitemapController` | Generate XML sitemaps |
| `WarfileController` | Serve the front-end UI WAR (standalone mode) |
| `VersionController` | Return the running version |
| `CsrfController` | Issue CSRF tokens |
| `UserSettingsController` | Per-user preferences |

---

## Services

Key services and their roles:

| Service | Role |
|---|---|
| `PageService` | Read pages, resolve ACLs, page metadata |
| `PageUpdateService` | Write pages, manage history |
| `PageSearchService` | Full-text search |
| `RenderService` | Orchestrate page rendering (fetch → render → cache) |
| `MacroService` | Scan packages for `@CustomMacro` classes, dispatch macro invocations |
| `PluginService` | Scan packages for `@WikiPlugin` classes, serve toolbar definitions |
| `SiteService` | Resolve hostname → site name, site settings |
| `NamespaceService` | Namespace resolution and ACL checks |
| `MediaService` | File upload/download/metadata |
| `MediaCacheService` | Scaled image caching |
| `LinkService` | Track inter-page links |
| `LinkOverrideService` | Redirect/alias one page name to another — see [Link and Media Override Systems](link-override-system.md) |
| `MediaOverrideService` | Redirect/alias one media file reference to another — see [Link and Media Override Systems](link-override-system.md) |
| `ImageRefService` | Track image usage per page |
| `RegenCacheService` | Rebuild the rendered-page cache in bulk |
| `ExportService` | Produce archive exports |
| `TemplateService` | Load per-site HTML/CSS templates |
| `UserService` | User CRUD and authentication |
| `ActivityLogService` | Audit log |
| `EmailService` | SMTP email sending |
| `TOCRenderService` | Generate table-of-contents from rendered headers |
| `SitemapService` | Build sitemap XML |

---

## Multi-Site Model

A single deployed instance serves multiple wikis. Each wiki is identified by:

- A **site name** (short key, e.g. `"default"`)
- One or more **hostnames** (e.g. `wiki.example.com`)

The `sites` database table maps hostname → site name. The wildcard hostname `*` matches any hostname not explicitly mapped.

`SiteService.getSiteForHostname(host)` resolves the hostname to a site name. This translation happens **once at the controller boundary** (via the `LazerWikiController` base class helpers `getHost` and `getSite`). All services below the controller layer receive the site name directly — no service translates host → site internally.

Per-site configuration (e.g. plugin blocklist, custom settings) is stored as a JSON blob in the `sites.settings` column and accessed via `SiteService.getSettingForHostname`.

---

## Rendering Pipeline

When a page is requested:

1. **`RenderService.getRenderedPage`** checks for a cached render in `page_cache`.
2. On a cache miss, it calls `IMarkupRenderer.renderWithInfo` (implemented by `CustomWikiRenderer`).
3. `CustomWikiRenderer` runs the **syntax engine** on the raw wiki markup.
4. The syntax engine produces a **`RenderResult`** containing rendered HTML and metadata (links found, images used, headers for TOC, etc.).
5. `MacroService` processes `~~MACRO~~name:args~~/MACRO~~` tokens found in the rendered output, replacing them with macro output. Macros outside code blocks are expanded; macros inside code blocks are left as-is.
6. The rendered HTML and metadata are stored in `page_cache` for future requests.

### Page cache invalidation

Cache entries are invalidated when:
- The page itself is saved
- A page that is included via the `include` macro is saved (because the `include` macro marks its host page as uncacheable)

---

## Syntax Engine

The syntax engine lives in `us.calubrecht.lazerwiki.syntax`.

Parsing is line-oriented. Each line (or block) is matched by one or more registered `ITreeParser` implementations. Matched content is turned into an AST node (`ITreeNode`). The full document becomes a tree of nodes which is then walked by `ITreeRenderer` implementations to produce HTML.

The parser/renderer pairs are registered through `ParserRegistrar`. Each construct (headers, bold/italic, links, images, tables, lists, code blocks, block quotes, etc.) has a dedicated parser class and a dedicated renderer class under `syntax/parser/` and `syntax/renderer/` respectively.

---

## Macro System

Macros are wiki-markup constructs of the form:

```
~~MACRO~~macroName:arguments~~/MACRO~~
```

Multi-line macros are also supported:

```
~~MACRO~~macroName:firstLine
secondLine
~~/MACRO~~
```

At startup, `MacroService` scans packages listed in `lazerwiki.plugin.scan.packages` for classes annotated with `@CustomMacro` that extend `Macro`. Each is instantiated via its no-arg constructor and registered.

The `Macro` base class provides:
- `getName()` — the macro identifier
- `render(MacroContext, String)` — returns the HTML fragment
- `getCSS()` — optional CSS injected into every page that uses this macro
- `allowCache(MacroContext, String)` — return `false` to prevent the host page from being cached
- `toArgsMap(String)` — parse `key1=val1&key2=val2` argument strings into a map

The `MacroContext` gives macros access to page queries, rendering utilities, and link tracking without tight coupling to the service layer.

See [PLUGINS.md](PLUGINS.md) for details on writing macros.

---

## Plugin System

Plugins contribute toolbar buttons to the wiki editor. They are scanned at startup from the same packages as macros, looking for classes annotated with `@WikiPlugin` that extend `Plugin`.

Each `Plugin` returns a list of `EditToolbarDef` records. Each toolbar definition carries a name, an icon filename, and a JavaScript function string that the React UI executes when the button is clicked.

Sites can disable individual plugins by name via the `pluginBlacklist` setting.

---

## Security

Authentication uses Spring Security with a custom `LazerWikiAuthenticationManager` and filter (`LazerWikiAuthenticationFilter`). Sessions are cookie-based.

Authorization is ACL-based per namespace. The `NamespaceService` evaluates whether the current user can read or write a given page given the namespace rules configured for the site.

CSRF protection is provided by Spring Security; tokens are exposed via `CsrfController`.

---

## Database Schema

Key tables:

| Table | Purpose |
|---|---|
| `sites` | Hostname → site name mapping, per-site settings JSON |
| `page` | All page revisions (`id + revision` = primary key) |
| `page_ids` | Auto-increment ID source for new pages |
| `page_cache` | Cached rendered HTML per page |
| `links` | Inter-page link graph |
| `image_ref` | Image usage per page |
| `namespaceRestriction` | ACL rules per namespace |
| `userRecord` | User accounts |
| `userRole` | User roles (admin, reader, writer per site) |
| `mediaRecord` | Uploaded file metadata |
| `tags` | Page tags |
| `activityLog` | Audit trail |

Schema SQL: `src/main/sql/create.sql` (MySQL) and `src/main/sql/create.sqlite` (SQLite).
