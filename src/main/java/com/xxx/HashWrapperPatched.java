package com.xxx;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.javers.common.validation.Validate;
import org.javers.core.metamodel.type.CustomComparableType;
import org.javers.core.metamodel.type.JaversType;

public class HashWrapperPatched {
    private final Object target;
    private final BiFunction<Object, Object, Boolean> equalsFunction;
    private final Function<Object, String> toStringFunction;

    public HashWrapperPatched(Object target, BiFunction<Object, Object, Boolean> equalsFunction, Function<Object, String> toStringFunction) {
        Validate.argumentIsNotNull(equalsFunction);
        Validate.argumentIsNotNull(toStringFunction);
        this.target = target;
        this.equalsFunction = equalsFunction;
        this.toStringFunction = toStringFunction;
    }

    @Override
    public boolean equals(Object that) {
        return equalsFunction.apply(target, ((HashWrapperPatched)that).target);
    }

    @Override
    public int hashCode() {
        return toStringFunction.apply(target).hashCode();
    }

    public Object unwrap() {
        return target;
    }

    public static Set wrapValuesIfNeeded(Set set, JaversType itemType) {
        if (hasCustomValueComparator(itemType)) {
            CustomComparableType customType = (CustomComparableType) itemType;
            return (Set)set.stream()
                    .map(it -> new HashWrapperPatched(it, itemType::equals, customType::valueToString))
                    .collect(Collectors.toSet());
        }
        return set;
    }

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

    private static boolean hasCustomValueComparator(JaversType javersType) {
        return (javersType instanceof CustomComparableType &&
                ((CustomComparableType) javersType).hasCustomValueComparator());
    }
}
