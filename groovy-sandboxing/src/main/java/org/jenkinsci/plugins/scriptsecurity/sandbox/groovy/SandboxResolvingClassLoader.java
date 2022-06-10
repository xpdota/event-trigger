package org.jenkinsci.plugins.scriptsecurity.sandbox.groovy;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import groovy.lang.GroovyShell;
import java.net.URL;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Makes sure that the class references to groovy-sandbox resolves to our own copy of
 * <tt>groovy-sandbox.jar</tt> instead of the random one picked up by the classloader
 * given to {@link GroovyShell} via the constructor.
 * <p>Also tries to cache parent class loading calls, to work around various issues including lack of parallelism.
 * @see <a href="https://issues.jenkins-ci.org/browse/JENKINS-25348">JENKINS-25438</a>
 * @see <a href="https://issues.jenkins-ci.org/browse/JENKINS-23784">JENKINS-23784</a>
 */
class SandboxResolvingClassLoader extends ClassLoader {

    private static final Logger LOGGER = Logger.getLogger(SandboxResolvingClassLoader.class.getName());

    static final LoadingCache<ClassLoader, Cache<String, Class<?>>> parentClassCache = makeParentCache(true);

    static final LoadingCache<ClassLoader, Cache<String, Optional<URL>>> parentResourceCache = makeParentCache(false);

    SandboxResolvingClassLoader(ClassLoader parent) {
        super(parent);
    }

    /**
     * Marker value for a {@link ClassNotFoundException} negative cache hit.
     * Cannot use null, since the cache API does not permit null values.
     * Cannot use {@code Optional<Class<?>>} since weak values would mean this is always collected.
     * This value is non-null, not a legitimate return value
     * (no script should be trying to load this implementation detail), and strongly held.
     */
    static final Class<?> CLASS_NOT_FOUND = Unused.class;
    private static final class Unused {}

    @Override protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (name.startsWith("org.kohsuke.groovy.sandbox.")) {
            return this.getClass().getClassLoader().loadClass(name);
        } else {
            ClassLoader parentLoader = getParent();
            Class<?> c = load(parentClassCache, name, parentLoader, () -> {
                try {
                    return parentLoader.loadClass(name);
                } catch (ClassNotFoundException x) {
                    return CLASS_NOT_FOUND;
                }
            });
            if (c != CLASS_NOT_FOUND) {
                if (resolve) {
                    super.resolveClass(c);
                }
                return c;
            } else {
                throw new ClassNotFoundException(name) {
                    @Override public synchronized Throwable fillInStackTrace() {
                        return this; // super call is too expensive
                    }
                };
            }
        }
    }

    @Override public URL getResource(String name) {
        ClassLoader parentLoader = getParent();
        return load(parentResourceCache, name, parentLoader, () -> Optional.ofNullable(parentLoader.getResource(name))).orElse(null);
    }

    // We cannot have the inner cache be a LoadingCache and just use .get(name), since then the values of the outer cache would strongly refer to the keys.
    private static <T> T load(LoadingCache<ClassLoader, Cache<String, T>> cache, String name, ClassLoader parentLoader, Supplier<T> supplier) {
        Cache<String, T> classCache = cache.get(parentLoader);
        assert classCache != null; // Never null, see makeParentCache, but we need the assertion to convince SpotBugs.
        // itemName is ignored but caffeine requires a function<String, T>
        return classCache.get(name, (String itemName) -> {
            Thread t = Thread.currentThread();
            String origName = t.getName();
            t.setName(origName + " loading " + name);
            long start = System.nanoTime(); // http://stackoverflow.com/q/19052316/12916
            try {
                return supplier.get();
            } finally {
                t.setName(origName);
                long ms = (System.nanoTime() - start) / 1000000;
                if (ms > 1000) {
                    LOGGER.log(Level.INFO, "took {0}ms to load/not load {1} from {2}", new Object[] {ms, name, parentLoader});
                }
            }
        });
    }

    private static <T> LoadingCache<ClassLoader, Cache<String, T>> makeParentCache(boolean weakValuesInnerCache) {
        // The outer cache has weak keys, so that we do not leak class loaders, but strong values, because the
        // inner caches are only referenced by the outer cache internally.
        Caffeine<Object, Object> outerBuilder = Caffeine.newBuilder().recordStats().weakKeys();
        // The inner cache has strong keys, since they are just strings, and expires entries 15 minutes after they are
        // added to the cache, so that classes defined by dynamically installed plugins become available even if there
        // were negative cache hits prior to the installation (ideally this would be done with a listener). The values
        // for the inner cache may be weak if needed, for example parentClassCache uses weak values to avoid leaking
        // classes and their loaders.
        Caffeine<Object, Object> innerBuilder = Caffeine.newBuilder().recordStats().expireAfterWrite(Duration.ofMinutes(15));
        if (weakValuesInnerCache) {
            innerBuilder.weakValues();
        }

        return outerBuilder.build(parentLoader -> innerBuilder.build());
    }
}
