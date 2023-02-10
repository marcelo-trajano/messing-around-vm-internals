/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.vm.annotation;

import java.lang.annotation.*;

/**
 * A method or constructor must be annotated as "changes current
 * thread" if it calls Thread.setCurrentThread. This annotation also
 * disables inlining for the method to which it is applied unless the
 * method being inlined into is also annotated ChangesCurrentThread.

 * @implNote
 * This annotation only takes effect for methods or constructors of classes
 * loaded by the boot loader.  Annotations on methods or constructors of classes
 * loaded outside of the boot loader are ignored.
 */
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
public @interface ChangesCurrentThread {
}
