package cloud.mindbox.mobile_sdk

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FcmMessagingService : FirebaseMessagingService() {

    override fun onNewToken(s: String) {
    }

    override fun onMessageReceived(message: RemoteMessage) {
    }
}