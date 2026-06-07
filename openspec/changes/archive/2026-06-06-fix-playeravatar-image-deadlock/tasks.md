## 1. Fix PlayerAvatar render logic

- [x] 1.1 Change render condition from `status === 'loaded'` to `status !== 'error'` so `<img>` mounts during loading state
- [x] 1.2 Verify `onLoad` fires and transitions status to `'loaded'`, making the image visible
- [x] 1.3 Verify `onError` transitions status to `'error'`, showing initial-letter fallback
- [x] 1.4 Verify null/empty/invalid URL still shows initials immediately

## 2. Verify in browser

- [ ] 2.1 Navigate to Players page and confirm photos render in table and card views
- [ ] 2.2 Open a PlayerProfile and confirm the large avatar shows the photo
- [ ] 2.3 Confirm players without photos still show initials fallback
