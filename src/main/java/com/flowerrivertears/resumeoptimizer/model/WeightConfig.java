package com.flowerrivertears.resumeoptimizer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeightConfig {
    private boolean enabled;
    private Map<String, Double> dimensions;

    public static final String DIM_SKILL_MATCH = "skill-match";
    public static final String DIM_SEMANTIC_SIMILARITY = "semantic-similarity";
    public static final String DIM_CATEGORY_RELEVANCE = "category-relevance";
    public static final String DIM_EXPERIENCE_LEVEL = "experience-level";

    public double getWeight(String dimension) {
        if (dimensions == null) return 0.25;
        return dimensions.getOrDefault(dimension, 0.25);
    }

    public void setWeight(String dimension, double weight) {
        if (dimensions != null) {
            dimensions.put(dimension, weight);
        }
    }

    public void normalize() {
        if (dimensions == null || dimensions.isEmpty()) return;
        double sum = dimensions.values().stream().mapToDouble(Double::doubleValue).sum();
        if (sum > 0) {
            dimensions.replaceAll((k, v) -> v / sum);
        }
    }
}
