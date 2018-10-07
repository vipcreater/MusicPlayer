package tech.soit.quiet.utils.component.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import tech.soit.quiet.AppContext
import tech.soit.quiet.repository.db.QuietDatabase
import tech.soit.quiet.repository.db.dao.LocalMusicDao

open class QuietViewModelProvider : ViewModelProvider.AndroidViewModelFactory(AppContext) {

    protected val database get() = QuietDatabase.instance

    final override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return createViewModel(modelClass as Class<ViewModel>) as T
    }

    /**
     * create view model by [modelClass]
     */
    open fun createViewModel(modelClass: Class<ViewModel>): ViewModel {
        try {
            val constructor = modelClass.getConstructor(LocalMusicDao::class.java)
            return constructor.newInstance(database.localMusicDao())
        } catch (e: Exception) {

        }
        try {
            val constructor = modelClass.getConstructor(QuietDatabase::class.java)
            return constructor.newInstance(database)
        } catch (e: Exception) {

        }
        return super.create(modelClass)
    }

}