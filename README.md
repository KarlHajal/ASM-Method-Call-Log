# ASM Task


### Task
***
Use the ASM library for Java to print the name of each method when it's executed.

### Usage
***
```
sh printmethods.sh NAME
```
where NAME refers to the name (without the extension) of the .java file we would like to use the tool on.

The script compiles the NAME.java file, compiles MethodCallLog, runs it on NAME.class to modify it such that methods print their name at the start of their execution, and finally runs the modified NAME.class .

Note that as it stands, the script is configured to use the ASM 6.1.1 jar file.

### How I did it
***
I first ran the test Java class through the ASMifier to see what it looked like in ASM format.
Here's a snippet showing what the methods looked like.
```Java
        {
            mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
            mv.visitCode();
            mv.visitMethodInsn(INVOKESTATIC, "C", "m", "()V", false);
            mv.visitMethodInsn(INVOKESTATIC, "C", "n", "()V", false);
            mv.visitMethodInsn(INVOKESTATIC, "C", "m", "()V", false);
            mv.visitMethodInsn(INVOKESTATIC, "C", "m", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 1);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "m", "()V", null, null);
            mv.visitCode();
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "n", "()V", null, null);
            mv.visitCode();
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
```

It appeared that the printing code would have to be added right before the MethodVisitor's visitCode() method.

Consequently, I implemented an Adapter for MethodVisitor which overrides the class' visitCode() method to do just that:

```Java
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
```
where I got the ASM format for System.out.println(name) by running it through the ASMifier:
```Java
 mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
 mv.visitLdcInsn("main");
 mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
```

Finally, to make sure that MethodAdapter is used, I implemented an adapter for ClassVisitor called ClassAdapter which returns a MethodAdapter when visitMethod(...) is called, as shown in the ASM code above.

```
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
```

The modification of a target java class happens in the MethodCallLog class shown below:
```
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
```
