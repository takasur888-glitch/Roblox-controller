# DualStick Overlay

An experimental Android floating touch-control prototype.

## Permissions and privacy

- The app uses an Android Accessibility Service to show floating accessibility overlays and dispatch gestures when the user interacts with its controls.
- It explicitly declares `isAccessibilityTool=false`; this app is a game-control utility, not an accessibility tool for disability support.
- It does not request permission to retrieve window or screen content.
- The service is configured to receive no accessibility event types.
- The app has no network permission and contains no analytics or data-upload code.
- Gesture access is sensitive. The service uses it only to dispatch touch gestures in response to direct user interaction with the on-screen controls. It does not read screen text, inspect other apps, or make decisions on the user's behalf.
- The service is not marked as an accessibility tool; the app does not claim to support disability-related accessibility needs.
- Disabling the service in Android Accessibility settings removes the controls.

## Project status

This is an experimental prototype, not a finished or Play Protect-verified product. Android's gesture-dispatch API may not support the sustained, independent simultaneous joystick behavior this project aims for. Real-device testing and further engineering are required. A source-code change does not guarantee that Google Play Protect will allow sideload installation.
