package com.example.agroaviedocalling

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.agroaviedocalling.databinding.ActivityMainBinding
import com.example.agroaviedocalling.media.RtcTokenBuilder2
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val appId = "7ac6c604008c4036ba8ec3d366f7ede1"
    private val appCertificate = "049f668b993f43e689e76ac86d9e3a09"
    private val channelName = "testExample"
    private val expirationTimeInSeconds = 360000
    private val uid = 0

    private var agoraEngine: RtcEngine? = null
    private var isJoined = false
    private var token: String? = null

    private var localSurfaceView: SurfaceView? = null
    private var remoteSurfaceView: SurfaceView? = null

    private val PERMISSION_REQ_ID = 22
    private val REQUESTED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        token = generateToken()

        if (!checkSelfPermission()) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID)
        } else {
            setupVideoSDKEngine()
        }

        binding.joinBtn.setOnClickListener {
            joinChannel()
            binding.joinBtn.visibility = View.GONE
            setupCallControls()
        }
    }

    private fun generateToken(): String {
        val tokenBuilder = RtcTokenBuilder2()
        val timestamp = (System.currentTimeMillis() / 1000 + expirationTimeInSeconds).toInt()
        return tokenBuilder.buildTokenWithUid(
            appId, appCertificate,
            channelName, uid, RtcTokenBuilder2.Role.ROLE_PUBLISHER, timestamp, timestamp
        )
    }

    private fun setupVideoSDKEngine() {
        try {
            val config = RtcEngineConfig()
            config.mContext = baseContext
            config.mAppId = appId
            config.mEventHandler = mRtcEventHandler
            agoraEngine = RtcEngine.create(config)
            agoraEngine!!.enableVideo()
        } catch (e: Exception) {
            showMessage("Agora Engine init failed: ${e.message}")
        }
    }

    private fun checkSelfPermission(): Boolean {
        return REQUESTED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun showMessage(message: String?) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private val mRtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            Log.d("Agora", "Joined channel successfully!")
            isJoined = true
            runOnUiThread {
                setupLocalVideo()
            }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            Log.d("Agora", "User joined: $uid")
            runOnUiThread {
                setupRemoteVideo(uid)
                binding.localVideoViewContainer.bringToFront()
                binding.localVideoViewContainer.visibility = View.VISIBLE
                binding.remoteVideoViewContainer.visibility = View.VISIBLE
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            Log.d("Agora", "User offline: $uid")
            runOnUiThread {
                remoteSurfaceView?.visibility = View.GONE
                binding.remoteVideoViewContainer.removeAllViews()
            }
        }
    }

    private fun setupLocalVideo() {
        localSurfaceView = SurfaceView(baseContext).apply {
            setZOrderMediaOverlay(true)
        }

        binding.localVideoViewContainer.removeAllViews()
        binding.localVideoViewContainer.addView(localSurfaceView)

        agoraEngine?.setupLocalVideo(
            VideoCanvas(localSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0)
        )

        localSurfaceView?.visibility = View.VISIBLE
        binding.localVideoViewContainer.visibility = View.VISIBLE

        // Make it draggable
        enableLocalVideoDragging()
    }


    private fun setupRemoteVideo(uid: Int) {
        remoteSurfaceView = SurfaceView(baseContext)
        binding.remoteVideoViewContainer.removeAllViews()
        binding.remoteVideoViewContainer.addView(remoteSurfaceView)

        // Set the render mode for remote video to FILL so it stretches to fit the container
        agoraEngine?.setupRemoteVideo(
            VideoCanvas(remoteSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid)
        )
        remoteSurfaceView?.visibility = View.VISIBLE
    }

    private fun joinChannel() {
        if (!checkSelfPermission()) {
            showMessage("Permissions not granted")
            return
        }

        val options = ChannelMediaOptions().apply {
            channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
        }

        agoraEngine?.joinChannel(token, channelName, uid, options)
    }

    override fun onDestroy() {
        super.onDestroy()
        agoraEngine?.stopPreview()
        agoraEngine?.leaveChannel()

        Thread {
            RtcEngine.destroy()
            agoraEngine = null
        }.start()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun enableLocalVideoDragging() {
        var dX = 0f
        var dY = 0f

        binding.localVideoViewContainer.setOnTouchListener { view, event ->
            val parent = binding.root
            val screenWidth = parent.width
            val screenHeight = parent.height

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dX = view.x - event.rawX
                    dY = view.y - event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val newX = event.rawX + dX
                    val newY = event.rawY + dY

                    // Clamp X within screen
                    val finalX = newX.coerceIn(0f, (screenWidth - view.width).toFloat())

                    // Clamp Y within screen
                    val finalY = newY.coerceIn(0f, (screenHeight - view.height).toFloat())

                    view.animate()
                        .x(finalX)
                        .y(finalY)
                        .setDuration(0)
                        .start()

                    true
                }
                else -> false
            }
        }
    }

    private fun setupCallControls() {
        binding.callControlsLayout.visibility = View.VISIBLE

        var isMuted = false

        binding.btnMute.setOnClickListener {
            isMuted = !isMuted
            agoraEngine?.muteLocalAudioStream(isMuted)
            binding.btnMute.setImageResource(if (isMuted) R.drawable.ic_mic_off else R.drawable.ic_mic)
        }

        binding.btnEnd.setOnClickListener {
            agoraEngine?.leaveChannel()
            finish() // End activity or return to previous screen
        }

        binding.btnSwitchCamera.setOnClickListener {
            agoraEngine?.switchCamera()
        }
    }


}
