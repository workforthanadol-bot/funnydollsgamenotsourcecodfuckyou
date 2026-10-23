package com.roadforge.launcher.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

/**
 * โปรไฟล์เกมที่บันทึกไว้ใน Launcher
 * ไม่เก็บไฟล์เกมจริง — เป็นเพียง pointer ไปยังตำแหน่งที่ผู้ใช้เลือก
 */
@Entity(tableName = "game_profiles")
@TypeConverters(GameStatusConverter::class)
data class GameProfile(
    @PrimaryKey val id: String,          // UUID
    val name: String,
    val exeUri: String,                  // SAF URI ของ EXE
    val gameFolderUri: String?,          // SAF URI ของโฟลเดอร์เกม (nullable)
    val exePath: String,                 // path ที่อ่านได้สำหรับแสดงผล
    val status: GameStatus,
    val errorMessage: String?,
    val createdAt: Long,
    val lastLaunchedAt: Long?,
    val runtimeConfigJson: String?,      // JSON ของ RuntimeConfig
    val coverArtPath: String?,           // path รูปปก (optional)
    val gameVersion: String?,            // version string ที่ตรวจพบ
    val detectedExecutableType: String?, // "x86_64", "x86", "arm64" etc.
)

enum class GameStatus {
    NOT_CONFIGURED,  // เพิ่งเพิ่ม ยังไม่ได้ setup
    PREPARING,       // กำลัง setup อยู่
    READY,           // setup เสร็จ พร้อม launch
    INCOMPLETE,      // ไฟล์ไม่ครบ
    RUNTIME_MISSING, // Runtime ยังไม่พร้อม
    LAUNCH_FAILED,   // launch ล้มเหลวครั้งล่าสุด
    ERROR            // ข้อผิดพลาดอื่น
}

class GameStatusConverter {
    @TypeConverter
    fun fromStatus(status: GameStatus): String = status.name
    @TypeConverter
    fun toStatus(value: String): GameStatus = GameStatus.valueOf(value)
}
