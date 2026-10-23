# RoadForge Launcher

**Android launcher สำหรับรันเกม Windows บน ARM64**
ใช้ Wine + Box64 + DXVK เพื่อรันไฟล์ EXE จริงบนอุปกรณ์ Android

---

## ⚠️ ข้อกำหนดสำคัญ

- **ต้องมีไฟล์เกมที่ซื้อถูกกฎหมายเท่านั้น** — Launcher ไม่รวมไฟล์เกมใดๆ
- ต้องติดตั้ง Runtime Components (Wine, Box64, DXVK) แยกต่างหาก
- รองรับเฉพาะ **Android ARM64 (arm64-v8a)** เท่านั้น
- Android **API 26+** (Android 8.0+)

---

## สถาปัตยกรรม

```
RoadForge Launcher
├── UI Layer (Jetpack Compose + Material 3)
│   ├── LibraryScreen     — รายการเกมที่เพิ่มไว้
│   ├── SetupScreen       — ตั้งค่า + ตรวจสอบไฟล์
│   ├── SettingsScreen    — การตั้งค่าแอป
│   ├── OverlayEditorScreen — แก้ไข Touch Overlay
│   ├── DiagnosticsScreen — ตรวจสอบระบบ / Runtime
│   └── PermissionsScreen — จัดการสิทธิ์
│
├── Data Layer (Room + DataStore)
│   ├── GameProfile       — โปรไฟล์เกมแต่ละเกม
│   ├── RuntimeConfig     — ค่า Wine/Box64/GPU
│   └── AppSettings       — การตั้งค่า DataStore
│
├── Runtime Layer
│   ├── RuntimeService    — Foreground Service รัน Wine+Box64
│   ├── RuntimeChecker    — ตรวจสอบ binary ที่มีอยู่จริง
│   └── DeviceInspector   — ตรวจสอบ GPU/RAM/Vulkan
│
├── Overlay Layer
│   ├── TouchOverlayService — Foreground Service แสดง overlay
│   ├── OverlayViewHost     — Custom View วาดด้วย Canvas
│   └── InputBackend        — xdotool → Wine/X11
│
└── Setup Layer
    ├── SetupManager      — Pipeline 10 ขั้นตอน
    └── SetupTask         — weight-based progress
```

---

## Runtime ที่รองรับ

| Component     | License    | หมายเหตุ                              |
|---------------|------------|---------------------------------------|
| Wine          | LGPL-2.1   | Windows compatibility layer           |
| Box64         | LGPL-2.1   | x86_64 → ARM64 dynamic recompiler     |
| DXVK          | zlib       | DirectX 9/10/11 → Vulkan              |
| VKD3D-Proton  | LGPL-2.1   | DirectX 12 → Vulkan                   |
| Turnip/Mesa   | MIT        | Adreno GPU เท่านั้น                   |
| VirGL         | MIT        | fallback สำหรับ GPU อื่น              |

---

## การ Build

### ข้อกำหนด
- Android Studio Ladybug (2024.2.1) หรือใหม่กว่า
- JDK 17
- Android NDK r27 (arm64-v8a)

### Build ด้วย Android Studio
1. เปิดโฟลเดอร์ `RoadForgeLauncher` ใน Android Studio
2. รอ Gradle sync เสร็จ
3. Build > Build Bundle(s) / APK(s) > Build APK(s)

### Build ด้วย Command Line
```bash
chmod +x gradlew
./gradlew assembleDebug
# APK อยู่ที่: app/build/outputs/apk/debug/app-debug.apk
```

### GitHub Actions
Push ไปที่ `main` หรือ `develop` → GitHub Actions build APK อัตโนมัติ
ดาวน์โหลด artifact ได้จาก Actions tab

---

## การติดตั้ง Runtime

> Launcher ไม่ดาวน์โหลด Runtime ให้อัตโนมัติ ต้องเตรียมไฟล์เองดังนี้

1. คอมไพล์หรือดาวน์โหลด Box64 สำหรับ Android ARM64
2. คอมไพล์หรือดาวน์โหลด Wine สำหรับ Android
3. วางไฟล์ใน directory ที่ตั้งค่าไว้ใน Settings > Runtime
4. เปิดแอป → Diagnostics → ตรวจสอบสถานะ Runtime

โครงสร้างไดเรกทอรีที่คาดไว้:
```
<runtimeBaseDir>/
├── bin/
│   ├── box64
│   └── wine64
├── lib/
│   ├── libvulkan_freedreno.so   (Turnip — Adreno only)
│   └── dxvk/
│       ├── d3d9.dll
│       ├── d3d10core.dll
│       └── d3d11.dll
└── xdotool
```

---

## Touch Overlay

Layout เริ่มต้นออกแบบมาสำหรับ BeamNG.drive:

| ตำแหน่ง       | ปุ่ม                                              |
|---------------|--------------------------------------------------|
| บนซ้าย        | Settings, Screenshot, Pause, Speedometer, Headlight |
| กลางซ้าย      | Zoom In, Zoom Out                                 |
| ล่างซ้าย      | Steer Left (ใหญ่), Steer Right (ใหญ่)             |
| บนขวา         | Map, Change Car, Share                            |
| กลางขวา       | Camera FP, Camera TP, Camera Interior, Horn       |
| ล่างขวา       | Handbrake, Car Control, Gear, Brake, Throttle     |

แก้ไข layout ได้จาก Library > ⋮ > แก้ไข Overlay

---

## License

RoadForge Launcher source code: **MIT License**

Runtime components มี License แยกต่างหาก — ดูตารางด้านบน
ไฟล์เกมอยู่ภายใต้ License ของผู้พัฒนาเกมนั้นๆ
