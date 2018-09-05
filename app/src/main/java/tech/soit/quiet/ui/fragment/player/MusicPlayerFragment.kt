package tech.soit.quiet.ui.fragment.player

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.SeekBar
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.fragment_music_player.*
import kotlinx.android.synthetic.main.player_content_music_controller.*
import tech.soit.quiet.R
import tech.soit.quiet.player.MusicPlayerManager
import tech.soit.quiet.player.core.IMediaPlayer
import tech.soit.quiet.ui.fragment.base.BaseFragment
import tech.soit.quiet.ui.view.CircleOutlineProvider
import tech.soit.quiet.utils.annotation.LayoutId
import tech.soit.quiet.utils.subTitle

@LayoutId(R.layout.fragment_music_player)
class MusicPlayerFragment : BaseFragment() {

    companion object {

        const val TAG = "MusicPlayerFragment"

    }

    private var isUserTracking = false

    private lateinit var albumRotationAnimator: ValueAnimator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MusicPlayerManager.playerState.observe(this, Observer {
            if (it == IMediaPlayer.PLAYING) {
                buttonPlayPause.setImageResource(R.drawable.ic_pause_black_24dp)
                if (albumRotationAnimator.isPaused) {
                    albumRotationAnimator.resume()
                } else {
                    albumRotationAnimator.start()
                }
            } else {
                buttonPlayPause.setImageResource(R.drawable.ic_play_arrow_black_24dp)
                albumRotationAnimator.pause()
            }
        })
        MusicPlayerManager.playingMusic.observe(this, Observer { music ->
            music ?: return@Observer
            textTitle.text = music.title
            textSubTitle.text = music.subTitle
        })
        MusicPlayerManager.position.observe(this, Observer { position ->
            val current = position?.current ?: 0
            val max = position?.total ?: 0
            if (!isUserTracking) {
                seekBar.progress = current.toInt()
                textCurrentPosition.text = toMusicTimeStamp(current.toInt())
            }
            seekBar.max = max.toInt()
            textDuration.text = toMusicTimeStamp(max.toInt())
        })
    }

    private fun toMusicTimeStamp(_millisecond: Int): String {
        var millisecond = _millisecond
        millisecond /= 1000
        val second = millisecond % 60
        val minute = millisecond / 60
        return String.format("%02d:%02d", minute, second)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        iconUp.setOnClickListener { onBackPressed() }
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    textCurrentPosition.text = toMusicTimeStamp(progress)
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                isUserTracking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                isUserTracking = false
                val player = MusicPlayerManager.musicPlayer.mediaPlayer
                player.seekTo(seekBar.progress.toLong())
            }
        })
        buttonPlayMode.setOnClickListener {
            MusicPlayerManager.musicPlayer.playlist.playMode = MusicPlayerManager.musicPlayer.playlist.playMode.next()
        }
        buttonPlayPrevious.setOnClickListener {
            MusicPlayerManager.musicPlayer.playPrevious()
        }
        buttonPlayPause.setOnClickListener {
            MusicPlayerManager.musicPlayer.playPause()
        }
        buttonPlayNext.setOnClickListener {
            MusicPlayerManager.musicPlayer.playNext()
        }
        buttonPlayerPlaylist.setOnClickListener {
            //TODO
        }
        imageArtwork.outlineProvider = CircleOutlineProvider()
        imageArtwork.clipToOutline = true
        albumRotationAnimator = ValueAnimator.ofFloat(0f, 360f)
        with(albumRotationAnimator) {
            duration = 10000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                val value = it.animatedValue as Float
                imageArtwork.rotation = value
            }
        }

    }

}