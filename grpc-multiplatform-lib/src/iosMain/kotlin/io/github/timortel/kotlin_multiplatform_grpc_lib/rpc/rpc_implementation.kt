package io.github.timortel.kotlin_multiplatform_grpc_lib.rpc

import cocoapods.GRPCClient.GRPCCall2
import cocoapods.GRPCClient.GRPCCallOptions
import cocoapods.GRPCClient.GRPCMutableCallOptions
import cocoapods.GRPCClient.GRPCResponseHandlerProtocol
import io.github.timortel.kotlin_multiplatform_grpc_lib.KMChannel
import io.github.timortel.kotlin_multiplatform_grpc_lib.KMCode
import io.github.timortel.kotlin_multiplatform_grpc_lib.KMStatus
import io.github.timortel.kotlin_multiplatform_grpc_lib.KMStatusException
import io.github.timortel.kotlin_multiplatform_grpc_lib.message.KMMessage
import io.github.timortel.kotlin_multiplatform_grpc_lib.message.MessageDeserializer
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.darwin.*
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Throws(KMStatusException::class, CancellationException::class)
suspend fun <REQ : KMMessage, RES : KMMessage> unaryCallImplementation(
    channel: KMChannel,
    callOptions: GRPCCallOptions,
    path: String,
    request: REQ,
    responseDeserializer: MessageDeserializer<RES, NSData>
): RES {
    val data = request.serialize()

    return suspendCoroutine { continuation ->
        val handler = CallHandler(
            onReceive = { data: Any ->
                val response = responseDeserializer.deserialize(data as NSData)
                continuation.resume(response)
            },
            onError = { error: NSError ->
                val exception =
                    KMStatusException(
                        KMStatus(KMCode.getCodeForValue(error.code.toInt()), error.description ?: "No description"),
                        null
                    )

                continuation.resumeWithException(exception)
            },
            onDone = {}
        )

        val call = GRPCCall2(
            requestOptions = channel.buildRequestOptions(path),
            responseHandler = handler,
            callOptions = channel.applyToCallOptions(callOptions)
        )

        call.start()
        call.writeData(data)

        call.finish()
    }
}

fun <REQ : KMMessage, RES : KMMessage> serverSideStreamingCallImplementation(
    channel: KMChannel,
    callOptions: GRPCCallOptions,
    path: String,
    request: REQ,
    responseDeserializer: MessageDeserializer<RES, NSData>
): Flow<RES> {
    return callbackFlow {
        val handler = CallHandler(
            onReceive = { data ->
                val msg = responseDeserializer.deserialize(data as NSData)

                trySend(msg)
            },
            onError = { error ->
                val exception = KMStatusException(
                    KMStatus(KMCode.getCodeForValue(error.code.toInt()), error.description ?: "No description"),
                    null
                )

                close(exception)
            },
            onDone = {
                close()
            }
        )

        val call = GRPCCall2(channel.buildRequestOptions(path), handler, channel.applyToCallOptions(callOptions))

        call.start()
        call.writeData(request.serialize())
        call.finish()

        awaitClose {
            call.cancel()
        }
    }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
fun <REQ : KMMessage, RES : KMMessage> serverSideNonSuspendingStreamingCallImplementation(
    channel: KMChannel,
    path: String,
    request: REQ,
    responseDeserializer: MessageDeserializer<RES>
): Flow<RES> {
    return callbackFlow {
        val scope = this
        val handler = CallHandler(
            onReceive = { data ->
                val msg = responseDeserializer.deserialize(data as NSData)
                trySend(msg)
            },
            onError = { error ->
                val exception = KMStatusException(
                    KMStatus(KMCode.getCodeForValue(error.code.toInt()), error.description ?: "No description"),
                    null
                )
                cancel(CancellationException(error.code.toString(), exception))
            },
            onDone = {
                scope.close()
            }
        )
        val call = GRPCCall2(channel.buildRequestOptions(path), handler, channel.callOptions)
        call.start()
        call.writeData(request.serialize())
        call.finish()

        invokeOnClose {
            call.cancel()
        }
    }
}

private class CallHandler(
    private val onReceive: (data: Any) -> Unit,
    private val onError: (error: NSError) -> Unit,
    private val onDone: () -> Unit
) :
    NSObject(), GRPCResponseHandlerProtocol {

    override fun dispatchQueue(): dispatch_queue_t = null

    override fun didReceiveData(data: Any) = onReceive(data)

    override fun didCloseWithTrailingMetadata(trailingMetadata: Map<Any?, *>?, error: NSError?) {
        if (error != null) {
            onError(error)
        }
        onDone()
    }
}
