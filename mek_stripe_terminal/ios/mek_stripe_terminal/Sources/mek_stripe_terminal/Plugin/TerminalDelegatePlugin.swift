import Foundation
import StripeTerminal

// The Terminal is a process-wide singleton that can be initialized only once, so it keeps this
// delegate for as long as the process lives, while the Flutter engine that initialized it can be
// destroyed and replaced. The delegate is therefore shared and forwards to the handlers of the
// engine that called `initialize` last, or fails fast when that engine has been detached.
class TerminalDelegatePlugin: NSObject, ConnectionTokenProvider, TerminalDelegate {
    static let shared = TerminalDelegatePlugin()

    var handlers: TerminalHandlersApi?

    func fetchConnectionToken(_ completion: @escaping (String?, (any Error)?) -> Void) {
        DispatchQueue.main.async {
            guard let handlers = self.handlers else {
                completion(nil, createPigeonError(".no_engine", "No Flutter engine is attached to the Terminal"))
                return
            }
            handlers.requestConnectionToken { result in
                switch result {
                    case .success(let token):
                        completion(token, nil)

                    case .failure(let error):
                        completion(nil, error)
                    }
            }
        }
    }

    public func terminal(_: Terminal, didChangePaymentStatus status: PaymentStatus) {
        DispatchQueue.main.async {
            self.handlers?.paymentStatusChange(
                status: status.toApi(),
                completion: logUnexpectedResultError
            )
        }
    }

    public func terminal(_: Terminal, didChangeConnectionStatus status: ConnectionStatus) {
        DispatchQueue.main.async {
            self.handlers?.connectionStatusChange(
                status: status.toApi(),
                completion: logUnexpectedResultError
            )
        }
    }

}
