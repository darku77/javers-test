package com.xxx;

import static org.javers.core.diff.ListCompareAlgorithm.LEVENSHTEIN_DISTANCE;

import java.util.HashMap;
import java.util.Map;

import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.javers.core.diff.custom.NullAsBlankStringComparator;
import org.javers.core.metamodel.annotation.Entity;
import org.javers.core.metamodel.annotation.Id;

/**
 * @author catalin
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("START");

        final var probe1 = getProbe1();
        final var probe2 = getProbe2();

        try {
            final Diff compare = initWithSmartStringComparison().compare(probe1, probe2);
            System.out.println(compare.prettyPrint());
        } catch (Exception e) {
            e.printStackTrace();
        }

        final Diff compare = initStandard().compare(probe1, probe2);
        System.out.println(compare.prettyPrint());

    }

    public static Javers initWithSmartStringComparison() {
        return JaversBuilder.javers()
                .registerValue(String.class, new NullAsBlankStringComparator())
                .withListCompareAlgorithm(LEVENSHTEIN_DISTANCE)
                .build();
    }

    public static Javers initStandard() {
        return JaversBuilder.javers().withListCompareAlgorithm(LEVENSHTEIN_DISTANCE).build();
    }

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

    /**
     * @author Catalin on 13.07.2022
     */
    @Entity
    public static class Probe {

        @Id
        private int id;
        private String name;
        private Map<String, String> attributes;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public Map<String, String> getAttributes() {
            return attributes;
        }

        public void setAttributes(Map<String, String> attributes) {
            this.attributes = attributes;
        }
    }
}