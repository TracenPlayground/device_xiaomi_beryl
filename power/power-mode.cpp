/*
 * Copyright (C) 2021 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#include <aidl/android/hardware/power/BnPower.h>
#include <android-base/file.h>
#include <android-base/logging.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <unistd.h>

#include <atomic>
#include <thread>
#include <chrono>

namespace aidl::google::hardware::power::impl::pixel {

using ::aidl::android::hardware::power::Mode;
using ::android::base::WriteStringToFile;

namespace {

constexpr const char* kTpGesturePath = "/proc/tp_gesture";

#define CMD_DATA_BUF_SIZE 256
#define TOUCH_MAGIC 0x54
#define COMMON_DATA_CMD 0
#define SELECT_TOUCH_ID 3

typedef struct {
    int8_t touch_id;
    uint8_t cmd;
    uint16_t mode;
    uint16_t data_len;
    uint16_t reserved;
    int32_t data_buf[CMD_DATA_BUF_SIZE];
} touch_base;

#define TOUCH_IOC_SELECT_TOUCH_ID _IOW(TOUCH_MAGIC, SELECT_TOUCH_ID, int)
#define TOUCH_IOC_COMMON_DATA _IOW(TOUCH_MAGIC, COMMON_DATA_CMD, touch_base)

constexpr int32_t kTouchId = 0;
constexpr int32_t kTouchGameMode = 0;
constexpr int32_t kTouchActiveMode = 1;

std::atomic<bool> gGameModeEnabled{false};

bool setTouchMode(int mode, int val) {
    int fd = open("/dev/xiaomi-touch", O_RDWR);
    if (fd < 0) {
        LOG(ERROR) << "Failed to open /dev/xiaomi-touch";
        return false;
    }

    int sel_ret = ioctl(fd, TOUCH_IOC_SELECT_TOUCH_ID, 0);
    if (sel_ret < 0) {
        LOG(ERROR) << "TOUCH_IOC_SELECT_TOUCH_ID failed";
        close(fd);
        return false;
    }

    touch_base tb;
    memset(&tb, 0, sizeof(tb));
    tb.touch_id = kTouchId;
    tb.cmd = 0; // SET_CUR_VALUE
    tb.mode = (uint16_t)mode;
    tb.data_len = 1;
    tb.data_buf[0] = val;

    int ret = ioctl(fd, TOUCH_IOC_COMMON_DATA, &tb);
    if (ret < 0) {
        LOG(ERROR) << "TOUCH_IOC_COMMON_DATA failed for mode " << mode << " val " << val;
        close(fd);
        return false;
    }

    close(fd);
    return true;
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
            gGameModeEnabled = enabled;
            bool gameStatus = setTouchMode(kTouchGameMode, enabled ? 1 : 0);
            if (!gameStatus) {
                LOG(ERROR) << "setTouchMode failed for GAME";
                return false;
            }

            bool activeStatus = setTouchMode(kTouchActiveMode, enabled ? 1 : 0);
            if (!activeStatus) {
                LOG(ERROR) << "setTouchMode failed for ACTIVE";
                return false;
            }

            return true;
        }
        case Mode::INTERACTIVE: {
            if (enabled && gGameModeEnabled) {
                std::thread([]() {
                    std::this_thread::sleep_for(std::chrono::milliseconds(200));
                    setTouchMode(kTouchGameMode, 1);
                    setTouchMode(kTouchActiveMode, 1);
                }).detach();
            }
            // Return false so Power.cpp continues handling INTERACTIVE
            return false;
        }
        default:
            return false;
    }
}

}  // namespace aidl::google::hardware::power::impl::pixel
