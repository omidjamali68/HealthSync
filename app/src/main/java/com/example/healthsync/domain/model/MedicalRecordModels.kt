package com.example.healthsync.domain.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

object FlexibleStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleStringSerializer", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        return if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            if (element is JsonPrimitive) {
                element.content
            } else {
                element.toString()
            }
        } else {
            decoder.decodeString()
        }
    }

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }
}

object FlexibleNullableStringSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleNullableStringSerializer", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String? {
        return if (decoder is JsonDecoder) {
            val element = decoder.decodeJsonElement()
            if (element is JsonPrimitive) {
                element.contentOrNull
            } else {
                element.toString()
            }
        } else {
            runCatching { decoder.decodeString() }.getOrNull()
        }
    }

    override fun serialize(encoder: Encoder, value: String?) {
        if (value != null) {
            encoder.encodeString(value)
        } else {
            encoder.encodeNull()
        }
    }
}

enum class MedicalRecordCategory(val value: Int, val title: String) {
    VISIT(1, "ویزیت"),
    LAB(2, "آزمایش"),
    PRESCRIPTION(3, "نسخه"),
    SONOGRAPHY(4, "سونوگرافی"),
    MRI(5, "ام آر آی(MRI)");

    companion object {
        fun fromValue(value: Int): MedicalRecordCategory {
            return entries.find { it.value == value } ?: VISIT
        }
    }
}

@Serializable
data class MedicalRecordDto(
    @SerialName("id")
    @Serializable(with = FlexibleStringSerializer::class)
    val id: String = "",
    @SerialName("userId")
    @Serializable(with = FlexibleNullableStringSerializer::class)
    val userId: String? = null,
    @SerialName("title")
    @Serializable(with = FlexibleStringSerializer::class)
    val title: String = "",
    @SerialName("category")
    val category: Int = MedicalRecordCategory.VISIT.value,
    @SerialName("description")
    @Serializable(with = FlexibleNullableStringSerializer::class)
    val description: String? = null,
    @SerialName("doctorName")
    @Serializable(with = FlexibleNullableStringSerializer::class)
    val doctorName: String? = null,
    @SerialName("centerName")
    @Serializable(with = FlexibleNullableStringSerializer::class)
    val centerName: String? = null,
    @SerialName("recordDate")
    @Serializable(with = FlexibleNullableStringSerializer::class)
    val recordDate: String? = null,
    @SerialName("createdAt")
    @Serializable(with = FlexibleNullableStringSerializer::class)
    val createdAt: String? = null,
    @SerialName("files")
    val files: List<MedicalRecordFileDto> = emptyList()
)

@Serializable
data class MedicalRecordFileDto(
    @SerialName("id")
    @Serializable(with = FlexibleStringSerializer::class)
    val id: String = "",
    @SerialName("fileName")
    @Serializable(with = FlexibleStringSerializer::class)
    val fileName: String = "",
    @SerialName("fileUrl")
    @Serializable(with = FlexibleNullableStringSerializer::class)
    val fileUrl: String? = null,
    @SerialName("filePath")
    @Serializable(with = FlexibleNullableStringSerializer::class)
    val filePath: String? = null,
    @SerialName("extension")
    @Serializable(with = FlexibleNullableStringSerializer::class)
    val extension: String? = null,
    @SerialName("fileType")
    @Serializable(with = FlexibleNullableStringSerializer::class)
    val fileType: String? = null,
    @SerialName("fileSize")
    val fileSize: Long = 0L,
    @SerialName("interpretation")
    @Serializable(with = FlexibleNullableStringSerializer::class)
    val interpretation: String? = null
)

@Serializable
data class CreateResponseDto(
    @SerialName("id")
    @Serializable(with = FlexibleStringSerializer::class)
    val id: String = "",
    @SerialName("message")
    @Serializable(with = FlexibleNullableStringSerializer::class)
    val message: String? = null
)

@Serializable
data class FileInterpretationDto(
    @SerialName("fileId")
    @Serializable(with = FlexibleStringSerializer::class)
    val fileId: String = "",
    @SerialName("interpretation")
    @Serializable(with = FlexibleNullableStringSerializer::class)
    val interpretation: String? = null,
    @SerialName("status")
    @Serializable(with = FlexibleNullableStringSerializer::class)
    val status: String? = null,
    @SerialName("createdAt")
    @Serializable(with = FlexibleNullableStringSerializer::class)
    val createdAt: String? = null
)
