# Link and Media Override Systems

## Problem

When a wiki page or media file is moved (renamed), every page that references the old name contains stale markup. Rewriting every source page's markup immediately on each move would be expensive and would pollute page history.

Both override systems solve this with a deferred redirect table: redirect rules are recorded at move time and applied transparently at render time. `LinkOverrideService` handles internal page links; `MediaOverrideService` handles image/media references. They are structurally identical.

---

## Data Models

### `LinkOverride` (`link_overrides` table)

| Field | Meaning |
|---|---|
| `site` | Which wiki site |
| `sourcePageNS` / `sourcePageName` | The page whose markup contains the stale link |
| `targetPageNS` / `targetPageName` | The original (old) link target as written in that page's markup |
| `newTargetPageNS` / `newTargetPageName` | Where that link should actually go now |

The `links` table (managed by `LinkService`) records the live link graph from the most recently saved markup. `link_overrides` holds redirect rules surviving from past moves.

### `MediaOverride` (`media_overrides` table)

Same structure, but the target fields refer to media file namespace/name rather than page namespace/name:

| Field | Meaning |
|---|---|
| `site` | Which wiki site |
| `sourcePageNS` / `sourcePageName` | The page whose markup references the stale image |
| `targetFileNS` / `targetFileName` | The original (old) media reference as written in that page's markup |
| `newTargetFileNS` / `newTargetFileName` | Where that image reference should resolve now |

The `image_ref` table (managed by `ImageRefService`) records the live image-usage graph. `media_overrides` holds redirect rules from past media moves.

---

## Lifecycle

### On page move (`PageMetaService.moveMetaData`)

Both services are called in the same sequence:

1. **`createOverride(host, oldPage, newPage)`** *(link)* — Queries `links` for every page that currently points at `oldPage`. For each source page, writes a `LinkOverride` redirecting `oldPage → newPage`. Also chains multi-hop moves: any existing override whose `newTarget` was `oldPage` is updated to point to `newPage` instead.

2. **`moveOverrides(host, oldPage, newPage)`** *(both)* — Transfers overrides *owned by* the moved page. If `oldPage` itself had outbound overrides (because its links were rewritten by a prior move), those overrides are reattached under `newPage`'s identity.

### On media file move (`MediaService.moveMedia`)

**`MediaOverrideService.createOverride(host, oldFileNS, oldFileName, newFileNS, newFileName)`** — Queries `image_ref` for every page that currently references the old file. For each source page, writes a `MediaOverride` redirecting the old file reference to the new location. Also chains multi-hop moves the same way as the link equivalent. Note: `createOverride` for media takes explicit namespace/name parameters rather than a page descriptor string, because media is moved by `MediaService` directly rather than through `PageMetaService`.

### On page save or delete (`PageMetaService.updateMetaData` / `deleteMetaData`)

**`deleteOverrides(host, page)`** *(both)* — Removes all overrides sourced from the given page. When a page is saved, its markup is fresh and any stale redirect rules from it are no longer needed.

---

## Render-Time Application

### Links (`LinkRenderer.doOverrides`)

When `LinkRenderer` renders an internal wiki link:

1. On the first link in a page, calls `LinkOverrideService.getOverrides(host, page)` and caches the result in `RenderContext.renderState` under `LINK_OVERRIDES` (one DB query per page render, not per link).
2. If the link target matches an override rule, the target is silently replaced with `newTarget` before the `<a>` tag is emitted.
3. Each applied override is recorded in `renderState` under `OVERRIDE_STATS` as a `LinkOverrideInstance` (old target, new target, source position), so callers can surface in-page warnings about rewritten links.

`MacroService` also calls `getOverrides` the same way when macros need to resolve links during expansion.

### Images (`ImageRenderer.doOverrides`)

Identical pattern: on the first image in a page, `MediaOverrideService.getOverrides(host, page)` is called and cached in `renderState` under `MEDIA_OVERRIDES`. Matching image references are silently replaced and also recorded in `OVERRIDE_STATS` as `LinkOverrideInstance` records (the same type is reused for both link and media stats).

---

## Backlinks and Cache Invalidation

### Link backlinks (`PageService`)

When fetching page data, `PageService` computes backlinks by merging two sources:

- **Direct backlinks**: from the `links` table (pages whose current markup names this page).
- **Override backlinks**: from `getOverridesForNewTargetPage` — pages whose link overrides have this page as `newTarget`, i.e. pages that used to link to an old name now redirecting here.

Both lists are unioned and filtered by namespace ACL before being returned to the UI.

### "Page moved" message

When a page has been deleted but still has `LinkOverride` records where it is the original `targetPage`, `PageService` treats that as a "page has moved" condition and returns a synthetic page body:

```
This page has been moved to [[newPage]] (newPage)
```

There is no equivalent for media files.

### Cache regeneration (`RegenCacheService`)

- After a **page move**, `regenCachesForBacklinks` uses `getOverridesForNewTargetPage` to find pages with overrides pointing to the moved page, then invalidates their render caches.
- After a **media move**, `regenCachesForImageRefs` uses `getOverridesForImage` to find pages with media overrides pointing to the moved file, then invalidates their render caches.

In both cases the goal is the same: ensure pages that display stale-but-overridden references are re-rendered with fresh override rules on next request.

---

## Flow Summary

```
Page moved: A → B                       Media file moved: img1 → img2
│                                       │
├── LinkOverrideService.createOverride  ├── MediaOverrideService.createOverride
│     for every page X linking to A:   │     for every page X referencing img1:
│     write (source=X, A→B)            │     write (source=X, img1→img2)
│                                       │
├── LinkOverrideService.moveOverrides   ├── MediaOverrideService.moveOverrides
│     reattach A's own overrides to B  │     (called via PageMetaService on page move,
│                                       │      not applicable to media-only moves)
└── At render time (page X requested): └── At render time (page X requested):
      LinkRenderer fetches overrides         ImageRenderer fetches overrides
      → finds (A→B)                          → finds (img1→img2)
      → emits link to B                      → emits src pointing to img2
      → records in OVERRIDE_STATS            → records in OVERRIDE_STATS
```

---

## Related Components

| Component | Role |
|---|---|
| `LinkOverrideService` | CRUD for link override records |
| `MediaOverrideService` | CRUD for media override records |
| `LinkOverrideRepository` | JPA repository for `link_overrides` table |
| `MediaOverrideRepository` | JPA repository for `media_overrides` table |
| `LinkRenderer` | Applies link overrides at render time; records stats |
| `ImageRenderer` | Applies media overrides at render time; records stats |
| `PageMetaService` | Calls create/move/delete on both services during page lifecycle events |
| `MediaService` | Calls `MediaOverrideService.createOverride` on media file move |
| `PageService` | Merges override backlinks; generates "moved" page content |
| `RegenCacheService` | Uses both override services to determine which caches to invalidate |
| `MacroService` | Applies link overrides when macros resolve links |
