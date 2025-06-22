#!/usr/bin/env -S PYTHONPATH=../../../tools/extract-utils python3
#
# SPDX-FileCopyrightText: 2025 The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

from extract_utils.fixups_blob import blob_fixups_user_type, blob_fixup
from extract_utils.fixups_lib import (
    lib_fixup_remove_arch_suffix,
    lib_fixup_remove_proto_version_suffix,
    lib_fixup_vendorcompat,
    lib_fixups_user_type,
    libs_clang_rt_ubsan,
    libs_proto_3_9_1,
    libs_proto_21_12,
)
from extract_utils.fixups_lib import (
    lib_fixups,
    lib_fixups_user_type,
)
from extract_utils.main import (
    ExtractUtils,
    ExtractUtilsModule,
)

namespace_imports = [
    'device/xiaomi/beryl',
    "hardware/mediatek",
    "hardware/mediatek/libmtkperf_client",
    "hardware/lineage/compat",
    "hardware/xiaomi"
]


lib_fixups: lib_fixups_user_type = {
    libs_clang_rt_ubsan: lib_fixup_remove_arch_suffix,
    libs_proto_3_9_1: lib_fixup_vendorcompat,
    libs_proto_21_12: lib_fixup_remove_proto_version_suffix,
}


def fixup_ndk_platform(libname: str) -> tuple[str, str]:
    """
    Replace -ndk_platform with -ndk
    """
    return (libname, libname.replace("-ndk_platform.so", "-ndk.so"))


patchelf_version = "0_17_2"

def lib_fixup_vendor_suffix(lib: str, partition: str, *args, **kwargs):
    return f'{lib}_{partition}' if partition == 'vendor' else None


lib_fixups: lib_fixups_user_type = {
    **lib_fixups,
    ('vendor.mediatek.hardware.videotelephony@1.0',): lib_fixup_vendor_suffix,
}

blob_fixups: blob_fixups_user_type = {
    "vendor/bin/hw/android.hardware.security.keymint@1.0-service.mitee": blob_fixup()
    .patchelf_version(patchelf_version)
    .replace_needed(
        "android.hardware.security.keymint-V1-ndk_platform.so",
        "android.hardware.security.keymint-V4-ndk.so",
    )
    .add_needed("android.hardware.security.rkp-V3-ndk.so")
    .replace_needed(
        *fixup_ndk_platform("android.hardware.security.secureclock-V1-ndk_platform.so")
    )
    .replace_needed(
        *fixup_ndk_platform("android.hardware.security.sharedsecret-V1-ndk_platform.so")
    ),
    "vendor/etc/init/android.hardware.graphics.allocator@4.0-service-mediatek.rc": blob_fixup().regex_replace(
        "android.hardware.graphics.allocator@4.0-service-mediatek",
        "mt6855/android.hardware.graphics.allocator@4.0-service-mediatek.mt6855",
    ),
    (
        "vendor/lib/libwvhidl.so",
        "vendor/lib/mediadrm/libwvdrmengine.so",
        "vendor/lib64/libwvhidl.so",
        "vendor/lib64/mediadrm/libwvdrmengine.so",
    ): blob_fixup()
    .patchelf_version(patchelf_version)
    .replace_needed("libprotobuf-cpp-lite-3.9.1.so", "libprotobuf-cpp-full-3.9.1.so"),
    (
        "vendor/bin/mnld",
        "vendor/lib64/hw/android.hardware.sensors@2.X-subhal-mediatek.so",
        "vendor/lib64/mt6855/libaalservice.so",
    ): blob_fixup()
    .patchelf_version(patchelf_version)
    .replace_needed("libsensorndkbridge.so", "android.hardware.sensors@1.0-convert-shared.so"),
    "vendor/bin/hw/android.hardware.media.c2@1.2-mediatek-64b": blob_fixup()
    .patchelf_version(patchelf_version)
    .replace_needed("libavservices_minijail_vendor.so", "libavservices_minijail.so")
    .add_needed("libstagefright_foundation-v33.so"),
    "vendor/etc/init/android.hardware.media.c2@1.2-mediatek.rc": blob_fixup().regex_replace(
        "@1.2-mediatek", "@1.2-mediatek-64b"
    ),
    "vendor/etc/init/android.hardware.bluetooth@1.1-service-mediatek.rc": blob_fixup().regex_replace(
        "on property:vts(.|\n)*", ""
    ),
    "vendor/etc/init/android.hardware.neuralnetworks-shim-service-mtk.rc": blob_fixup().regex_replace(
        "start", "enable"
    ),
    (
        "vendor/lib64/libteei_daemon_vfs.so",
        "vendor/lib64/mt6855/lib3a.flash.so",
        "vendor/lib64/mt6855/lib3a.ae.stat.so",
        "vendor/lib64/mt6855/lib3a.sensors.color.so",
        "vendor/lib64/mt6855/lib3a.sensors.flicker.so",
    ): blob_fixup()
    .patchelf_version(patchelf_version)
    .add_needed("liblog.so"),
    (
        "vendor/lib64/mt6855/libmtkcam_stdutils.so",
        "vendor/lib64/hw/mt6855/android.hardware.camera.provider@2.6-impl-mediatek.so"
    ): blob_fixup()
    .patchelf_version(patchelf_version)
    .replace_needed("libutils.so", "libutils-v32.so"),
    "vendor/lib64/libmorpho_video_stabilizer.so": blob_fixup()
    .add_needed("libutils.so"),
    "vendor/lib64/hw/mt6855/vendor.mediatek.hardware.pq@2.15-impl.so": blob_fixup()
    .patchelf_version(patchelf_version)
    .add_needed("android.hardware.sensors@1.0-convert-shared.so")
    .replace_needed("libutils.so", "libutils-v32.so"),
    (
        'vendor/lib64/libmorpho_Ldc.so',
        'vendor/lib64/libTrueSight.so',
	    'vendor/lib64/libMiVideoFilter.so',
    ): blob_fixup()
        .clear_symbol_version('AHardwareBuffer_acquire')
        .clear_symbol_version('AHardwareBuffer_allocate')
        .clear_symbol_version('AHardwareBuffer_describe')
        .clear_symbol_version('AHardwareBuffer_lock')
        .clear_symbol_version('AHardwareBuffer_lockPlanes')
        .clear_symbol_version('AHardwareBuffer_release')
        .clear_symbol_version('AHardwareBuffer_unlock'),
    ('vendor/lib64/mt6855/libneuralnetworks_sl_driver_mtk_prebuilt.so', 'vendor/lib64/hw/mt6855/vulkan.mtk.so'): blob_fixup()
        .clear_symbol_version('AHardwareBuffer_allocate')
        .clear_symbol_version('AHardwareBuffer_createFromHandle')
        .clear_symbol_version('AHardwareBuffer_describe')
        .clear_symbol_version('AHardwareBuffer_getNativeHandle')
        .clear_symbol_version('AHardwareBuffer_lock')
        .clear_symbol_version('AHardwareBuffer_release')
        .clear_symbol_version('AHardwareBuffer_unlock')
        .clear_symbol_version('AHardwareBuffer_acquire')
        .add_needed('libbase_shim.so'),
    ('vendor/lib64/libnvram.so', 'vendor/lib64/libsysenv.so'): blob_fixup()
        .add_needed('libbase_shim.so'),
    'vendor/lib64/hw/hwcomposer.mtk_common.so': blob_fixup()
        .add_needed('libprocessgroup_shim.so'),
    'vendor/lib64/mt6855/libmnl.so': blob_fixup()
    .add_needed('libcutils.so'),
    ('vendor/lib64/hw/android.hardware.gnss-impl-mediatek.so', 'vendor/bin/hw/android.hardware.gnss-service.mediatek'): blob_fixup()
    .replace_needed('android.hardware.gnss-V1-ndk_platform.so', 'android.hardware.gnss-V1-ndk.so'),
    'vendor/bin/mtk_agpsd': blob_fixup()
    .replace_needed('libcrypto.so', 'libcrypto-v32.so')
    .add_needed('libssl-v32.so'),
    'vendor/lib64/mt6855/libmnl.so': blob_fixup()
    .add_needed('libcutils.so'),
    'vendor/lib64/libdlbdsservice.so': blob_fixup()
    .replace_needed("libstagefright_foundation.so", "libstagefright_foundation-v33.so")
}  # fmt: skip

module = ExtractUtilsModule(
    'beryl',
    'xiaomi',
    blob_fixups=blob_fixups,
    lib_fixups=lib_fixups,
    namespace_imports=namespace_imports,
    check_elf=True,
    add_firmware_proprietary_file=True,
)

if __name__ == "__main__":
    utils = ExtractUtils.device(module)
    utils.run()