/*
    This file is part of NetGuard.

    NetGuard is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    NetGuard is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with NetGuard.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2015-2017 by Marcel Bokhorst (M66B)
*/

#include "netguard.h"

// It is assumed that no packets will get lost and that packets arrive in order
// https://android.googlesource.com/platform/frameworks/base.git/+/master/services/core/jni/com_android_server_connectivity_Vpn.cpp

// Global variables

JavaVM *jvm = NULL;
int pipefds[2];
pthread_t thread_id = 0;
pthread_mutex_t lock;
char socks5_addr[INET6_ADDRSTRLEN + 1];
int socks5_port = 0;
char socks5_username[127 + 1];
char socks5_password[127 + 1];
int loglevel = ANDROID_LOG_WARN;

extern int max_tun_msg;
extern struct ng_session *ng_session;
extern FILE *pcap_file;
extern size_t pcap_record_size;
extern long pcap_file_size;

// JNI

jclass clsPacket;
jclass clsRR;
jclass clsData;

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    log_android(ANDROID_LOG_INFO, "JNI load");

    JNIEnv *env;
    if ((*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        log_android(ANDROID_LOG_INFO, "JNI load GetEnv failed");
        return -1;
    }

    const char *packet = "eu/faircode/netguard/Packet";
    clsPacket = jniGlobalRef(env, jniFindClass(env, packet));

    const char *rr = "eu/faircode/netguard/ResourceRecord";
    clsRR = jniGlobalRef(env, jniFindClass(env, rr));

    const char *vpndata = "com/antest1/kcanotify/KcaVpnData";
    clsData = jniGlobalRef(env, jniFindClass(env, vpndata));

    // Raise file number limit to maximum
    struct rlimit rlim;
    if (getrlimit(RLIMIT_NOFILE, &rlim))
        log_android(ANDROID_LOG_WARN, "getrlimit error %d: %s", errno, strerror(errno));
    else {
        rlim_t soft = rlim.rlim_cur;
        rlim.rlim_cur = rlim.rlim_max;
        if (setrlimit(RLIMIT_NOFILE, &rlim))
            log_android(ANDROID_LOG_WARN, "setrlimit error %d: %s", errno, strerror(errno));
        else
            log_android(ANDROID_LOG_WARN, "raised file limit from %d to %d", soft, rlim.rlim_cur);
    }

    return JNI_VERSION_1_6;
}

void JNI_OnUnload(JavaVM *vm, void *reserved) {
    log_android(ANDROID_LOG_INFO, "JNI unload");

    JNIEnv *env;
    if ((*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_6) != JNI_OK)
        log_android(ANDROID_LOG_INFO, "JNI load GetEnv failed");
    else {
        (*env)->DeleteGlobalRef(env, clsPacket);
        (*env)->DeleteGlobalRef(env, clsRR);
    }
}

// JNI ServiceSinkhole

JNIEXPORT void JNICALL
Java_com_antest1_kcanotify_KcaVpnService_jni_1init(JNIEnv *env, jobject instance, jint sdk) {
    loglevel = ANDROID_LOG_WARN;

    struct arguments args;
    args.env = env;
    args.instance = instance;
    args.sdk = sdk;
    init(&args);

    *socks5_addr = 0;
    socks5_port = 0;
    *socks5_username = 0;
    *socks5_password = 0;
    pcap_file = NULL;

    if (pthread_mutex_init(&lock, NULL))
        log_android(ANDROID_LOG_ERROR, "pthread_mutex_init failed");

    // Create signal pipe
    if (pipe(pipefds))
        log_android(ANDROID_LOG_ERROR, "Create pipe error %d: %s", errno, strerror(errno));
    else
        for (int i = 0; i < 2; i++) {
            int flags = fcntl(pipefds[i], F_GETFL, 0);
            if (flags < 0 || fcntl(pipefds[i], F_SETFL, flags | O_NONBLOCK) < 0)
                log_android(ANDROID_LOG_ERROR, "fcntl pipefds[%d] O_NONBLOCK error %d: %s",
                            i, errno, strerror(errno));
        }
}

JNIEXPORT void JNICALL
Java_com_antest1_kcanotify_KcaVpnService_jni_1start(
        JNIEnv *env, jobject instance, jint tun, jboolean fwd53, jint rcode, jint loglevel_) {

    loglevel = loglevel_;
    max_tun_msg = 0;
    log_android(ANDROID_LOG_WARN,
                "Starting tun %d fwd53 %d level %d thread %x",
                tun, fwd53, loglevel, thread_id);

    // Set blocking
    int flags = fcntl(tun, F_GETFL, 0);
    if (flags < 0 || fcntl(tun, F_SETFL, flags & ~O_NONBLOCK) < 0)
        log_android(ANDROID_LOG_ERROR, "fcntl tun ~O_NONBLOCK error %d: %s",
                    errno, strerror(errno));

    if (thread_id && pthread_kill(thread_id, 0) == 0)
        log_android(ANDROID_LOG_ERROR, "Already running thread %x", thread_id);
    else {
        jint rs = (*env)->GetJavaVM(env, &jvm);
        if (rs != JNI_OK)
            log_android(ANDROID_LOG_ERROR, "GetJavaVM failed");

        // Get arguments
        struct arguments *args = malloc(sizeof(struct arguments));
        // args->env = will be set in thread
        args->instance = (*env)->NewGlobalRef(env, instance);
        args->tun = tun;
        args->fwd53 = fwd53;
        args->rcode = rcode;

        // Start native thread
        int err = pthread_create(&thread_id, NULL, handle_events, (void *) args);
        if (err == 0)
            log_android(ANDROID_LOG_WARN, "Started thread %x", thread_id);
        else
            log_android(ANDROID_LOG_ERROR, "pthread_create error %d: %s", err, strerror(err));
    }
}

JNIEXPORT void JNICALL
Java_com_antest1_kcanotify_KcaVpnService_jni_1stop(
        JNIEnv *env, jobject instance, jint tun, jboolean clr) {
    pthread_t t = thread_id;
    log_android(ANDROID_LOG_WARN, "Stop tun %d  thread %x", tun, t);
    if (t && pthread_kill(t, 0) == 0) {
        log_android(ANDROID_LOG_WARN, "Write pipe thread %x", t);
        if (write(pipefds[1], "x", 1) < 0)
            log_android(ANDROID_LOG_WARN, "Write pipe error %d: %s", errno, strerror(errno));
        else {
            log_android(ANDROID_LOG_WARN, "Join thread %x", t);
            int err = pthread_join(t, NULL);
            if (err != 0)
                log_android(ANDROID_LOG_WARN, "pthread_join error %d: %s", err, strerror(err));
        }

        if (clr)
            clear();

        log_android(ANDROID_LOG_WARN, "Stopped thread %x", t);
    }
    else
        log_android(ANDROID_LOG_WARN, "Not running thread %x", t);
}

JNIEXPORT jint JNICALL
Java_com_antest1_kcanotify_KcaVpnService_jni_1get_1mtu(JNIEnv *env, jobject instance) {
    return get_mtu();
}

JNIEXPORT jintArray JNICALL
Java_com_antest1_kcanotify_KcaVpnService_jni_1get_1stats(JNIEnv *env, jobject instance) {
    if (pthread_mutex_lock(&lock))
        log_android(ANDROID_LOG_ERROR, "pthread_mutex_lock failed");

    jintArray jarray = (*env)->NewIntArray(env, 5);
    jint *jcount = (*env)->GetIntArrayElements(env, jarray, NULL);


    struct ng_session *s = ng_session;
    while (s != NULL) {
        if (s->protocol == IPPROTO_ICMP || s->protocol == IPPROTO_ICMPV6) {
            if (!s->icmp.stop)
                jcount[0]++;
        }
        else if (s->protocol == IPPROTO_UDP) {
            if (s->udp.state == UDP_ACTIVE)
                jcount[1]++;
        }
        else if (s->protocol == IPPROTO_TCP) {
            if (s->tcp.state != TCP_CLOSING && s->tcp.state != TCP_CLOSE)
                jcount[2]++;
        }
        s = s->next;
    }

    if (pthread_mutex_unlock(&lock))
        log_android(ANDROID_LOG_ERROR, "pthread_mutex_unlock failed");

    jcount[3] = 0;
    DIR *d = opendir("/proc/self/fd");
    if (d) {
        struct dirent *dir;
        while ((dir = readdir(d)) != NULL)
            if (dir->d_type != DT_DIR)
                jcount[3]++;
        closedir(d);
    }

    struct rlimit rlim;
    memset(&rlim, 0, sizeof(struct rlimit));
    getrlimit(RLIMIT_NOFILE, &rlim);
    jcount[4] = (jint) rlim.rlim_cur;

    (*env)->ReleaseIntArrayElements(env, jarray, jcount, NULL);
    return jarray;
}

JNIEXPORT void JNICALL
Java_com_antest1_kcanotify_KcaVpnService_jni_1pcap(
        JNIEnv *env, jclass type,
        jstring name_, jint record_size, jint file_size) {

    pcap_record_size = (size_t) record_size;
    pcap_file_size = file_size;

    if (pthread_mutex_lock(&lock))
        log_android(ANDROID_LOG_ERROR, "pthread_mutex_lock failed");

    if (name_ == NULL) {
        if (pcap_file != NULL) {
            int flags = fcntl(fileno(pcap_file), F_GETFL, 0);
            if (flags < 0 || fcntl(fileno(pcap_file), F_SETFL, flags & ~O_NONBLOCK) < 0)
                log_android(ANDROID_LOG_ERROR, "PCAP fcntl ~O_NONBLOCK error %d: %s",
                            errno, strerror(errno));

            if (fsync(fileno(pcap_file)))
                log_android(ANDROID_LOG_ERROR, "PCAP fsync error %d: %s", errno, strerror(errno));

            if (fclose(pcap_file))
                log_android(ANDROID_LOG_ERROR, "PCAP fclose error %d: %s", errno, strerror(errno));

            pcap_file = NULL;
        }
        log_android(ANDROID_LOG_WARN, "PCAP disabled");
    }
    else {
        const char *name = (*env)->GetStringUTFChars(env, name_, 0);
        log_android(ANDROID_LOG_WARN, "PCAP file %s record size %d truncate @%ld",
                    name, pcap_record_size, pcap_file_size);

        pcap_file = fopen(name, "ab+");
        if (pcap_file == NULL)
            log_android(ANDROID_LOG_ERROR, "PCAP fopen error %d: %s", errno, strerror(errno));
        else {
            int flags = fcntl(fileno(pcap_file), F_GETFL, 0);
            if (flags < 0 || fcntl(fileno(pcap_file), F_SETFL, flags | O_NONBLOCK) < 0)
                log_android(ANDROID_LOG_ERROR, "PCAP fcntl O_NONBLOCK error %d: %s",
                            errno, strerror(errno));

            long size = ftell(pcap_file);
            if (size == 0) {
                log_android(ANDROID_LOG_WARN, "PCAP initialize");
                write_pcap_hdr();
            }
            else
                log_android(ANDROID_LOG_WARN, "PCAP current size %ld", size);
        }

        (*env)->ReleaseStringUTFChars(env, name_, name);
    }

    if (pthread_mutex_unlock(&lock))
        log_android(ANDROID_LOG_ERROR, "pthread_mutex_unlock failed");
}

JNIEXPORT void JNICALL
Java_com_antest1_kcanotify_KcaVpnService_jni_1socks5(JNIEnv *env, jobject instance, jstring addr_,
                                                      jint port, jstring username_,
                                                      jstring password_) {
    const char *addr = (*env)->GetStringUTFChars(env, addr_, 0);
    const char *username = (*env)->GetStringUTFChars(env, username_, 0);
    const char *password = (*env)->GetStringUTFChars(env, password_, 0);

    strcpy(socks5_addr, addr);
    socks5_port = port;
    strcpy(socks5_username, username);
    strcpy(socks5_password, password);

    log_android(ANDROID_LOG_WARN, "SOCKS5 %s:%d user=%s",
                socks5_addr, socks5_port, socks5_username);

    (*env)->ReleaseStringUTFChars(env, addr_, addr);
    (*env)->ReleaseStringUTFChars(env, username_, username);
    (*env)->ReleaseStringUTFChars(env, password_, password);
}

JNIEXPORT void JNICALL
Java_com_antest1_kcanotify_KcaVpnService_jni_1done(JNIEnv *env, jobject instance) {
    log_android(ANDROID_LOG_INFO, "Done");

    clear();

    if (pthread_mutex_destroy(&lock))
        log_android(ANDROID_LOG_ERROR, "pthread_mutex_destroy failed");

    for (int i = 0; i < 2; i++)
        if (close(pipefds[i]))
            log_android(ANDROID_LOG_ERROR, "Close pipe error %d: %s", errno, strerror(errno));
}

// JNI Util

JNIEXPORT jstring JNICALL
Java_eu_faircode_netguard_Util_jni_1getprop(JNIEnv *env, jclass type, jstring name_) {
    const char *name = (*env)->GetStringUTFChars(env, name_, 0);

    char value[PROP_VALUE_MAX + 1] = "";
    __system_property_get(name, value);

    (*env)->ReleaseStringUTFChars(env, name_, name);

    return (*env)->NewStringUTF(env, value);
}

JNIEXPORT jboolean JNICALL
Java_eu_faircode_netguard_Util_is_1numeric_1address(JNIEnv *env, jclass type, jstring ip_) {
    jboolean numeric = 0;
    const char *ip = (*env)->GetStringUTFChars(env, ip_, 0);

    struct addrinfo hints;
    memset(&hints, 0, sizeof(struct addrinfo));
    hints.ai_family = AF_UNSPEC;
    hints.ai_flags = AI_NUMERICHOST;
    struct addrinfo *result;
    int err = getaddrinfo(ip, NULL, &hints, &result);
    if (err)
        log_android(ANDROID_LOG_DEBUG, "getaddrinfo(%s) error %d: %s", ip, err, gai_strerror(err));
    else
        numeric = (jboolean) (result != NULL);

    (*env)->ReleaseStringUTFChars(env, ip_, ip);
    return numeric;
}

void report_exit(const struct arguments *args, const char *fmt, ...) {
    jclass cls = (*args->env)->GetObjectClass(args->env, args->instance);
    jmethodID mid = jniGetMethodID(args->env, cls, "nativeExit", "(Ljava/lang/String;)V");

    jstring jreason = NULL;
    if (fmt != NULL) {
        char line[1024];
        va_list argptr;
        va_start(argptr, fmt);
        vsprintf(line, fmt, argptr);
        jreason = (*args->env)->NewStringUTF(args->env, line);
        va_end(argptr);
    }

    (*args->env)->CallVoidMethod(args->env, args->instance, mid, jreason);
    jniCheckException(args->env);

    if (jreason != NULL)
        (*args->env)->DeleteLocalRef(args->env, jreason);
    (*args->env)->DeleteLocalRef(args->env, cls);
}

void report_error(const struct arguments *args, jint error, const char *fmt, ...) {
    jclass cls = (*args->env)->GetObjectClass(args->env, args->instance);
    jmethodID mid = jniGetMethodID(args->env, cls, "nativeError", "(ILjava/lang/String;)V");

    jstring jreason = NULL;
    if (fmt != NULL) {
        char line[1024];
        va_list argptr;
        va_start(argptr, fmt);
        vsprintf(line, fmt, argptr);
        jreason = (*args->env)->NewStringUTF(args->env, line);
        va_end(argptr);
    }

    (*args->env)->CallVoidMethod(args->env, args->instance, mid, error, jreason);
    jniCheckException(args->env);

    if (jreason != NULL)
        (*args->env)->DeleteLocalRef(args->env, jreason);
    (*args->env)->DeleteLocalRef(args->env, cls);
}

static jmethodID midProtect = NULL;

int protect_socket(const struct arguments *args, int socket) {
    jclass cls = (*args->env)->GetObjectClass(args->env, args->instance);
    if (cls == NULL) {
        log_android(ANDROID_LOG_ERROR, "protect socket failed to get class");
        return -1;
    }
    if (midProtect == NULL)
        midProtect = jniGetMethodID(args->env, cls, "protect", "(I)Z");
    if (midProtect == NULL) {
        log_android(ANDROID_LOG_ERROR, "protect socket failed to get method");
        return -1;
    }

    jboolean isProtected = (*args->env)->CallBooleanMethod(
            args->env, args->instance, midProtect, socket);
    jniCheckException(args->env);

    if (!isProtected) {
        log_android(ANDROID_LOG_ERROR, "protect socket failed");
        return -1;
    }

    (*args->env)->DeleteLocalRef(args->env, cls);

    return 0;
}

// http://docs.oracle.com/javase/7/docs/technotes/guides/jni/spec/functions.html
// http://journals.ecs.soton.ac.uk/java/tutorial/native1.1/implementing/index.html

jobject jniGlobalRef(JNIEnv *env, jobject cls) {
    jobject gcls = (*env)->NewGlobalRef(env, cls);
    if (gcls == NULL)
        log_android(ANDROID_LOG_ERROR, "Global ref failed (out of memory?)");
    return gcls;
}

jclass jniFindClass(JNIEnv *env, const char *name) {
    jclass cls = (*env)->FindClass(env, name);
    if (cls == NULL)
        log_android(ANDROID_LOG_ERROR, "Class %s not found", name);
    else
        jniCheckException(env);
    return cls;
}

jmethodID jniGetMethodID(JNIEnv *env, jclass cls, const char *name, const char *signature) {
    jmethodID method = (*env)->GetMethodID(env, cls, name, signature);
    if (method == NULL) {
        log_android(ANDROID_LOG_ERROR, "Method %s %s not found", name, signature);
        jniCheckException(env);
    }
    return method;
}

jfieldID jniGetFieldID(JNIEnv *env, jclass cls, const char *name, const char *type) {
    jfieldID field = (*env)->GetFieldID(env, cls, name, type);
    if (field == NULL)
        log_android(ANDROID_LOG_ERROR, "Field %s type %s not found", name, type);
    return field;
}

jobject jniNewObject(JNIEnv *env, jclass cls, jmethodID constructor, const char *name) {
    jobject object = (*env)->NewObject(env, cls, constructor);
    if (object == NULL)
        log_android(ANDROID_LOG_ERROR, "Create object %s failed", name);
    else
        jniCheckException(env);
    return object;
}

int jniCheckException(JNIEnv *env) {
    jthrowable ex = (*env)->ExceptionOccurred(env);
    if (ex) {
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
        (*env)->DeleteLocalRef(env, ex);
        return 1;
    }
    return 0;
}

static jmethodID midLogPacket = NULL;

/*
void log_packet(const struct arguments *args, jobject jpacket) {
#ifdef PROFILE_JNI
    float mselapsed;
    struct timeval start, end;
    gettimeofday(&start, NULL);
#endif

    jclass clsService = (*args->env)->GetObjectClass(args->env, args->instance);

    const char *signature = "(Leu/faircode/netguard/Packet;)V";
    if (midLogPacket == NULL)
        midLogPacket = jniGetMethodID(args->env, clsService, "logPacket", signature);

    (*args->env)->CallVoidMethod(args->env, args->instance, midLogPacket, jpacket);
    jniCheckException(args->env);

    (*args->env)->DeleteLocalRef(args->env, clsService);
    (*args->env)->DeleteLocalRef(args->env, jpacket);

#ifdef PROFILE_JNI
    gettimeofday(&end, NULL);
    mselapsed = (end.tv_sec - start.tv_sec) * 1000.0 +
                (end.tv_usec - start.tv_usec) / 1000.0;
    if (mselapsed > PROFILE_JNI)
        log_android(ANDROID_LOG_WARN, "log_packet %f", mselapsed);
#endif
}
*/

static jmethodID midDnsResolved = NULL;
static jmethodID midInitRR = NULL;
jfieldID fidQTime = NULL;
jfieldID fidQName = NULL;
jfieldID fidAName = NULL;
jfieldID fidResource = NULL;
jfieldID fidTTL = NULL;

void dns_resolved(const struct arguments *args,
                  const char *qname, const char *aname, const char *resource, int ttl) {
#ifdef PROFILE_JNI
    float mselapsed;
    struct timeval start, end;
    gettimeofday(&start, NULL);
#endif

    jclass clsService = (*args->env)->GetObjectClass(args->env, args->instance);

    const char *signature = "(Leu/faircode/netguard/ResourceRecord;)V";
    if (midDnsResolved == NULL)
        midDnsResolved = jniGetMethodID(args->env, clsService, "dnsResolved", signature);

    const char *rr = "eu/faircode/netguard/ResourceRecord";
    if (midInitRR == NULL)
        midInitRR = jniGetMethodID(args->env, clsRR, "<init>", "()V");

    jobject jrr = jniNewObject(args->env, clsRR, midInitRR, rr);

    if (fidQTime == NULL) {
        const char *string = "Ljava/lang/String;";
        fidQTime = jniGetFieldID(args->env, clsRR, "Time", "J");
        fidQName = jniGetFieldID(args->env, clsRR, "QName", string);
        fidAName = jniGetFieldID(args->env, clsRR, "AName", string);
        fidResource = jniGetFieldID(args->env, clsRR, "Resource", string);
        fidTTL = jniGetFieldID(args->env, clsRR, "TTL", "I");
    }

    jlong jtime = time(NULL) * 1000LL;
    jstring jqname = (*args->env)->NewStringUTF(args->env, qname);
    jstring janame = (*args->env)->NewStringUTF(args->env, aname);
    jstring jresource = (*args->env)->NewStringUTF(args->env, resource);

    (*args->env)->SetLongField(args->env, jrr, fidQTime, jtime);
    (*args->env)->SetObjectField(args->env, jrr, fidQName, jqname);
    (*args->env)->SetObjectField(args->env, jrr, fidAName, janame);
    (*args->env)->SetObjectField(args->env, jrr, fidResource, jresource);
    (*args->env)->SetIntField(args->env, jrr, fidTTL, ttl);

    (*args->env)->CallVoidMethod(args->env, args->instance, midDnsResolved, jrr);
    jniCheckException(args->env);

    (*args->env)->DeleteLocalRef(args->env, jresource);
    (*args->env)->DeleteLocalRef(args->env, janame);
    (*args->env)->DeleteLocalRef(args->env, jqname);
    (*args->env)->DeleteLocalRef(args->env, jrr);
    (*args->env)->DeleteLocalRef(args->env, clsService);

#ifdef PROFILE_JNI
    gettimeofday(&end, NULL);
    mselapsed = (end.tv_sec - start.tv_sec) * 1000.0 +
                (end.tv_usec - start.tv_usec) / 1000.0;
    if (mselapsed > PROFILE_JNI)
        log_android(ANDROID_LOG_WARN, "log_packet %f", mselapsed);
#endif
}

static jmethodID midIsDomainBlocked = NULL;

/*
jboolean is_domain_blocked(const struct arguments *args, const char *name) {
#ifdef PROFILE_JNI
    float mselapsed;
    struct timeval start, end;
    gettimeofday(&start, NULL);
#endif

    jclass clsService = (*args->env)->GetObjectClass(args->env, args->instance);

    const char *signature = "(Ljava/lang/String;)Z";
    if (midIsDomainBlocked == NULL)
        midIsDomainBlocked = jniGetMethodID(args->env, clsService, "isDomainBlocked", signature);

    jstring jname = (*args->env)->NewStringUTF(args->env, name);

    jboolean jallowed = (*args->env)->CallBooleanMethod(
            args->env, args->instance, midIsDomainBlocked, jname);
    jniCheckException(args->env);

    (*args->env)->DeleteLocalRef(args->env, jname);
    (*args->env)->DeleteLocalRef(args->env, clsService);

#ifdef PROFILE_JNI
    gettimeofday(&end, NULL);
    mselapsed = (end.tv_sec - start.tv_sec) * 1000.0 +
                (end.tv_usec - start.tv_usec) / 1000.0;
    if (mselapsed > PROFILE_JNI)
        log_android(ANDROID_LOG_WARN, "is_domain_blocked %f", mselapsed);
#endif

    return jallowed;
}
*/

jmethodID midInitPacket = NULL;

jfieldID fidTime = NULL;
jfieldID fidVersion = NULL;
jfieldID fidProtocol = NULL;
jfieldID fidFlags = NULL;
jfieldID fidSaddr = NULL;
jfieldID fidSport = NULL;
jfieldID fidDaddr = NULL;
jfieldID fidDport = NULL;
jfieldID fidData = NULL;
jfieldID fidUid = NULL;
jfieldID fidAllowed = NULL;

jobject create_packet(const struct arguments *args,
                      jint version,
                      jint protocol,
                      const char *flags,
                      const char *source,
                      jint sport,
                      const char *dest,
                      jint dport,
                      const char *data,
                      jint uid,
                      jboolean allowed) {
    JNIEnv *env = args->env;

#ifdef PROFILE_JNI
    float mselapsed;
    struct timeval start, end;
    gettimeofday(&start, NULL);
#endif

    /*
        jbyte b[] = {1,2,3};
        jbyteArray ret = env->NewByteArray(3);
        env->SetByteArrayRegion (ret, 0, 3, b);
     */

    const char *packet = "eu/faircode/netguard/Packet";
    if (midInitPacket == NULL)
        midInitPacket = jniGetMethodID(env, clsPacket, "<init>", "()V");
    jobject jpacket = jniNewObject(env, clsPacket, midInitPacket, packet);

    if (fidTime == NULL) {
        const char *string = "Ljava/lang/String;";
        fidTime = jniGetFieldID(env, clsPacket, "time", "J");
        fidVersion = jniGetFieldID(env, clsPacket, "version", "I");
        fidProtocol = jniGetFieldID(env, clsPacket, "protocol", "I");
        fidFlags = jniGetFieldID(env, clsPacket, "flags", string);
        fidSaddr = jniGetFieldID(env, clsPacket, "saddr", string);
        fidSport = jniGetFieldID(env, clsPacket, "sport", "I");
        fidDaddr = jniGetFieldID(env, clsPacket, "daddr", string);
        fidDport = jniGetFieldID(env, clsPacket, "dport", "I");
        fidData = jniGetFieldID(env, clsPacket, "data", string);
        fidUid = jniGetFieldID(env, clsPacket, "uid", "I");
        fidAllowed = jniGetFieldID(env, clsPacket, "allowed", "Z");
    }

    struct timeval tv;
    gettimeofday(&tv, NULL);
    jlong t = tv.tv_sec * 1000LL + tv.tv_usec / 1000;
    jstring jflags = (*env)->NewStringUTF(env, flags);
    jstring jsource = (*env)->NewStringUTF(env, source);
    jstring jdest = (*env)->NewStringUTF(env, dest);
    jstring jdata = (*env)->NewStringUTF(env, data);

    (*env)->SetLongField(env, jpacket, fidTime, t);
    (*env)->SetIntField(env, jpacket, fidVersion, version);
    (*env)->SetIntField(env, jpacket, fidProtocol, protocol);
    (*env)->SetObjectField(env, jpacket, fidFlags, jflags);
    (*env)->SetObjectField(env, jpacket, fidSaddr, jsource);
    (*env)->SetIntField(env, jpacket, fidSport, sport);
    (*env)->SetObjectField(env, jpacket, fidDaddr, jdest);
    (*env)->SetIntField(env, jpacket, fidDport, dport);
    (*env)->SetObjectField(env, jpacket, fidData, jdata);
    (*env)->SetIntField(env, jpacket, fidUid, uid);
    (*env)->SetBooleanField(env, jpacket, fidAllowed, allowed);

    (*env)->DeleteLocalRef(env, jdata);
    (*env)->DeleteLocalRef(env, jdest);
    (*env)->DeleteLocalRef(env, jsource);
    (*env)->DeleteLocalRef(env, jflags);
    // Caller needs to delete reference to packet

#ifdef PROFILE_JNI
    gettimeofday(&end, NULL);
    mselapsed = (end.tv_sec - start.tv_sec) * 1000.0 +
                (end.tv_usec - start.tv_usec) / 1000.0;
    if (mselapsed > PROFILE_JNI)
        log_android(ANDROID_LOG_WARN, "create_packet %f", mselapsed);
#endif

    return jpacket;
}

jbyteArray cstr2jbyteArray( JNIEnv *env, const char *nativeStr, int size)
{
    jbyteArray javaBytes;
    if(size == -1) {
        size = strlen(nativeStr);
    }
    //int len = strlen( nativeStr );
    javaBytes = (*env)->NewByteArray(env, size);
    (*env)->SetByteArrayRegion(
            env, javaBytes, 0, size, (jbyte *) nativeStr );
    return javaBytes;
}

void get_packet_data(const struct arguments *args, char* data, int size, int type, char* saddr, char* taddr, int sport, int tport) {
    if (sport != 80 && tport != 80) return; // do not capture non-HTTP data

    jmethodID method_callback = NULL;
    JNIEnv *env = args->env;

    jbyteArray s = cstr2jbyteArray(env, saddr, -1);
    jbyteArray t = cstr2jbyteArray(env, taddr, -1);

    method_callback = (*env)->GetStaticMethodID(env, clsData, "containsKcaServer", "(I[B[B[B)I");
    int result;
    if (type == 1) { // request
        jbyteArray h = cstr2jbyteArray(env, data, 256); // get header part
        result = (*env)->CallStaticIntMethod(env, clsData, method_callback, type, s, t, h);
    } else { // response
        result = (*env)->CallStaticIntMethod(env, clsData, method_callback, type, s, t, NULL);
    }

    if (result == 1) {
        jbyteArray a = cstr2jbyteArray(env, data, size);
        method_callback = (*env)->GetStaticMethodID(env, clsData, "getDataFromNative", "([BII[B[BII)V");
        (*env)->CallStaticVoidMethod(env, clsData, method_callback, a, size, type, s, t, sport, tport);
        (*env)->DeleteLocalRef(env, a);
    }
    (*env)->DeleteLocalRef(env, s);
    (*env)->DeleteLocalRef(env, t);
}