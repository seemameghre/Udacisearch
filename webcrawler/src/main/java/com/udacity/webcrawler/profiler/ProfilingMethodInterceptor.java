package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

  private final Clock clock;
  private final Object target;
  private final ProfilingState profilingState;

  // TODO: You will need to add more instance fields and constructor arguments to this class.
  ProfilingMethodInterceptor(Clock clock, Object target, ProfilingState profilingState) {

    this.clock = Objects.requireNonNull(clock);
    this.target = Objects.requireNonNull(target);
    this.profilingState = Objects.requireNonNull(profilingState);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable{
    // TODO: This method interceptor should inspect the called method to see if it is a profiled
    //       method. For profiled methods, the interceptor should record the start time, then
    //       invoke the method using the object that is being profiled. Finally, for profiled
    //       methods, the interceptor should record how long the method call took, using the
    //       ProfilingState methods.

    Instant startTime = null;
    Duration elapsedTime;
    boolean profiled = false;
    Object returnValue;
    if(method.getAnnotation(Profiled.class) != null){
      profiled = true;
      startTime = clock.instant();
    }
    try{
      if(method.getName().equals("equals")){
        //if equals called on proxy, compare with this
        //else method will be called on target
        if(args[0].getClass().equals(this.getClass())){
          return this.equals(args[0]);
        }
      }
      returnValue = method.invoke(target, args);
    }catch (IllegalAccessException ex){
      throw new RuntimeException(ex);
    }catch (Throwable t){
      throw t.getCause();
    }finally {
      if(profiled) {
        elapsedTime = Duration.between(startTime, clock.instant());
        profilingState.record(target.getClass(),method,elapsedTime);
      }
    }
    return returnValue;
  }

}
