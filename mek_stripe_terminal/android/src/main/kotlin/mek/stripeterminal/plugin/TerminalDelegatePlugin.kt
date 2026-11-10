package mek.stripeterminal.plugin

import TerminalHandlersApi
import com.stripe.stripeterminal.external.callable.ConnectionTokenCallback
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider
import com.stripe.stripeterminal.external.callable.TerminalListener
import com.stripe.stripeterminal.external.models.ConnectionStatus
import com.stripe.stripeterminal.external.models.ConnectionTokenException
import com.stripe.stripeterminal.external.models.PaymentStatus
import mek.stripeterminal.mappings.toApi
import mek.stripeterminal.runOnMainThread

// The Terminal is a process-wide singleton that can be initialized only once, so it keeps this
// delegate for as long as the process lives, while the Flutter engine that initialized it can be
// destroyed and replaced (e.g. the activity is finished but the process survives).
// The delegate is therefore shared and forwards to the handlers of the engine that called
// `initialize` last, or fails fast when that engine has been detached.
object TerminalDelegatePlugin : ConnectionTokenProvider, TerminalListener {
    var handlers: TerminalHandlersApi? = null

    override fun fetchConnectionToken(callback: ConnectionTokenCallback) = runOnMainThread {
        val handlers = handlers
        if (handlers == null) {
            callback.onFailure(ConnectionTokenException("No Flutter engine is attached to the Terminal"))
            return@runOnMainThread
        }
        handlers.requestConnectionToken{ result -> result.fold(
            onSuccess = { token ->
                callback.onSuccess(token)
            },
            onFailure = { failure ->
                callback.onFailure(ConnectionTokenException(failure.message!!, failure.cause)) }
            );
        }
    }

    // region Terminal listeners
    override fun onConnectionStatusChange(status: ConnectionStatus) = runOnMainThread {
        handlers?.connectionStatusChange(status.toApi()) {}
    }

    override fun onPaymentStatusChange(status: PaymentStatus) = runOnMainThread {
        handlers?.paymentStatusChange(status.toApi()) {}
    }
    // endregion
}
