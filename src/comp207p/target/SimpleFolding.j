; Jasmin Java assembler code that assembles the SimpleFolding example class

.source Type1.j
.class public comp207p/target/SimpleFolding
.super java/lang/Object

.method public <init>()V
    aload_0
    invokenonvirtual java/lang/Object/<init>()V
    return
.end method

.method public ldcAdd()V
    .limit stack 3

    getstatic java/lang/System/out Ljava/io/PrintStream;
    ldc 67
    ldc 12345
    iadd
    invokevirtual java/io/PrintStream/println(I)V
    return
.end method

.method public ldcNestedAdd()V
    .limit stack 3

    getstatic java/lang/System/out Ljava/io/PrintStream;
    ldc 67
    ldc 12345
    iadd
    ldc 32
    iadd
    invokevirtual java/io/PrintStream/println(I)V
    return
.end method

.method public ldcNeg()V
    .limit stack 2

    getstatic java/lang/System/out Ljava/io/PrintStream;
    ldc -67
    ineg
    invokevirtual java/io/PrintStream/println(I)V
    return
.end method

.method public ldcSub()V
    .limit stack 3

    getstatic java/lang/System/out Ljava/io/PrintStream;
    ldc 67
    ldc 12345
    isub
    invokevirtual java/io/PrintStream/println(I)V
    return
.end method

.method public iconstMul()V
    .limit stack 3

    getstatic java/lang/System/out Ljava/io/PrintStream;
    iconst_2
    iconst_3
    imul
    invokevirtual java/io/PrintStream/println(I)V
    return
.end method

.method public combinationMulDiv()V
    .limit stack 4

    getstatic java/lang/System/out Ljava/io/PrintStream;
    bipush 45
    sipush 10000
    iconst_5
    idiv
    imul
    invokevirtual java/io/PrintStream/println(I)V
    return
.end method

.method public fAdd()V
    .limit stack 3

    getstatic java/lang/System/out Ljava/io/PrintStream;
    fconst_1
    ldc 2.0
    fadd
    invokevirtual java/io/PrintStream/println(F)V
    return
.end method

.method public lSub()V
    .limit stack 5

    getstatic java/lang/System/out Ljava/io/PrintStream;
    ldc2_w 20000
    lconst_1
    lsub
    invokevirtual java/io/PrintStream/println(J)V
    return
.end method

.method public dDiv()V
    .limit stack 5

    getstatic java/lang/System/out Ljava/io/PrintStream;
    ldc2_w 9.0
    ldc2_w 2.0
    ddiv
    invokevirtual java/io/PrintStream/println(D)V
    return
.end method

.method public i2d()V
    .limit stack 3

    getstatic java/lang/System/out Ljava/io/PrintStream;
    sipush 5
    i2d
    invokevirtual java/io/PrintStream/println(D)V
    return
.end method
