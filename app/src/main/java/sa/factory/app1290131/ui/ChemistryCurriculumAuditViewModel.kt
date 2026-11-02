package sa.factory.app1290131.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import sa.factory.app1290131.data.ChemistryCurriculumAuditRepository
import sa.factory.app1290131.model.ChemistryCurriculumAudit
import sa.factory.app1290131.model.TahsiliLessonItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChemistryCurriculumAuditViewModel(
    private val repository: ChemistryCurriculumAuditRepository
) : ViewModel() {
    val catalog: StateFlow<List<TahsiliLessonItem>> = repository.getAvailableCatalog()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeRequest: StateFlow<ChemistryCurriculumAudit?> = repository.getActiveRequest()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun submitNewOrder(request: ChemistryCurriculumAudit) {
        viewModelScope.launch {
            repository.submitRequest(request)
        }
    }
}
