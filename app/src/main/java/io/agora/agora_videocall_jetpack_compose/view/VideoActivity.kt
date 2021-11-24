package io.agora.agora_videocall_jetpack_compose

import android.Manifest
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.TextureView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import io.agora.agora_videocall_jetpack_compose.config.APP_ID
import io.agora.agora_videocall_jetpack_compose.config.token
import io.agora.rtc.Constants
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas


private const val PERMISSION_REQ_ID = 22

// Ask for Android device permissions at runtime.
private val REQUESTED_PERMISSIONS = arrayOf<String>(
    Manifest.permission.RECORD_AUDIO,
    Manifest.permission.CAMERA,
    Manifest.permission.WRITE_EXTERNAL_STORAGE
)
private val permissions = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)

class VideoActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Turn off the decor fitting system windows, which allows us to handle insets,
        // including IME animations
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val channelName = intent.getStringExtra("ChannelName")
        val userRole = intent.getStringExtra("UserRole")

        setContent {
            Scaffold() {
                UIRequirePermissions(
                    permissions = permissions,
                    onPermissionGranted = {
                        if (channelName != null && userRole != null) {
                            CallScreen(channelName = channelName, userRole = userRole)
                        }
                    },
                    onPermissionDenied = {
                        AlertScreen(it)
                    }
                )
            }
        }
    }
}

@Composable
private fun CallScreen(channelName: String, userRole: String) {
    val context = LocalContext.current

    val localSurfaceView: TextureView? by remember {
        mutableStateOf(RtcEngine.CreateTextureView(context))
    }

    var remoteUserMap by remember {
        mutableStateOf(mapOf<Int, TextureView?>())
    }

    val mEngine = remember {
        initEngine(context, object : IRtcEngineEventHandler() {
            override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                Log.d(TAG, "channel:$channel,uid:$uid,elapsed:$elapsed")
            }

            override fun onUserJoined(uid: Int, elapsed: Int) {
                Log.d(TAG, "onUserJoined:$uid")
                val desiredUserList = remoteUserMap.toMutableMap()
                desiredUserList[uid] = null
                remoteUserMap = desiredUserList.toMap()
            }

            override fun onUserOffline(uid: Int, reason: Int) {
                Log.d(TAG, "onUserOffline:$uid")
                val desiredUserList = remoteUserMap.toMutableMap()
                desiredUserList.remove(uid)
                remoteUserMap = desiredUserList.toMap()
            }


        }, channelName, userRole)
    }
    if(userRole == "Broadcaster") {
        mEngine.setupLocalVideo(VideoCanvas(localSurfaceView, Constants.RENDER_MODE_FIT, 0))
    }

    Box(Modifier.fillMaxSize()) {
        localSurfaceView?.let { local ->
            AndroidView(factory = { local }, Modifier.fillMaxSize())
        }
        RemoteView(remoteListInfo = remoteUserMap, mEngine = mEngine)
        UserControls(mEngine = mEngine)
    }

}

@Composable
private fun RemoteView(remoteListInfo: Map<Int, TextureView?>, mEngine: RtcEngine) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(fraction = 0.2f)
            .horizontalScroll(state = rememberScrollState())
    ) {
        remoteListInfo.forEach { entry ->
            val remoteTextureView =
                RtcEngine.CreateTextureView(context).takeIf { entry.value == null }?:entry.value
            AndroidView(
                factory = { remoteTextureView!! },
                modifier = Modifier.size(Dp(180f), Dp(240f))
            )
            mEngine.setupRemoteVideo(
                VideoCanvas(
                    remoteTextureView,
                    Constants.RENDER_MODE_HIDDEN,
                    entry.key
                )
            )
        }
    }
}

fun initEngine(current: Context, eventHandler: IRtcEngineEventHandler, channelName: String, userRole: String): RtcEngine =
    RtcEngine.create(current, APP_ID, eventHandler).apply {
        enableVideo()
        setChannelProfile(1)
        if (userRole == "Broadcaster") {
            setClientRole(1)
        } else {
            setClientRole(0)
        }
        joinChannel(token, channelName, "", 0)
    }

@Composable
private fun UserControls(mEngine: RtcEngine) {
    var muted by remember { mutableStateOf(false) }
    var videoDisabled by remember { mutableStateOf(false) }
    val activity = (LocalContext.current as? Activity)

    Row(
        modifier = Modifier.fillMaxSize().padding(bottom = 50.dp),
        Arrangement.SpaceEvenly,
        Alignment.Bottom
    ) {
        OutlinedButton(
            onClick = { 
                muted = !muted
                mEngine.muteLocalAudioStream(muted)
            },
            shape = CircleShape,
            modifier = Modifier.size(50.dp),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.outlinedButtonColors(backgroundColor = if (muted) Color.Blue else Color.White)
            ) {
            if (muted) {
                Icon(Icons.Rounded.MicOff, contentDescription = "Tap to unmute mic", tint = Color.White)
            } else {
                Icon(Icons.Rounded.Mic, contentDescription = "Tap to mute mic", tint = Color.Blue)
            }
        }
        OutlinedButton(
            onClick = {
                mEngine.leaveChannel()
                activity?.finish()
            },
            shape = CircleShape,
            modifier = Modifier.size(70.dp),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Red)
        ) {
            Icon(Icons.Rounded.CallEnd, contentDescription = "Tap to disconnect Call", tint = Color.White)

        }
        OutlinedButton(
            onClick = {
                videoDisabled = !videoDisabled
                mEngine.muteLocalVideoStream(videoDisabled)
            },
            shape = CircleShape,
            modifier = Modifier.size(50.dp),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.outlinedButtonColors(backgroundColor = if (videoDisabled) Color.Blue else Color.White)
        ) {
            if (videoDisabled) {
                Icon(Icons.Rounded.VideocamOff, contentDescription = "Tap to enable Video", tint = Color.White)
            } else {
                Icon(Icons.Rounded.Videocam, contentDescription = "Tap to disable Video", tint = Color.Blue)
            }
        }
    }
}
@Composable
private fun AlertScreen(requester: () -> Unit) {
    val context = LocalContext.current

    Log.d(TAG, "AlertScreen: ")
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.Red),
        contentAlignment = Alignment.Center
    ) {
        Button(onClick = {
            requestPermissions(
                context as Activity,
                permissions,
                22
            )
            requester()
        }) {
            Icon(Icons.Rounded.Warning, "Permission Required")
            Text(text = "Permission Required")
        }
    }
}

/**
 * Helper Function for Permission Check
 */
@Composable
private fun UIRequirePermissions(
    permissions: Array<String>,
    onPermissionGranted: @Composable () -> Unit,
    onPermissionDenied: @Composable (requester: () -> Unit) -> Unit
) {
    Log.d(TAG, "UIRequirePermissions: ")
    val context = LocalContext.current

    var grantState by remember {
        mutableStateOf(permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        })
    }

    if (grantState) onPermissionGranted()
    else {
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
            onResult = {
                grantState = !it.containsValue(false)
            }
        )
        onPermissionDenied {
            Log.d(TAG, "launcher.launch")
            launcher.launch(permissions)
        }
    }
}
