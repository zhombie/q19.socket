package kz.q19.socket.model

import androidx.annotation.Keep
import kz.q19.domain.model.language.Language

@Keep
data class Category constructor(
    val id: Long,
    val title: String? = null,
    val language: Language.ID = Language.ID.RU,
    val parentId: Long = NO_PARENT_ID,
    val photo: String? = null,
    val children: List<Category> = emptyList(),
    val responses: List<Long> = emptyList(),
    val config: Config? = null
) {

    companion object {
        const val NO_ID: Long = -1L
        const val NO_PARENT_ID: Long = 0L
    }

    @Keep
    data class Config constructor(val order: Int)

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is Category) return false
        if (id == other.id && parentId == other.parentId) return true
        return false
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

}