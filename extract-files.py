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
}

blob_fixups: blob_fixups_user_type = {
    'vendor/lib64/libmifpext.so': blob_fixup()
        .replace_needed('android.hardware.sensors-V2-ndk.so', 'android.hardware.sensors-V3-ndk.so'),
    'vendor/lib64/libmt_mitee.so': blob_fixup()
    .replace_needed('android.hardware.security.keymint-V3-ndk.so', 'android.hardware.security.keymint-V4-ndk.so'),
    ('vendor/lib64/libpqxmlparser.so',
     'vendor/lib64/librt_extamp_intf.so',
     'vendor/lib64/libsilkybrightnesscore.so'): blob_fixup()
        .replace_needed('libtinyxml2.so', 'libtinyxml2-v36.so'),
    (
        "vendor/lib/libwvhidl.so",
        "vendor/lib/mediadrm/libwvdrmengine.so",
        "vendor/lib64/libwvhidl.so",
        "vendor/lib64/mediadrm/libwvdrmengine.so",
    ): blob_fixup()
    .patchelf_version(patchelf_version)
    .replace_needed("libprotobuf-cpp-lite-3.9.1.so", "libprotobuf-cpp-full-3.9.1.so"),
    (
        "vendor/lib64/hw/android.hardware.sensors@2.X-subhal-mediatek.so",
        "vendor/lib64/mt6855/libaalservice.so",
    ): blob_fixup()
    .patchelf_version(patchelf_version)
    .replace_needed("libsensorndkbridge.so", "android.hardware.sensors@1.0-convert-shared.so"),
    (
        "vendor/lib64/libteei_daemon_vfs.so",
        "vendor/lib64/mt6855/lib3a.ae.stSat.so",
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

    'vendor/lib64/mt6855/libmtkcam_hal_aidl_common.so': blob_fixup()
        .replace_needed('android.hardware.camera.common-V2-ndk.so', 'android.hardware.camera.common-V1-ndk.so'),
    (
        'vendor/lib64/libmorpho_Ldc.so',
        'vendor/lib64/libTrueSight.so',
        'vendor/lib64/libMiVideoFilter.so',
        'vendor/lib64/libMiPhotoFilter.so',
        'vendor/lib64/libtflite_mtk.so',
    ): blob_fixup()
        .clear_symbol_version('AHardwareBuffer_acquire')
        .clear_symbol_version('AHardwareBuffer_allocate')
        .clear_symbol_version('AHardwareBuffer_describe')
        .clear_symbol_version('AHardwareBuffer_isSupported')
        .clear_symbol_version('AHardwareBuffer_lock')
        .clear_symbol_version('AHardwareBuffer_lockPlanes')
        .clear_symbol_version('AHardwareBuffer_release')
        .clear_symbol_version('AHardwareBuffer_unlock'),
    ('vendor/lib64/libneuralnetworks_sl_driver_mtk_prebuilt.so', 'vendor/lib64/mt6855/libneuron_adapter_mgvi.so', 'vendor/lib64/hw/mt6855/vulkan.mtk.so'): blob_fixup()
        .clear_symbol_version('AHardwareBuffer_allocate')
        .clear_symbol_version('AHardwareBuffer_createFromHandle')
        .clear_symbol_version('AHardwareBuffer_describe')
        .clear_symbol_version('AHardwareBuffer_getNativeHandle')
        .clear_symbol_version('AHardwareBuffer_lock')
        .clear_symbol_version('AHardwareBuffer_release')
        .clear_symbol_version('AHardwareBuffer_unlock')
        .clear_symbol_version('AHardwareBuffer_acquire')
        .add_needed('libbase_shim.so'),
    'vendor/lib64/mt6855/libIMGegl.so': blob_fixup()
        .clear_symbol_version('ANativeWindowBuffer_getHardwareBuffer')
        .clear_symbol_version('ANativeWindow_cancelBuffer')
        .clear_symbol_version('ANativeWindow_dequeueBuffer')
        .clear_symbol_version('ANativeWindow_getFormat')
        .clear_symbol_version('ANativeWindow_query')
        .clear_symbol_version('ANativeWindow_queueBuffer')
        .clear_symbol_version('ANativeWindow_setBuffersDimensions')
        .clear_symbol_version('ANativeWindow_setBuffersFormat')
        .clear_symbol_version('ANativeWindow_setBuffersTransform')
        .clear_symbol_version('ANativeWindow_setSharedBufferMode')
        .clear_symbol_version('ANativeWindow_setSwapInterval')
        .clear_symbol_version('ANativeWindow_setUsage'),
    ('vendor/lib64/libnvram.so', 'vendor/lib64/libsysenv.so'): blob_fixup()
        .add_needed('libbase_shim.so'),
    'vendor/lib64/mt6855/libmnl.so': blob_fixup()
    .add_needed('libcutils.so'),
    'vendor/bin/mtk_agpsd': blob_fixup()
    .replace_needed('libcrypto.so', 'libcrypto-v33.so')
    .add_needed('libssl.so'),
    'vendor/lib64/mt6855/libmnl.so': blob_fixup()
    .add_needed('libcutils.so'),
#    'system_ext/priv-app/ImsService/ImsService.apk': blob_fixup()
#    .apktool_patch('blob-patches/ImsService.patch', '-r'),
    'system_ext/lib64/libimsma.so': blob_fixup()
    .replace_needed('libsink.so', 'libsink-mtk.so'),
    'system_ext/lib64/libsink-mtk.so': blob_fixup()
    .add_needed('libaudioclient_shim.so'),
    'system_ext/lib64/libsource.so': blob_fixup()
    .add_needed('libui_shim.so'),
    'vendor/lib64/hw/hwcomposer.mtk_common.so': blob_fixup()
        .add_needed('libprocessgroup_shim.so'),
    'vendor/lib64/mt6855/libmtkcam_hal_aidl_common.so': blob_fixup()
        .replace_needed('android.hardware.camera.common-V2-ndk.so', 'android.hardware.camera.common-V1-ndk.so'),
    'vendor/lib64/libdlbdsservice.so': blob_fixup()
    .replace_needed("libstagefright_foundation.so", "libstagefright_foundation-v33.so"),
    'vendor/bin/hw/mtkfusionrild': blob_fixup()
        .add_needed('libutils-v33.so'),
    (
        'vendor/lib64/vendor.mediatek.hardware.pq_aidl-V7-ndk.so',
        'vendor/lib64/libcodec2_fsr.so',
        'vendor/lib64/libgpud.so',
        'vendor/lib64/libcodec2_vpp_AIMEMC_plugin.so',
        'vendor/lib64/libcodec2_vpp_AISR_plugin.so',
        'vendor/lib64/hw/hwcomposer.mtk_common.so',
        'vendor/lib64/hw/mt6855/android.hardware.graphics.allocator-V2-mediatek.so',
        'vendor/bin/hw/mt6855/android.hardware.graphics.allocator-V2-service-mediatek.mt6789',
        'vendor/lib64/vendor.mediatek.hardware.camera.isphal-V1-ndk.so',
    ): blob_fixup()
        .replace_needed('android.hardware.graphics.common-V6-ndk.so', 'android.hardware.graphics.common-V7-ndk.so'),
         'vendor/lib64/mt6855/libmtkcam_grallocutils.so': blob_fixup()
        .replace_needed('android.hardware.graphics.common-V5-ndk.so', 'android.hardware.graphics.common-V7-ndk.so'),
     'vendor/lib64/hw/audio.primary.mediatek.so': blob_fixup()
        .replace_needed('libtinyxml2.so', 'libtinyxml2-v36.so')
        .replace_needed('android.media.audio.common.types-V3-ndk.sp', 'android.media.audio.common.types-V4-ndk.so')
        .replace_needed('android.hardware.bluetooth.audio-V4-ndk.so', 'android.hardware.bluetooth.audio-V5-ndk.so')
        .replace_needed( 'android.hardware.audio.effect-V2-ndk.so', 'android.hardware.audio.effect-V3-ndk.so'),
    ('vendor/bin/mnld', 'vendor/lib64/mt6855/libcam.utils.sensorprovider.so', 'vendor/lib64/libmifpext.so'): blob_fixup()
        .replace_needed('android.hardware.sensors-V2-ndk.so', 'android.hardware.sensors-V3-ndk.so'),
    'vendor/lib64/hw/mt6855/vendor.mediatek.hardware.pq_aidl-impl.so': blob_fixup()
        .replace_needed("android.hardware.graphics.common-V6-ndk.so", "android.hardware.graphics.common-V7-ndk.so")
        .replace_needed('android.hardware.sensors-V2-ndk.so', 'android.hardware.sensors-V3-ndk.so')
        .replace_needed('libtinyxml2.so', 'libtinyxml2-v36.so'),
    'vendor/bin/hw/android.hardware.audio.service-aidl.mediatek': blob_fixup()
        .replace_needed('libaudio_aidl_conversion_common_ndk.so', 'libaudio_aidl_conversion_common_ndk_prebuilt.so'),
    'vendor/lib64/hw/android.hardware.audio.effect.aidl-impl-mediatek.so': blob_fixup()
        .replace_needed('libtinyxml2.so', 'libtinyxml2-v36.so'),
    'vendor/lib64/hw/mt6855/libpvr_mapper_utils.so': blob_fixup()
        .clear_symbol_version('AHardwareBuffer_describe')
        .clear_symbol_version('AHardwareBuffer_getNativeHandle')
        .add_needed('libPVRMtkutils.so'),
    'system_ext/bin/hw/android.hardware.audio.parameter_parser.service': blob_fixup()
        .replace_needed('av-audio-types-aidl-ndk.so', 'av-audio-types-aidl-V3-ndk.so'),
    'vendor/lib64/hw/mt6855/mapper.powervr.so': blob_fixup()
        .add_needed('libPVRMtkutils.so')
        .add_needed('libIMGegl.so')
        .add_needed('libgralloc_extra.so')
        .add_needed('libsync.so'),
    'vendor/bin/mtk_agpsd': blob_fixup()
        .replace_needed('libssl.so', 'libssl-v36.so'),
    'vendor/lib64/android.hardware.audio.core-impl-mediatek.so': blob_fixup()
        .add_needed('libaudioutils-v36.so'),
     (
        'vendor/lib64/libcodec2_mtk_venc.so',
        'vendor/lib64/libcodec2_mtk_vdec.so',
     ): blob_fixup()
        .replace_needed('libformatter.so', 'libformatter_mtk.so'),
}  # fmt: skip

module = ExtractUtilsModule(
    'beryl',
    'xiaomi',
    blob_fixups=blob_fixups,
    lib_fixups=lib_fixups,
    namespace_imports=namespace_imports,
    check_elf=True,
)

if __name__ == "__main__":
    utils = ExtractUtils.device(module)
    utils.run()
