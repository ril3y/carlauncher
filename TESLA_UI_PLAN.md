# Tesla-Style Unified Interface Plan

## Design Philosophy
**Single integrated canvas** - No tabs, no grids. Everything on one screen like Tesla Model 3/Y.

## Screen Layout (1280x720)

```
┌─────────────────────────────────────────────────────────────┐
│  Battery: 78%    Time: 6:19 PM         Temp: 25°C          │ ← Status Bar
├─────────────────────────────────────────────────────────────┤
│         │                                 │                  │
│  Map    │         SPEED: 65              │  Power Graph     │
│  Nav    │         ────────               │  ─────────       │
│  3D     │          km/h                  │  Real-time       │
│         │                                │  Energy Usage    │
│         │      [3D Car View]             │                  │
│         │     /─────────────\            │                  │
│  Route  │    │    ┌───┐     │            │  Efficiency      │
│  Info   │    │    └───┘     │            │  kW/h            │
│         │     \─────────────/            │                  │
│         │                                │                  │
├─────────┴────────────────────────────────┴──────────────────┤
│  P R N D          Range: 62 km         Power: 2.5 kW       │ ← Bottom Bar
└─────────────────────────────────────────────────────────────┘
```

## Implementation Strategy

### Phase 1: Core Layout (This iteration)
- [ ] Create single-screen unified layout
- [ ] Large centered speed display
- [ ] Bottom bar with gear/stats
- [ ] Top status bar (always visible)

### Phase 2: 3D Car Visualization
**Library**: Filament or SceneView
- [ ] Load 3D golf cart model (.glb format)
- [ ] Rotate/animate based on steering
- [ ] Show door open/close states
- [ ] Highlight based on vehicle status

### Phase 3: Map Integration
**Library**: Mapbox GL or Tangram ES
- [ ] 3D map view (left side)
- [ ] Navigation routing
- [ ] Real-time position tracking
- [ ] Integrate with CAN speed/location

### Phase 4: Power Graph
**Library**: Custom Canvas or Vico
- [ ] Real-time energy consumption
- [ ] Historical power usage
- [ ] Efficiency metrics
- [ ] Battery discharge rate

### Phase 5: Gestures & Interactions
- [ ] Swipe left → Show full map
- [ ] Swipe right → Show full energy details
- [ ] Tap car → Vehicle info overlay
- [ ] Pinch to zoom map
- [ ] Long press for settings

## 3D Model Requirements
- **Golf cart 3D model** (.glb or .gltf format)
- Low poly count (mobile optimized)
- Separate components for doors, lights, etc.
- PBR materials for realistic rendering

## Dependencies to Add
```kotlin
// Filament 3D rendering
implementation("com.google.android.filament:filament-android:1.x.x")

// Mapbox for 3D maps
implementation("com.mapbox.maps:android:11.x.x")

// Charts (if not using custom Canvas)
implementation("com.patrykandpatrick.vico:compose:1.x.x")

// 3D model loading
implementation("com.google.ar.sceneform:core:1.x.x")
```

## Performance Targets
- 60 FPS constant
- < 100ms CAN data update
- Smooth 3D rendering
- Instant touch response

## Next Immediate Steps
1. Redesign main screen to single unified layout
2. Add placeholder for 3D car (will replace with actual 3D later)
3. Create custom Canvas power graph
4. Implement swipe gestures
