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

// Double-tap-to-wake gesture node
#define TP_GESTURE_PATH "/proc/tp_gesture"

#define TOUCH_ACTIVE_MODE 202
#define TOUCH_GAME_MODE 0
#define TOUCH_ID 0

namespace aidl {
namespace google {
namespace hardware {
namespace power {
namespace impl {
namespace pixel {

using ::aidl::android::hardware::power::Mode;
using ::aidl::vendor::xiaomi::hw::touchfeature::ITouchFeature;
using ::android::base::WriteStringToFile;

static std::shared_ptr<ITouchFeature> getTouchFeatureService() {
    static std::mutex service_mutex;
    static std::shared_ptr<ITouchFeature> service;

    std::lock_guard<std::mutex> lock(service_mutex);
    if (service != nullptr) {
        return service;
    }

    ndk::SpAIBinder binder(AServiceManager_checkService(
        "vendor.xiaomi.hw.touchfeature.ITouchFeature/default"));
    if (!binder.get()) {
        LOG(ERROR) << "Failed to get touchfeature service";
        return nullptr;
    }

    service = ITouchFeature::fromBinder(binder);
    if (service == nullptr) {
        LOG(ERROR) << "Failed to convert touchfeature binder to interface";
    }

    return service;
}

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
            if (!WriteStringToFile(enabled ? "1" : "0",
                                   TP_GESTURE_PATH,
                                   true)) {
                LOG(ERROR) << "Failed to write "
                           << (enabled ? "1" : "0")
                           << " to " << TP_GESTURE_PATH;
                return false;
            }

            LOG(INFO) << "Double Tap to Wake "
                      << (enabled ? "enabled" : "disabled");

            return true;
        }
        case Mode::GAME: {
            auto touchfeature = getTouchFeatureService();
            if (touchfeature == nullptr) {
                return false;
            }

            const auto gameStatus = touchfeature->setTouchMode(
                TOUCH_ID, TOUCH_GAME_MODE, enabled ? 1 : 0);
            if (!gameStatus.isOk()) {
                LOG(ERROR) << "setTouchMode failed for GAME: "
                           << gameStatus.getDescription();
                return false;
            }

            const auto activeStatus = touchfeature->setTouchMode(
                TOUCH_ID, TOUCH_ACTIVE_MODE, enabled ? 1 : 0);
            if (!activeStatus.isOk()) {
                LOG(ERROR) << "setTouchMode failed for ACTIVE: "
                           << activeStatus.getDescription();
                return false;
            }

            return true;
        }
        default:
            return false;
    }
}

}  // namespace pixel
}  // namespace impl
}  // namespace power
}  // namespace hardware
}  // namespace google
}  // namespace aidl
