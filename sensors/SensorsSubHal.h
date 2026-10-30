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

#pragma once

#include <atomic>
#include <map>
#include <memory>
#include <string>
#include <thread>
#include <vector>

#include "Sensor.h"
#include "V2_1/SubHal.h"

namespace android {
namespace hardware {
namespace sensors {
namespace V2_1 {
namespace subhal {
namespace implementation {

using ::android::hardware::sensors::V1_0::OperationMode;
using ::android::hardware::sensors::V1_0::RateLevel;
using ::android::hardware::sensors::V1_0::Result;
using ::android::hardware::sensors::V1_0::SharedMemInfo;
using ::android::hardware::sensors::V2_1::Event;
using ::android::hardware::sensors::V2_1::implementation::IHalProxyCallback;
using ::android::hardware::sensors::V2_1::implementation::ISensorsSubHal;

class SensorsSubHal : public ISensorsSubHal, public ISensorsEventCallback {
  public:
    SensorsSubHal();
    ~SensorsSubHal() override;

    Return<void> getSensorsList_2_1(ISensors::getSensorsList_2_1_cb _hidl_cb) override;
    Return<Result> injectSensorData_2_1(const Event& event) override;
    Return<Result> initialize(const sp<IHalProxyCallback>& halProxyCallback) override;

    Return<Result> setOperationMode(OperationMode mode) override;
    OperationMode getOperationMode() const { return mCurrentOperationMode; }

    Return<Result> activate(int32_t sensorHandle, bool enabled) override;
    Return<Result> batch(int32_t sensorHandle, int64_t samplingPeriodNs,
                         int64_t maxReportLatencyNs) override;
    Return<Result> flush(int32_t sensorHandle) override;

    Return<void> registerDirectChannel(const SharedMemInfo& mem,
                                       ISensors::registerDirectChannel_cb _hidl_cb) override;
    Return<Result> unregisterDirectChannel(int32_t channelHandle) override;
    Return<void> configDirectReport(int32_t sensorHandle, int32_t channelHandle, RateLevel rate,
                                    ISensors::configDirectReport_cb _hidl_cb) override;

    Return<void> debug(const hidl_handle& fd, const hidl_vec<hidl_string>& args) override;

    const std::string getName() override { return "sensors.beryl"; }

    void postEvents(const std::vector<Event>& events, bool wakeup) override;

  protected:
    template <class SensorType>
    std::shared_ptr<SensorType> AddSensor() {
        std::shared_ptr<SensorType> sensor =
                std::make_shared<SensorType>(mNextHandle++, this);
        mSensors[sensor->getSensorInfo().sensorHandle] = sensor;
        return sensor;
    }

    std::map<int32_t, std::shared_ptr<Sensor>> mSensors;
    sp<IHalProxyCallback> mCallback;

  private:
    void touchMonitorThread();
    int findFtsTouchDevice();

    OperationMode mCurrentOperationMode = OperationMode::NORMAL;
    int32_t mNextHandle = 1;

    std::shared_ptr<UdfpsSensor> mUdfpsSensor;
    std::shared_ptr<SingleTapSensor> mSingleTapSensor;

    std::thread mTouchThread;
    std::atomic_bool mStopTouchThread{false};
};

}  // namespace implementation
}  // namespace subhal
}  // namespace V2_1
}  // namespace sensors
}  // namespace hardware
}  // namespace android

