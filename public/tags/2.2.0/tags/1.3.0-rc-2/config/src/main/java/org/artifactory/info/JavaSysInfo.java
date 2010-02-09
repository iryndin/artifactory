package org.artifactory.info;

/**
 * An information group for all the java system properties
 *
 * @author Noam Tenne
 */
public class JavaSysInfo extends SystemInfoGroup {

    public JavaSysInfo() {
        super("java.class.version",
                "java.home",
                "java.io.tmpdir",
                "java.runtime.name",
                "java.runtime.version",
                "java.specification.name",
                "java.specification.vendor",
                "java.specification.version",
                "java.vendor",
                "java.vendor.url",
                "java.vendor.url.bug",
                "java.version",
                "java.vm.info",
                "java.vm.name",
                "java.vm.specification.name",
                "java.vm.specification.vendor",
                "java.vm.specification.version",
                "java.vm.vendor",
                "java.vm.version",
                "sun.arch.data.model",
                "sun.boot.library.path",
                "sun.cpu.endian",
                "sun.cpu.isalist",
                "sun.io.unicode.encoding",
                "sun.java.launcher",
                "sun.jnu.encoding",
                "sun.management.compiler",
                "sun.os.patch.level");
    }
}