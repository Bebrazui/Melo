#include <jni.h>
#include <string.h>
#include <stdlib.h>
#include <pthread.h>
#include <unistd.h>
#include <getopt.h>
#include <sys/socket.h>
#include <android/log.h>

#define LOG_TAG "ByeDPI"
// Логи заглушены (no-op) — движок работает, в logcat ничего не пишет.
#define LOGI(...) ((void)0)
#define LOGE(...) ((void)0)

extern int server_fd;

static void *proxy_thread_func(void *arg);
static pthread_t proxy_thread;
static volatile int proxy_running = 0;
static char **saved_argv = NULL;
static int saved_argc = 0;

static void *proxy_thread_func(void *arg) {
    int argc = saved_argc;
    char **argv = saved_argv;

    LOGI("ByeDPI thread: argc=%d, calling main()...", argc);

    optind = 1;

    extern int main(int argc, char **argv);
    int result = main(argc, argv);

    LOGI("ByeDPI thread: main() returned %d", result);
    proxy_running = 0;
    return NULL;
}

JNIEXPORT jint JNICALL
Java_com_melo_music_byedpi_ByeDpiProxy_jniStartProxy(JNIEnv *env, jobject thiz, jobjectArray args) {
    if (proxy_running) {
        LOGE("Proxy already running");
        return -1;
    }

    int argc = (*env)->GetArrayLength(env, args);
    if (argc <= 0) {
        LOGE("No arguments provided");
        return -1;
    }

    // Free previous
    if (saved_argv) {
        for (int i = 0; i < saved_argc; i++) {
            free(saved_argv[i]);
        }
        free(saved_argv);
        saved_argv = NULL;
    }

    // argv[0] = "ciadpi" (program name for ByeDPI's arg parsing)
    saved_argc = argc + 1;
    saved_argv = (char **)calloc(saved_argc + 1, sizeof(char *));
    if (!saved_argv) return -1;

    saved_argv[0] = strdup("ciadpi");
    for (int i = 0; i < argc; i++) {
        jstring jarg = (jstring)(*env)->GetObjectArrayElement(env, args, i);
        const char *arg = (*env)->GetStringUTFChars(env, jarg, NULL);
        saved_argv[i + 1] = strdup(arg);
        (*env)->ReleaseStringUTFChars(env, jarg, arg);
        (*env)->DeleteLocalRef(env, jarg);
    }

    proxy_running = 1;
    int ret = pthread_create(&proxy_thread, NULL, proxy_thread_func, NULL);
    if (ret != 0) {
        LOGE("Failed to create thread: %d", ret);
        proxy_running = 0;
        return -1;
    }
    pthread_detach(proxy_thread);

    LOGI("ByeDPI proxy thread created");
    return 0;
}

JNIEXPORT jint JNICALL
Java_com_melo_music_byedpi_ByeDpiProxy_jniStopProxy(JNIEnv *env, jobject thiz) {
    if (!proxy_running) {
        LOGI("Proxy not running");
        return 0;
    }

    LOGI("Stopping ByeDPI proxy...");
    proxy_running = 0;

    // Shutdown the server socket to break the event loop
    if (server_fd >= 0) {
        shutdown(server_fd, SHUT_RDWR);
        close(server_fd);
        server_fd = -1;
    }

    LOGI("ByeDPI proxy stop signal sent");
    return 0;
}

JNIEXPORT jboolean JNICALL
Java_com_melo_music_byedpi_ByeDpiProxy_jniIsRunning(JNIEnv *env, jobject thiz) {
    return proxy_running ? JNI_TRUE : JNI_FALSE;
}
