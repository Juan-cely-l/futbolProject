## Why

PlayerAvatar.jsx has a React state deadlock: the initial status is `'loading'` but the `<img>` element only mounts when `status === 'loaded'`. Since `<img>` never mounts, `onLoad` never fires, and the user always sees initial-letter fallback instead of the player photo — even though photo URLs are correctly stored in the database and served by the backend.

## What Changes

- **PlayerAvatar.jsx**: Fix the render condition so `<img>` mounts when status is `'loading'` or `'loaded'`, and falls back to initials only on `'error'`
- No API, backend, or database schema changes — the data pipeline is already correct

## Capabilities

### New Capabilities

- `player-avatar-image`: Renders a player's photo from URL when available; shows initial-letter fallback on load failure or missing URL. Handles loading, error, and empty states correctly.

### Modified Capabilities

None — no existing specs in this project.

## Impact

- **Files changed**: `frontend/src/components/PlayerAvatar.jsx` only
- **No backend changes**: API returns correct photo URLs, DB has correct data
- **No API contract changes**: PlayerResponse.photo field unchanged
