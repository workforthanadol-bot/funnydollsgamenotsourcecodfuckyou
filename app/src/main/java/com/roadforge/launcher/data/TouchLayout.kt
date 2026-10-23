package com.roadforge.launcher.data

/**
 * Layout ของ Touch Overlay — serialize เป็น JSON เก็บใน DataStore
 */
data class TouchLayout(
    val id: String,
    val name: String,
    val buttons: List<TouchButton>,
    val isDefault: Boolean = false,
)

data class TouchButton(
    val id: String,
    val type: ButtonType,
    val label: String,
    val iconName: String,            // ชื่อ Material icon
    val inputMapping: InputMapping,
    // ตำแหน่งและขนาดเป็น fraction ของหน้าจอ (0.0–1.0)
    val xFraction: Float,
    val yFraction: Float,
    val widthFraction: Float,
    val heightFraction: Float,
    val alpha: Float = 0.85f,
    val isVisible: Boolean = true,
)

enum class ButtonType {
    MOMENTARY,   // กดค้าง = keydown, ปล่อย = keyup
    TOGGLE,      // กดสลับ on/off
    JOYSTICK,    // analog stick
    TOUCHPAD,    // area สำหรับจำลองเมาส์
}

data class InputMapping(
    val keyCode: Int? = null,          // Android KeyEvent keycode
    val mouseButton: MouseButton? = null,
    val axisX: GameAxis? = null,       // สำหรับ Joystick
    val axisY: GameAxis? = null,
    val wineKeySym: String? = null,    // XKeySym string สำหรับ xdotool
)

enum class MouseButton { LEFT, RIGHT, MIDDLE }
enum class GameAxis { STEERING, THROTTLE_BRAKE }

/** Layout เริ่มต้นตามภาพ BeamNG mobile overlay */
fun defaultBeamNGLayout(): TouchLayout = TouchLayout(
    id = "default_beamng",
    name = "BeamNG Default",
    isDefault = true,
    buttons = listOf(
        // ========== บนซ้าย ==========
        TouchButton(
            id = "btn_settings", type = ButtonType.MOMENTARY,
            label = "ตั้งค่า", iconName = "Settings",
            inputMapping = InputMapping(wineKeySym = "Escape"),
            xFraction = 0.035f, yFraction = 0.04f,
            widthFraction = 0.065f, heightFraction = 0.10f
        ),
        TouchButton(
            id = "btn_screenshot", type = ButtonType.MOMENTARY,
            label = "กล้อง", iconName = "CameraAlt",
            inputMapping = InputMapping(wineKeySym = "F12"),
            xFraction = 0.11f, yFraction = 0.04f,
            widthFraction = 0.065f, heightFraction = 0.10f
        ),
        TouchButton(
            id = "btn_pause", type = ButtonType.MOMENTARY,
            label = "หยุด/เล่น", iconName = "Pause",
            inputMapping = InputMapping(wineKeySym = "p"),
            xFraction = 0.185f, yFraction = 0.04f,
            widthFraction = 0.065f, heightFraction = 0.10f
        ),
        TouchButton(
            id = "btn_speedometer", type = ButtonType.MOMENTARY,
            label = "เรือนไมล์", iconName = "Speed",
            inputMapping = InputMapping(wineKeySym = "i"),
            xFraction = 0.26f, yFraction = 0.04f,
            widthFraction = 0.065f, heightFraction = 0.10f
        ),
        TouchButton(
            id = "btn_headlight", type = ButtonType.TOGGLE,
            label = "ไฟหน้า", iconName = "LightMode",
            inputMapping = InputMapping(wineKeySym = "l"),
            xFraction = 0.335f, yFraction = 0.04f,
            widthFraction = 0.065f, heightFraction = 0.10f
        ),
        // ========== ซ้ายกลาง ==========
        TouchButton(
            id = "btn_zoom_in", type = ButtonType.MOMENTARY,
            label = "+", iconName = "Add",
            inputMapping = InputMapping(wineKeySym = "equal"),
            xFraction = 0.03f, yFraction = 0.35f,
            widthFraction = 0.065f, heightFraction = 0.10f
        ),
        TouchButton(
            id = "btn_zoom_out", type = ButtonType.MOMENTARY,
            label = "-", iconName = "Remove",
            inputMapping = InputMapping(wineKeySym = "minus"),
            xFraction = 0.03f, yFraction = 0.47f,
            widthFraction = 0.065f, heightFraction = 0.10f
        ),
        // ========== ซ้ายล่าง ==========
        TouchButton(
            id = "btn_steer_left", type = ButtonType.MOMENTARY,
            label = "เลี้ยวซ้าย", iconName = "ArrowBack",
            inputMapping = InputMapping(wineKeySym = "a"),
            xFraction = 0.03f, yFraction = 0.72f,
            widthFraction = 0.11f, heightFraction = 0.18f
        ),
        TouchButton(
            id = "btn_steer_right", type = ButtonType.MOMENTARY,
            label = "เลี้ยวขวา", iconName = "ArrowForward",
            inputMapping = InputMapping(wineKeySym = "d"),
            xFraction = 0.155f, yFraction = 0.72f,
            widthFraction = 0.11f, heightFraction = 0.18f
        ),
        // ========== บนขวา ==========
        TouchButton(
            id = "btn_map", type = ButtonType.MOMENTARY,
            label = "แผนที่", iconName = "Map",
            inputMapping = InputMapping(wineKeySym = "m"),
            xFraction = 0.79f, yFraction = 0.04f,
            widthFraction = 0.065f, heightFraction = 0.10f
        ),
        TouchButton(
            id = "btn_change_car", type = ButtonType.MOMENTARY,
            label = "เปลี่ยนรถ", iconName = "DirectionsCar",
            inputMapping = InputMapping(wineKeySym = "Tab"),
            xFraction = 0.865f, yFraction = 0.04f,
            widthFraction = 0.065f, heightFraction = 0.10f
        ),
        TouchButton(
            id = "btn_share", type = ButtonType.MOMENTARY,
            label = "แชร์", iconName = "Share",
            inputMapping = InputMapping(wineKeySym = "F2"),
            xFraction = 0.94f, yFraction = 0.04f,
            widthFraction = 0.055f, heightFraction = 0.10f
        ),
        // ========== ขวากลาง ==========
        TouchButton(
            id = "btn_cam_fp", type = ButtonType.MOMENTARY,
            label = "มุมที่ 1", iconName = "Videocam",
            inputMapping = InputMapping(wineKeySym = "1"),
            xFraction = 0.93f, yFraction = 0.17f,
            widthFraction = 0.065f, heightFraction = 0.10f
        ),
        TouchButton(
            id = "btn_cam_tp", type = ButtonType.MOMENTARY,
            label = "มุมที่ 3", iconName = "CameraAlt",
            inputMapping = InputMapping(wineKeySym = "2"),
            xFraction = 0.93f, yFraction = 0.29f,
            widthFraction = 0.065f, heightFraction = 0.10f
        ),
        TouchButton(
            id = "btn_cam_interior", type = ButtonType.MOMENTARY,
            label = "ภายในรถ", iconName = "AirlineSeatReclineNormal",
            inputMapping = InputMapping(wineKeySym = "3"),
            xFraction = 0.93f, yFraction = 0.41f,
            widthFraction = 0.065f, heightFraction = 0.10f
        ),
        TouchButton(
            id = "btn_horn", type = ButtonType.MOMENTARY,
            label = "แตร", iconName = "Campaign",
            inputMapping = InputMapping(wineKeySym = "h"),
            xFraction = 0.93f, yFraction = 0.53f,
            widthFraction = 0.065f, heightFraction = 0.10f
        ),
        // ========== ขวาล่าง ==========
        TouchButton(
            id = "btn_handbrake", type = ButtonType.MOMENTARY,
            label = "เบรกมือ", iconName = "LocalParking",
            inputMapping = InputMapping(wineKeySym = "space"),
            xFraction = 0.79f, yFraction = 0.72f,
            widthFraction = 0.085f, heightFraction = 0.14f
        ),
        TouchButton(
            id = "btn_car_control", type = ButtonType.MOMENTARY,
            label = "ควบคุมรถ", iconName = "Settings",
            inputMapping = InputMapping(wineKeySym = "r"),
            xFraction = 0.885f, yFraction = 0.72f,
            widthFraction = 0.085f, heightFraction = 0.14f
        ),
        TouchButton(
            id = "btn_gear", type = ButtonType.MOMENTARY,
            label = "เกียร์", iconName = "SwapVert",
            inputMapping = InputMapping(wineKeySym = "g"),
            xFraction = 0.73f, yFraction = 0.83f,
            widthFraction = 0.075f, heightFraction = 0.13f
        ),
        TouchButton(
            id = "btn_brake", type = ButtonType.MOMENTARY,
            label = "เบรก", iconName = "Stop",
            inputMapping = InputMapping(wineKeySym = "s"),
            xFraction = 0.815f, yFraction = 0.83f,
            widthFraction = 0.075f, heightFraction = 0.13f
        ),
        TouchButton(
            id = "btn_throttle", type = ButtonType.MOMENTARY,
            label = "คันเร่ง", iconName = "ArrowUpward",
            inputMapping = InputMapping(wineKeySym = "w"),
            xFraction = 0.90f, yFraction = 0.83f,
            widthFraction = 0.075f, heightFraction = 0.13f
        ),
    )
)
