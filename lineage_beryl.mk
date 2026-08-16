#
# Copyright (C) 2025 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit_only.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base_telephony.mk)

# Inherit some common Lineage stuff.
TARGET_DISABLE_EPPE := true
$(call inherit-product, vendor/lineage/config/common_full_phone.mk)

# Inherit from beryl device
$(call inherit-product, device/xiaomi/beryl/device.mk)

PRODUCT_DEVICE := beryl
PRODUCT_NAME := lineage_beryl
PRODUCT_BRAND := Redmi
PRODUCT_MODEL := Redmi Note 14 5G
PRODUCT_MANUFACTURER := Xiaomi

PRODUCT_GMS_CLIENTID_BASE := android-xiaomi

PRODUCT_BUILD_PROP_OVERRIDES += \
    BuildDesc="missi-user 16 BP2A.250605.031.A3 OS3.0.1.0.WOQMIXM release-keys" \
    BuildFingerprint=Redmi/beryl/beryl:16/BP2A.250605.031.A3/OS3.0.1.0.WOQMIXM:user/release-keys

# Axion Device Configuration
TARGET_INCLUDE_AXFX := false
AXION_MAINTAINER := Naoko_Shoto
AXION_PROCESSOR := Dimensity_7025_Ultra

# Camera Info
AXION_CAMERA_REAR_INFO := 108,8,2
AXION_CAMERA_FRONT_INFO := 20

# Graphics & Display
TARGET_ENABLE_BLUR := true
TARGET_SUPPORTED_REFRESH_RATES := 60,90,120

# Features & Performance
TARGET_DISABLES_LIBPERF ?= false
BYPASS_CHARGE_SUPPORTED := true
BYPASS_CHARGE_TOGGLE_PATH := /sys/class/power_supply/battery/input_suspend