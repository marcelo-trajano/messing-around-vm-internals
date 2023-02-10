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

import java.util.stream.Stream;

/**
 * A container of threads.
 */
public abstract class ThreadContainer extends StackableScope {

    /**
     * Creates a ThreadContainer.
     * @param shared true for a shared container, false for a container
     * owned by the current thread
     */
    protected ThreadContainer(boolean shared) {
        super(shared);
    }

    /**
     * Returns the parent of this container or null if this is the root container.
     */
    public ThreadContainer parent() {
        return ThreadContainers.parent(this);
    }

    /**
     * Return the stream of children of this container.
     */
    public final Stream<ThreadContainer> children() {
        return ThreadContainers.children(this);
    }

    /**
     * Return a count of the number of threads in this container.
     */
    public long threadCount() {
        return threads().mapToLong(e -> 1L).sum();
    }

    /**
     * Returns a stream of the live threads in this container.
     */
    public abstract Stream<Thread> threads();

    /**
     * Invoked by Thread::start before the given Thread is started.
     */
    public void onStart(Thread thread) {
        // do nothing
    }

    /**
     * Invoked when a Thread terminates or starting it fails.
     *
     * For a platform thread, this method is invoked by the thread itself when it
     * terminates. For a virtual thread, this method is invoked on its carrier
     * after the virtual thread has terminated.
     *
     * If starting the Thread failed then this method is invoked on the thread
     * that invoked onStart.
     */
    public void onExit(Thread thread) {
        // do nothing
    }

    /**
     * The extent locals captured when the thread container was created.
     */
    public ExtentLocalContainer.BindingsSnapshot extentLocalBindings() {
        return null;
    }
}
