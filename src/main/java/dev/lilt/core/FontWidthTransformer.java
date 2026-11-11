package dev.lilt.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Narrow patch; if the expected vanilla methods are absent, leave the class untouched. */
public final class FontWidthTransformer implements IClassTransformer {
    private static final String HOOK = "dev/lilt/core/WidthCache";
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !"net.minecraft.client.gui.FontRenderer".equals(transformedName)) return bytes;
        try {
            ClassNode cls = new ClassNode();
            new ClassReader(bytes).accept(cls, 0);
            MethodNode width = null;
            int invalidators = 0;
            for (MethodNode method : cls.methods) {
                if ("(Ljava/lang/String;)I".equals(method.desc) &&
                    named(method, "getStringWidth", "func_78256_a", "a")) width = method;
                if (invalidates(method)) invalidators++;
            }
            // Both flags and resource reload must be hooked, or caching is not safe.
            if (width == null || invalidators != 3) {
                System.err.println("[Lilt] Font metrics signature mismatch; leaving vanilla FontRenderer intact.");
                return bytes;
            }
            for (AbstractInsnNode instruction : width.instructions.toArray()) {
                if (instruction instanceof MethodInsnNode && HOOK.equals(((MethodInsnNode) instruction).owner)) return bytes;
            }
            for (AbstractInsnNode instruction : width.instructions.toArray()) {
                if (instruction.getOpcode() == Opcodes.IRETURN) {
                    InsnList save = new InsnList();
                    save.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    save.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    save.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "remember", "(ILjava/lang/Object;Ljava/lang/String;)I", false));
                    width.instructions.insertBefore(instruction, save);
                }
            }
            LabelNode miss = new LabelNode();
            InsnList probe = new InsnList();
            probe.add(new VarInsnNode(Opcodes.ALOAD, 0));
            probe.add(new VarInsnNode(Opcodes.ALOAD, 1));
            probe.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "find", "(Ljava/lang/Object;Ljava/lang/String;)I", false));
            probe.add(new InsnNode(Opcodes.DUP));
            probe.add(new JumpInsnNode(Opcodes.IFLT, miss));
            probe.add(new InsnNode(Opcodes.IRETURN));
            probe.add(miss);
            probe.add(new FrameNode(Opcodes.F_SAME1, 0, null, 1, new Object[]{Opcodes.INTEGER}));
            probe.add(new InsnNode(Opcodes.POP));
            width.instructions.insert(probe);
            for (MethodNode method : cls.methods) {
                if (invalidates(method)) {
                    method.instructions.insert(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "invalidate", "()V", false));
                }
            }
            // Preserve existing frames. No class loading during LaunchWrapper transformation.
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            cls.accept(writer);
            byte[] result = writer.toByteArray();
            WidthCache.installed();
            System.out.println("[Lilt] Bounded font metrics cache installed.");
            return result;
        } catch (RuntimeException failure) {
            System.err.println("[Lilt] Could not apply font metrics cache; using vanilla: " + failure);
            return bytes;
        }
    }
    private static boolean named(MethodNode method, String mcp, String srg, String obf) {
        return method.name.equals(mcp) || method.name.equals(srg) || method.name.equals(obf);
    }
    private static boolean invalidates(MethodNode method) {
        return (method.desc.equals("(Z)V") && (named(method, "setUnicodeFlag", "func_78264_a", "a") || named(method, "setBidiFlag", "func_78275_b", "b")))
            || (method.desc.endsWith(";)V") && named(method, "onResourceManagerReload", "func_110549_a", "a")
                && (method.desc.contains("IResourceManager") || method.desc.equals("(Lcep;)V")));
    }
}
