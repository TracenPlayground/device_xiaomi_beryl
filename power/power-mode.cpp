/*
 * Copyright (C) 2021 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#include <aidl/android/hardware/power/BnPower.h>
#include <aidl/vendor/xiaomi/hw/touchfeature/ITouchFeature.h>
#include <android-base/file.h>
#include <android-base/logging.h>
#include <android/binder_manager.h>

#include <mutex>

namespace aidl::google::hardware::power::impl::pixel {

using ::aidl::android::hardware::power::Mode;
using ::aidl::vendor::xiaomi::hw::touchfeature::ITouchFeature;
using ::android::base::WriteStringToFile;

namespace {

constexpr const char* kTpGesturePath = "/proc/tp_gesture";
constexpr int32_t kTouchId = 0;
constexpr int32_t kTouchGameMode = 0;
constexpr int32_t kTouchActiveMode = 202;

std::mutex gServiceMutex;
std::shared_ptr<ITouchFeature> gTouchFeatureService;

std::shared_ptr<ITouchFeature> getTouchFeatureService() {
    std::lock_guard<std::mutex> lock(gServiceMutex);
    if (gTouchFeatureService != nullptr) {
        if (AIBinder_isAlive(gTouchFeatureService->asBinder().get())) {
            return gTouchFeatureService;
        }
        gTouchFeatureService = nullptr;
    }

    ndk::SpAIBinder binder(AServiceManager_checkService(
        "vendor.xiaomi.hw.touchfeature.ITouchFeature/default"));
    if (!binder.get()) {
        LOG(ERROR) << "Failed to get touchfeature service";
        return nullptr;
    }

    gTouchFeatureService = ITouchFeature::fromBinder(binder);
    if (gTouchFeatureService == nullptr) {
        LOG(ERROR) << "Failed to convert touchfeature binder to interface";
    }

    return gTouchFeatureService;
}

void invalidateTouchFeatureService() {
    std::lock_guard<std::mutex> lock(gServiceMutex);
    gTouchFeatureService = nullptr;
}

}  // anonymous namespace

bool isDeviceSpecificModeSupported(Mode type, bool* _aidl_return) {
    switch (type) {
        case Mode::DOUBLE_TAP_TO_WAKE:
        case Mode::GAME:
            *_aidl_return = true;
            return true;
        default:
            return false;
    }
}

bool setDeviceSpecificMode(Mode type, bool enabled) {
    switch (type) {
        case Mode::DOUBLE_TAP_TO_WAKE: {
            if (!WriteStringToFile(enabled ? "1" : "0", kTpGesturePath, true)) {
                LOG(ERROR) << "Failed to write " << (enabled ? "1" : "0")
                           << " to " << kTpGesturePath;
                return false;
            }

            LOG(INFO) << "Double Tap to Wake " << (enabled ? "enabled" : "disabled");
            return true;
        }
        case Mode::GAME: {
            auto touchfeature = getTouchFeatureService();
            if (touchfeature == nullptr) {
                return false;
            }

            const auto gameStatus = touchfeature->setTouchMode(
                kTouchId, kTouchGameMode, enabled ? 1 : 0);
            if (!gameStatus.isOk()) {
                LOG(ERROR) << "setTouchMode failed for GAME: "
                           << gameStatus.getDescription();
                invalidateTouchFeatureService();
                return false;
            }

            const auto activeStatus = touchfeature->setTouchMode(
                kTouchId, kTouchActiveMode, enabled ? 1 : 0);
            if (!activeStatus.isOk()) {
                LOG(ERROR) << "setTouchMode failed for ACTIVE: "
                           << activeStatus.getDescription();
                invalidateTouchFeatureService();
                return false;
            }

            return true;
        }
        default:
            return false;
    }
}

}  // namespace aidl::google::hardware::power::impl::pixel
