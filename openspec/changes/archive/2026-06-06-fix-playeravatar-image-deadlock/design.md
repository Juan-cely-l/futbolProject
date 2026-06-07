## Context

`PlayerAvatar` is a reusable React component used in three places: `PlayerCard`, `Players` table, and `PlayerProfile`. It receives a `photo` URL string from the `PlayerResponse` API and is responsible for either rendering the image or showing initial-letter fallback.

The current implementation has a state deadlock:
1. Initial status is `'loading'` (when `photo` is truthy)
2. `<img>` only renders when `status === 'loaded'`
3. Since `<img>` never mounts, `onLoad` never fires
4. Status never transitions to `'loaded'`
5. User always sees `<span>` with initials

The data pipeline (API → DB → backend JSON) is verified correct — every synced player has a valid `https://media.api-sports.io/...` URL.

## Goals / Non-Goals

**Goals:**
- Fix the render logic so the player photo appears when a valid URL exists
- Preserve fallback to initials when no URL or load error
- Keep the component self-contained (no new dependencies)
- Maintain the existing `isValidPhotoUrl` validation

**Non-Goals:**
- No changes to how photos are fetched, stored, or served by the backend
- No CSS/image-processing changes (object-fit, borders, etc. stay as-is)

## Decisions

1. **Mount `<img>` on `'loading'` or `'loaded'` instead of only `'loaded'`**
   - The `onLoad`/`onError` callbacks need a mounted DOM element to fire
   - Keeping `<img>` mounted during loading allows the browser to download and render the image naturally
   - Visually hidden during loading via `display: none` on the `<img>` wrapper, swapping to initials or visible `<img>` when state resolves

2. **Keep the `status` state machine (`loading` → `loaded`/`error`)**
   - The existing pattern is correct conceptually — only the render condition was wrong
   - No need to introduce `useRef` or CSS-based approaches for this simple case

3. **Single file change only**
   - The bug is isolated to the render ternary in `PlayerAvatar.jsx`
   - No prop changes, no new components, no CSS changes needed

## Risks / Trade-offs

- **[Low] Brief flash of initials before image loads**: The component shows initials while status is `'loading'`. On a fast connection the swap to `<img>` is near-instant. On slow connections the initials remain visible as a placeholder, which is acceptable UX.
- **[Low] Broken image URL fallback**: If the API-Football image server is down, `onError` triggers and initials display. Already handled by existing code.
