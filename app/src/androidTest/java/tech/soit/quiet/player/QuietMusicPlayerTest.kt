package tech.soit.quiet.player

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tech.soit.quiet.player.core.IMediaPlayer
import tech.soit.quiet.player.playlist.Playlist
import tech.soit.quiet.utils.Dummy

/**
 * @author : summer
 * @date : 18-8-19
 */
class QuietMusicPlayerTest {


    private lateinit var player: QuietMusicPlayer

    @get:Rule
    val r = InstantTaskExecutorRule()

    // inflate with dummy play uri
    private val musics = Dummy.MUSICS

    @Before
    fun setUp() {
        player = QuietMusicPlayer()
        player.playlist = Playlist("test", musics)
    }

    @After
    fun tearDown() {
        player.quiet()
    }

    @Test
    fun playEmpty() = runBlocking {
        player.playlist = Playlist.EMPTY

        val music = Dummy.MUSICS[0].copy(id = 12004)
        player.play(music, playWhenReady = false)

        delay(1000)

        assertTrue("player contains ${music.getId()}", player.playlist.list.contains(music))
        assertFalse("not isPlayWhenReady", player.mediaPlayer.isPlayWhenReady)

    }

    @Test
    fun play() = runBlocking {
        val music = musics[1]
        player.play(music)
        delay(1000)
        assertEquals(music, player.playlist.current)
        assertTrue("is playWhenReady", player.mediaPlayer.isPlayWhenReady)
    }

    @Test
    fun testPlayNext() = runBlocking {
        assertTrue("music.size : ${musics.size} greater than 3", musics.size > 3)

        player.playNext()
        delay(1000)
        assertEquals(musics[1], player.playlist.current)
        player.playNext()
        delay(1000)
        assertEquals(musics[2], player.playlist.current)
        player.playNext()
        delay(1000)
        assertEquals(musics[3], player.playlist.current)
    }

    @Test
    fun playPause() = runBlocking {
        assertEquals(player.mediaPlayer.getState(), IMediaPlayer.IDLE)
        assertFalse(player.mediaPlayer.isPlayWhenReady)

        player.playPause()
        delay(1000)
        assertTrue(player.mediaPlayer.isPlayWhenReady)
        assertEquals("expect state is playing", player.mediaPlayer.getState(), IMediaPlayer.PLAYING)


        player.playPause()
        delay(1000)
        assertFalse(player.mediaPlayer.isPlayWhenReady)
        assertEquals(player.mediaPlayer.getState(), IMediaPlayer.PAUSING)

    }

    @Test
    fun playPrevious() = runBlocking {
        assertTrue("music.size : ${musics.size} greater than 3", musics.size > 3)

        player.playPrevious()
        delay(1000)
        assertEquals(musics[musics.size - 1], player.playlist.current)

        player.playPrevious()
        delay(1000)
        assertEquals(musics[musics.size - 2], player.playlist.current)

        player.playPrevious()
        delay(1000)
        assertEquals(musics[musics.size - 3], player.playlist.current)

    }


    @Test
    fun testQuiet() = runBlocking {
        val music = musics[1]
        player.play(music)
        delay(120)
        assertEquals(music, player.playlist.current)
        assertTrue(player.mediaPlayer.isPlayWhenReady)
        player.quiet()
        assertEquals(music, player.playlist.current)
        assertEquals(IMediaPlayer.IDLE, player.mediaPlayer.getState())

    }


}