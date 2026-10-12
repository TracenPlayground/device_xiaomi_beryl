/*
 * Copyright (C) 2026 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#define LOG_TAG "OpenCL-pixel"

#include <dlfcn.h>
#include <log/log.h>
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>

static void* g_opencl_handle = NULL;
static pthread_mutex_t g_lock = PTHREAD_MUTEX_INITIALIZER;

typedef void* (*pfn_clGetExtensionFunctionAddress)(const char*);
static pfn_clGetExtensionFunctionAddress g_clGetExtensionFunctionAddress = NULL;

static void* get_opencl_handle(void) {
    pthread_mutex_lock(&g_lock);
    if (!g_opencl_handle) {
        g_opencl_handle = dlopen("libOpenCL.so", RTLD_NOW | RTLD_GLOBAL);
        if (!g_opencl_handle) {
            g_opencl_handle = dlopen("/vendor/lib64/libOpenCL.so", RTLD_NOW | RTLD_GLOBAL);
        }
        if (g_opencl_handle) {
            ALOGI("Successfully loaded backend OpenCL driver (libOpenCL.so)");
            g_clGetExtensionFunctionAddress =
                (pfn_clGetExtensionFunctionAddress)dlsym(g_opencl_handle, "clGetExtensionFunctionAddress");
        } else {
            ALOGE("Failed to open libOpenCL.so: %s", dlerror());
        }
    }
    pthread_mutex_unlock(&g_lock);
    return g_opencl_handle;
}

__attribute__((visibility("default")))
void enableOpenCL(void) {
    ALOGI("enableOpenCL() invoked");
    get_opencl_handle();
}

__attribute__((visibility("default")))
void* loadOpenCLPointer(const char* name) {
    if (!name) {
        return NULL;
    }

    void* handle = get_opencl_handle();
    if (!handle) {
        ALOGE("loadOpenCLPointer(%s): OpenCL handle is NULL", name);
        return NULL;
    }

    void* symbol = dlsym(handle, name);
    if (symbol) {
        ALOGV("loadOpenCLPointer(%s) -> %p", name, symbol);
        return symbol;
    }

    if (g_clGetExtensionFunctionAddress) {
        symbol = g_clGetExtensionFunctionAddress(name);
        if (symbol) {
            ALOGV("loadOpenCLPointer(%s) via extension -> %p", name, symbol);
            return symbol;
        }
    }

    ALOGW("loadOpenCLPointer(%s) symbol not found in backend OpenCL", name);
    return NULL;
}
