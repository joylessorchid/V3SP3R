package com.vesper.flipper.ble

enum class FirmwareFamily {
    UNKNOWN,
    OFFICIAL,
    MOMENTUM,
    UNLEASHED,
    ROGUEMASTER,
    XTREME
}

enum class FirmwareTransportMode {
    UNAVAILABLE,
    PROBING,
    CLI_ONLY,
    RPC_ONLY,
    CLI_AND_RPC
}

enum class FirmwareCommandRoute {
    DIRECT_CLI,
    RPC_APP_BRIDGE,
    UNSUPPORTED
}

data class FirmwareCompatibilityProfile(
    val family: FirmwareFamily = FirmwareFamily.UNKNOWN,
    val label: String = "Unknown",
    val transportMode: FirmwareTransportMode = FirmwareTransportMode.UNAVAILABLE,
    val supportsCli: Boolean = false,
    val supportsRpc: Boolean = false,
    val supportsRpcAppBridge: Boolean = false,
    val confidence: Float = 0f,
    val notes: String = "No firmware compatibility data yet."
)

data class FirmwareCommandCompatibility(
    val supported: Boolean,
    val route: FirmwareCommandRoute,
    val message: String
)

object FirmwareCompatibilityLayer {

    fun assessCliCommand(
        profile: FirmwareCompatibilityProfile,
        command: String,
        hasRpcMapping: Boolean
    ): FirmwareCommandCompatibility {
        val normalized = command.trim().lowercase()
        if (normalized.isBlank()) {
            return FirmwareCommandCompatibility(
                supported = false,
                route = FirmwareCommandRoute.UNSUPPORTED,
                message = "Command is empty."
            )
        }

        // During PROBING, allow commands through even though supportsCli/supportsRpc
        // haven't been confirmed yet — the probe hasn't completed.
        if (profile.transportMode == FirmwareTransportMode.PROBING) {
            return FirmwareCommandCompatibility(
                supported = true,
                route = if (hasRpcMapping) FirmwareCommandRoute.RPC_APP_BRIDGE
                         else FirmwareCommandRoute.DIRECT_CLI,
                message = "Transport probing in progress — attempting command."
            )
        }

        // A command with an RPC mapping is allowed through the RPC bridge as long as
        // the transport is not explicitly torn down, WITHOUT first consulting
        // supportsRpc. That flag comes from the CLI capability probe, which on
        // BLE-only firmware (Momentum reports exactly this) fails or is deferred, and
        // it is cleared to false on every connection reset. Gating a mapped command on
        // it meant "transmit this .sub", "run this BadUSB", "emulate this card" were
        // refused with "transport unavailable" while RPC was in fact working — the
        // same stale-flag failure that stopped the remote buttons. The RPC send itself
        // is the real test: if RPC is genuinely down the bridge returns a clean error,
        // rather than the command being blocked before it is ever attempted.
        if (hasRpcMapping && profile.transportMode != FirmwareTransportMode.UNAVAILABLE) {
            return FirmwareCommandCompatibility(
                supported = true,
                route = FirmwareCommandRoute.RPC_APP_BRIDGE,
                message = "Routing via RPC app bridge."
            )
        }

        if (!profile.supportsCli && !profile.supportsRpc) {
            return FirmwareCommandCompatibility(
                supported = false,
                route = FirmwareCommandRoute.UNSUPPORTED,
                message = "Automation transport is unavailable for this connection."
            )
        }

        return when (profile.transportMode) {
            FirmwareTransportMode.CLI_AND_RPC -> {
                if (hasRpcMapping && prefersRpcBridge(normalized)) {
                    FirmwareCommandCompatibility(
                        supported = true,
                        route = FirmwareCommandRoute.RPC_APP_BRIDGE,
                        message = "Using RPC app bridge for best compatibility on ${profile.label}."
                    )
                } else {
                    FirmwareCommandCompatibility(
                        supported = true,
                        route = FirmwareCommandRoute.DIRECT_CLI,
                        message = "Using direct CLI on ${profile.label}."
                    )
                }
            }

            FirmwareTransportMode.CLI_ONLY -> FirmwareCommandCompatibility(
                supported = true,
                route = FirmwareCommandRoute.DIRECT_CLI,
                message = "Using CLI-only transport."
            )

            FirmwareTransportMode.RPC_ONLY -> {
                if (hasRpcMapping) {
                    FirmwareCommandCompatibility(
                        supported = true,
                        route = FirmwareCommandRoute.RPC_APP_BRIDGE,
                        message = "CLI is unavailable on this transport, routing via RPC app bridge."
                    )
                } else {
                    FirmwareCommandCompatibility(
                        supported = false,
                        route = FirmwareCommandRoute.UNSUPPORTED,
                        message = "CLI is unavailable on this transport and this command has no RPC mapping."
                    )
                }
            }

            FirmwareTransportMode.PROBING -> FirmwareCommandCompatibility(
                supported = true,
                route = if (hasRpcMapping) FirmwareCommandRoute.RPC_APP_BRIDGE
                         else FirmwareCommandRoute.DIRECT_CLI,
                message = "Transport probing in progress — attempting command."
            )

            FirmwareTransportMode.UNAVAILABLE -> FirmwareCommandCompatibility(
                supported = false,
                route = FirmwareCommandRoute.UNSUPPORTED,
                message = "Command transport is unavailable."
            )
        }
    }

    private fun prefersRpcBridge(command: String): Boolean {
        return command.startsWith("badusb ") ||
                command.startsWith("subghz tx ") ||
                command.startsWith("subghz tx_from_file ") ||
                command.startsWith("ir tx ") ||
                command.startsWith("infrared tx ") ||
                command == "ble_spam" ||
                command == "blespam" ||
                command == "ble spam" ||
                command.startsWith("ble_spam ") ||
                command.startsWith("blespam ") ||
                command.startsWith("ble spam ") ||
                command.startsWith("nfc emulate ") ||
                command.startsWith("nfc emu ") ||
                command.startsWith("rfid emulate ") ||
                command.startsWith("ibutton emulate ")
    }
}
