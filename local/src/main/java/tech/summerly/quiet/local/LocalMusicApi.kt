package tech.summerly.quiet.local

import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.support.annotation.VisibleForTesting
import kotlinx.coroutines.experimental.async
import tech.summerly.quiet.commonlib.bean.Album
import tech.summerly.quiet.commonlib.bean.Artist
import tech.summerly.quiet.commonlib.bean.Music
import tech.summerly.quiet.commonlib.bean.Playlist
import tech.summerly.quiet.commonlib.utils.inTransaction
import tech.summerly.quiet.local.database.database.LocalMusicDatabase
import tech.summerly.quiet.local.database.entity.MusicArtistRelation
import tech.summerly.quiet.local.database.entity.MusicPlaylistRelation
import tech.summerly.quiet.local.database.entity.PlaylistEntity
import tech.summerly.quiet.local.fragments.BaseLocalFragment
import tech.summerly.quiet.local.utils.EntityMapper
import java.io.File

/**
 * Created by summer on 17-12-21
 */
class LocalMusicApi private constructor(context: Context) {
    companion object Observe : MutableLiveData<Version>() {
        fun getLocalMusicApi(context: Context) = LocalMusicApi(context.applicationContext)

        private fun postChange(table: String) {
            postValue(table v System.currentTimeMillis())
        }
    }

    private val mapper = EntityMapper()

    private val database = LocalMusicDatabase.getInstance(context)

    private val musicDao = database.musicDao()

    /**
     * insert a music to database
     */
    fun insertMusic(music: Music) {
        val albumId = insertAlbumSafely(music.album)
        val musicEntity = mapper.convertToMusicEntity(music, albumId)
        val musicId = musicDao.insertMusic(musicEntity)
        if (musicId == -1L) { // insert failed
            val musicOld = musicDao.getMusicByPlayUri(musicEntity.playUri)
            if (musicOld.copy(id = musicEntity.id) == musicEntity) {
                return
            } else {
                deleteMusic(music)
                insertMusic(music)
            }
            return
        }
        //insert artist
        val artistIds = insertArtistSafely(music.artist)

        //insert relation of artist and music
        artistIds.map { MusicArtistRelation(musicId, it) }.let { musicDao.insertMusicArtist(it) }
        BaseLocalFragment.postValue(System.currentTimeMillis())
    }

    fun deleteMusic(music: Music,
                    isDeleteFromDisk: Boolean = false) {

        //delete all info for this music
        database.openHelper.writableDatabase.inTransaction {
            delete("relation_music_artist", "music_id = ?", arrayOf(music.id))
            delete("relation_music_playlist", "music_id = ?", arrayOf(music.id))
            delete("entity_music", "id = ?", arrayOf(music.id))
        }

        //remove unlinked artists
        music.artist
                .map(mapper::convertToArtistEntity)
                .filter {
                    musicDao.getMusicByArtist(it.id).isEmpty()
                }
                .let {
                    musicDao.removeArtist(it)
                }

        //remove unlinked album
        if (musicDao.getMusicByAlbum(music.album.id).isEmpty()) {
            musicDao.removeAlbum(mapper.convertToAlbumEntity(music.album))
        }

        //remove from disk
        if (isDeleteFromDisk) {
            val file = File(music.playUri.getOrNull(0)?.uri)
            if (file.exists()) {
                file.delete()
            }
        }
        postChange("music")
    }

    /**
     * get all musics in local database
     */
    fun getTotalMusics(): List<Music> {
        return musicDao.getTotalMusics().map { musicEntity ->
            val artists = musicDao.getArtistByMusic(musicEntity.id).map(mapper::convertToArtist)
            val album = musicDao.getAlbum(musicEntity.albumId).let(mapper::convertToAlbum)
            mapper.convertToMusic(musicEntity, artists, album)
        }
    }

    /**
     * get all artist in database
     */
    fun getArtists(): List<Artist> {
        return musicDao.getArtists().map(mapper::convertToArtist)
    }

    private fun insertAlbumSafely(album: Album): Long {
        val albumEntity = mapper.convertToAlbumEntity(album)
        val albumId = musicDao.insertAlbum(albumEntity)
        return if (albumId == -1L) {
            musicDao.getAlbumByName(album.name).id
        } else {
            albumId
        }
    }

    /**
     * insert artist to table [tech.summerly.quiet.local.database.entity.ArtistEntity]
     * return the id which artist insert to
     */
    private fun insertArtistSafely(artists: List<Artist>): List<Long> {
        return artists.map(mapper::convertToArtistEntity)
                .map { artistEntity ->
                    val id = musicDao.insertArtist(artistEntity)
                    if (id == -1L) {
                        musicDao.getArtistByName(artistEntity.name).id
                    } else {
                        id
                    }
                }
    }

    fun getPlaylists() = async {
        val list = ArrayList<Playlist>()
        database.openHelper.readableDatabase.inTransaction {
            musicDao.getPlaylists().forEach {
                val cursor = query("select count(*) from relation_music_playlist where playlist_id = ?", arrayOf(it.id))
                cursor ?: return@forEach
                cursor.moveToFirst()
                val count = cursor.getInt(0)
                list.add(mapper.convertToPlaylist(it, count))
                cursor.close()
            }
        }
        return@async list
    }

    fun createPlaylist(title: String) = async {
        require(title.isNotEmpty())
        //first check if name is exist
        if (musicDao.getPlaylistByTitle(title) != null) {
            return@async -2
        }
        val playlist = PlaylistEntity(id = 0, title = title, coverUri = null)
        val id = musicDao.insertPlaylist(playlist)
        if (id == -1L) {
            return@async -1
        }
        BaseLocalFragment.postChange()
        return@async 0
    }

    fun insertMusic(playlist: Playlist, musics: Array<Music>) {
        val relations = musics.map {
            MusicPlaylistRelation(it.id, playlist.id)
        }
        musicDao.insertMusicPlaylist(relations)
    }
}

data class Version(
        val name: String,
        val version: Long
)
