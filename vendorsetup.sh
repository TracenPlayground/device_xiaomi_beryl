export USE_CCACHE=0

# Apply GMS ANGLE allowlist patch for compatibility
(
    ROOT_DIR="${ANDROID_BUILD_TOP:-$(pwd)}"
    PATCH_FILE="$ROOT_DIR/device/xiaomi/beryl/patches/FrameworkResOverlay_GMS.patch"
    GMS_DIR="$ROOT_DIR/vendor/pixel/gms"

    if [ -d "$GMS_DIR" ] && [ -f "$PATCH_FILE" ]; then
        cd "$GMS_DIR" || exit
        if git apply --check "$PATCH_FILE" >/dev/null 2>&1; then
            echo "[beryl] Applying FrameworkResOverlay_GMS ANGLE allowlist patch..."
            git apply "$PATCH_FILE"
        fi
    fi
)

# Apply sepolicy label for net.pixelos.build_type (unofficial OTA)
(
    ROOT_DIR="${ANDROID_BUILD_TOP:-$(pwd)}"
    PATCH_FILE="$ROOT_DIR/device/xiaomi/beryl/patches/SePolicyNetPixelosBuildType.patch"
    SEPOLICY_DIR="$ROOT_DIR/device/custom/sepolicy"

    if [ -d "$SEPOLICY_DIR" ] && [ -f "$PATCH_FILE" ]; then
        cd "$SEPOLICY_DIR" || exit
        if git apply --check "$PATCH_FILE" >/dev/null 2>&1; then
            echo "[beryl] Applying SePolicyNetPixelosBuildType patch..."
            git apply "$PATCH_FILE"
        fi
    fi
)

