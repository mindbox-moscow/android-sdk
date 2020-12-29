package com.mindbox.androidsdk.screens

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import cloud.mindbox.mobile_sdk.Configuration
import cloud.mindbox.mobile_sdk.Mindbox
import cloud.mindbox.mobile_sdk.models.MindboxResponse
import com.mindbox.androidsdk.Prefs
import com.mindbox.androidsdk.R
import kotlinx.android.synthetic.main.fragment_entering_data.*
import org.json.JSONObject

class EnteringDataFragment(private val callback: (String, String, String) -> Unit) :
    Fragment(R.layout.fragment_entering_data) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupFields()

        nextButton.setOnClickListener {
            initSdk()
        }
    }

    private fun setupFields() {
        loadProgress.visibility = View.VISIBLE

        endpointData.setText(Prefs.enteredEndpoint)
        endpointData.setSelection(endpointData.text.toString().length)

        loadProgress.visibility = View.GONE
    }

    private fun initSdk() {
        loadProgress.visibility = View.VISIBLE
        val endpoint = endpointData.text.toString()
        val deviceId = deviceUuidData.text.toString()
        val installId = installationIdData.text.toString()

        if (endpoint.isNotEmpty() && endpoint != Prefs.enteredEndpoint) {
            Prefs.enteredEndpoint = endpoint
        }

        val configs = Configuration.Builder(endpoint)
            .setDeviceId(deviceId)
            .setInstallationId(installId)
            .build()

        Mindbox.init(this.requireContext(), configs) { response ->
            activity?.runOnUiThread {
                loadProgress.visibility = View.GONE
                when (response) {
                    is MindboxResponse.Error -> {
                        errorContainer.text =
                            """code: ${response.status}
                                        |
                                        |message: ${response.message}
                                        |
                                        |error body: ${JSONObject(response.errorBody?.string())}
                                    """.trimMargin()
                    }
                    is MindboxResponse.ValidationError -> {
                        errorContainer.text = response.messages.toString()
                    }
                    is MindboxResponse.SuccessResponse<*> -> {
                        callback.invoke(endpoint, deviceId, installId)
                    }
                }
            }
        }
    }
}