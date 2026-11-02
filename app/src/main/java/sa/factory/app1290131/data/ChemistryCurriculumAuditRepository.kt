package sa.factory.app1290131.data

import sa.factory.app1290131.model.ChemistryCurriculumAudit
import sa.factory.app1290131.model.TahsiliLessonItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface ChemistryCurriculumAuditRepository {
    fun getAvailableCatalog(): Flow<List<TahsiliLessonItem>>
    fun getActiveRequest(): Flow<ChemistryCurriculumAudit?>
    suspend fun submitRequest(request: ChemistryCurriculumAudit): Result<String>
}

class InMemoryChemistryCurriculumAuditRepository : ChemistryCurriculumAuditRepository {
    private val _catalog = MutableStateFlow<List<TahsiliLessonItem>>(listOf(
        TahsiliLessonItem("1", "حزمة تدقيق دروس كيمياء 1 التفاعلية", "مراجعة علمية شاملة للمفاهيم والأمثلة المحلولة وربطها بالمرجع الوزاري المعتمد", 5000L, "verified"),
        TahsiliLessonItem("2", "تدقيق واعتماد بنك أسئلة التحصيلي الشامل", "فحص 198 سؤالا تخصصيا مع تصحيح خيارات الإجابة وشرح خطوات الحل النموذجية", 9500L, "biotech")
    ))
    private val _active = MutableStateFlow<ChemistryCurriculumAudit?>(null)

    override fun getAvailableCatalog(): Flow<List<TahsiliLessonItem>> = _catalog.asStateFlow()
    override fun getActiveRequest(): Flow<ChemistryCurriculumAudit?> = _active.asStateFlow()

    override suspend fun submitRequest(request: ChemistryCurriculumAudit): Result<String> {
        _active.value = request
        return Result.success(request.requestId)
    }
}
