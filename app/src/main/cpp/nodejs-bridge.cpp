#include <jni.h>
#include <string>
#include <cstdlib>
#include <cstring>
#include "node.h"

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_whabotpro_engine_NodeJSEngine_startNodeWithArguments(
        JNIEnv *env,
        jobject /* this */,
        jobjectArray arguments) {

    jsize argument_count = env->GetArrayLength(arguments);

    int c_arguments_size = 0;
    for (int i = 0; i < argument_count; i++) {
        jstring arg = (jstring) env->GetObjectArrayElement(arguments, i);
        const char *str = env->GetStringUTFChars(arg, 0);
        c_arguments_size += strlen(str);
        c_arguments_size++;
        env->ReleaseStringUTFChars(arg, str);
    }

    char* args_buffer = (char*) calloc(c_arguments_size, sizeof(char));
    char* argv[argument_count];
    char* current_args_position = args_buffer;

    for (int i = 0; i < argument_count; i++) {
        jstring arg = (jstring) env->GetObjectArrayElement(arguments, i);
        const char *str = env->GetStringUTFChars(arg, 0);

        strncpy(current_args_position, str, strlen(str));
        argv[i] = current_args_position;
        current_args_position += strlen(current_args_position) + 1;

        env->ReleaseStringUTFChars(arg, str);
    }

    return jint(node::Start(argument_count, argv));
}
