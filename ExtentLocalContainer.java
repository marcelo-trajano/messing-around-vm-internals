/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
package jdk.internal.vm;

import java.util.concurrent.Callable;
import jdk.internal.access.JavaLangAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.misc.StructureViolationExceptions;
import jdk.internal.misc.Unsafe;
import jdk.internal.vm.annotation.DontInline;
import jdk.internal.vm.annotation.ReservedStackAccess;

/**
 * A StackableScope to represent extent-local bindings.
 *
 * This class defines static methods to run an operation with a ExtentLocalContainer
 * on the scope stack. It also defines a method to get the latest ExtentLocalContainer
 * and a method to return a snapshot of the extent local bindings.
 */
public class ExtentLocalContainer extends StackableScope {
    private static final JavaLangAccess JLA = SharedSecrets.getJavaLangAccess();
    static {
        Unsafe.getUnsafe().ensureClassInitialized(StructureViolationExceptions.class);
    }

    private ExtentLocalContainer() {
    }

    /**
     * Returns the "latest" ExtentLocalContainer for the current Thread. This may be on
     * the current thread's scope task or ma require walking up the tree to find it.
     */
    public static <T extends ExtentLocalContainer> T latest(Class<T> containerClass) {
        StackableScope scope = head();
        if (scope == null) {
            scope = JLA.threadContainer(Thread.currentThread());
            if (scope == null || scope.owner() == null)
                return null;
        }
        if (containerClass.isInstance(scope)) {
            @SuppressWarnings("unchecked")
            T tmp = (T) scope;
            return tmp;
        } else {
            return scope.enclosingScope(containerClass);
        }
    }

    /**
     * Returns the "latest" ExtentLocalContainer for the current Thread. This
     * may be on the current thread's scope task or may require walking up the
     * tree to find it.
     */
    public static ExtentLocalContainer latest() {
        return latest(ExtentLocalContainer.class);
    }

    /**
     * A snapshot of the extent local bindings. The snapshot includes the bindings
     * established for the current thread and extent local container.
     */
    public record BindingsSnapshot(Object extentLocalBindings,
                                   ExtentLocalContainer container) { }

    /**
     * Returns the extent local bindings for the current thread.
     */
    public static BindingsSnapshot captureBindings() {
        return new BindingsSnapshot(JLA.extentLocalBindings(), latest());
    }

    /**
     * For use by ExtentLocal to run an operation in a structured context.
     */
    public static void run(Runnable op) {
        if (head() == null) {
            // no need to push scope when stack is empty
            runWithoutScope(op);
        } else {
            new ExtentLocalContainer().doRun(op);
        }
    }

    /**
     * Run an operation without a scope on the stack.
     */
    private static void runWithoutScope(Runnable op) {
        assert head() == null;
        Throwable ex;
        boolean atTop;
        try {
            op.run();
            ex = null;
        } catch (Throwable e) {
            ex = e;
        } finally {
            atTop = (head() == null);
            if (!atTop) popAll();   // may block
        }
        throwIfFailed(ex, atTop);
    }

    /**
     * Run an operation with this scope on the stack.
     */
    private void doRun(Runnable op) {
        Throwable ex;
        boolean atTop;
        push();
        try {
            op.run();
            ex = null;
        } catch (Throwable e) {
            ex = e;
        } finally {
            atTop = popForcefully();  // may block
        }
        throwIfFailed(ex, atTop);
    }

    /**
     * For use by ExtentLocal to call a value returning operation in a structured context.
     */
    public static <V> V call(Callable<V> op) throws Exception {
        if (head() == null) {
            // no need to push scope when stack is empty
            return callWithoutScope(op);
        } else {
            return new ExtentLocalContainer().doCall(op);
        }
    }

    /**
     * Call an operation without a scope on the stack.
     */
    private static <V> V callWithoutScope(Callable<V> op) {
        assert head() == null;
        Throwable ex;
        boolean atTop;
        V result;
        try {
            result = op.call();
            ex = null;
        } catch (Throwable e) {
            result = null;
            ex = e;
        } finally {
            atTop = (head() == null);
            if (!atTop) popAll();  // may block
        }
        throwIfFailed(ex, atTop);
        return result;
    }

    /**
     * Call an operation with this scope on the stack.
     */
    private <V> V doCall(Callable<V> op) {
        Throwable ex;
        boolean atTop;
        V result;
        push();
        try {
            result = op.call();
            ex = null;
        } catch (Throwable e) {
            result = null;
            ex = e;
        } finally {
            atTop = popForcefully();  // may block
        }
        throwIfFailed(ex, atTop);
        return result;
    }

    /**
     * Throws {@code ex} if not null. StructureViolationException is thrown or added
     * as a suppressed exception when {@code atTop} is false.
     */
    @DontInline @ReservedStackAccess
    private static void throwIfFailed(Throwable ex, boolean atTop) {
        if (ex != null || !atTop) {
            if (!atTop) {
                var sve = StructureViolationExceptions.newException();
                if (ex == null) {
                    ex = sve;
                } else {
                    ex.addSuppressed(sve);
                }
            }
            Unsafe.getUnsafe().throwException(ex);
        }
    }
}
