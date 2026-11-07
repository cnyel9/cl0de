package com.cl0de.app.data

/**
 * The offline model catalog: copied field for field from PocketPal's bundled Android
 * device rules (rules_version 2026-09-24.1, repo a-ghorbani/pocketpal-ai, MIT).
 * Models are grouped into the two RAM classes this app offers: the PocketPal low+mid
 * tiers for devices under 8 GB, the high+flagship tiers for 8 GB and up. A model that
 * appears in both tier groups appears in both classes, exactly as in the rules.
 */
data class OfflineFile(val repo: String, val filename: String, val sizeBytes: Long, val sha256: String?) {
    val url: String get() = "https://huggingface.co/$repo/resolve/main/$filename"
}

data class OfflineModel(
    val id: String, val displayName: String, val quant: String,
    val file: OfflineFile, val mmproj: OfflineFile?, val minRamGb: Double,
    val inLowMid: Boolean, val inHighFlagship: Boolean,
) {
    val totalBytes: Long get() = file.sizeBytes + (mmproj?.sizeBytes ?: 0L)
    val files: List<OfflineFile> get() = listOfNotNull(file, mmproj)
}

object OfflineCatalog {
    val all: List<OfflineModel> = listOf(
        OfflineModel(
            id = "gemma-3-1b", displayName = "Gemma 3 1B", quant = "q4_k_m",
            file = OfflineFile("ggml-org/gemma-3-1b-it-GGUF", "gemma-3-1b-it-Q4_K_M.gguf", 806058240L, "8ccc5cd1f1b3602548715ae25a66ed73fd5dc68a210412eea643eb20eb75a135"),
            mmproj = null, minRamGb = 2.0,
            inLowMid = true, inHighFlagship = true,
        ),
        OfflineModel(
            id = "bonsai-1.7b", displayName = "Bonsai 1.7B", quant = "q1_0",
            file = OfflineFile("prism-ml/Bonsai-1.7B-gguf", "Bonsai-1.7B-Q1_0.gguf", 248302272L, "3d7c6c90dd98717a203adb22d5eacd2581850e40aa5327e144b97766cae5f7e3"),
            mmproj = null, minRamGb = 1.5,
            inLowMid = true, inHighFlagship = false,
        ),
        OfflineModel(
            id = "qwen3-0.6b", displayName = "Qwen3 0.6B", quant = "q4_k_m",
            file = OfflineFile("bartowski/Qwen_Qwen3-0.6B-GGUF", "Qwen_Qwen3-0.6B-Q4_K_M.gguf", 484220320L, "9acfc1e001311f34b4252001b626f2e466d592a42065f66571bff3790d4e1b14"),
            mmproj = null, minRamGb = 1.3,
            inLowMid = true, inHighFlagship = false,
        ),
        OfflineModel(
            id = "lfm2.5-350m", displayName = "LFM2.5 350M", quant = "q4_k_m",
            file = OfflineFile("LiquidAI/LFM2.5-350M-GGUF", "LFM2.5-350M-Q4_K_M.gguf", 229312224L, "7e6f72643caafc9a68256686638c4d7916f2cec76d1df478d4c3ddcd95a6aed4"),
            mmproj = null, minRamGb = 1.2,
            inLowMid = true, inHighFlagship = false,
        ),
        OfflineModel(
            id = "smollm3-3b", displayName = "SmolLM3 3B", quant = "q4_0",
            file = OfflineFile("bartowski/HuggingFaceTB_SmolLM3-3B-GGUF", "HuggingFaceTB_SmolLM3-3B-Q4_0.gguf", 1811455808L, "1e0e56d458e6b6d5ea18d0e5fe598cca252450308f1648afab49984fceae47da"),
            mmproj = null, minRamGb = 3.1,
            inLowMid = true, inHighFlagship = false,
        ),
        OfflineModel(
            id = "lfm2-2.6b", displayName = "LFM2 2.6B", quant = "q4_k_m",
            file = OfflineFile("LiquidAI/LFM2-2.6B-GGUF", "LFM2-2.6B-Q4_K_M.gguf", 1563668704L, "384bc877b6c37064982f96885bef69e4475919f5969218ed4e3b9399ae0340df"),
            mmproj = null, minRamGb = 2.5,
            inLowMid = true, inHighFlagship = false,
        ),
        OfflineModel(
            id = "qwen3-1.7b", displayName = "Qwen3 1.7B", quant = "q4_k_m",
            file = OfflineFile("bartowski/Qwen_Qwen3-1.7B-GGUF", "Qwen_Qwen3-1.7B-Q4_K_M.gguf", 1282439584L, "72c5c3cb38fa32d5256e2fe30d03e7a64c6c79e668ad84057e3bd66e250b24fb"),
            mmproj = null, minRamGb = 2.1,
            inLowMid = true, inHighFlagship = false,
        ),
        OfflineModel(
            id = "lfm2.5-1.2b", displayName = "LFM2.5 1.2B", quant = "q4_k_m",
            file = OfflineFile("LiquidAI/LFM2.5-1.2B-Instruct-GGUF", "LFM2.5-1.2B-Instruct-Q4_K_M.gguf", 730895168L, "b1b3de114215d9507409a662a501a631095a479a419584e8a2ded6304b19b4f5"),
            mmproj = null, minRamGb = 1.4,
            inLowMid = true, inHighFlagship = false,
        ),
        OfflineModel(
            id = "lfm2-vl-1.6b", displayName = "LFM2-VL 1.6B", quant = "q4_0",
            file = OfflineFile("LiquidAI/LFM2-VL-1.6B-GGUF", "LFM2-VL-1.6B-Q4_0.gguf", 695750048L, "ce0d4b122d328d14390ef160785da3a51a527f96844f392a04cb2db96f134e5d"),
            mmproj = OfflineFile("LiquidAI/LFM2-VL-1.6B-GGUF", "mmproj-LFM2-VL-1.6B-Q8_0.gguf", 564115648L, null), minRamGb = 2.5,
            inLowMid = true, inHighFlagship = false,
        ),
        OfflineModel(
            id = "bonsai-8b", displayName = "Bonsai 8B", quant = "q1_0",
            file = OfflineFile("prism-ml/Bonsai-8B-gguf", "Bonsai-8B-Q1_0.gguf", 1158654496L, "284a335aa3fb2ced3b1b01fcb40b08aa783e3b70832767f0dd2e3fdfa134bd54"),
            mmproj = null, minRamGb = 2.3,
            inLowMid = true, inHighFlagship = true,
        ),
        OfflineModel(
            id = "spark-x2.5-1.7b", displayName = "Spark-X2.5 1.7B", quant = "q4_k_m",
            file = OfflineFile("XHToken/Spark-X2.5-1.7B-GGUF", "Spark-X2.5-1.7B-Q4_K_M.gguf", 1107457856L, "902bde2522394954ac17821b3e5fd0df02defbc6944f122253f2580acf0503f4"),
            mmproj = null, minRamGb = 2.2,
            inLowMid = true, inHighFlagship = true,
        ),
        OfflineModel(
            id = "gemma-3-4b", displayName = "Gemma 3 4B", quant = "q4_0",
            file = OfflineFile("unsloth/gemma-3-4b-it-GGUF", "gemma-3-4b-it-Q4_0.gguf", 2370065536L, "a3aa2db653fc95923d76e5d9039861d56f10dfb3c906afe6bcf8f72340ef2cbf"),
            mmproj = null, minRamGb = 4.5,
            inLowMid = false, inHighFlagship = true,
        ),
        OfflineModel(
            id = "gemma-4-e2b", displayName = "Gemma 4 E2B", quant = "q4_0",
            file = OfflineFile("unsloth/gemma-4-E2B-it-GGUF", "gemma-4-E2B-it-Q4_0.gguf", 3041376384L, "9c4c1d48a462f7f883b28984ac4f42d366c9c573434eab68553e0f38bf7eb50c"),
            mmproj = OfflineFile("unsloth/gemma-4-E2B-it-GGUF", "mmproj-F16.gguf", 985654080L, null), minRamGb = 4.5,
            inLowMid = false, inHighFlagship = true,
        ),
        OfflineModel(
            id = "phi-4-mini-instruct", displayName = "Phi-4 Mini", quant = "q4_0",
            file = OfflineFile("bartowski/microsoft_Phi-4-mini-instruct-GGUF", "microsoft_Phi-4-mini-instruct-Q4_0.gguf", 2331442560L, "2124412a2d3410dd05c5d01796457283812210633165a2a86c909f815971518e"),
            mmproj = null, minRamGb = 4.1,
            inLowMid = false, inHighFlagship = true,
        ),
        OfflineModel(
            id = "bonsai-4b", displayName = "Bonsai 4B", quant = "q1_0",
            file = OfflineFile("prism-ml/Bonsai-4B-gguf", "Bonsai-4B-Q1_0.gguf", 572270624L, "4524b3f997f0f06444e568d1f26e2efd69effa3218c7ad3047432fb171e42168"),
            mmproj = null, minRamGb = 1.6,
            inLowMid = false, inHighFlagship = true,
        ),
        OfflineModel(
            id = "gemma-4-e4b", displayName = "Gemma 4 E4B", quant = "q4_0",
            file = OfflineFile("unsloth/gemma-4-E4B-it-GGUF", "gemma-4-E4B-it-Q4_0.gguf", 4836000928L, "d51af14007fe16deb45107de2411660be3b59c9e1c89bbde3bfec8ece65eeedc"),
            mmproj = OfflineFile("unsloth/gemma-4-E4B-it-GGUF", "mmproj-F16.gguf", 990372672L, null), minRamGb = 7.9,
            inLowMid = false, inHighFlagship = true,
        ),
        OfflineModel(
            id = "lfm2.5-8b-a1b", displayName = "LFM2.5 8B-A1B", quant = "q4_0",
            file = OfflineFile("LiquidAI/LFM2.5-8B-A1B-GGUF", "LFM2.5-8B-A1B-Q4_0.gguf", 4844678368L, "48ed1465d761311b2fd57b7fb46cf969a20b3a8281945b04e10f52bd1609e715"),
            mmproj = null, minRamGb = 6.3,
            inLowMid = false, inHighFlagship = true,
        ),
        OfflineModel(
            id = "ministral-3-3b", displayName = "Ministral 3 3B", quant = "q4_0",
            file = OfflineFile("bartowski/mistralai_Ministral-3-3B-Instruct-2512-GGUF", "mistralai_Ministral-3-3B-Instruct-2512-Q4_0.gguf", 2046375904L, "c2993f4b86ddcae94031b2e384169889ceecd51b148ceda30c0f826fcce721c8"),
            mmproj = null, minRamGb = 3.7,
            inLowMid = false, inHighFlagship = true,
        ),
        OfflineModel(
            id = "qwen3.5-4b", displayName = "Qwen3.5 4B", quant = "q4_0",
            file = OfflineFile("unsloth/Qwen3.5-4B-GGUF", "Qwen3.5-4B-Q4_0.gguf", 2583221408L, "298fcb5fe7a77ccc79745ae24751560c5ac56874caff4bb39b1f2055bd72b8bb"),
            mmproj = null, minRamGb = 4.3,
            inLowMid = false, inHighFlagship = true,
        ),
        OfflineModel(
            id = "spark-x2.5-4b", displayName = "Spark-X2.5 4B", quant = "q4_k_m",
            file = OfflineFile("XHToken/Spark-X2.5-4B-GGUF", "Spark-X2.5-4B-Q4_K_M.gguf", 2600224352L, "adfcfa19a4ed6a5985da8bf565fe15f8e1a7e131d79bae2d19d48d1c40109428"),
            mmproj = null, minRamGb = 4.0,
            inLowMid = false, inHighFlagship = true,
        ),
    )

    fun byId(id: String): OfflineModel? = all.firstOrNull { it.id == id }

    /** Marketed 8 GB devices report a little under 8e9 bytes of total RAM; 7e9 separates them from 6 GB ones. */
    fun isHighClass(totalRamBytes: Long): Boolean = totalRamBytes >= 7_000_000_000L

    fun forRam(totalRamBytes: Long): List<OfflineModel> {
        val high = isHighClass(totalRamBytes)
        return all.filter { if (high) it.inHighFlagship else it.inLowMid }
    }

    /** Per-model gate on top of the class: the device must meet the model's own minimum RAM (with slack for reported totals). */
    fun ramOk(totalRamBytes: Long, m: OfflineModel): Boolean =
        totalRamBytes / 1_073_741_824.0 + 0.6 >= m.minRamGb
}
