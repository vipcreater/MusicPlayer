package tech.summerly.quiet.local.fragments

import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
import me.drakeet.multitype.MultiTypeAdapter
import tech.summerly.quiet.commonlib.base.BaseFragment
import tech.summerly.quiet.commonlib.bean.Artist
import tech.summerly.quiet.commonlib.bean.Music
import tech.summerly.quiet.commonlib.utils.log
import tech.summerly.quiet.commonlib.utils.multiTypeAdapter
import tech.summerly.quiet.commonlib.utils.observeFilterNull
import tech.summerly.quiet.commonlib.utils.setItemsByDiff
import tech.summerly.quiet.local.LocalMusicActivity
import tech.summerly.quiet.local.LocalMusicApi
import tech.summerly.quiet.local.R
import tech.summerly.quiet.local.fragments.items.LocalArtistItemViewBinder
import tech.summerly.quiet.local.fragments.items.LocalMusicItemViewBinder
import tech.summerly.quiet.local.fragments.items.LocalOverviewNavItemViewBinder
import tech.summerly.quiet.local.fragments.items.LocalPlaylistHeaderViewBinder
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Created by summer on 17-12-23
 * template fragment for [tech.summerly.quiet.local.LocalMusicActivity],
 * only contains a recycle view
 */
@Suppress("MemberVisibilityCanPrivate")
abstract class BaseLocalFragment : BaseFragment() {

    /**
     * used to sense the changes of the database
     */
    companion object Observe : MutableLiveData<Pair<String, Long>>() {

        fun postChange(table: String) {
            postValue(table to System.currentTimeMillis())
        }
    }

    protected lateinit var localMusicApi: LocalMusicApi

    private var version = 0L

    override fun onAttach(context: Context) {
        super.onAttach(context)
        localMusicApi = LocalMusicApi.getLocalMusicApi(context)
        Observe.observeFilterNull(this) { table, newVersion ->
            log { "is database changed : ${newVersion > this.version}" }
            if (newVersion > this.version) {
                fetchDataAndDisplay()
                this.version = newVersion
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val recyclerView = RecyclerView(context)
        val layoutManager = GridLayoutManager(context, getSpanCount())
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return 1
            }
        }
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = MultiTypeAdapter().also {
            it.register(Music::class.java, LocalMusicItemViewBinder())
            it.register(Artist::class.java, LocalArtistItemViewBinder())
            it.register(LocalOverviewNavItemViewBinder.Navigator::class.java,
                    LocalOverviewNavItemViewBinder(this::onOverviewNavClick))
            it.register(LocalPlaylistHeaderViewBinder.PlaylistHeader::class.java,
                    LocalPlaylistHeaderViewBinder())
        }
        return recyclerView
    }

    private var jobLoadData: Job? = null

    /**
     * display the
     */
    private fun fetchDataAndDisplay() {
        jobLoadData?.cancel()
        jobLoadData = launch {
            val data = loadData(localMusicApi)
            coroutineContext.checkCompletion()
            showItems(data)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchDataAndDisplay()
    }

    protected abstract suspend fun loadData(localMusicApi: LocalMusicApi): List<Any>

    /**
     * set recycler view layout manager's span count
     */
    protected open fun getSpanCount() = 1

    /**
     * show data with animation which detected by diff utils
     * need to use this method after root view has been created
     */
    private fun showItems(items: List<Any>) = runWithRoot {
        this as? RecyclerView ?: return@runWithRoot
        multiTypeAdapter.setItemsByDiff(items)
    }

    private fun CoroutineContext.checkCompletion() {
        val job = get(Job)
        if (job != null && !job.isActive) throw job.getCancellationException()
    }

    private fun onOverviewNavClick(nav: LocalOverviewNavItemViewBinder.Navigator) {
        val activity = this.activity as? LocalMusicActivity ?: return
        when (nav.title) {
            R.string.local_overview_nav_total -> activity.setCurrentPage(1)
            R.string.local_overview_nav_artist -> activity.setCurrentPage(2)
            R.string.local_overview_nav_album -> activity.setCurrentPage(3)
            R.string.local_overview_nav_trend -> {

            }
            R.string.local_overview_nav_latest -> {

            }
        }
    }

}