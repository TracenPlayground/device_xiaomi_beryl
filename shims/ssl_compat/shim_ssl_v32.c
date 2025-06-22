// Intentionally empty. The shim exists to provide SONAME "libssl-v32.so"
// and to depend on libssl so the real symbols are available at runtime.
__attribute__((visibility("default"))) void __ssl_v32_shim_anchor(void) {}
