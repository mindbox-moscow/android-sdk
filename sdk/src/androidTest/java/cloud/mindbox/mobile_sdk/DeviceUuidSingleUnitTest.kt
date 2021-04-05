package cloud.mindbox.mobile_sdk

import androidx.test.platform.app.InstrumentationRegistry
import cloud.mindbox.mobile_sdk.managers.DbManager
import cloud.mindbox.mobile_sdk.repository.MindboxPreferences
import org.junit.After
import org.junit.Assert
import org.junit.Test

class DeviceUuidSingleUnitTest {

    @Test
    fun subscribe_isCorrectReturn() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        var result = ""

        Mindbox.subscribeDeviceUuid(appContext) { deviceUuid ->
            result = deviceUuid
            println(deviceUuid)
        }

        val configs = MindboxConfiguration.Builder(appContext, "epi.ru", "some").build()

        Mindbox.init(appContext, configs)

        Thread.sleep(10000)

        Assert.assertEquals(true, result.isUuid())
    }

    @Test
    fun unsubscribe_isCorrect() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        var result = ""

        val subscribeId = Mindbox.subscribeDeviceUuid(appContext) { deviceUuid ->
            result = deviceUuid
        }

        Mindbox.disposeDeviceUuidSubscription(subscribeId)

        Thread.sleep(3000)

        val configs =
            MindboxConfiguration.Builder(appContext, "example.com", "someEndpoint").build()

        Mindbox.init(appContext, configs)

        Thread.sleep(5000)

        Assert.assertEquals("", result)
    }

    @Test
    fun wrongUnsubscribe_isCorrect() {
        Mindbox.disposeDeviceUuidSubscription("wrong_subscribe")
        Mindbox.disposeDeviceUuidSubscription("")

        Mindbox.initComponents(InstrumentationRegistry.getInstrumentation().targetContext) //for cancel method after test
    }

    @Test
    fun subscribeIdGeneration_isCorrect() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val subscribeId = Mindbox.subscribeDeviceUuid(appContext) { }

        Mindbox.disposeDeviceUuidSubscription(subscribeId)

        Assert.assertEquals(true, subscribeId.isUuid())
    }

    @After
    fun clear() {
        MindboxPreferences.clear()
        DbManager.removeConfiguration()
    }
}