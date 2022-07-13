# javers-test - Custom String Comparator - NullAsBlankStringComparator 

## The `model`

````
@Entity  // javers
public class Probe {

    @Id // javers
    private int id;
    private String name;
    // here attributes value can be null
    private Map<String, String> attributes;
}
````

## Ways to init javers

````
    // smart strings comparison
    public static Javers initWithSmartStringComparison() {
        return JaversBuilder.javers()
                .registerValue(String.class, new NullAsBlankStringComparator())
                .withListCompareAlgorithm(LEVENSHTEIN_DISTANCE)
                .build();
    }

    
    public static Javers initStandard() {
        return JaversBuilder.javers().withListCompareAlgorithm(LEVENSHTEIN_DISTANCE).build();
    }
````

## Test data input

````
    private static Probe getProbe2() {

        final var p = new Probe();
        p.setId(1);
        p.setName("Probe");

        final Map<String, String> attributes = new HashMap<>();
        attributes.put("probe-key", "");
        p.setAttributes(attributes);

        return p;
    }

    private static Probe getProbe1() {

        final var p = new Probe();
        p.setId(1);
        p.setName("Probe");

        final Map<String, String> attributes = new HashMap<>();
        attributes.put("probe-key", null);
        p.setAttributes(attributes);

        return p;
    }
````

## Test results

###  Test 1 - Error
````
        try {
            final Diff compare = initWithSmartStringComparison().compare(probe1, probe2);
            System.out.println(compare.prettyPrint());
        } catch (Exception e) {
            e.printStackTrace();
        }
------------------------------------------------------------------------------------------
       java.lang.NullPointerException
	at java.base/java.util.Objects.requireNonNull(Objects.java:208)
	at java.base/java.util.stream.Collectors.lambda$uniqKeysMapAccumulator$1(Collectors.java:180)
	at java.base/java.util.stream.ReduceOps$3ReducingSink.accept(ReduceOps.java:169)
	at java.base/java.util.Collections$UnmodifiableMap$UnmodifiableEntrySet.lambda$entryConsumer$0(Collections.java:1625)
	at java.base/java.util.HashMap$EntrySpliterator.forEachRemaining(HashMap.java:1850)
	at java.base/java.util.Collections$UnmodifiableMap$UnmodifiableEntrySet$UnmodifiableEntrySetSpliterator.forEachRemaining(Collections.java:1650)
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509)
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499)
	at java.base/java.util.stream.ReduceOps$ReduceOp.evaluateSequential(ReduceOps.java:921)
	at java.base/java.util.stream.AbstractPipeline.evaluate(AbstractPipeline.java:234)
	at java.base/java.util.stream.ReferencePipeline.collect(ReferencePipeline.java:682)
	at org.javers.core.diff.appenders.HashWrapper.wrapKeysIfNeeded(HashWrapper.java:54)
	at org.javers.core.diff.appenders.MapChangeAppender.wrapKeysIfNeeded(MapChangeAppender.java:57)
	at org.javers.core.diff.appenders.MapChangeAppender.calculateChanges(MapChangeAppender.java:39)
	at org.javers.core.diff.appenders.MapChangeAppender.calculateChanges(MapChangeAppender.java:19)
	at org.javers.core.diff.DiffFactory.appendChanges(DiffFactory.java:161)
	at org.javers.core.diff.DiffFactory.appendPropertyChanges(DiffFactory.java:151)
	at org.javers.core.diff.DiffFactory.createAndAppendChanges(DiffFactory.java:133)
	at org.javers.core.diff.DiffFactory.create(DiffFactory.java:70)
	at org.javers.core.diff.DiffFactory.compare(DiffFactory.java:55)
	at org.javers.core.JaversCore.compare(JaversCore.java:176)
	at com.xxx.Main.main(Main.java:27)
````
### Test 2 - Works
````
final Diff compare = initStandard().compare(probe1, probe2);

------------------------------------------------------------
Diff:
* changes on com.xxx.Main$Probe/1 :
  - 'attributes' map changes :
     · entry ['probe-key' : ''] -> ['probe-key' : '']
````

### The problem

````
    public static Map wrapKeysIfNeeded(Map map, JaversType keyType) {
        if (hasCustomValueComparator(keyType)) {   // -> yes it has 
            CustomComparableType customType = (CustomComparableType) keyType;
            return (Map)map.entrySet().stream().collect(Collectors.toMap(
                    e -> new HashWrapper(((Map.Entry)e).getKey(), keyType::equals, customType::valueToString),
                    e -> ((Map.Entry)e).getValue())); // -> cause of NPE since the value mapper will return the value as being null
        }
        return map;
    }
````

#### Patch with Byte-Buddy
* Provide in a new class the patched version of that method:
````
    public static Map wrapKeysIfNeeded(Map map, JaversType keyType) {
        if (hasCustomValueComparator(keyType)) {
            CustomComparableType customType = (CustomComparableType) keyType;

            Map res = new HashMap();
            map.entrySet().forEach(e -> {
                res.put(new HashWrapperPatched(((Map.Entry)e).getKey(), keyType::equals, customType::valueToString), ((Map.Entry<?, ?>) e).getValue());
            });
           return res;

        //   OLD CODE
        //            return (Map)map.entrySet().stream().collect(Collectors.toMap(
        //                    e -> new HashWrapperPatched(((Map.Entry)e).getKey(), keyType::equals, customType::valueToString),
        //                    e -> ((Map.Entry)e).getValue()));
        }
        return map;
    }
````
* Injecting with Byte-Buddy
````
    private static void medkit() {
        ByteBuddyAgent.install();

        new ByteBuddy().redefine(HashWrapper.class)
                .method(named("wrapKeysIfNeeded"))
                .intercept(MethodDelegation.to(HashWrapperPatched.class))
                .make()
                .load(HashWrapper.class.getClassLoader(), ClassReloadingStrategy.fromInstalledAgent());
    }
````
#### The open JDK bug

* [JDK-8261865-Unexpected NullPointerException from Collectors.toMap](https://bugs.openjdk.org/browse/JDK-8261865)
* Tested with: `EclipseAdoptium.Temurin.17, Release version: 17.0.3.7`