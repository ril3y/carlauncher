# BattleWithBytes Car Launcher - Findings

## Hardware Button Investigation

### Working Buttons
- **BACK button** - KeyCode 4, captured successfully in our app
  - Can be used for navigation/dismissing overlays

### Non-Working Buttons (Hardcoded by System)
The following buttons are **hardcoded to launch system apps** and cannot be captured:
- **Play/Pause button** - Launches music app
- **Next track button** - Launches music app
- **Previous track button** - Launches music app
- **Music button** - Launches music app
- **Nav button** - Launches navigation app
- **Circle button** - Launches specific app

**Reason**: Aftermarket head units often hardcode physical buttons to specific system apps.

**Solutions**:
1. Use on-screen touch controls instead (recommended)
2. Requires root access to remap buttons
3. Focus on touch-based Tesla-style UI

## Display Characteristics
- **Resolution**: 1280x720 (16:9)
- **Brightness**: Can read/monitor (0-255), no ambient light sensor
- **Day/Night Mode**: Can implement manual toggle

## Next Steps
- Create unified Tesla-style canvas interface
- Implement 3D car visualization
- Integrate map view (OpenGL/3D libraries)
- Use touch gestures for all controls
