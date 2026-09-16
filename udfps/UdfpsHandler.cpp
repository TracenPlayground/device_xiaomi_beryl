/*
 * Copyright (C) 2022 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#define LOG_TAG "UdfpsHandler.beryl"

#include <aidl/android/hardware/biometrics/fingerprint/BnFingerprint.h>
#include <android-base/logging.h>
#include <android-base/unique_fd.h>

#include <poll.h>
#include <sys/ioctl.h>
#include <fstream>
#include <mutex>
#include <thread>

#include "UdfpsHandler.h"
#include "mi_disp.h"

#define CMD_DATA_BUF_SIZE 256

#define COMMON_DATA_CMD 0
#define SELECT_TOUCH_ID 3
#define SET_CUR_VALUE 0

#define Touch_Fod_Enable 10
#define THP_FOD_DOWNUP_CTL 1001

#define COMMAND_NIT 10
#define PARAM_NIT_FOD 1
#define PARAM_NIT_NONE 0

#define COMMAND_FOD_PRESS_STATUS 1
#define COMMAND_FOD_PRESS_X 2
#define COMMAND_FOD_PRESS_Y 3
#define PARAM_FOD_PRESSED 1
#define PARAM_FOD_RELEASED 0

#define FOD_STATUS_PATH "/sys/class/touch/touch_dev/fod_press_status"
#define FOD_STATUS_OFF 0
#define FOD_STATUS_ON 1

#define TOUCH_DEV_PATH "/dev/xiaomi-touch"
#define TOUCH_MAGIC 0x54

#define DISP_FEATURE_PATH "/dev/mi_display/disp_feature"

#define FINGERPRINT_ACQUIRED_VENDOR 7

typedef struct {
    int8_t touch_id;
    uint8_t cmd;
    uint16_t mode;
    uint16_t data_len;
    int32_t data_buf[CMD_DATA_BUF_SIZE];
} touch_base;

#define TOUCH_IOC_SELECT_TOUCH_ID _IOW(TOUCH_MAGIC, SELECT_TOUCH_ID, int)
#define TOUCH_IOC_COMMON_DATA _IOW(TOUCH_MAGIC, COMMON_DATA_CMD, touch_base)

using ::aidl::android::hardware::biometrics::fingerprint::AcquiredInfo;

namespace {

template <typename T>
static void set(const std::string& path, const T& value) {
    std::ofstream file(path);
    file << value;
}

static bool readBool(int fd) {
    char c;
    int rc;

    rc = lseek(fd, 0, SEEK_SET);
    if (rc) {
        LOG(ERROR) << "failed to seek fd, err: " << rc;
        return false;
    }

    rc = read(fd, &c, sizeof(char));
    if (rc != 1) {
        LOG(ERROR) << "failed to read bool from fd, err: " << rc;
        return false;
    }

    return c != '0';
}

static disp_event_resp* parseDispEvent(int fd) {
    static char event_data[1024] = {0};
    ssize_t size = read(fd, event_data, sizeof(event_data));

    if (size < 0) {
        LOG(ERROR) << "read fod event failed";
        return nullptr;
    }
    if (size < sizeof(struct disp_event)) {
        LOG(ERROR) << "Invalid event size " << size << ", expect at least "
                   << sizeof(struct disp_event);
        return nullptr;
    }

    return (struct disp_event_resp*)&event_data[0];
}

touch_base touchDataPrimary = {
        .touch_id = MI_DISP_PRIMARY,
        .cmd = SET_CUR_VALUE,
        .mode = 0,
        .data_len = 1,
        .data_buf = {},
};

}  // anonymous namespace

class XiaomiBerylUdfpsHandler : public UdfpsHandler {
  public:
    void init(fingerprint_device_t* device) override {
        mDevice = device;
        disp_fd_ = android::base::unique_fd(open(DISP_FEATURE_PATH, O_RDWR));
        touch_fd_ = android::base::unique_fd(open(TOUCH_DEV_PATH, O_RDWR));

        // Thread to listen for fod ui changes
        std::thread([this]() {
            int fd = open(DISP_FEATURE_PATH, O_RDWR);
            if (fd < 0) {
                LOG(ERROR) << "failed to open " << DISP_FEATURE_PATH << " , err: " << fd;
                return;
            }

            // Register for FOD events
            disp_event_req req;
            req.base.flag = 0;
            req.base.disp_id = MI_DISP_PRIMARY;
            req.type = MI_DISP_EVENT_FOD;
            ioctl(fd, MI_DISP_IOCTL_REGISTER_EVENT, &req);

            struct pollfd dispEventPoll = {
                    .fd = fd,
                    .events = POLLIN,
                    .revents = 0,
            };

            while (true) {
                int rc = poll(&dispEventPoll, 1, -1);
                if (rc < 0) {
                    LOG(ERROR) << "failed to poll " << DISP_FEATURE_PATH << ", err: " << rc;
                    continue;
                }

                struct disp_event_resp* response = parseDispEvent(fd);
                if (response == nullptr) {
                    continue;
                }

                if (response->base.type != MI_DISP_EVENT_FOD) {
                    LOG(ERROR) << "unexpected display event: " << response->base.type;
                    continue;
                }

                int value = response->data[0];
                LOG(DEBUG) << "received data: " << std::bitset<8>(value);

                bool localHbmUiReady = value & LOCAL_HBM_UI_READY;

                std::lock_guard<std::mutex> lock(mLock);
                if (mDevice) {
                    mDevice->extCmd(mDevice, COMMAND_NIT,
                                    localHbmUiReady ? PARAM_NIT_FOD : PARAM_NIT_NONE);
                }
            }
        }).detach();
    }

    void onFingerDown(uint32_t /*x*/, uint32_t /*y*/, float /*minor*/, float /*major*/) override {
        std::lock_guard<std::mutex> lock(mLock);
        if (mAuthSuccess) return;
        setFingerDownLocked(true);
    }

    void onFingerUp() override {
        std::lock_guard<std::mutex> lock(mLock);
        setFingerDownLocked(false);
    }

    void onAcquired(int32_t result, int32_t vendorCode) override {
        LOG(DEBUG) << __func__ << " result: " << result << " vendorCode: " << vendorCode;
        switch (static_cast<AcquiredInfo>(result)) {
            case AcquiredInfo::GOOD:
            case AcquiredInfo::PARTIAL:
            case AcquiredInfo::INSUFFICIENT:
            case AcquiredInfo::SENSOR_DIRTY:
            case AcquiredInfo::TOO_SLOW:
            case AcquiredInfo::TOO_FAST:
            case AcquiredInfo::TOO_DARK:
            case AcquiredInfo::TOO_BRIGHT:
            case AcquiredInfo::IMMOBILE:
            case AcquiredInfo::LIFT_TOO_SOON:
                onFingerUp();
                break;
            default:
                break;
        }
        if (vendorCode == 21) {
            std::lock_guard<std::mutex> lock(mLock);
            setFodStatusLocked(FOD_STATUS_ON);
        }
    }

    void onAuthenticationFailed() override {
        std::lock_guard<std::mutex> lock(mLock);
        setFingerDownLocked(false);
        setFodStatusLocked(FOD_STATUS_OFF);
    }

    void onAuthenticationSucceeded() override {
        {
            std::lock_guard<std::mutex> lock(mLock);
            mAuthSuccess = true;
            setFingerDownLocked(false);
            setFodStatusLocked(FOD_STATUS_OFF);
        }
        std::thread([this]() {
            std::this_thread::sleep_for(std::chrono::milliseconds(500));
            std::lock_guard<std::mutex> lock(mLock);
            mAuthSuccess = false;
        }).detach();
    }

    void cancel() override {
        std::lock_guard<std::mutex> lock(mLock);
        setFingerDownLocked(false);
        setFodStatusLocked(FOD_STATUS_OFF);
    }

  private:
    std::mutex mLock;
    fingerprint_device_t* mDevice = nullptr;
    android::base::unique_fd disp_fd_;
    android::base::unique_fd touch_fd_;
    bool mAuthSuccess = false;

    void setFodStatusLocked(int value) {
        if (touch_fd_ < 0) return;
        ioctl(touch_fd_.get(), TOUCH_IOC_SELECT_TOUCH_ID, MI_DISP_PRIMARY);
        touch_base data = {
            .mode = Touch_Fod_Enable,
            .data_buf = {value},
        };
        ioctl(touch_fd_.get(), TOUCH_IOC_COMMON_DATA, &data);
        set(FOD_STATUS_PATH, value);
    }

    void setFingerDownLocked(bool pressed) {
        if (mDevice) {
            mDevice->extCmd(mDevice, COMMAND_FOD_PRESS_STATUS, pressed ? PARAM_FOD_PRESSED : PARAM_FOD_RELEASED);
        }
        if (disp_fd_ < 0) return;
        disp_local_hbm_req req;
        req.base.flag = 0;
        req.base.disp_id = MI_DISP_PRIMARY;
        req.local_hbm_value = pressed ? LHBM_TARGET_BRIGHTNESS_WHITE_1000NIT : LHBM_TARGET_BRIGHTNESS_OFF_FINGER_UP;
        ioctl(disp_fd_.get(), MI_DISP_IOCTL_SET_LOCAL_HBM, &req);
    }
};

static UdfpsHandler* create() {
    return new XiaomiBerylUdfpsHandler();
}

static void destroy(UdfpsHandler* handler) {
    delete handler;
}

extern "C" UdfpsHandlerFactory UDFPS_HANDLER_FACTORY = {
        .create = create,
        .destroy = destroy,
};
