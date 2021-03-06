/*
 * Copyright (c) 2017-2018 The Regents of the University of California
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package edu.berkeley.cs.jqf.instrument.tracing;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;
import edu.berkeley.cs.jqf.instrument.util.DoublyLinkedList;


@SuppressWarnings("unused") // Dynamically loaded
public final class SingleSnoop {


    static DoublyLinkedList<Thread> threadsToUnblock = new DoublyLinkedList<>();

    private static ThreadLocal<Boolean> block = new ThreadLocal<Boolean>() {
        @Override
        public Boolean initialValue() {
        String threadName = Thread.currentThread().getName();
            if (threadName.startsWith("__JWIG_TRACER__")) {
                return true; // Always block snooping on the tracing thread to prevent cycles
            } else if (threadsToUnblock.synchronizedRemove(Thread.currentThread())){
                return false; // Snoop on threads that were added to the queue explicitly
            } else {
                return true; // Block all other threads (e.g. JVM cleanup threads)
            }
        }
    };

    static final Map<Thread, String> entryPoints = new WeakHashMap<>();


    /** A supplier of callbacks for each thread (does nothing by default). */
    static Function<Thread, Consumer<TraceEvent>> callbackGenerator = (t) -> (e) -> {};


    private static TraceLogger intp = new TraceLogger();

    private SingleSnoop() {}


    /**
     * Register a supplier of callbacks for each named thread, which will consume
     * {@link TraceEvent}s.
     *
     * @param callbackGenerator a supplier of thread-specific callbacks
     */
    public static void setCallbackGenerator(Function<Thread, Consumer<TraceEvent>> callbackGenerator) {
        SingleSnoop.callbackGenerator = callbackGenerator;
    }


    /** Start snooping for this thread, with the top-level call being
     * the <tt>entryPoint</tt>.
     *
     * @param entryPoint the top-level method, formatted as
     *                   <tt>CLASS#METHOD</tt> (e.g.
     *                   <tt>FooBar#main</tt>).
     */
    public static void startSnooping(String entryPoint) {
        // Mark entry point
        entryPoints.put(Thread.currentThread(), entryPoint);
        // XXX: Offer a dummy instruction to warm-up
        // class-loaders of the logger, in order to avoid
        // deadlocks when tracing is triggered from
        // SnoopInstructionTransformer#transform()
        intp.SPECIAL(-1);
        // Unblock snooping for current thread
        unblock();
    }

    public static void unblock() {
        block.set(false);
    }

    public static void REGISTER_THREAD(Thread thread) {
        // Mark entry point as run()
        try {
            // Get a reference to the Thread's Runnable if it exists
            Field targetField = Thread.class.getDeclaredField("target");
            targetField.setAccessible(true);
            Object target =  targetField.get(thread);
            if (target == null) {
                // If the Runnable is not provided explicitly,
                // it is likely a sub-class of Thread with an overriden run() method
                target = thread;
            }
            Method runMethod = target.getClass().getMethod("run");
            String entryPoint = runMethod.getDeclaringClass().getName() + "#run";
            entryPoints.put(thread, entryPoint);
            // Mark thread for unblocking when we snoop its first instruction
            threadsToUnblock.synchronizedAddFirst(thread);
        } catch (NoSuchMethodException | NoSuchFieldException | IllegalAccessException e) {
            // Print error and keep going
            e.printStackTrace();
        }
    }

    public static void LDC(int iid, int mid, int c) {
        if (block.get()) return; else block.set(true);
        intp.LDC(iid, mid, c); block.set(false);
    }

    public static void LDC(int iid, int mid, long c) {
        if (block.get()) return; else block.set(true);
        intp.LDC(iid, mid, c); block.set(false);
    }

    public static void LDC(int iid, int mid, float c) {
        if (block.get()) return; else block.set(true);
        intp.LDC(iid, mid, c); block.set(false);
    }

    public static void LDC(int iid, int mid, double c) {
        if (block.get()) return; else block.set(true);
        intp.LDC(iid, mid, c); block.set(false);
    }

    public static void LDC(int iid, int mid, String c) {
        if (block.get()) return; else block.set(true);
        intp.LDC(iid, mid, c); block.set(false);
    }

    public static void LDC(int iid, int mid, Object c) {
        if (block.get()) return; else block.set(true);
        intp.LDC(iid, mid, c); block.set(false);
    }

    public static void IINC(int iid, int mid, int var, int increment) {
        if (block.get()) return; else block.set(true);
        intp.IINC(iid, mid, var, increment); block.set(false);
    }

    public static void MULTIANEWARRAY(int iid, int mid, String desc, int dims) {
        if (block.get()) return; else block.set(true);
        intp.MULTIANEWARRAY(iid, mid, desc, dims); block.set(false);
    }

    public static void LOOKUPSWITCH(int iid, int mid, int dflt, int[] keys, int[] labels) {
        if (block.get()) return; else block.set(true);
        intp.LOOKUPSWITCH(iid, mid, dflt, keys, labels); block.set(false);
    }

    public static void TABLESWITCH(int iid, int mid, int min, int max, int dflt, int[] labels) {
        if (block.get()) return; else block.set(true);
        intp.TABLESWITCH(iid, mid, min, max, dflt, labels); block.set(false);
    }

    public static void IFEQ(int iid, int mid, int label) {
        if (block.get()) return; else block.set(true);
        intp.IFEQ(iid, mid, label); block.set(false);
    }

    public static void IFNE(int iid, int mid, int label) {
        if (block.get()) return; else block.set(true);
        intp.IFNE(iid, mid, label); block.set(false);
    }

    public static void IFLT(int iid, int mid, int label) {
        if (block.get()) return; else block.set(true);
        intp.IFLT(iid, mid, label); block.set(false);
    }

    public static void IFGE(int iid, int mid, int label) {
        if (block.get()) return; else block.set(true);
        intp.IFGE(iid, mid, label); block.set(false);
    }

    public static void IFGT(int iid, int mid, int label) {
        if (block.get()) return; else block.set(true);
        intp.IFGT(iid, mid, label); block.set(false);
    }

    public static void IFLE(int iid, int mid, int label) {
        if (block.get()) return; else block.set(true);
        intp.IFLE(iid, mid, label); block.set(false);
    }

    public static void IF_ICMPEQ(int iid, int mid, int label) {
        if (block.get()) return; else block.set(true);
        intp.IF_ICMPEQ(iid, mid, label); block.set(false);
    }

    public static void IF_ICMPNE(int iid, int mid, int label) {
        if (block.get()) return; else block.set(true);
        intp.IF_ICMPNE(iid, mid, label); block.set(false);
    }

    public static void IF_ICMPLT(int iid, int mid, int label) {
        if (block.get()) return; else block.set(true);
        intp.IF_ICMPLT(iid, mid, label); block.set(false);
    }

    public static void IF_ICMPGE(int iid, int mid, int label) {
        if (block.get()) return; else block.set(true);
        intp.IF_ICMPGE(iid, mid, label); block.set(false);
    }

    public static void IF_ICMPGT(int iid, int mid, int label) {
        if (block.get()) return; else block.set(true);
        intp.IF_ICMPGT(iid, mid, label); block.set(false);
    }

    public static void IF_ICMPLE(int iid, int mid, int label) {
        if (block.get()) return; else block.set(true);
        intp.IF_ICMPLE(iid, mid, label); block.set(false);
    }

    public static void IF_ACMPEQ(int iid, int mid, int label) {
        if (block.get()) return; else block.set(true);
        intp.IF_ACMPEQ(iid, mid, label); block.set(false);
    }

    public static void IF_ACMPNE(int iid, int mid, int label) {
        if (block.get()) return; else block.set(true);
        intp.IF_ACMPNE(iid, mid, label); block.set(false);
    }

    public static void GOTO(int iid, int mid, int label) {
        if (block.get()) return; else block.set(true);
        intp.GOTO(iid, mid, label); block.set(false);
    }

    public static void JSR(int iid, int mid, int label) {
        if (block.get()) return; else block.set(true);
        intp.JSR(iid, mid, label); block.set(false);
    }

    public static void IFNULL(int iid, int mid, int label) {
        if (block.get()) return; else block.set(true);
        intp.IFNULL(iid, mid, label); block.set(false);
    }

    public static void IFNONNULL(int iid, int mid, int label) {
        if (block.get()) return; else block.set(true);
        intp.IFNONNULL(iid, mid, label); block.set(false);
    }

    public static void INVOKEVIRTUAL(int iid, int mid, String owner, String name, String desc) {
        if (block.get()) return; else block.set(true);
        intp.INVOKEVIRTUAL(iid, mid, owner, name, desc); block.set(false);
    }

    public static void INVOKESPECIAL(int iid, int mid, String owner, String name, String desc) {
        if (block.get()) return; else block.set(true);
        intp.INVOKESPECIAL(iid, mid, owner, name, desc); block.set(false);
    }

    public static void INVOKESTATIC(int iid, int mid, String owner, String name, String desc) {
        if (block.get()) return; else block.set(true);
        intp.INVOKESTATIC(iid, mid, owner, name, desc); block.set(false);
    }

    public static void INVOKEINTERFACE(int iid, int mid, String owner, String name, String desc) {
        if (block.get()) return; else block.set(true);
        intp.INVOKEINTERFACE(iid, mid, owner, name, desc); block.set(false);
    }

    public static void GETSTATIC(int iid, int mid, int cIdx, int fIdx, String desc) {
        if (block.get()) return; else block.set(true);
        intp.GETSTATIC(iid, mid, cIdx, fIdx, desc); block.set(false);
    }

    public static void PUTSTATIC(int iid, int mid, int cIdx, int fIdx, String desc) {
        if (block.get()) return; else block.set(true);
        intp.PUTSTATIC(iid, mid, cIdx, fIdx, desc); block.set(false);
    }

    public static void GETFIELD(int iid, int mid, int cIdx, int fIdx, String desc) {
        if (block.get()) return; else block.set(true);
        intp.GETFIELD(iid, mid, cIdx, fIdx, desc); block.set(false);
    }

    public static void PUTFIELD(int iid, int mid, int cIdx, int fIdx, String desc) {
        if (block.get()) return; else block.set(true);
        intp.PUTFIELD(iid, mid, cIdx, fIdx, desc); block.set(false);
    }

    public static void HEAPLOAD1(Object object, String field, int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.HEAPLOAD(iid, mid, System.identityHashCode(object), field); block.set(false);
    }

    public static void HEAPLOAD2(Object object, int idx, int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.HEAPLOAD(iid, mid, System.identityHashCode(object), String.valueOf(idx)); block.set(false);
    }

    public static void NEW(int iid, int mid, String type) {
        if (block.get()) return; else block.set(true);
        intp.NEW(iid, mid, type, 0); block.set(false);
    }

    public static void ANEWARRAY(int iid, int mid, String type) {
        if (block.get()) return; else block.set(true);
        intp.ANEWARRAY(iid, mid, type); block.set(false);
    }

    public static void CHECKCAST(int iid, int mid, String type) {
        if (block.get()) return; else block.set(true);
        intp.CHECKCAST(iid, mid, type); block.set(false);
    }

    public static void INSTANCEOF(int iid, int mid, String type) {
        if (block.get()) return; else block.set(true);
        intp.INSTANCEOF(iid, mid, type); block.set(false);
    }

    public static void BIPUSH(int iid, int mid, int value) {
        if (block.get()) return; else block.set(true);
        intp.BIPUSH(iid, mid, value); block.set(false);
    }

    public static void SIPUSH(int iid, int mid, int value) {
        if (block.get()) return; else block.set(true);
        intp.SIPUSH(iid, mid, value); block.set(false);
    }

    public static void NEWARRAY(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.NEWARRAY(iid, mid); block.set(false);
    }

    public static void ILOAD(int iid, int mid, int var) {
        if (block.get()) return; else block.set(true);
        intp.ILOAD(iid, mid, var); block.set(false);
    }

    public static void LLOAD(int iid, int mid, int var) {
        if (block.get()) return; else block.set(true);
        intp.LLOAD(iid, mid, var); block.set(false);
    }

    public static void FLOAD(int iid, int mid, int var) {
        if (block.get()) return; else block.set(true);
        intp.FLOAD(iid, mid, var); block.set(false);
    }

    public static void DLOAD(int iid, int mid, int var) {
        if (block.get()) return; else block.set(true);
        intp.DLOAD(iid, mid, var); block.set(false);
    }

    public static void ALOAD(int iid, int mid, int var) {
        if (block.get()) return; else block.set(true);
        intp.ALOAD(iid, mid, var); block.set(false);
    }

    public static void ISTORE(int iid, int mid, int var) {
        if (block.get()) return; else block.set(true);
        intp.ISTORE(iid, mid, var); block.set(false);
    }

    public static void LSTORE(int iid, int mid, int var) {
        if (block.get()) return; else block.set(true);
        intp.LSTORE(iid, mid, var); block.set(false);
    }

    public static void FSTORE(int iid, int mid, int var) {
        if (block.get()) return; else block.set(true);
        intp.FSTORE(iid, mid, var); block.set(false);
    }

    public static void DSTORE(int iid, int mid, int var) {
        if (block.get()) return; else block.set(true);
        intp.DSTORE(iid, mid, var); block.set(false);
    }

    public static void ASTORE(int iid, int mid, int var) {
        if (block.get()) return; else block.set(true);
        intp.ASTORE(iid, mid, var); block.set(false);
    }

    public static void RET(int iid, int mid, int var) {
        if (block.get()) return; else block.set(true);
        intp.RET(iid, mid, var); block.set(false);
    }

    public static void NOP(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.NOP(iid, mid); block.set(false);
    }

    public static void ACONST_NULL(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.ACONST_NULL(iid, mid); block.set(false);
    }

    public static void ICONST_M1(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.ICONST_M1(iid, mid); block.set(false);
    }

    public static void ICONST_0(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.ICONST_0(iid, mid); block.set(false);
    }

    public static void ICONST_1(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.ICONST_1(iid, mid); block.set(false);
    }

    public static void ICONST_2(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.ICONST_2(iid, mid); block.set(false);
    }

    public static void ICONST_3(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.ICONST_3(iid, mid); block.set(false);
    }

    public static void ICONST_4(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.ICONST_4(iid, mid); block.set(false);
    }

    public static void ICONST_5(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.ICONST_5(iid, mid); block.set(false);
    }

    public static void LCONST_0(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.LCONST_0(iid, mid); block.set(false);
    }

    public static void LCONST_1(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.LCONST_1(iid, mid); block.set(false);
    }

    public static void FCONST_0(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.FCONST_0(iid, mid); block.set(false);
    }

    public static void FCONST_1(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.FCONST_1(iid, mid); block.set(false);
    }

    public static void FCONST_2(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.FCONST_2(iid, mid); block.set(false);
    }

    public static void DCONST_0(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.DCONST_0(iid, mid); block.set(false);
    }

    public static void DCONST_1(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.DCONST_1(iid, mid); block.set(false);
    }

    public static void IALOAD(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.IALOAD(iid, mid); block.set(false);
    }

    public static void LALOAD(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.LALOAD(iid, mid); block.set(false);
    }

    public static void FALOAD(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.FALOAD(iid, mid); block.set(false);
    }

    public static void DALOAD(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.DALOAD(iid, mid); block.set(false);
    }

    public static void AALOAD(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.AALOAD(iid, mid); block.set(false);
    }

    public static void BALOAD(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.BALOAD(iid, mid); block.set(false);
    }

    public static void CALOAD(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.CALOAD(iid, mid); block.set(false);
    }

    public static void SALOAD(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.SALOAD(iid, mid); block.set(false);
    }

    public static void IASTORE(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.IASTORE(iid, mid); block.set(false);
    }

    public static void LASTORE(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.LASTORE(iid, mid); block.set(false);
    }

    public static void FASTORE(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.FASTORE(iid, mid); block.set(false);
    }

    public static void DASTORE(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.DASTORE(iid, mid); block.set(false);
    }

    public static void AASTORE(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.AASTORE(iid, mid); block.set(false);
    }

    public static void BASTORE(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.BASTORE(iid, mid); block.set(false);
    }

    public static void CASTORE(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.CASTORE(iid, mid); block.set(false);
    }

    public static void SASTORE(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.SASTORE(iid, mid); block.set(false);
    }

    public static void POP(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.POP(iid, mid); block.set(false);
    }

    public static void POP2(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.POP2(iid, mid); block.set(false);
    }

    public static void DUP(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.DUP(iid, mid); block.set(false);
    }

    public static void DUP_X1(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.DUP_X1(iid, mid); block.set(false);
    }

    public static void DUP_X2(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.DUP_X2(iid, mid); block.set(false);
    }

    public static void DUP2(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.DUP2(iid, mid); block.set(false);
    }

    public static void DUP2_X1(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.DUP2_X1(iid, mid); block.set(false);
    }

    public static void DUP2_X2(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.DUP2_X2(iid, mid); block.set(false);
    }

    public static void SWAP(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.SWAP(iid, mid); block.set(false);
    }

    public static void IADD(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.IADD(iid, mid); block.set(false);
    }

    public static void LADD(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.LADD(iid, mid); block.set(false);
    }

    public static void FADD(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.FADD(iid, mid); block.set(false);
    }

    public static void DADD(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.DADD(iid, mid); block.set(false);
    }

    public static void ISUB(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.ISUB(iid, mid); block.set(false);
    }

    public static void LSUB(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.LSUB(iid, mid); block.set(false);
    }

    public static void FSUB(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.FSUB(iid, mid); block.set(false);
    }

    public static void DSUB(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.DSUB(iid, mid); block.set(false);
    }

    public static void IMUL(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.IMUL(iid, mid); block.set(false);
    }

    public static void LMUL(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.LMUL(iid, mid); block.set(false);
    }

    public static void FMUL(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.FMUL(iid, mid); block.set(false);
    }

    public static void DMUL(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.DMUL(iid, mid); block.set(false);
    }

    public static void IDIV(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.IDIV(iid, mid); block.set(false);
    }

    public static void LDIV(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.LDIV(iid, mid); block.set(false);
    }

    public static void FDIV(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.FDIV(iid, mid); block.set(false);
    }

    public static void DDIV(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.DDIV(iid, mid); block.set(false);
    }

    public static void IREM(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.IREM(iid, mid); block.set(false);
    }

    public static void LREM(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.LREM(iid, mid); block.set(false);
    }

    public static void FREM(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.FREM(iid, mid); block.set(false);
    }

    public static void DREM(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.DREM(iid, mid); block.set(false);
    }

    public static void INEG(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.INEG(iid, mid); block.set(false);
    }

    public static void LNEG(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.LNEG(iid, mid); block.set(false);
    }

    public static void FNEG(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.FNEG(iid, mid); block.set(false);
    }

    public static void DNEG(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.DNEG(iid, mid); block.set(false);
    }

    public static void ISHL(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.ISHL(iid, mid); block.set(false);
    }

    public static void LSHL(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.LSHL(iid, mid); block.set(false);
    }

    public static void ISHR(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.ISHR(iid, mid); block.set(false);
    }

    public static void LSHR(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.LSHR(iid, mid); block.set(false);
    }

    public static void IUSHR(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.IUSHR(iid, mid); block.set(false);
    }

    public static void LUSHR(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.LUSHR(iid, mid); block.set(false);
    }

    public static void IAND(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.IAND(iid, mid); block.set(false);
    }

    public static void LAND(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.LAND(iid, mid); block.set(false);
    }

    public static void IOR(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.IOR(iid, mid); block.set(false);
    }

    public static void LOR(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.LOR(iid, mid); block.set(false);
    }

    public static void IXOR(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.IXOR(iid, mid); block.set(false);
    }

    public static void LXOR(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.LXOR(iid, mid); block.set(false);
    }

    public static void I2L(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.I2L(iid, mid); block.set(false);
    }

    public static void I2F(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.I2F(iid, mid); block.set(false);
    }

    public static void I2D(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.I2D(iid, mid); block.set(false);
    }

    public static void L2I(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.L2I(iid, mid); block.set(false);
    }

    public static void L2F(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.L2F(iid, mid); block.set(false);
    }

    public static void L2D(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.L2D(iid, mid); block.set(false);
    }

    public static void F2I(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.F2I(iid, mid); block.set(false);
    }

    public static void F2L(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.F2L(iid, mid); block.set(false);
    }

    public static void F2D(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.F2D(iid, mid); block.set(false);
    }

    public static void D2I(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.D2I(iid, mid); block.set(false);
    }

    public static void D2L(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.D2L(iid, mid); block.set(false);
    }

    public static void D2F(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.D2F(iid, mid); block.set(false);
    }

    public static void I2B(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.I2B(iid, mid); block.set(false);
    }

    public static void I2C(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.I2C(iid, mid); block.set(false);
    }

    public static void I2S(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.I2S(iid, mid); block.set(false);
    }

    public static void LCMP(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.LCMP(iid, mid); block.set(false);
    }

    public static void FCMPL(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.FCMPL(iid, mid); block.set(false);
    }

    public static void FCMPG(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.FCMPG(iid, mid); block.set(false);
    }

    public static void DCMPL(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.DCMPL(iid, mid); block.set(false);
    }

    public static void DCMPG(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.DCMPG(iid, mid); block.set(false);
    }

    public static void IRETURN(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.IRETURN(iid, mid); block.set(false);
    }

    public static void LRETURN(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.LRETURN(iid, mid); block.set(false);
    }

    public static void FRETURN(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.FRETURN(iid, mid); block.set(false);
    }

    public static void DRETURN(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.DRETURN(iid, mid); block.set(false);
    }

    public static void ARETURN(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.ARETURN(iid, mid); block.set(false);
    }

    public static void RETURN(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.RETURN(iid, mid); block.set(false);
    }

    public static void ARRAYLENGTH(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.ARRAYLENGTH(iid, mid); block.set(false);
    }

    public static void ATHROW(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.ATHROW(iid, mid); block.set(false);
    }

    public static void MONITORENTER(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.MONITORENTER(iid, mid); block.set(false);
    }

    public static void MONITOREXIT(int iid, int mid) {
        if (block.get()) return; else block.set(true);
        intp.MONITOREXIT(iid, mid); block.set(false);
    }

    public static void GETVALUE_double(double v) {
        if (block.get()) return; else block.set(true);
        intp.GETVALUE_double(v); block.set(false);
    }

    public static void GETVALUE_long(long v) {
        if (block.get()) return; else block.set(true);
        intp.GETVALUE_long(v); block.set(false);
    }

    public static void GETVALUE_Object(Object v) {
        if (block.get()) return; else block.set(true);
        intp.GETVALUE_Object(v); block.set(false);
    }

    public static void GETVALUE_boolean(boolean v) {
        if (block.get()) return; else block.set(true);
        intp.GETVALUE_boolean(v); block.set(false);
    }

    public static void GETVALUE_byte(byte v) {
        if (block.get()) return; else block.set(true);
        intp.GETVALUE_byte(v); block.set(false);
    }

    public static void GETVALUE_char(char v) {
        if (block.get()) return; else block.set(true);
        intp.GETVALUE_char(v); block.set(false);
    }

    public static void GETVALUE_float(float v) {
        if (block.get()) return; else block.set(true);
        intp.GETVALUE_float(v); block.set(false);
    }

    public static void GETVALUE_int(int v) {
        if (block.get()) return; else block.set(true);
        intp.GETVALUE_int(v); block.set(false);
    }

    public static void GETVALUE_short(short v) {
        if (block.get()) return; else block.set(true);
        intp.GETVALUE_short(v); block.set(false);
    }

    public static void GETVALUE_void() {
        if (block.get()) return; else block.set(true);
        intp.GETVALUE_void(); block.set(false);
    }

    public static void METHOD_BEGIN(String className, String methodName, String desc) {
        if (block.get()) return; else block.set(true);
        intp.METHOD_BEGIN(className, methodName, desc); block.set(false);
    }

    public static void METHOD_THROW() {
        if (block.get()) return; else block.set(true);
        intp.METHOD_THROW(); block.set(false);
    }

    public static void INVOKEMETHOD_EXCEPTION() {
        if (block.get()) return; else block.set(true);
        intp.INVOKEMETHOD_EXCEPTION(); block.set(false);
    }

    public static void INVOKEMETHOD_END() {
        if (block.get()) return; else block.set(true);
        intp.INVOKEMETHOD_END(); block.set(false);
    }

    public static void SPECIAL(int i) {
        if (block.get()) return; else block.set(true);
        intp.SPECIAL(i); block.set(false);
    }

    public static void MAKE_SYMBOLIC() {
        if (block.get()) return; else block.set(true);
        intp.MAKE_SYMBOLIC(); block.set(false);
    }

    public static void flush() {
        intp.flush();
    }
}
