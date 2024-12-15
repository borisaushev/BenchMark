package backend.academy;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(value = 1)
@Warmup(iterations = 1, timeUnit = TimeUnit.SECONDS, time = 10)
@Measurement(iterations = 5, timeUnit = TimeUnit.SECONDS, time = 20)
public class AccessBenchmark {
    private Student student;
    private Method method;
    private MethodHandle methodHandle;
    private NameInterface nameInterface;

    @Setup(Level.Trial)
    public void setup() throws Throwable {
        student = new Student("Boris", "Genius");

        // Reflection setup
        String methodName = "name";
        method = Student.class.getMethod(methodName);

        // Method Handle setup
        //lookup context
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        // method type for Student name()
        MethodType nameMethodType = MethodType.methodType(String.class);
        // method handle for Student name()
        methodHandle = lookup.findVirtual(Student.class, methodName, nameMethodType);

        // Lambda Meta-Factory setup
        // Method type for produced lambda
        MethodType invokedType = MethodType.methodType(NameInterface.class);
        // method type for NameInterface getName()
        MethodType targetMethodType = MethodType.methodType(String.class, Student.class);
        CallSite callSite = LambdaMetafactory.metafactory(
            lookup,
            "getName", // Method of NameInterface
            invokedType,
            targetMethodType,
            methodHandle,
            targetMethodType
        );
        nameInterface = (NameInterface) callSite.getTarget().invokeExact();
    }

    @Benchmark
    public void directAccess(Blackhole bh) {
        String name = student.name();
        bh.consume(name);
    }

    @Benchmark
    public void reflection(Blackhole bh) throws InvocationTargetException, IllegalAccessException {
        String name = (String) method.invoke(student);
        bh.consume(name);
    }

    @Benchmark
    public void methodHandlers(Blackhole bh) throws Throwable {
        String name = (String) methodHandle.invokeExact(student);
        bh.consume(name);
    }

    @Benchmark
    public void lambdaMetaFactory(Blackhole bh) {
        String name = nameInterface.getName(student);
        bh.consume(name);
    }

    /**
     * Inner record serving as a test subject
     */
    record Student(String name, String surname) {
    }

    /**
     * Functional interface to access method name
     */
    @FunctionalInterface
    interface NameInterface {
        String getName(Student student);
    }
}
