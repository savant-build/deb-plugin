/*
 * Copyright (c) 2014, Inversoft Inc., All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.savantbuild.plugin.deb
import org.savantbuild.dep.domain.License
import org.savantbuild.dep.domain.Version
import org.savantbuild.domain.Project
import org.savantbuild.output.Output
import org.savantbuild.output.SystemOutOutput
import org.savantbuild.runtime.RuntimeConfiguration
import org.testng.annotations.BeforeMethod
import org.testng.annotations.BeforeSuite
import org.testng.annotations.Test

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

import static org.testng.Assert.*
/**
 * Tests the FilePlugin class.
 *
 * @author Brian Pontarelli
 */
class DebPluginTest {
  public static Path projectDir

  Output output

  Project project

  DebPlugin plugin

  @BeforeSuite
  public static void beforeSuite() {
    projectDir = Paths.get("")
    if (!Files.isRegularFile(projectDir.resolve("LICENSE"))) {
      projectDir = Paths.get("../deb-plugin")
    }
  }

  @BeforeMethod
  public void beforeMethod() {
    output = new SystemOutOutput(true)
    output.enableDebug()

    project = new Project(projectDir, output)
    project.group = "org.savantbuild.test"
    project.name = "dev-plugin-test"
    project.version = new Version("1.0")
    project.licenses.put(License.ApacheV2_0, null)

    plugin = new DebPlugin(project, new RuntimeConfiguration(), output)
  }

  @Test
  void build() {
    plugin.build(to: "build/test", package: "test", architecture: "x86", conflicts: "nothing", depends: "test2", enhances: "test3",
        preDepends: "test4", priority: "required", provides: "test5", recommends: "test6", replaces: "test7", section: "web", suggests: "test8",
        preInst: "src/test/scripts/preinst", preRm: "src/test/scripts/prerm",
        postInst: "src/test/scripts/postinst", postRm: "src/test/scripts/postrm") {
      version(upstream: "3.0.0-M4", debian: "1")
      description(synopsis: "Description synopsis", extended: "Description extended")
      maintainer(name: "Inversoft", email: "sales@inversoft.com")
      tarFileSet(dir: "src/main/groovy", prefix: "main-groovy-files")
      tarFileSet(dir: "src/test/groovy", prefix: "test-groovy-files")
      confFileSet(dir: "src/test/conf", prefix: "etc/init.d")
      directory(name: "directory/nested", mode: 0x755)
    }

    // Unpack everything

  }

  private static void assertJarContains(Path jarFile, String... entries) {
    JarFile jf = new JarFile(jarFile.toFile())
    entries.each({ entry -> assertNotNull(jf.getEntry(entry), "Jar [${jarFile}] is missing entry [${entry}]") })
    jf.close()
  }

  private static void assertJarFileEquals(Path jarFile, String entry, Path original) throws IOException {
    JarInputStream jis = new JarInputStream(Files.newInputStream(jarFile))
    JarEntry jarEntry = jis.getNextJarEntry()
    while (jarEntry != null && !jarEntry.getName().equals(entry)) {
      jarEntry = jis.getNextJarEntry()
    }

    if (jarEntry == null) {
      fail("Jar [" + jarFile + "] is missing entry [" + entry + "]")
    }

    ByteArrayOutputStream baos = new ByteArrayOutputStream()
    byte[] buf = new byte[1024]
    int length
    while ((length = jis.read(buf)) != -1) {
      baos.write(buf, 0, length)
    }

    assertEquals(Files.readAllBytes(original), baos.toByteArray())
    assertEquals(jarEntry.getSize(), Files.size(original))
    assertEquals(jarEntry.getCreationTime(), Files.getAttribute(original, "creationTime"))
    jis.close()
  }

  private static void assertJarManifest(Path jarFile, Manifest expected) throws IOException {
    JarFile jf = new JarFile(jarFile.toFile())
    Manifest actual = jf.getManifest()
    println "jarFile ${jarFile} mf ${actual.getMainAttributes()}"
    expected.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_VERSION, "1.0.0")
    expected.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_VENDOR, "org.savantbuild.test.file-plugin-test")

    assertEquals(actual.getMainAttributes(), expected.getMainAttributes(), "Actual " + actual.getMainAttributes() + " expected " + expected.getMainAttributes());
    jf.close()
  }

  private static void assertZipContains(Path zipFile, String... entries) {
    ZipFile jf = new ZipFile(zipFile.toFile())
    entries.each({ entry -> assertNotNull(jf.getEntry(entry), "Zip [${zipFile}] is missing entry [${entry}]") })
    jf.close()
  }

  private static void assertZipNotContains(Path zipFile, String... entries) {
    ZipFile jf = new ZipFile(zipFile.toFile())
    entries.each({ entry -> assertNull(jf.getEntry(entry), "Zip [${zipFile}] is contains entry [${entry}] and shouldn't") })
    jf.close()
  }

  private static void assertZipFileEquals(Path zipFile, String entry, Path original) throws IOException {
    ZipInputStream jis = new ZipInputStream(Files.newInputStream(zipFile))
    ZipEntry zipEntry = jis.getNextEntry()
    while (zipEntry != null && !zipEntry.getName().equals(entry)) {
      zipEntry = jis.getNextEntry()
    }

    if (zipEntry == null) {
      fail("Zip [" + zipFile + "] is missing entry [" + entry + "]")
    }

    ByteArrayOutputStream baos = new ByteArrayOutputStream()
    byte[] buf = new byte[1024]
    int length
    while ((length = jis.read(buf)) != -1) {
      baos.write(buf, 0, length)
    }

    assertEquals(Files.readAllBytes(original), baos.toByteArray())
    assertEquals(zipEntry.getSize(), Files.size(original))
    jis.close()
  }
}
