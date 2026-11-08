package com.cl0de.app.runtime

/** The offline-model server (llama-server in the same proot), supervised separately from OpenCode. */
sealed interface LlamaState {
    data object Stopped : LlamaState
    data class Starting(val modelId: String) : LlamaState
    data class Running(val modelId: String) : LlamaState
    data class Failed(val message: String) : LlamaState
}

/** The Deep bridge (a Node.js process in the same proot fronting the DeepChat module), supervised separately. */
sealed interface DeepState {
    data object Stopped : DeepState
    data object Starting : DeepState
    data object Running : DeepState
    data class Failed(val message: String) : DeepState
}

sealed interface RuntimeState {
    /** The environment isn't installed yet. */
    data object NotInstalled : RuntimeState
    data class Installing(val step: String, val progress: Float?, val detail: String = "") : RuntimeState
    data class InstallFailed(val message: String) : RuntimeState
    data object Stopped : RuntimeState
    data object Starting : RuntimeState
    data class Running(val version: String) : RuntimeState
    data class Crashed(val message: String) : RuntimeState
}
