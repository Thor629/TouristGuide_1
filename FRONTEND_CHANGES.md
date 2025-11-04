# Frontend Changes - Admin Edit/Delete Functionality

## Overview
This document describes the changes made to the Android frontend to support admin-only edit and delete functionality for places.

## Changes Made

### 1. Data Models (`Place.kt`)

#### Added PlacePermissions Data Class
```kotlin
data class PlacePermissions(
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
    val isOwner: Boolean = false,
    val isAdmin: Boolean = false
)
```

#### Updated Place Model
- Added `permissions: PlacePermissions?` field to the `Place` data class
- This field is populated by the backend API and indicates what actions the current user can perform

#### Added UserInfo Data Class
```kotlin
data class UserInfo(
    val id: String,
    val role: String,
    val isAdmin: Boolean
)
```

#### Updated Response Models
- `PlaceResponse` now includes `user: UserInfo?` field
- `SinglePlaceResponse` now includes `user: UserInfo?` field

### 2. UI Layout (`item_place_card.xml`)

#### Added Admin Action Buttons
Added a new LinearLayout containing Edit and Delete buttons:
- **Edit Button**: Material outlined button with edit icon
- **Delete Button**: Material outlined button with delete icon in red color
- Both buttons are hidden by default (`visibility="gone"`)
- Buttons are shown only when user has appropriate permissions

### 3. PlacesAdapter (`PlacesAdapter.kt`)

#### Updated Constructor
```kotlin
class PlacesAdapter(
    private val onPlaceClick: (Place) -> Unit,
    private val onEditClick: ((Place) -> Unit)? = null,
    private val onDeleteClick: ((Place) -> Unit)? = null
)
```

#### Added Permission-Based Button Visibility
In the `bind()` method:
```kotlin
val permissions = place.permissions
if (permissions != null && (permissions.canEdit || permissions.canDelete)) {
    llAdminActions.visibility = android.view.View.VISIBLE
    
    // Show edit button if user can edit
    btnEdit.visibility = if (permissions.canEdit) {
        android.view.View.VISIBLE
    } else {
        android.view.View.GONE
    }
    
    // Show delete button if user can delete
    btnDelete.visibility = if (permissions.canDelete) {
        android.view.View.VISIBLE
    } else {
        android.view.View.GONE
    }
    
    // Set click listeners
    btnEdit.setOnClickListener {
        onEditClick?.invoke(place)
    }
    
    btnDelete.setOnClickListener {
        onDeleteClick?.invoke(place)
    }
} else {
    llAdminActions.visibility = android.view.View.GONE
}
```

### 4. MainActivity (`MainActivity.kt`)

#### Updated PlacesAdapter Initialization
```kotlin
placesAdapter = PlacesAdapter(
    onPlaceClick = { place ->
        openPlaceDetails(place)
    },
    onEditClick = { place ->
        editPlace(place)
    },
    onDeleteClick = { place ->
        confirmDeletePlace(place)
    }
)
```

#### Added Edit Functionality
```kotlin
private fun editPlace(place: Place) {
    val intent = Intent(this, AddPlaceActivity::class.java)
    intent.putExtra("PLACE_ID", place.id)
    intent.putExtra("EDIT_MODE", true)
    startActivity(intent)
}
```

#### Added Delete Functionality
```kotlin
private fun confirmDeletePlace(place: Place) {
    MaterialAlertDialogBuilder(this)
        .setTitle("Delete Place")
        .setMessage("Are you sure you want to delete \"${place.name}\"? This action cannot be undone.")
        .setPositiveButton("Delete") { _, _ ->
            deletePlace(place)
        }
        .setNegativeButton("Cancel", null)
        .show()
}

private fun deletePlace(place: Place) {
    showLoading()
    lifecycleScope.launch {
        try {
            val response = RetrofitClient.apiService.deletePlace(place.id)
            if (response.isSuccessful && response.body()?.success == true) {
                showToast("Place deleted successfully")
                loadPlaces() // Reload the list
            } else {
                val errorMessage = response.body()?.message ?: "Failed to delete place"
                showToast(errorMessage)
            }
        } catch (e: Exception) {
            showToast("Error: ${e.message}")
        } finally {
            hideLoading()
        }
    }
}
```

## How It Works

### Permission Flow

1. **User logs in** → Backend returns JWT token with user role
2. **App fetches places** → Backend includes `permissions` object for each place
3. **Adapter displays places** → Shows edit/delete buttons based on permissions
4. **User clicks edit/delete** → App performs action if authorized

### Backend Integration

The frontend now expects the following response format from the backend:

```json
{
  "success": true,
  "count": 10,
  "data": [
    {
      "_id": "place_id",
      "name": "Place Name",
      "location": "Location",
      "description": "Description",
      "images": ["image1.jpg"],
      "category": {...},
      "addedBy": {...},
      "permissions": {
        "canEdit": true,
        "canDelete": true,
        "isOwner": false,
        "isAdmin": true
      }
    }
  ],
  "user": {
    "id": "user_id",
    "role": "admin",
    "isAdmin": true
  }
}
```

### Authorization Rules

- **Admin users**: Can edit and delete ANY place
- **Regular users**: Can only edit and delete their OWN places
- **Guest users**: Cannot see edit/delete buttons (no permissions object)

## UI/UX Features

### Edit Button
- Icon: Edit icon (pencil)
- Color: Primary color (outlined)
- Action: Opens AddPlaceActivity in edit mode
- Visibility: Shown only if `permissions.canEdit == true`

### Delete Button
- Icon: Delete icon (trash)
- Color: Red (outlined)
- Action: Shows confirmation dialog, then deletes place
- Visibility: Shown only if `permissions.canDelete == true`

### Confirmation Dialog
- Title: "Delete Place"
- Message: "Are you sure you want to delete \"[Place Name]\"? This action cannot be undone."
- Buttons: "Delete" (positive), "Cancel" (negative)

## Testing

### Test Scenarios

1. **As Admin User:**
   - Login with admin credentials
   - View places list
   - Verify edit and delete buttons appear on ALL place cards
   - Click edit → Should open edit screen
   - Click delete → Should show confirmation → Delete place

2. **As Regular User:**
   - Login with regular user credentials
   - View places list
   - Verify edit and delete buttons appear ONLY on own places
   - Try to edit own place → Should work
   - Try to delete own place → Should work

3. **As Guest (Not Logged In):**
   - View places list
   - Verify NO edit/delete buttons appear
   - All places are view-only

## Backend Requirements

The backend must:
1. Include `permissions` object in place responses
2. Include `user` object in place responses (when authenticated)
3. Validate authorization on edit/delete endpoints
4. Return appropriate error messages for unauthorized actions

## Security Notes

- Frontend permission checks are for UX only
- Backend MUST validate all edit/delete requests
- JWT token is sent with all authenticated requests
- Attempting to edit/delete without permission will fail on backend

## Future Enhancements

Potential improvements:
- Add edit/delete buttons in PlaceDetailsActivity
- Add bulk delete for admins
- Add place status indicators (approved/pending)
- Add undo functionality for delete
- Add edit history tracking

## Files Modified

1. `app/src/main/java/com/touristguide/app/data/model/Place.kt`
   - Added `PlacePermissions` data class
   - Added `UserInfo` data class
   - Updated `Place`, `PlaceResponse`, and `SinglePlaceResponse`

2. `app/src/main/res/layout/item_place_card.xml`
   - Added admin action buttons layout
   - Added edit and delete MaterialButtons

3. `app/src/main/java/com/touristguide/app/ui/main/PlacesAdapter.kt`
   - Added edit and delete callbacks
   - Added permission-based button visibility logic

4. `app/src/main/java/com/touristguide/app/ui/main/MainActivity.kt`
   - Added `editPlace()` method
   - Added `confirmDeletePlace()` method
   - Added `deletePlace()` method
   - Updated adapter initialization

## API Endpoints Used

- `DELETE /api/places/:id` - Delete a place
- `PUT /api/places/:id` - Update a place (via AddPlaceActivity)
- `GET /api/places` - Get places with permissions

## Dependencies

No new dependencies were added. The implementation uses existing libraries:
- Material Components (for buttons and dialogs)
- Retrofit (for API calls)
- Kotlin Coroutines (for async operations)
- ViewBinding (for view access)
