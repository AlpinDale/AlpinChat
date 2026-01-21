package com.alpin.chat.tokenizer

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Native tokenizer wrapper for GPT2 BPE tokenization.
 *
 * This class provides a Kotlin interface to the native C tokenizer library.
 * It handles loading tokenizer files from assets and extracting them to the cache directory.
 *
 * Usage:
 * ```
 * val tokenizer = NativeTokenizer.getInstance(context)
 * if (tokenizer.isLoaded) {
 *     val tokens = tokenizer.encode("Hello, world!")
 *     val text = tokenizer.decode(tokens)
 *     val count = tokenizer.countTokens("Some text")
 * }
 * ```
 */
class NativeTokenizer private constructor(context: Context) {

    private var nativeHandle: Long = 0
    private val appContext = context.applicationContext

    val isLoaded: Boolean
        get() = nativeHandle != 0L && nativeIsLoaded(nativeHandle)

    val vocabSize: Int
        get() = if (nativeHandle != 0L) nativeVocabSize(nativeHandle) else 0

    init {
        nativeHandle = nativeCreate()
    }

    /**
     * Load tokenizer from assets.
     * Extracts vocab.json and merges.txt to cache directory if needed.
     *
     * @param vocabAssetPath Asset path for vocab.json (e.g., "tokenizers/glm4/vocab.json")
     * @param mergesAssetPath Asset path for merges.txt (e.g., "tokenizers/glm4/merges.txt")
     * @return true if loaded successfully
     */
    fun loadFromAssets(vocabAssetPath: String, mergesAssetPath: String): Boolean {
        if (nativeHandle == 0L) {
            Log.e(TAG, "Native handle not initialized")
            return false
        }

        try {
            // Extract assets to cache directory
            val cacheDir = File(appContext.cacheDir, "tokenizer")
            cacheDir.mkdirs()

            val vocabFile = extractAsset(vocabAssetPath, File(cacheDir, "vocab.json"))
            val mergesFile = extractAsset(mergesAssetPath, File(cacheDir, "merges.txt"))

            if (vocabFile == null || mergesFile == null) {
                Log.e(TAG, "Failed to extract tokenizer assets")
                return false
            }

            return nativeLoad(nativeHandle, vocabFile.absolutePath, mergesFile.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading tokenizer from assets", e)
            return false
        }
    }

    /**
     * Load tokenizer from file paths.
     *
     * @param vocabPath Path to vocab.json
     * @param mergesPath Path to merges.txt
     * @return true if loaded successfully
     */
    fun loadFromFiles(vocabPath: String, mergesPath: String): Boolean {
        if (nativeHandle == 0L) {
            Log.e(TAG, "Native handle not initialized")
            return false
        }

        return nativeLoad(nativeHandle, vocabPath, mergesPath)
    }

    /**
     * Encode text to token IDs.
     *
     * @param text Input text
     * @return Array of token IDs, or empty array on error
     */
    fun encode(text: String): IntArray {
        if (!isLoaded) {
            Log.w(TAG, "Tokenizer not loaded")
            return IntArray(0)
        }

        return nativeEncode(nativeHandle, text) ?: IntArray(0)
    }

    /**
     * Decode token IDs back to text.
     *
     * @param tokenIds Array of token IDs
     * @return Decoded text, or empty string on error
     */
    fun decode(tokenIds: IntArray): String {
        if (!isLoaded) {
            Log.w(TAG, "Tokenizer not loaded")
            return ""
        }

        return nativeDecode(nativeHandle, tokenIds) ?: ""
    }

    /**
     * Count tokens in text without returning the token array.
     * More efficient than encode() when you only need the count.
     *
     * @param text Input text
     * @return Number of tokens, or -1 on error
     */
    fun countTokens(text: String): Int {
        if (!isLoaded) {
            Log.w(TAG, "Tokenizer not loaded")
            return -1
        }

        return nativeCountTokens(nativeHandle, text)
    }

    /**
     * Release native resources.
     * After calling this, the tokenizer cannot be used.
     */
    fun destroy() {
        if (nativeHandle != 0L) {
            nativeDestroy(nativeHandle)
            nativeHandle = 0
        }
    }

    private fun extractAsset(assetPath: String, destFile: File): File? {
        // Check if already extracted and up-to-date
        if (destFile.exists()) {
            return destFile
        }

        return try {
            appContext.assets.open(assetPath).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract asset: $assetPath", e)
            null
        }
    }

    protected fun finalize() {
        destroy()
    }

    companion object {
        private const val TAG = "NativeTokenizer"

        @Volatile
        private var instance: NativeTokenizer? = null

        init {
            System.loadLibrary("tokenizer")
        }

        /**
         * Get singleton instance of NativeTokenizer.
         * The tokenizer is automatically initialized but not loaded.
         * Call loadFromAssets() or loadFromFiles() to load the vocabulary.
         */
        fun getInstance(context: Context): NativeTokenizer {
            return instance ?: synchronized(this) {
                instance ?: NativeTokenizer(context).also { instance = it }
            }
        }

        /**
         * Get singleton instance and load GLM4 tokenizer from assets.
         * This is a convenience method for the common case.
         */
        fun getGlm4Tokenizer(context: Context): NativeTokenizer {
            val tokenizer = getInstance(context)
            if (!tokenizer.isLoaded) {
                tokenizer.loadFromAssets(
                    "tokenizers/glm4/vocab.json",
                    "tokenizers/glm4/merges.txt"
                )
            }
            return tokenizer
        }

        // Native methods
        @JvmStatic
        private external fun nativeCreate(): Long

        @JvmStatic
        private external fun nativeLoad(handle: Long, vocabPath: String, mergesPath: String): Boolean

        @JvmStatic
        private external fun nativeEncode(handle: Long, text: String): IntArray?

        @JvmStatic
        private external fun nativeDecode(handle: Long, tokenIds: IntArray): String?

        @JvmStatic
        private external fun nativeCountTokens(handle: Long, text: String): Int

        @JvmStatic
        private external fun nativeVocabSize(handle: Long): Int

        @JvmStatic
        private external fun nativeIsLoaded(handle: Long): Boolean

        @JvmStatic
        private external fun nativeDestroy(handle: Long)
    }
}
