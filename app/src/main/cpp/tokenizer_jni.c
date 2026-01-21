/**
 * JNI bridge for GPT2 BPE tokenizer
 *
 * This provides the native interface between Kotlin and the C tokenizer library.
 */

#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <android/log.h>
#include "tokenizer/gpt2bpe.h"
#include "tokenizer/simd.h"

#define LOG_TAG "TokenizerJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Maximum tokens for encode output buffer
#define MAX_TOKENS 32768

// Store tokenizer pointer in a native handle
typedef struct {
    GPT2BPETokenizer tokenizer;
    jboolean loaded;
} TokenizerHandle;

// Helper to get native handle from Java long
static TokenizerHandle* get_handle(jlong ptr) {
    return (TokenizerHandle*)(intptr_t)ptr;
}

/**
 * Initialize SIMD and create a new tokenizer instance
 * Returns: native pointer handle (long)
 */
JNIEXPORT jlong JNICALL
Java_com_alpin_chat_tokenizer_NativeTokenizer_nativeCreate(JNIEnv *env, jclass clazz) {
    // Initialize SIMD on first use
    simd_init();

    TokenizerHandle *handle = (TokenizerHandle*)calloc(1, sizeof(TokenizerHandle));
    if (!handle) {
        LOGE("Failed to allocate tokenizer handle");
        return 0;
    }

    gpt2_init(&handle->tokenizer);
    handle->loaded = JNI_FALSE;

    LOGI("Created tokenizer handle: %p", handle);
    return (jlong)(intptr_t)handle;
}

/**
 * Load tokenizer from vocab.json and merges.txt files
 * @param handle: native pointer
 * @param vocabPath: path to vocab.json
 * @param mergesPath: path to merges.txt
 * Returns: true if loaded successfully
 */
JNIEXPORT jboolean JNICALL
Java_com_alpin_chat_tokenizer_NativeTokenizer_nativeLoad(
    JNIEnv *env, jclass clazz, jlong ptr, jstring vocabPath, jstring mergesPath) {

    TokenizerHandle *handle = get_handle(ptr);
    if (!handle) {
        LOGE("Invalid handle");
        return JNI_FALSE;
    }

    const char *vocab = (*env)->GetStringUTFChars(env, vocabPath, NULL);
    const char *merges = (*env)->GetStringUTFChars(env, mergesPath, NULL);

    if (!vocab || !merges) {
        LOGE("Failed to get path strings");
        if (vocab) (*env)->ReleaseStringUTFChars(env, vocabPath, vocab);
        if (merges) (*env)->ReleaseStringUTFChars(env, mergesPath, merges);
        return JNI_FALSE;
    }

    LOGI("Loading tokenizer: vocab=%s, merges=%s", vocab, merges);

    jboolean result = gpt2_load(&handle->tokenizer, vocab, merges) ? JNI_TRUE : JNI_FALSE;
    handle->loaded = result;

    if (result) {
        LOGI("Tokenizer loaded successfully. Vocab size: %d", gpt2_vocab_size(&handle->tokenizer));
    } else {
        LOGE("Failed to load tokenizer");
    }

    (*env)->ReleaseStringUTFChars(env, vocabPath, vocab);
    (*env)->ReleaseStringUTFChars(env, mergesPath, merges);

    return result;
}

/**
 * Encode text to token IDs
 * @param handle: native pointer
 * @param text: input text to encode
 * Returns: int array of token IDs
 */
JNIEXPORT jintArray JNICALL
Java_com_alpin_chat_tokenizer_NativeTokenizer_nativeEncode(
    JNIEnv *env, jclass clazz, jlong ptr, jstring text) {

    TokenizerHandle *handle = get_handle(ptr);
    if (!handle || !handle->loaded) {
        LOGE("Tokenizer not loaded");
        return NULL;
    }

    const char *input = (*env)->GetStringUTFChars(env, text, NULL);
    if (!input) {
        LOGE("Failed to get input string");
        return NULL;
    }

    // Allocate output buffer
    uint32_t *tokens = (uint32_t*)malloc(MAX_TOKENS * sizeof(uint32_t));
    if (!tokens) {
        LOGE("Failed to allocate token buffer");
        (*env)->ReleaseStringUTFChars(env, text, input);
        return NULL;
    }

    // Encode
    int count = gpt2_encode(&handle->tokenizer, input, tokens, MAX_TOKENS);

    (*env)->ReleaseStringUTFChars(env, text, input);

    if (count < 0) {
        LOGE("Encoding failed");
        free(tokens);
        return NULL;
    }

    // Create Java int array
    jintArray result = (*env)->NewIntArray(env, count);
    if (result) {
        (*env)->SetIntArrayRegion(env, result, 0, count, (jint*)tokens);
    }

    free(tokens);
    return result;
}

/**
 * Decode token IDs back to text
 * @param handle: native pointer
 * @param tokenIds: array of token IDs
 * Returns: decoded string
 */
JNIEXPORT jstring JNICALL
Java_com_alpin_chat_tokenizer_NativeTokenizer_nativeDecode(
    JNIEnv *env, jclass clazz, jlong ptr, jintArray tokenIds) {

    TokenizerHandle *handle = get_handle(ptr);
    if (!handle || !handle->loaded) {
        LOGE("Tokenizer not loaded");
        return NULL;
    }

    jsize count = (*env)->GetArrayLength(env, tokenIds);
    if (count == 0) {
        return (*env)->NewStringUTF(env, "");
    }

    jint *ids = (*env)->GetIntArrayElements(env, tokenIds, NULL);
    if (!ids) {
        LOGE("Failed to get token array");
        return NULL;
    }

    // Decode
    char *decoded = gpt2_decode(&handle->tokenizer, (const uint32_t*)ids, (size_t)count);

    (*env)->ReleaseIntArrayElements(env, tokenIds, ids, JNI_ABORT);

    if (!decoded) {
        LOGE("Decoding failed");
        return NULL;
    }

    jstring result = (*env)->NewStringUTF(env, decoded);
    free(decoded);

    return result;
}

/**
 * Get vocabulary size
 */
JNIEXPORT jint JNICALL
Java_com_alpin_chat_tokenizer_NativeTokenizer_nativeVocabSize(
    JNIEnv *env, jclass clazz, jlong ptr) {

    TokenizerHandle *handle = get_handle(ptr);
    if (!handle || !handle->loaded) {
        return 0;
    }

    return gpt2_vocab_size(&handle->tokenizer);
}

/**
 * Count tokens in text (encode without returning the array)
 */
JNIEXPORT jint JNICALL
Java_com_alpin_chat_tokenizer_NativeTokenizer_nativeCountTokens(
    JNIEnv *env, jclass clazz, jlong ptr, jstring text) {

    TokenizerHandle *handle = get_handle(ptr);
    if (!handle || !handle->loaded) {
        LOGE("Tokenizer not loaded");
        return -1;
    }

    const char *input = (*env)->GetStringUTFChars(env, text, NULL);
    if (!input) {
        LOGE("Failed to get input string");
        return -1;
    }

    // Allocate output buffer
    uint32_t *tokens = (uint32_t*)malloc(MAX_TOKENS * sizeof(uint32_t));
    if (!tokens) {
        LOGE("Failed to allocate token buffer");
        (*env)->ReleaseStringUTFChars(env, text, input);
        return -1;
    }

    // Encode just to count
    int count = gpt2_encode(&handle->tokenizer, input, tokens, MAX_TOKENS);

    (*env)->ReleaseStringUTFChars(env, text, input);
    free(tokens);

    return count;
}

/**
 * Free tokenizer resources
 */
JNIEXPORT void JNICALL
Java_com_alpin_chat_tokenizer_NativeTokenizer_nativeDestroy(
    JNIEnv *env, jclass clazz, jlong ptr) {

    TokenizerHandle *handle = get_handle(ptr);
    if (handle) {
        LOGI("Destroying tokenizer handle: %p", handle);
        gpt2_free(&handle->tokenizer);
        free(handle);
    }
}

/**
 * Check if tokenizer is loaded
 */
JNIEXPORT jboolean JNICALL
Java_com_alpin_chat_tokenizer_NativeTokenizer_nativeIsLoaded(
    JNIEnv *env, jclass clazz, jlong ptr) {

    TokenizerHandle *handle = get_handle(ptr);
    return (handle && handle->loaded) ? JNI_TRUE : JNI_FALSE;
}
