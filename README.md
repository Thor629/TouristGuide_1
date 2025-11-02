<<<<<<< HEAD
# Tourist Guide Android Ap
=======
# Tourist Guide Android ApP
>>>>>>> 46fc1ae (Your commit message)

Native Android app built with Kotlin for browsing tourist places in Surat.

## Features

✅ Splash screen with auto-navigation
✅ Login/Register with validation
✅ Browse places in grid layout
✅ Search and filter functionality
✅ Place details with image gallery
✅ Like/Unlike system
✅ Reviews and ratings
✅ Add new places with images
✅ Admin panel for approvals
✅ Persistent login
✅ Material Design UI

## Requirements

- Android Studio Hedgehog or later
- Min SDK: 24 (Android 7.0)
- Target SDK: 34 (Android 14)
- Kotlin 1.9+
- Gradle 8.0+

## Setup

1. Open project in Android Studio
2. Wait for Gradle sync
3. Update API URL in `app/build.gradle` if needed:
   - For emulator: `10.0.2.2:5000` (default)
   - For physical device: Your computer's IP address
4. Run the app

## Project Structure

```
app/src/main/
├── java/com/touristguide/app/
│   ├── data/
│   │   ├── api/          # Retrofit API service
│   │   └── model/        # Data models
│   ├── ui/
│   │   ├── splash/       # Splash screen
│   │   ├── auth/         # Login/Register
│   │   ├── main/         # Home screen
│   │   ├── placedetails/ # Place details
│   │   ├── addplace/     # Add place
│   │   ├── liked/        # Liked places
│   │   └── admin/        # Admin panel
│   └── utils/            # Helper classes
└── res/
    ├── layout/           # XML layouts
    ├── drawable/         # Icons and graphics
    └── values/           # Strings, colors, themes
```

## Architecture

- MVVM pattern
- Repository pattern for data
- Coroutines for async operations
- LiveData for reactive UI
- ViewBinding for view access

## Libraries Used

- **Retrofit** - REST API client
- **Glide** - Image loading
- **OkHttp** - HTTP client
- **Coroutines** - Async programming
- **Material Components** - UI components
- **ViewPager2** - Image slider
- **Socket.IO** - Real-time updates

## Configuration

### Backend URL
Update in `app/build.gradle`:
```gradle
buildConfigField "String", "API_BASE_URL", '"http://YOUR_IP:5000/api/"'
```

### Permissions
Required permissions (auto-requested):
- Internet access
- Storage access (for image uploads)
- Camera access (optional)

## Building

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
```bash
./gradlew assembleRelease
```

## Testing

Run app on:
- Android Emulator (API 24+)
- Physical device (Android 7.0+)

## Troubleshooting

**Gradle sync failed:**
- Clean project: `Build → Clean Project`
- Rebuild: `Build → Rebuild Project`

**Cannot connect to backend:**
- Check backend is running
- Verify API URL in build.gradle
- For physical device: Use computer's IP, not localhost

**Image upload fails:**
- Grant storage permissions in app settings
- Check file size (max 5MB)

## Screens

1. **Splash Screen** - App logo with auto-navigation
2. **Auth Screen** - Login/Register toggle
3. **Home Screen** - Browse, search, filter places
4. **Place Details** - Gallery, info, reviews, likes
5. **Add Place** - Form to submit new places
6. **Liked Places** - User's favorite places
7. **Admin Panel** - Approve/reject places (admin only)
