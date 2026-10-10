#define main launcher_main
#include "../../app/src/main/cpp/run_server.c"
#undef main
#include <assert.h>

int main(void) {
    const char *valid[] = {
        "am.jar", "main.jar",
        "/data/user/0/io.github.sysopbp.sphere.debug/files/server/am.jar",
        "/data/user/10/io.github.sysopbp.sphere/files/server/main.jar",
        "/data/user_de/0/io.github.sysopbp.sphere/files/server/am.jar"
    };
    const char *invalid[] = {
        "", ".", "..", "/", "/data//am.jar", "/data/../am.jar",
        "/data/./am.jar", "/data/am.jar/", "relative/am.jar",
        "/data/am.jar;id", "/data/am.jar\n", "/data/$(id).jar"
    };
    for (size_t i = 0; i < sizeof(valid) / sizeof(valid[0]); ++i)
        assert(is_safe_jar_path(valid[i]));
    for (size_t i = 0; i < sizeof(invalid) / sizeof(invalid[0]); ++i)
        assert(!is_safe_jar_path(invalid[i]));
    assert(strcmp(jar_basename(valid[2]), "am.jar") == 0);
    assert(strcmp(jar_basename("main.jar"), "main.jar") == 0);

    // Verify direct private-file staging independent of Android runtime.
    char source[] = "/tmp/sphere-jar-test-XXXXXX";
    int fd = mkstemp(source);
    assert(fd >= 0);
    assert(write(fd, "test-jar", 8) == 8);
    close(fd);
    char destination[128];
    snprintf(destination, sizeof(destination), "%s-copy", source);
    assert(copy_file(source, destination) == 0);
    char data[8];
    fd = open(destination, O_RDONLY);
    assert(fd >= 0 && read(fd, data, sizeof(data)) == 8);
    assert(memcmp(data, "test-jar", 8) == 0);
    close(fd);
    unlink(source);
    unlink(destination);
    puts("Sphere native launcher regression checks passed.");
    return 0;
}
