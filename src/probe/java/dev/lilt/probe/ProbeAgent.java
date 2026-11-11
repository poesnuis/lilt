package dev.lilt.probe;

import java.io.*;
import java.lang.instrument.*;
import java.security.ProtectionDomain;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.nio.file.*;
import org.objectweb.asm.*;

/** Optional identical observer for genuine vanilla and Forge/Lilt comparison runs. */
public final class ProbeAgent {
    public static void premain(String arguments, Instrumentation instrumentation) throws Exception {
        // The hook must have one identity across the application and LaunchWrapper class loaders.
        // Isolate only the hook, so the agent and its ASM visitors stay in one app loader.
        Path hooks = Files.createTempFile("lilt-probe-hook-", ".jar");
        hooks.toFile().deleteOnExit();
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(hooks));
             InputStream in = ProbeAgent.class.getResourceAsStream("/dev/lilt/probe/FrameProbe.class")) {
            out.putNextEntry(new JarEntry("dev/lilt/probe/FrameProbe.class"));
            byte[] buffer = new byte[8192]; int length;
            while ((length = in.read(buffer)) != -1) out.write(buffer, 0, length);
            out.closeEntry();
        }
        instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(hooks.toFile()));
        FrameProbe.initialize(arguments);
        instrumentation.addTransformer(new ClassFileTransformer() {
            public byte[] transform(ClassLoader loader, String name, Class<?> redefining, ProtectionDomain domain, byte[] bytes) {
                if (!"bib".equals(name) && !"net/minecraft/client/Minecraft".equals(name)) return null;
                try {
                    final boolean[] found = {false};
                    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                    ClassVisitor visitor = new ClassVisitor(Opcodes.ASM5, writer) {
                        public MethodVisitor visitMethod(int access, String method, String desc, String signature, String[] exceptions) {
                            MethodVisitor base = super.visitMethod(access, method, desc, signature, exceptions);
                            if (!"()V".equals(desc) || !(method.equals("az") || method.equals("func_71411_J") || method.equals("runGameLoop"))) return base;
                            found[0] = true;
                            return new MethodVisitor(Opcodes.ASM5, base) {
                                public void visitCode() {
                                    super.visitCode();
                                    visitVarInsn(Opcodes.ALOAD, 0);
                                    visitMethodInsn(Opcodes.INVOKESTATIC, "dev/lilt/probe/FrameProbe", "frame", "(Ljava/lang/Object;)V", false);
                                }
                            };
                        }
                    };
                    new ClassReader(bytes).accept(visitor, 0);
                    if (!found[0]) { System.err.println("[Lilt probe] Minecraft 1.12.2 frame method not found."); return null; }
                    System.out.println("[Lilt probe] Observing Minecraft 1.12.2 game-loop entry.");
                    return writer.toByteArray();
                } catch (RuntimeException failure) { System.err.println("[Lilt probe] Disabled: " + failure); return null; }
            }
        });
    }
}
