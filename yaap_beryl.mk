#
# Copyright (C) 2025 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit_only.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base_telephony.mk)

# Inherit from beryl device
$(call inherit-product, device/xiaomi/beryl/device.mk)

# Inherit some common Lineage stuff.
$(call inherit-product, vendor/yaap/config/common_full_phone.mk)


TARGET_BOOT_ANIMATION_RES := 1080
scr_resolution := 1080
TARGET_USES_AOSP_RECOVERY := true


# Device config
TARGET_HAS_UDFPS := true
TARGET_ENABLE_BLUR := true
TARGET_EXCLUDES_AUDIOFX := true
TARGET_FACE_UNLOCK_SUPPORTED := true

# Build config

PRODUCT_DEVICE := beryl
PRODUCT_NAME := yaap_beryl
PRODUCT_BRAND := Redmi
PRODUCT_MODEL := Redmi Note 14 5G
PRODUCT_MANUFACTURER := Xiaomi

PRODUCT_GMS_CLIENTID_BASE := android-xiaomi

PRODUCT_BUILD_PROP_OVERRIDES += \
    BuildDesc="missi-user 15 AP3A.240905.015.A2 OS2.0.3.0.VOQTWXM release-keys" \
    BuildFingerprint=Redmi/citrine_global/citrine:14/SP1A.210812.016/OS2.0.3.0.VOQTWXM:user/release-keys

