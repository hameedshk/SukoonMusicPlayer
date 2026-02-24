# Go Premium Section Redesign Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Redesign the Go Premium settings card to match the approved snapshot with accent border, gold star icon, and two distinct states (premium vs non-premium).

**Architecture:**
- Add optional `accentBorder` parameter to `SettingsGroupCard` for visual distinction
- Redesign Go Premium row using `SettingsRowModel` with custom trailing content logic
- Implement two states: "Premium Active" (disabled, green checkmark) and price display (clickable, opens dialog)
- Star icon in warm gold color with conditional right-side content based on `isPremium` flag

**Tech Stack:** Jetpack Compose, Material 3, SettingsGroupCard, SettingsRowModel, StateFlow

---

## Task 1: Add String Resources

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

**Step 1: Add the "Premium Active" string**

Add this line to `strings.xml` in the appropriate section:

```xml
<string name="settings_screen_premium_active_status">Premium Active</string>
```

**Step 2: Verify strings are accessible**

Check that these strings exist or add if missing:
```xml
<string name="settings_screen_remove_ads_title">Go Premium</string>
<string name="settings_screen_premium_active">Premium Active</string>
```

**Step 3: Commit**

```bash
git add app/src/main/res/values/strings.xml
git commit -m "feat: add premium active status string resource"
```

---

## Task 2: Update SettingsGroupCard to Support Accent Border

**Files:**
- Modify: `app/src/main/java/com/sukoon/music/ui/components/CommonUIComponents.kt:1208-1232`

**Step 1: Read current SettingsGroupCard implementation**

Current code at lines 1208-1232 shows:
```kotlin
Card(
    modifier = modifier.border(
        width = 1.dp,
        color = accentTokens.primary.copy(alpha = 0.24f),
        shape = RoundedCornerShape(20.dp)
    ),
    ...
)
```

**Step 2: Add optional `isAccentBorder` parameter**

Modify the function signature and add parameter:

```kotlin
@Composable
internal fun SettingsGroupCard(
    modifier: Modifier = Modifier,
    rows: List<SettingsRowModel>,
    isAccentBorder: Boolean = false  // ADD THIS LINE
) {
    val accentTokens = accent()
    Card(
        modifier = modifier.border(
            width = 1.dp,
            color = if (isAccentBorder) {
                accentTokens.primary.copy(alpha = 0.5f)  // Stronger opacity for accent border
            } else {
                accentTokens.primary.copy(alpha = 0.24f)  // Default subtle border
            },
            shape = RoundedCornerShape(20.dp)
        ),
        ...
    )
}
```

**Step 3: Commit**

```bash
git add app/src/main/java/com/sukoon/music/ui/components/CommonUIComponents.kt
git commit -m "feat: add optional accent border parameter to SettingsGroupCard"
```

---

## Task 3: Redesign Go Premium Row in SettingsScreen

**Files:**
- Modify: `app/src/main/java/com/sukoon/music/ui/screen/SettingsScreen.kt:236-265`

**Step 1: Replace the Go Premium card section**

Replace lines 236-265 with the new design:

```kotlin
            // 2. Go Premium
            item(key = "go_premium") {
                SettingsGroupCard(
                    modifier = Modifier.padding(horizontal = SpacingLarge),
                    isAccentBorder = true,  // USE NEW PARAMETER
                    rows = listOf(
                        SettingsRowModel(
                            icon = Icons.Default.Star,  // Changed from WorkspacePremium
                            title = stringResource(R.string.settings_screen_remove_ads_title),  // "Go Premium"
                            value = stringResource(R.string.settings_screen_premium_benefit_support_development),  // "Remove ads • Lifetime" OR create new string
                            valuePlacement = ValuePlacement.Below,
                            onClick = if (!isPremium) {
                                {
                                    analyticsTracker?.logEvent(
                                        name = "remove_ads_tap",
                                        params = mapOf("source" to "settings")
                                    )
                                    showPremiumDialog = true
                                }
                            } else null,
                            trailingContent = {
                                if (isPremium) {
                                    // Premium Active State
                                    Row(
                                        modifier = Modifier.width(120.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.settings_screen_premium_active_status),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else {
                                    // Non-Premium State - Price Display
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.settings_screen_premium_price_text),  // "$4.99"
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = accentTokens.active,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        )
                                    }
                                }
                            }
                        )
                    )
                )
            }
```

**Step 2: Update Star Icon Color (Optional)**

If you want to tint the Star icon gold, modify the icon rendering in `SettingsGroupRow` or add a custom composable. For now, use the default color from `MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)`.

To make it gold, you'd need to add a special case in SettingsGroupRow. Alternative: Wrap in a Box with tint:

```kotlin
// In the when block of SettingsGroupRow (around line 1253-1258)
Icon(
    imageVector = row.icon,
    contentDescription = null,
    modifier = Modifier.size(24.dp),
    tint = if (row.icon == Icons.Default.Star) {
        Color(0xFFFFD700)  // Gold color
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    }
)
```

**Step 3: Commit**

```bash
git add app/src/main/java/com/sukoon/music/ui/screen/SettingsScreen.kt
git commit -m "feat: redesign go premium section with accent border and dual states"
```

---

## Task 4: Add Gold Star Icon Tinting

**Files:**
- Modify: `app/src/main/java/com/sukoon/music/ui/components/CommonUIComponents.kt:1253-1258`

**Step 1: Update SettingsGroupRow icon tinting logic**

Replace the Icon rendering code in SettingsGroupRow:

```kotlin
            Icon(
                imageVector = row.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = when {
                    row.icon == Icons.Default.Star -> Color(0xFFFFD700)  // Gold for Star
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                }
            )
```

**Step 2: Commit**

```bash
git add app/src/main/java/com/sukoon/music/ui/components/CommonUIComponents.kt
git commit -m "feat: add gold tinting for star icon in settings"
```

---

## Task 5: Build and Verify

**Files:**
- Test: Build output

**Step 1: Build the project**

```bash
cd C:\Users\ksham\Documents\SukoonMusicPlayer
./gradlew assembleDebug
```

Expected: Build succeeds with no errors.

**Step 2: Verify no compilation errors**

Check that:
- All strings resolve correctly
- `SettingsGroupCard` accepts `isAccentBorder` parameter
- `SettingsRowModel` values and trailing content render
- No crashes on Settings screen

**Step 3: Visual Verification (Preview or Device)**

Run the app and navigate to Settings to verify:
- Go Premium card appears between "Listening" and "Music Library"
- Card has accent-colored border (visible distinction from other cards)
- Non-premium state shows: Star icon (gold) + "Go Premium" + "Remove ads • Lifetime" + Price in blue
- Premium state shows: Star icon (gold) + "Go Premium" + "Remove ads • Lifetime" + Green checkmark + "Premium Active"
- Non-premium card is clickable and opens premium dialog
- Premium card is non-clickable (disabled state)

**Step 4: Commit**

```bash
git add -A
git commit -m "feat: complete go premium redesign implementation"
```

---

## Summary of Changes

| File | Change | Lines |
|------|--------|-------|
| strings.xml | Add "Premium Active" string | New |
| CommonUIComponents.kt | Add `isAccentBorder` parameter to SettingsGroupCard | 1208-1232 |
| CommonUIComponents.kt | Add gold Star icon tinting logic | 1253-1258 |
| SettingsScreen.kt | Replace Go Premium row with new design | 236-265 |

**Total commits:** 4

---

## Testing Checklist

- [ ] Non-premium user sees Go Premium card with price
- [ ] Premium user sees "Premium Active" with checkmark
- [ ] Card has visible accent border
- [ ] Star icon is gold colored
- [ ] Non-premium card opens premium dialog on click
- [ ] Premium card is non-clickable
- [ ] Build succeeds with no errors
- [ ] No crashes on Settings screen navigation

