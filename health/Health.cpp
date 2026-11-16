/*
 * Copyright (C) 2021 The Android Open Source Project
 * Copyright (C) 2022 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#include <android-base/file.h>
#include <android-base/logging.h>
#include <android-base/parseint.h>
#include <android-base/strings.h>
#include <android/binder_interface_utils.h>
#include <health-impl/Health.h>
#include <health/utils.h>
#include <unistd.h>

#include <algorithm>
#include <string>

#ifndef CHARGER_FORCE_NO_UI
#define CHARGER_FORCE_NO_UI 0
#endif

#if !CHARGER_FORCE_NO_UI
#include <health-impl/ChargerUtils.h>
#endif

using aidl::android::hardware::health::HalHealthLoop;
using aidl::android::hardware::health::Health;

#if !CHARGER_FORCE_NO_UI
using aidl::android::hardware::health::charger::ChargerCallback;
using aidl::android::hardware::health::charger::ChargerModeMain;
#endif

static constexpr const char* gInstanceName = "default";
static constexpr std::string_view gChargerArg{"--charger"};

#if !CHARGER_FORCE_NO_UI
namespace {
void SetBacklightForBlank(bool blank) {
    struct BacklightNodes {
        const char* brightness;
        const char* max_brightness;
    };
    static constexpr BacklightNodes kNodes[] = {
            {"/sys/class/leds/lcd-backlight/brightness",
             "/sys/class/leds/lcd-backlight/max_brightness"},
            {"/sys/class/backlight/panel0-backlight/brightness",
             "/sys/class/backlight/panel0-backlight/max_brightness"},
    };

    for (const auto& node : kNodes) {
        if (access(node.brightness, W_OK) != 0) continue;

        if (blank) {
            android::base::WriteStringToFile("0", node.brightness);
            return;
        }

        std::string cur_str;
        int cur_val = 0;
        if (android::base::ReadFileToString(node.brightness, &cur_str) &&
            android::base::ParseInt(android::base::Trim(cur_str), &cur_val) && cur_val > 0) {
            return;
        }

        int max_val = 2047;
        std::string max_str;
        if (android::base::ReadFileToString(node.max_brightness, &max_str)) {
            int parsed = 0;
            if (android::base::ParseInt(android::base::Trim(max_str), &parsed) && parsed > 0) {
                max_val = parsed;
            }
        }
        int target = std::max(1, max_val / 2);
        android::base::WriteStringToFile(std::to_string(target), node.brightness);
        return;
    }
}
}  // namespace

extern "C" void __real__Z11gr_fb_blankbi(bool blank, int index);
extern "C" void __wrap__Z11gr_fb_blankbi(bool blank, int index) {
    if (blank) {
        SetBacklightForBlank(true);
        __real__Z11gr_fb_blankbi(true, index);
    } else {
        __real__Z11gr_fb_blankbi(false, index);
        SetBacklightForBlank(false);
    }
}

namespace aidl::android::hardware::health {
class ChargerCallbackImpl : public ChargerCallback {
  public:
    using ChargerCallback::ChargerCallback;
    bool ChargerEnableSuspend() override { return true; }
};
}  // namespace aidl::android::hardware::health
#endif

int main(int argc, char** argv) {
#ifdef __ANDROID_RECOVERY__
    android::base::InitLogging(argv, android::base::KernelLogger);
#endif

    // make a default health service
    auto config = std::make_unique<healthd_config>();
    ::android::hardware::health::InitHealthdConfig(config.get());
    auto binder = ndk::SharedRefBase::make<Health>(gInstanceName, std::move(config));

    if (argc >= 2 && argv[1] == gChargerArg) {
#if !CHARGER_FORCE_NO_UI
        // If charger shouldn't have UI for your device, simply drop the line below
        // for your service implementation. This corresponds to
        // ro.charger.no_ui=true
        return ChargerModeMain(
                binder,
                std::make_shared<aidl::android::hardware::health::ChargerCallbackImpl>(binder));
#endif

        LOG(INFO) << "Starting charger mode without UI.";
    } else {
        LOG(INFO) << "Starting health HAL.";
    }

    auto hal_health_loop = std::make_shared<HalHealthLoop>(binder, binder);
    return hal_health_loop->StartLoop();
}
