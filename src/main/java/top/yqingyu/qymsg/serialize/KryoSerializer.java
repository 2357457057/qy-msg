package top.yqingyu.qymsg.serialize;

import org.objenesis.instantiator.ObjectInstantiator;
import org.objenesis.strategy.StdInstantiatorStrategy;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.DefaultSerializers;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import com.esotericsoftware.kryo.util.Pool;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.esotericsoftware.kryo.util.Util.className;

public class KryoSerializer {
    public static final KryoSerializer INSTANCE = new KryoSerializer();

    private static final class SimpleInstantiatorStrategy implements org.objenesis.strategy.InstantiatorStrategy {

        private final StdInstantiatorStrategy ss = new StdInstantiatorStrategy();

        @Override
        public <T> ObjectInstantiator newInstantiatorOf(Class<T> type) {
            // Reflection.
            try {
                Constructor ctor;
                try {
                    ctor = type.getConstructor((Class[]) null);
                } catch (Exception ex) {
                    ctor = type.getDeclaredConstructor((Class[]) null);
                    ctor.setAccessible(true);
                }
                final Constructor constructor = ctor;
                return () -> {
                    try {
                        return constructor.newInstance();
                    } catch (Exception ex) {
                        throw new KryoException("Error constructing instance of class: " + className(type), ex);
                    }
                };
            } catch (Exception ignored) {
            }

            return ss.newInstantiatorOf(type);
        }
    }

    private final Pool<Kryo> kryoPool;
    private final Pool<Input> inputPool;
    private final Pool<Output> outputPool;
    private final boolean registrationRequired;

    public KryoSerializer() {
        this(null, false);
    }

    public KryoSerializer(boolean registrationRequired) {
        this(null, registrationRequired);
    }

    public KryoSerializer(ClassLoader classLoader, KryoSerializer codec) {
        this(classLoader, codec.registrationRequired);
    }

    public KryoSerializer(ClassLoader classLoader) {
        this(null, false);
    }

    public KryoSerializer(ClassLoader classLoader, boolean registrationRequired) {
        this.registrationRequired = registrationRequired;
        this.kryoPool = new Pool<Kryo>(true, false, 1024) {
            @Override
            protected Kryo create() {
                return createKryo(classLoader);
            }
        };

        this.inputPool = new Pool<Input>(true, false, 512) {
            @Override
            protected Input create() {
                return new Input(8192);
            }
        };

        this.outputPool = new Pool<Output>(true, false, 512) {
            @Override
            protected Output create() {
                return new Output(8192, -1);
            }
        };
    }

    protected Kryo createKryo(ClassLoader classLoader) {
        Kryo kryo = new Kryo();
        if (classLoader != null) {
            kryo.setClassLoader(classLoader);
        }
        kryo.setInstantiatorStrategy(new SimpleInstantiatorStrategy());
        kryo.setRegistrationRequired(registrationRequired);
        kryo.setReferences(false);
        kryo.addDefaultSerializer(Throwable.class, new JavaSerializer());
        kryo.addDefaultSerializer(UUID.class, new DefaultSerializers.UUIDSerializer());
        kryo.addDefaultSerializer(URI.class, new DefaultSerializers.URISerializer());
        kryo.addDefaultSerializer(Pattern.class, new DefaultSerializers.PatternSerializer());
        return kryo;
    }

    @SuppressWarnings("unchecked")
    public <T> T decode(byte[] in) {
        Kryo kryo = kryoPool.obtain();
        Input input = inputPool.obtain();
        try {
            input.setBuffer(in);
            return (T) kryo.readClassAndObject(input);
        } finally {
            kryoPool.free(kryo);
        }
    }

    public <T> byte[] encode(T in) {
        Kryo kryo = kryoPool.obtain();
        try {
            Output output = outputPool.obtain();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            output.setOutputStream(outputStream);
            kryo.writeClassAndObject(output, in);
            return outputStream.toByteArray();
        } finally {
            kryoPool.free(kryo);
        }
    }
}
