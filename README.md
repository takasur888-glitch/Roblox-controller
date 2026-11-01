# DualStick Overlay

An experimental Android floating touch-control app for Roblox mobile.

## Controls

- **MOVE**: sends a left-side touch at the assumed Roblox Fixed Thumbstick position.
- **CAMERA**: continuously moves a simulated right-side touch in the direction held on the overlay. Camera drag speed can be set to Slow, Normal, or Fast in the app.
- **JUMP**: sends a tap to the assumed lower-right jump-button position.
- **STOP**: releases the simulated touch controls and pauses the overlay controls. Tap START to resume.

The MOVE and CAMERA outputs are submitted together in one Android GestureDescription. The implementation uses Android continued strokes to keep simulated touch pointers down across short gesture segments. This is designed to avoid the previous issue where separately dispatching the left and right sticks cancelled one another. JUMP is included in the same gesture segment when possible.

## Setup

1. Install the debug APK.
2. Open DualStick Overlay and enable its Accessibility Service in Android Settings.
3. Return to Roblox and use the Fixed Thumbstick control layout.
4. Test MOVE, CAMERA, and JUMP in a safe area before using them in a live match.
5. Use STOP immediately if touches land in the wrong places or controls behave unexpectedly.

The default touch coordinates are estimates. Different screen sizes, Roblox UI scaling, game layouts, and Android navigation modes can change where the game's movement and jump controls appear. The initial version does not yet include coordinate calibration or draggable overlay placement.

## Permissions and privacy

- Uses an Android Accessibility Service to display floating overlays and dispatch touch gestures.
- Declares `isAccessibilityTool=false`; this is a game-control utility, not a disability-support accessibility tool.
- Does not request access to retrieve screen/window contents.
- Subscribes to no accessibility events.
- Has no internet permission, analytics, or data-upload code.
- Gesture access is sensitive. The service should be enabled only when the user understands and trusts the app.
- Disabling the service in Android Accessibility settings removes the floating controls.

## Project status and limitations

This is an experimental prototype, not a finished or Play Protect-verified product. Continued-stroke support is documented by Android, but actual simultaneous input, touch targeting, sustained camera movement, and responsiveness must still be tested on physical devices and in Roblox. A successful APK build does not prove that the controls work correctly in-game. Sideload installation may trigger Android or Play Protect warnings; do not disable device protection to install it.
