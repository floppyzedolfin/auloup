package com.floppyzedolfin.auloup

import android.telecom.Call
import android.telecom.CallScreeningService
import kotlinx.coroutines.runBlocking

/**
 * Screens every incoming call once AuLoup! holds the call-screening role,
 * rejecting any call whose number starts with a blocked prefix.
 *
 * The system gives us only a few seconds to respond, so the small blocking
 * read of the stored prefixes here is acceptable.
 */
class PrefixCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        // Only act on incoming calls; let everything else through untouched.
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        val number = callDetails.handle?.schemeSpecificPart.orEmpty()
        // Canonicalise to international form using the device's region, so a
        // national-format caller ID (e.g. "0160…") matches a stored "+33160".
        val region = Countries.defaultFor(applicationContext)
        val international = Prefixes.toInternational(number, region.dialCode, region.trunkPrefix) ?: number
        val result = runBlocking {
            PrefixRepository(applicationContext)
                .screenAndRecord(number, international, System.currentTimeMillis())
        }

        if (result.blocked && result.notify) {
            Notifications.notifyBlocked(applicationContext, number)
        }

        val response = CallResponse.Builder()
            .setDisallowCall(result.blocked)
            .setRejectCall(result.blocked)
            .setSkipCallLog(result.blocked)
            .setSkipNotification(result.blocked)
            .build()
        respondToCall(callDetails, response)
    }
}
