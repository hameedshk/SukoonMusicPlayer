# Albums Tab Enhancement Plan

## Summary
Add selection mode UI and recently played albums to AlbumsScreen, reusing existing patterns from HomeScreen and SearchScreen.

## Requirements
1. **Recently Played Albums** - Display at top (reuse HomeScreen pattern)
2. **Album Row Actions**:
   - Click row → Navigate to AlbumDetailScreen ✓ (already wired)
   - 3-dot menu → Bottom sheet with 7 actions (a2 snapshot):
     - Play, Play next, Add to queue, Add to playlist
     - Edit tags, Change cover, Delete from device
3. **Selection Mode** - Icon next to sort triggers multi-select (like Spotify a4)
4. **Selection Actions** - Bottom bar: Play | Add to playlist | Delete | More
5. **Add to Playlist** - Bottom sheet dialog (reuse AddToPlaylistDialog)

## File Changes

### 1. AlbumsScreen.kt (MODIFY)
**Template**:
- HomeScreen.kt:2303-2322 (RecentlyPlayedAlbumsSection)
- SongContextMenu.kt (context menu pattern)

**Changes**:
- Add `RecentlyPlayedAlbumsSection` component (copy from HomeScreen)
- Replace DropdownMenu with `AlbumContextMenuBottomSheet` (7 actions per a2)
- Add selection mode icon button in TopAppBar (next to title)
- Add `AlbumSelectionBottomBar` component with 4 actions
- Modify `AlbumsGrid` to support checkbox selection when in selection mode
- Add `AddToPlaylistDialog` integration
- Add context menu states and handlers

**State Already in ViewModel** (No changes needed):
- `isSelectionMode` ✓
- `selectedAlbumIds` ✓
- `recentlyPlayedAlbums` ✓
- Functions: `toggleSelectionMode`, `toggleAlbumSelection`, `selectAllAlbums`, `clearSelection`, `playSelectedAlbums` ✓

**New State Needed**:
- `showPlaylistDialog: Boolean` (local state)
- `showContextMenu: Boolean` (local state)
- `selectedSongsForPlaylist: List<Song>` (derived from selected albums)

### 2. AlbumsViewModel.kt (MODIFY - Minor)
**Template**: AlbumsViewModel.kt (current)

**Changes**:
- Add `playAlbumNext(albumId)` - add album songs to front of queue
- Add `addAlbumToQueue(albumId)` - add album songs to end of queue
- Add `addSelectedAlbumsToQueue()` function
- Add `deleteSelectedAlbums()` function (calls SongRepository)
- Add `getSelectedAlbumSongs()` helper function
- Add `getSongsForAlbum(albumId)` helper (for playlist dialog)

### 3. Components to Reuse (NO CHANGES)
- `AddToPlaylistDialog.kt` ✓ (Lines 21-94)
- `RecentlyPlayedAlbumsSection` from HomeScreen ✓ (will copy)
- `SongMenuHandler` pattern ✓

## Implementation Steps

### Step 1: Add Recently Played Section
```kotlin
// In AlbumsScreen.kt, after TopAppBar
// Insert RecentlyPlayedAlbumsSection above main grid
// Condition: !isSelectionMode && recentlyPlayedAlbums.isNotEmpty()
```

### Step 2: Add Selection Mode Icon
```kotlin
// In TopAppBar actions
IconButton(onClick = { viewModel.toggleSelectionMode(true) }) {
    Icon(Icons.Default.CheckCircle, "Select")
}
```

### Step 3: Create AlbumContextMenuBottomSheet
```kotlin
// Replace DropdownMenu in AlbumCard with ModalBottomSheet
// Show album info header (icon, title, "X songs")
// 7 menu items:
//   - Play → viewModel.playAlbum(albumId)
//   - Play next → viewModel.playAlbumNext(albumId)
//   - Add to queue → viewModel.addAlbumToQueue(albumId)
//   - Add to playlist → show AddToPlaylistDialog
//   - Edit tags → TODO (show toast for now)
//   - Change cover → TODO (show toast for now)
//   - Delete from device → show confirmation dialog
```

### Step 4: Modify AlbumsGrid
```kotlin
// Add checkbox parameter to AlbumCard
// Show checkbox when isSelectionMode = true
// Handle click -> toggleAlbumSelection(album.id)
// 3-dot menu triggers AlbumContextMenuBottomSheet
```

### Step 5: Add Selection Bottom Bar
```kotlin
// In Scaffold bottomBar (when isSelectionMode)
Row with 4 buttons:
- Play: calls playSelectedAlbums()
- Add to playlist: shows AddToPlaylistDialog
- Delete: confirms then calls deleteSelectedAlbums()
- More: shows DropdownMenu (Play next, Add to queue)
```

### Step 6: Wire Playlist Dialog
```kotlin
// Fetch playlists from PlaylistViewModel
// On selection -> addSongsToPlaylist(playlistId, songs)
// Get songs via: selectedAlbumIds.flatMap { getSongsByAlbumId(it) }
```

## Code Reuse Map

| Feature | Template File | Lines | Reuse Method |
|---------|--------------|-------|--------------|
| Recently Played UI | HomeScreen.kt | 2303-2322 | Copy component |
| Album Context Menu (a2) | SongContextMenu.kt | Pattern | Create AlbumContextMenu |
| Selection Bottom Bar | HomeScreen.kt | 183-189 | Copy pattern |
| Add to Playlist Dialog | AddToPlaylistDialog.kt | 21-94 | Direct import |
| Album Card with Checkbox | AlbumCard | 122-259 | Modify existing |

## Architecture Compliance

✅ **MVVM**: UI observes StateFlows, ViewModel handles business logic
✅ **Clean Architecture**: Repository access via ViewModel only
✅ **Reusability**: No code duplication, reusing 3 existing components
✅ **Single Source of Truth**: MediaController via PlaybackRepository

## Testing Checklist
- [ ] Recently played albums appear when present
- [ ] Selection icon triggers selection mode
- [ ] Checkboxes appear on album cards
- [ ] Select all works
- [ ] Play selected albums works
- [ ] Add to playlist shows dialog and adds songs
- [ ] Delete confirmation works
- [ ] More menu shows options
- [ ] Exit selection mode clears selection

## Estimated Changes
- **AlbumsScreen.kt**: +200 lines (components + context menu)
- **AlbumsViewModel.kt**: +50 lines (6 functions)
- **Total**: ~250 lines, ~75% reused code

## UI Flow Summary
**Normal Mode**:
- Recently played albums (LazyRow) at top
- Album grid below
- Click album → AlbumDetailScreen
- Click 3-dot → AlbumContextMenuBottomSheet (a2)

**Selection Mode** (triggered by icon):
- Search bar + "Select all" row
- Albums with checkboxes
- Bottom bar: Play | Add to playlist | Delete | More
