# DualStick Overlay

An experimental Android floating touch-control prototype.

## Permissions and privacy

- The app uses an Android Accessibility Service to show floating accessibility overlays and dispatch gestures when the user interacts with its controls.
- It does not request permission to retrieve window or screen content.
- The service is configured to receive no accessibility event types.
- The app has no network permission and contains no analytics or data-upload code.
- Gesture access is sensitive. Only enable the service if you understand and trust the app.
- Disabling the service in Android Accessibility settings removes the controls.

## Project status

This is an experimental prototype, not a finished or Play Protect-verified product. Android's gesture-dispatch API may not support the sustained, independent simultaneous joystick behavior this project aims for. Real-device testing and further engineering are required. A source-code change does not guarantee that Google Play Protect will allow sideload installation.
