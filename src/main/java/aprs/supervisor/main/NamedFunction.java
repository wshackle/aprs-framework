/*
 * This software is public domain software, however it is preferred
 * that the following disclaimers be attached.
 * Software Copyright/Warranty Disclaimer
 * 
 * This software was developed at the National Institute of Standards and
 * Technology by employees of the Federal Government in the course of their
 * official duties. Pursuant to title 17 Section 105 of the United States
 * Code this software is not subject to copyright protection and is in the
 * public domain.
 * 
 * This software is experimental. NIST assumes no responsibility whatsoever 
 * for its use by other parties, and makes no guarantees, expressed or 
 * implied, about its quality, reliability, or any other characteristic. 
 * We would appreciate acknowledgement if the software is used. 
 * This software can be redistributed and/or modified freely provided 
 * that any derivative works bear some notice that they are derived from it, 
 * and any modified versions bear some notice that they have been modified.
 * 
 *  See http://www.copyright.gov/title17/92chap1.html#105
 * 
 */
package aprs.supervisor.main;

import aprs.system.AprsSystem;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class NamedFunction<T,R> implements Function<T,R> {

    private final Function<T,R> function;
    private final String name;
    private final AprsSystem[] systems;

    public String getName() {
        return name;
    }

    public AprsSystem[] getSystems() {
        return systems;
    }

    public NamedFunction(Function<T,R> f, String name, AprsSystem... systems) {
        this.function = f;
        this.name = name;
        this.systems = systems;
        assert (f != null) : "NamedRunnable: Runnable r == null";
    }

    @Override
    public String toString() {
        return "NamedRunnable{" + "r=" + function + ", name=" + name + '}';
    }

    @Override
    @SuppressWarnings("nullness")
    public R apply(@Nullable T arg) {
        return function.apply(arg);
    }
}
