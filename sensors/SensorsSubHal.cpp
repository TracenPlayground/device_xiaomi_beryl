/*
 * Copyright (C) 2019 The Android Open Source Project
 * Copyright (C) 2026 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "SensorsSubHal.h"

#include <android/hardware/sensors/2.1/types.h>
#include <cutils/properties.h>
#include <linux/input.h>
#include <log/log.h>
#include <poll.h>

#include <chrono>
#include <cstring>
#include <sstream>
#include <thread>

using ::android::hardware::sensors::V2_1::implementation::ISensorsSubHal;
using ::android::hardware::sensors::V2_1::subhal::implementation::SensorsSubHal;

namespace android {
namespace hardware {
namespace sensors {
namespace V2_1 {
namespace subhal {
namespace implementation {

using ::android::hardware::Void;
using ::android::hardware::sensors::V2_0::implementation::ScopedWakelock;

SensorsSubHal::SensorsSubHal() : mCallback(nullptr), mNextHandle(1) {
    if (property_get_bool("ro.vendor.sensors.xiaomi.udfps", true)) {
        mUdfpsSensor = AddSensor<UdfpsSensor>();
    }
    mSingleTapSensor = AddSensor<SingleTapSensor>();

    mTouchThread = std::thread(&SensorsSubHal::touchMonitorThread, this);
}

SensorsSubHal::~SensorsSubHal() {
    mStopTouchThread = true;
    if (mTouchThread.joinable()) {
        mTouchThread.join();
    }
}

int SensorsSubHal::findFtsTouchDevice() {
    char name[128] = {0};
    char path[64];

    for (int i = 0; i < 32; i++) {
        snprintf(path, sizeof(path), "/dev/input/event%d", i);
        int fd = open(path, O_RDONLY | O_CLOEXEC);
        if (fd < 0) continue;

        if (ioctl(fd, EVIOCGNAME(sizeof(name) - 1), name) >= 0) {
            if (strcmp(name, "fts_ts") == 0) {
                ALOGI("Found fts_ts input device at %s", path);
                return fd;
            }
        }
        close(fd);
    }
    return -1;
}

void SensorsSubHal::touchMonitorThread() {
    int touchFd = -1;
    while (!mStopTouchThread && touchFd < 0) {
        touchFd = findFtsTouchDevice();
        if (touchFd < 0) {
            std::this_thread::sleep_for(std::chrono::milliseconds(500));
        }
    }

    if (touchFd < 0) {
        ALOGE("Could not open fts_ts device, exiting monitor thread");
        return;
    }

    struct pollfd pfd = {
            .fd = touchFd,
            .events = POLLIN,
    };

    struct input_event ev;
    while (!mStopTouchThread) {
        int rc = poll(&pfd, 1, 500);
        if (rc <= 0) continue;

        if (pfd.revents & POLLIN) {
            ssize_t bytes = read(touchFd, &ev, sizeof(ev));
            if (bytes < static_cast<ssize_t>(sizeof(ev))) continue;

            if (ev.type == EV_KEY && ev.value == 1 && ev.code == 354) {
                if (mSingleTapSensor && mSingleTapSensor->isEnabled()) {
                    ALOGI("Single tap received (EV_KEY 354): pulsing Ambient Display");
                    mSingleTapSensor->fireEvent();
                }
            }
        }
    }

    close(touchFd);
}

Return<void> SensorsSubHal::getSensorsList_2_1(ISensors::getSensorsList_2_1_cb _hidl_cb) {
    std::vector<SensorInfo> sensors;
    for (const auto& sensor : mSensors) {
        sensors.push_back(sensor.second->getSensorInfo());
    }
    _hidl_cb(sensors);
    return Void();
}

Return<Result> SensorsSubHal::setOperationMode(OperationMode mode) {
    for (auto sensor : mSensors) {
        sensor.second->setOperationMode(mode);
    }
    mCurrentOperationMode = mode;
    return Result::OK;
}

Return<Result> SensorsSubHal::activate(int32_t sensorHandle, bool enabled) {
    auto sensor = mSensors.find(sensorHandle);
    if (sensor != mSensors.end()) {
        sensor->second->activate(enabled);
        return Result::OK;
    }
    return Result::BAD_VALUE;
}

Return<Result> SensorsSubHal::batch(int32_t sensorHandle, int64_t samplingPeriodNs,
                                    int64_t /* maxReportLatencyNs */) {
    auto sensor = mSensors.find(sensorHandle);
    if (sensor != mSensors.end()) {
        sensor->second->batch(samplingPeriodNs);
        return Result::OK;
    }
    return Result::BAD_VALUE;
}

Return<Result> SensorsSubHal::flush(int32_t sensorHandle) {
    auto sensor = mSensors.find(sensorHandle);
    if (sensor != mSensors.end()) {
        return sensor->second->flush();
    }
    return Result::BAD_VALUE;
}

Return<Result> SensorsSubHal::injectSensorData_2_1(const Event& event) {
    auto sensor = mSensors.find(event.sensorHandle);
    if (sensor != mSensors.end()) {
        return sensor->second->injectEvent(event);
    }
    return Result::BAD_VALUE;
}

Return<void> SensorsSubHal::registerDirectChannel(const SharedMemInfo& /* mem */,
                                                  ISensors::registerDirectChannel_cb _hidl_cb) {
    _hidl_cb(Result::INVALID_OPERATION, -1);
    return Return<void>();
}

Return<Result> SensorsSubHal::unregisterDirectChannel(int32_t /* channelHandle */) {
    return Result::INVALID_OPERATION;
}

Return<void> SensorsSubHal::configDirectReport(int32_t /* sensorHandle */,
                                               int32_t /* channelHandle */, RateLevel /* rate */,
                                               ISensors::configDirectReport_cb _hidl_cb) {
    _hidl_cb(Result::INVALID_OPERATION, 0);
    return Return<void>();
}

Return<void> SensorsSubHal::debug(const hidl_handle& fd, const hidl_vec<hidl_string>& args) {
    if (fd.getNativeHandle() == nullptr || fd->numFds < 1) {
        ALOGE("%s: missing fd for writing", __FUNCTION__);
        return Void();
    }

    FILE* out = fdopen(dup(fd->data[0]), "w");
    if (args.size() != 0) {
        fprintf(out, "Note: sub-HAL %s currently does not support args.\n", getName().c_str());
    }

    std::ostringstream stream;
    stream << "Available sensors:" << std::endl;
    for (auto sensor : mSensors) {
        SensorInfo info = sensor.second->getSensorInfo();
        stream << "Name: " << info.name << std::endl;
        stream << "Type: " << info.typeAsString << std::endl;
        stream << "Flags: " << info.flags << std::endl;
    }
    stream << std::endl;

    fprintf(out, "%s", stream.str().c_str());
    fclose(out);
    return Return<void>();
}

Return<Result> SensorsSubHal::initialize(const sp<IHalProxyCallback>& halProxyCallback) {
    mCallback = halProxyCallback;
    setOperationMode(OperationMode::NORMAL);
    return Result::OK;
}

void SensorsSubHal::postEvents(const std::vector<Event>& events, bool wakeup) {
    ScopedWakelock wakelock = mCallback->createScopedWakelock(wakeup);
    mCallback->postEvents(events, std::move(wakelock));
}

}  // namespace implementation
}  // namespace subhal
}  // namespace V2_1
}  // namespace sensors
}  // namespace hardware
}  // namespace android

extern "C" ISensorsSubHal* sensorsHalGetSubHal_2_1(uint32_t* version) {
    static SensorsSubHal subHal;
    *version = SUB_HAL_2_1_VERSION;
    return &subHal;
}

