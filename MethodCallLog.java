import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * An Adapter for MethodVisitor which overrides the 
 * visitCode method to print the method's name before
 * starting the code visit.
 */
class MethodAdapter extends MethodVisitor implements Opcodes {
  
  /** The name of the method receiving the visit 
   */
  final String name;

  public MethodAdapter(final MethodVisitor mv, final String _name) {
    super(ASM6, mv);
    name = _name;
  }

  @Override
  public void visitCode() {
    // Print the method's name : System.out.println(name) in asm format
    mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
    mv.visitLdcInsn( name );
    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
    
    mv.visitCode();
  }
}

/**
 * An Adapter for ClassVisitor which only overrides its
 * visitMethod method to return a MethodAdapter, since
 * a ClassWriter returns a MethodWriter.
 */
class ClassAdapter extends ClassVisitor implements Opcodes {
  
  public ClassAdapter(final ClassVisitor cv) {
    super(ASM6, cv);
  }

  @Override
  public MethodVisitor visitMethod(final int access, final String name,
          final String desc, final String signature, final String[] exceptions) {
    MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
    return mv == null ? null : new MethodAdapter(mv, name);
  }
}

public class MethodCallLog {

  public static void main(final String args[]) throws Exception {
    FileInputStream is = new FileInputStream(args[0]); // Open .class file taken as the first argument
    
    ClassReader cr = new ClassReader(is); 
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    ClassAdapter ca = new ClassAdapter(cw);
    cr.accept(ca, 0);

    FileOutputStream fos = new FileOutputStream(args[1]); // Write to .class file name taken as the second argument
    fos.write(cw.toByteArray());
    fos.close();
  }
}
