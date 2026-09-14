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

