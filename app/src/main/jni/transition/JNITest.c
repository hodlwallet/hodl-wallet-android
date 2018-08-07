//
// Created by Igor Guerrero on 8/7/18.
//

#include <jni.h>
#include <BRTest.h>

#include "JNITest.h"

JNIEXPORT jboolean JNICALL Java_com_jniwrappers_BRTest_BRCoreTests(
    JNIEnv *env,
    jobject thiz) {
    jboolean r = (jboolean) BRCoreTests();

    return r;
}
