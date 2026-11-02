package sa.factory.app1290131.model

import kotlinx.serialization.Serializable

@Serializable
data class TahsiliLessonItem(
    val id: String,
    val title: String,
    val description: String,
    val priceMinorUnits: Long,
    val iconVector: String = "verified"
)

@Serializable
data class ChemistryCurriculumAudit(
    val requestId: String,
    val items: List<TahsiliLessonItem>,
    val customerPhone: String,
    val totalAmountMinorUnits: Long,
    val status: ChemistryCurriculumAuditStatus = ChemistryCurriculumAuditStatus.PENDING,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

enum class ChemistryCurriculumAuditStatus {
    PENDING,
    ACCEPTED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
