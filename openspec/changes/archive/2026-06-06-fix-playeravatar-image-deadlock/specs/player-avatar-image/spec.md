## ADDED Requirements

### Requirement: Player avatar renders photo from URL
The system SHALL display a player's photo image when a valid photo URL is provided. While the image is loading, the component SHALL display the default initial-letter placeholder behind a visually hidden `<img>` element. On load error, the component SHALL fall back to the initial-letter placeholder.

#### Scenario: Valid photo URL provided
- **WHEN** the component receives a `photo` prop with a valid `https://media.api-sports.io/...` URL
- **THEN** the component SHALL render an `<img>` element with that URL as `src`
- **AND** the `<img>` element SHALL remain in the DOM throughout the loading process so that `onLoad` and `onError` callbacks fire correctly

#### Scenario: Photo loads successfully
- **WHEN** the `<img>` element fires its `onLoad` callback
- **THEN** the component SHALL display the loaded image visibly

#### Scenario: Photo fails to load
- **WHEN** the `<img>` element fires its `onError` callback
- **THEN** the component SHALL display the initial-letter placeholder as fallback

#### Scenario: No photo prop provided
- **WHEN** the component receives a `photo` prop that is `null`, `undefined`, or an empty string
- **THEN** the component SHALL display the initial-letter placeholder

#### Scenario: Invalid photo URL
- **WHEN** the component receives a `photo` prop that does not start with `https://`, `http://`, or `data:image/`
- **THEN** the component SHALL display the initial-letter placeholder
