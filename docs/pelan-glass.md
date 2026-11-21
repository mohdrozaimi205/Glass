# Pelan Pembinaan: Glass (Launcher Liquid Glass)

**Goal:** Launcher Android "Glass" dengan kesan liquid glass sebenar sebagai ciri utama, dua tema (Liquid Glass ikut post @_cxjhnu, dan Honor), untuk Honor 200 dan Samsung A16.

**Architecture:** Satu modul `app` Kotlin + Jetpack Compose. Launcher melukis wallpaper sendiri ke dalam satu `GraphicsLayer`; setiap `GlassCard` melukis semula bahagian layer itu di belakangnya dengan rantai `RenderEffect` (blur + shader AGSL untuk pembiasan tepi & kilauan). Data (cuaca, waktu solat, kalendar, apps, lencana) dalam repository kecil, disimpan dengan DataStore, dipaparkan melalui satu `HomeViewModel`.

**Tech stack:** Kotlin 2.0.21, AGP 8.7.3, Gradle 8.10.2, Compose BOM 2024.12.01, Material 3, DataStore Preferences 1.1.1, OkHttp 4.12.0, kotlinx-serialization-json 1.7.3, Coil 2.7.0, JUnit 4.13.2.

**Spec:** Rekabentuk yang dipersetujui dalam chat 21 Nov 2026 (ringkasan di bawah).

## Global constraints
- `applicationId` / package: `com.mohdrozaimi.glass`; nama app: **Glass**
- `minSdk 33` (AGSL `RuntimeShader` perlukan API 33), `targetSdk 35`, `compileSdk 35`, Java 17
- Activity utama berdaftar sebagai HOME (`CATEGORY_HOME` + `CATEGORY_DEFAULT`), `launchMode singleTask`, orientasi potret
- Tiada API key: cuaca = Open-Meteo, waktu solat = JAKIM e-Solat, peta = jubin OpenStreetMap
- Carian: `https://www.google.com/search?q=<URL-encoded>`
- Dok apps depan: maksimum **6**
- Mod Jimat: auto ON jika `ActivityManager.MemoryInfo.totalMem < 7 GB` atau `isLowRamDevice`; pengguna boleh tukar dalam Settings
- Folder projek: `D:\Projek Android Studio Apps\Glass`; repo GitHub `mohdrozaimi205/Glass`; Actions bina APK debug-signed → Releases

## Review focus (diuji dalam task berkaitan)
1. Tiada internet / API gagal → widget tunjuk data terakhir disimpan, tidak crash (Task 4, 5)
2. Kebenaran lokasi/kalendar ditolak → widget tunjuk mesej "Benarkan akses", tidak crash (Task 4, 6)
3. Tambah app ke-7 ke dok → ditolak (Task 3)
4. Carian kosong atau ada simbol (`&`, `#`, BM) → URL betul / tiada tindakan (Task 7)
5. Selepas waktu Isyak → "solat seterusnya" ialah Subuh esok (Task 5)

## Struktur fail
```
app/src/main/java/com/mohdrozaimi/glass/
  MainActivity.kt                 HOME activity, host Compose
  GlassApp.kt                     Application, bina repositories
  glass/GlassShaders.kt           sumber AGSL (refraction + highlight)
  glass/GlassBackdrop.kt          wallpaper → GraphicsLayer, CompositionLocal
  glass/GlassCard.kt              Modifier/komponen kad kaca
  glass/PerfMode.kt               keputusan Mod Jimat
  data/Settings.kt                DataStore: tema, wallpaper, zon, dok, mod jimat
  data/AppsRepository.kt          senarai app boleh lancar
  data/DockList.kt                logik tambah/buang dok (pure)
  data/WeatherRepository.kt       Open-Meteo + cache
  data/PrayerRepository.kt        JAKIM + cache + zon
  data/PrayerZones.kt             senarai zon Malaysia (pure)
  data/NextPrayer.kt              kira solat seterusnya (pure)
  data/CalendarRepository.kt      CalendarContract
  data/MapTile.kt                 lat/lon → URL jubin OSM (pure)
  data/SearchUrl.kt               bina URL carian (pure)
  notif/BadgeService.kt           NotificationListenerService → kiraan lencana
  ui/HomeViewModel.kt
  ui/theme/LiquidHome.kt          susun atur tema Liquid Glass
  ui/theme/HonorHome.kt           susun atur tema Honor
  ui/widgets/ClockWidget.kt, CalendarWidget.kt, WeatherWidget.kt,
            MapWidget.kt, SearchBar.kt, PrayerWidget.kt, Dock.kt
  ui/AppDrawer.kt                 semua apps + carian
  ui/SettingsScreen.kt
app/src/test/java/com/mohdrozaimi/glass/   ujian JVM
.github/workflows/build.yml
```

---

## Task 1: Rangka projek + HOME activity + CI
**Files:** create `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `gradle/wrapper/*`, `app/build.gradle.kts`, `AndroidManifest.xml`, `MainActivity.kt`, `GlassApp.kt`, `.github/workflows/build.yml`, ikon app.
- [ ] Manifest: `MainActivity` dengan intent-filter MAIN + HOME + DEFAULT + LAUNCHER; permissions `INTERNET`, `ACCESS_COARSE_LOCATION`, `READ_CALENDAR`, `QUERY_ALL_PACKAGES`; service `BadgeService` (BIND_NOTIFICATION_LISTENER_SERVICE)
- [ ] Workflow: on push `main` → JDK 17 → `./gradlew testDebugUnitTest assembleDebug` → upload `Glass.apk` ke Release tag `v${{ github.run_number }}`
- [ ] Ujian asap: `./gradlew assembleDebug` → BUILD SUCCESSFUL
- [ ] Commit `chore: rangka projek Glass`

## Task 2: Enjin liquid glass
**Interfaces produced:** `GlassBackdrop(wallpaper: ImageBitmap?, content: @Composable () -> Unit)`; `LocalBackdropLayer: CompositionLocal<GraphicsLayer?>`; `Modifier.glass(shape: Shape = RoundedCornerShape(28.dp), blur: Dp = 24.dp, refraction: Float = 0.35f, lite: Boolean)`; `GlassCard(modifier, lite, content)`; `object PerfMode { fun shouldLite(totalMemBytes: Long, isLowRam: Boolean): Boolean }`.
- [ ] Test `PerfModeTest`: `shouldLite(6L*GB,false)==true`, `shouldLite(12L*GB,false)==false`, `shouldLite(12L*GB,true)==true` → FAIL
- [ ] Implement `PerfMode.shouldLite` (ambang 7 GB) → PASS
- [ ] `GlassShaders.REFRACTION`: AGSL dengan uniform `size`, `radius`, `strength`, `shader content`; anjak sampel ke arah dalam berdasarkan jarak ke tepi rounded-rect (SDF), tambah kilauan putih 0–0.25 alpha pada tepi atas
- [ ] `Modifier.glass`: `drawBehind` lukis `LocalBackdropLayer` dengan `translate(-boundsInRoot)`, clip ke shape; `graphicsLayer.renderEffect = RenderEffect.createChainEffect(RuntimeShaderEffect(refraction), BlurEffect(blur))`; tint putih 12%, border 1dp gradien putih 40%→5%. Jika `lite`: blur sahaja, tiada shader, radius 16dp
- [ ] Alunan sentuh: `pointerInput` → `Animatable` skala 1.0→0.97→1.0 dan `strength` +0.15
- [ ] Commit `feat: enjin liquid glass`

## Task 3: Settings, apps & dok
**Interfaces:** `enum class ThemeId { LIQUID, HONOR }`; `class Settings(context)` dengan `Flow` bagi `theme`, `wallpaperUri: String?`, `prayerZone: String` (default `"SGR01"`), `dock: List<String>`, `liteOverride: Boolean?`; `object DockList { const val MAX = 6; fun add(list: List<String>, pkg: String): List<String>; fun remove(list, pkg): List<String> }`; `data class AppEntry(label: String, pkg: String, icon: ImageBitmap)`; `AppsRepository.load(): List<AppEntry>` (disusun ikut label).
- [ ] Test `DockListTest`: tambah ke senarai 5 → 6; tambah ke senarai 6 → tidak berubah; tambah pendua → tidak berubah; buang → hilang → FAIL
- [ ] Implement `DockList` → PASS
- [ ] Implement `Settings` (DataStore, dok disimpan sebagai string dipisah `|`) dan `AppsRepository` (`queryIntentActivities` MAIN/LAUNCHER, kecuali pakej sendiri)
- [ ] Commit `feat: settings, apps dan dok`

## Task 4: Cuaca
**Interfaces:** `data class Weather(tempC: Int, code: Int, label: String)`; `fun parseOpenMeteo(json: String): Weather`; `fun weatherLabel(code: Int): String` (BM: Cerah, Berawan, Hujan, Ribut petir...); `WeatherRepository.get(lat: Double, lon: Double): Weather?` (cache dalam DataStore, pulang cache bila gagal).
- [ ] Test: JSON contoh `{"current":{"temperature_2m":27.4,"weather_code":3}}` → `Weather(27,3,"Berawan")`; `weatherLabel(95)=="Ribut petir"`; JSON rosak → exception ditangkap, repository pulang cache → FAIL
- [ ] Implement; URL `https://api.open-meteo.com/v1/forecast?latitude=..&longitude=..&current=temperature_2m,weather_code&timezone=auto` → PASS
- [ ] Lokasi: `LocationManager.getLastKnownLocation(NETWORK)`; jika tiada kebenaran → `null` (widget tunjuk "Benarkan lokasi")
- [ ] Commit `feat: cuaca`

## Task 5: Waktu solat
**Interfaces:** `PrayerZones.all: List<Pair<String,String>>` (kod, nama, semua zon JAKIM); `data class PrayerDay(date: LocalDate, times: Map<String, LocalTime>)` kunci `Subuh, Zohor, Asar, Maghrib, Isyak`; `fun parseJakim(json: String): List<PrayerDay>`; `fun nextPrayer(days: List<PrayerDay>, now: LocalDateTime): Pair<String, LocalDateTime>?`; `PrayerRepository.month(zone): List<PrayerDay>` (period=month, cache).
- [ ] Test: `nextPrayer` jam 13:00 → Asar hari ini; jam 21:30 → Subuh esok; `parseJakim` contoh → 5 waktu betul; `PrayerZones.all` mengandungi `"SGR01"` dan `"WLY01"` → FAIL
- [ ] Implement; URL `https://www.e-solat.gov.my/index.php?r=esolatApi/TakwimSolat&period=month&zone=<zon>` → PASS
- [ ] Commit `feat: waktu solat`

## Task 6: Kalendar & peta
**Interfaces:** `data class CalEvent(title: String, start: LocalDateTime, allDay: Boolean)`; `CalendarRepository.upcoming(days: Int = 7): List<CalEvent>` (kosong jika tiada kebenaran); `fun tileUrl(lat: Double, lon: Double, zoom: Int = 15): String`.
- [ ] Test `MapTileTest`: KL (3.139, 101.687, z15) → `https://tile.openstreetmap.org/15/25639/16098.png` (formula Web Mercator) → FAIL
- [ ] Implement `tileUrl` → PASS
- [ ] Implement `CalendarRepository` dengan `CalendarContract.Instances`; butang + → `Intent(ACTION_INSERT, Events.CONTENT_URI)`; tekan peta → `geo:lat,lon`
- [ ] Commit `feat: kalendar dan peta`

## Task 7: Carian Google & lencana notifikasi
**Interfaces:** `fun searchUrl(query: String): String?` (null jika kosong/ruang); `BadgeService` mengemas kini `BadgeStore.counts: StateFlow<Map<String, Int>>`.
- [ ] Test: `searchUrl("  ")==null`; `searchUrl("nasi lemak & teh")=="https://www.google.com/search?q=nasi+lemak+%26+teh"`; `searchUrl("#1 cuaca")` mengekod `#` → FAIL
- [ ] Implement (`URLEncoder.encode(q.trim(), UTF_8)`) → PASS
- [ ] `BadgeService`: `onListenerConnected`/`onNotificationPosted`/`Removed` → kira `activeNotifications` ikut pakej (abaikan `isOngoing`)
- [ ] Commit `feat: carian dan lencana`

## Task 8: Home screen boleh suai (apps & widget)
**Interfaces:** `sealed class HomeItem { id: String; x: Int; y: Int; w: Int; h: Int }` dengan `AppItem(pkg)`, `GlassWidget(kind: WidgetKind)` (`CLOCK, CALENDAR, WEATHER, MAP, SEARCH, PRAYER`), `SystemWidget(appWidgetId: Int, provider: String)`; `object HomeGrid { const val COLS = 4; const val ROWS = 6; fun canPlace(items, item): Boolean; fun firstFree(items, w, h): Pair<Int,Int>?; fun move(items, id, x, y): List<HomeItem>; fun resize(items, id, w, h): List<HomeItem>; fun remove(items, id): List<HomeItem>; fun replaceApp(items, id, pkg): List<HomeItem> }`; `fun encodeHome(items): String` / `fun decodeHome(json): List<HomeItem>`; `fun defaultLayout(theme: ThemeId): List<HomeItem>`.
- [ ] Test `HomeGridTest`: item bertindih → `canPlace==false`; keluar grid (x+w>4) → false; `firstFree` atas grid penuh → null; `move` ke petak berisi → senarai tidak berubah; `resize` 2x2→4x2 bila ruang kosong → berjaya, bila bertindih → tidak berubah; `replaceApp` tukar pkg kekalkan kedudukan; encode→decode sama → FAIL
- [ ] Implement `HomeGrid` + serialisasi (kotlinx-serialization, disimpan dalam `Settings.home[theme]`) → PASS
- [ ] `defaultLayout(LIQUID)` = susun atur post (Jam 4x2, Kalendar 2x2, Cuaca 2x2, Peta 2x1, Carian 2x1); `defaultLayout(HONOR)` = Solat 4x2, Jam 4x1, apps
- [ ] Widget sistem: `AppWidgetHost(hostId=1024)`; pemilih widget sendiri (senarai `AppWidgetManager.installedProviders` dengan pratonton) → `bindAppWidgetIdIfAllowed` / `ACTION_APPWIDGET_BIND` → configure jika ada → letak di `firstFree`; `startListening` di `onStart`, `stopListening` di `onStop`; buang → `deleteAppWidgetId`
- [ ] Mod Edit (tekan lama ruang kosong atau item): item bergoyang; seret untuk alih (snap ke grid); menu item: **Buang**, **Tukar app** (buka pemilih app), **Saiz** (pilihan 1x1, 2x1, 2x2, 4x1, 4x2, 4x3 — hanya yang muat dan dalam min/max provider); butang **+ Widget** dan **+ App**; widget Glass dan widget sistem dibalut `Modifier.glass`
- [ ] Commit `feat: home screen boleh suai`

## Task 9: Skrin utama dua tema + drawer + Settings
Skrin utama melukis `HomeItem` dari Task 8; susun atur di bawah ialah **lalai** bagi setiap tema.
**Interfaces:** `HomeViewModel(settings, apps, weather, prayer, calendar)` dedahkan `uiState: StateFlow<HomeUi>`; `LiquidHome(ui, actions)`; `HonorHome(ui, actions)`.
- [ ] **LiquidHome** (ikut post): jam besar 72sp + tarikh BM + cuaca ringkas + acara seterusnya; baris 2 kad: Kalendar (2 hari, butang + kuning `#F5C518`) | Cuaca besar; baris 2 kad: Peta | Bar carian "Cari di Google..."; Dok kaca di bawah (≤6 apps, lencana)
- [ ] **HonorHome**: kad kaca waktu solat besar (solat seterusnya + kiraan masa), jam & tarikh, dok apps depan ≤6, butang search bulat → buka AppDrawer; semua kad guna `Modifier.glass`
- [ ] **AppDrawer**: leret ke atas / butang search; grid 4 lajur, medan carian, tekan lama → "Tambah ke dok"/"Buang dari dok"
- [ ] **Settings** (ikon gear dalam Mod Edit; "Set semula susun atur" → `defaultLayout`): tema, wallpaper (Photo Picker + 5 wallpaper siap), zon solat, Mod Jimat (Auto/On/Off), buka tetapan akses notifikasi, tetapkan sebagai launcher lalai
- [ ] Kali pertama buka: minta lokasi, kalendar; tunjuk kad "Benarkan akses notifikasi untuk lencana"
- [ ] Butang Back di home tidak keluar app; Home ditekan → tutup drawer/settings
- [ ] `./gradlew testDebugUnitTest assembleDebug` → semua PASS, APK terhasil
- [ ] Commit `feat: skrin utama, drawer, settings`

## Task 10: Hantar
- [ ] Salin projek ke `D:\Projek Android Studio Apps\Glass`
- [ ] Repo `mohdrozaimi205/Glass` (panduan langkah demi langkah jika perlu), push → Actions hijau → Release ada `Glass.apk`
- [ ] Simpan `Glass.apk` dalam folder projek di laptop
- [ ] Upload `Glass.apk` ke Google Drive "Apk Android Studio"

## Di luar skop v1
Tema Minimal/Kabus/Senja, versi tablet/TV, sync wallpaper sistem, berbilang halaman home.
