package com.flowerrivertears.resumeoptimizer.service;

import com.flowerrivertears.resumeoptimizer.model.SearchResult;
import com.flowerrivertears.resumeoptimizer.model.WeightConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@ConfigurationProperties(prefix = "ai.weight")
public class WeightedRetrievalService {

    private static final Logger log = LoggerFactory.getLogger(WeightedRetrievalService.class);

    private boolean enabled = true;
    private Map<String, Double> dimensions = new LinkedHashMap<>();

    private final Map<String, WeightConfig> customConfigs = new ConcurrentHashMap<>();

    public WeightedRetrievalService() {
        dimensions.put(WeightConfig.DIM_SKILL_MATCH, 0.35);
        dimensions.put(WeightConfig.DIM_SEMANTIC_SIMILARITY, 0.30);
        dimensions.put(WeightConfig.DIM_CATEGORY_RELEVANCE, 0.20);
        dimensions.put(WeightConfig.DIM_EXPERIENCE_LEVEL, 0.15);
    }

    public List<SearchResult> applyWeights(List<SearchResult> results,
                                            String query,
                                            Set<String> matchedSkills,
                                            Set<String> jobSkills) {
        if (!enabled) {
            log.debug("Weight scoring disabled, returning original results");
            return results;
        }

        log.info("Applying weighted scoring to {} results for query: '{}'", results.size(), query);

        List<SearchResult> weightedResults = new ArrayList<>();
        for (SearchResult result : results) {
            double skillMatchScore = calculateSkillMatchScore(result, matchedSkills, jobSkills);
            double semanticScore = result.getScore();
            double categoryScore = calculateCategoryScore(result, query);
            double experienceScore = calculateExperienceScore(result);

            double weightedScore =
                    skillMatchScore * getDimensionWeight(WeightConfig.DIM_SKILL_MATCH) +
                    semanticScore * getDimensionWeight(WeightConfig.DIM_SEMANTIC_SIMILARITY) +
                    categoryScore * getDimensionWeight(WeightConfig.DIM_CATEGORY_RELEVANCE) +
                    experienceScore * getDimensionWeight(WeightConfig.DIM_EXPERIENCE_LEVEL);

            weightedResults.add(SearchResult.builder()
                    .content(result.getContent())
                    .score(result.getScore())
                    .source(result.getSource())
                    .category(result.getCategory())
                    .weightedScore(Math.round(weightedScore * 10000.0) / 10000.0)
                    .build());
        }

        weightedResults.sort((a, b) -> Double.compare(b.getWeightedScore(), a.getWeightedScore()));

        log.info("Weighted scoring complete. Top result score: {}",
                weightedResults.isEmpty() ? 0 : weightedResults.get(0).getWeightedScore());
        return weightedResults;
    }

    private double calculateSkillMatchScore(SearchResult result, Set<String> matchedSkills, Set<String> jobSkills) {
        if (matchedSkills == null || matchedSkills.isEmpty() || jobSkills == null || jobSkills.isEmpty()) {
            return 0.5;
        }

        String content = result.getContent().toLowerCase();
        long matchedInResult = matchedSkills.stream()
                .filter(skill -> content.contains(skill.toLowerCase()))
                .count();

        long totalInResult = jobSkills.stream()
                .filter(skill -> content.contains(skill.toLowerCase()))
                .count();

        if (totalInResult == 0) return 0.3;
        return Math.min(1.0, (double) matchedInResult / Math.max(1, totalInResult));
    }

    private double calculateCategoryScore(SearchResult result, String query) {
        String category = result.getCategory();
        if (category == null) return 0.5;

        String lowerQuery = query.toLowerCase();
        return switch (category) {
            case "resume" -> lowerQuery.contains("简历") || lowerQuery.contains("resume") ? 0.9 : 0.5;
            case "job" -> lowerQuery.contains("职位") || lowerQuery.contains("job") || lowerQuery.contains("岗位") ? 0.9 : 0.5;
            case "skill" -> lowerQuery.contains("技能") || lowerQuery.contains("skill") ? 0.9 : 0.5;
            default -> 0.5;
        };
    }

    private double calculateExperienceScore(SearchResult result) {
        String content = result.getContent().toLowerCase();
        int experienceIndicators = 0;

        if (content.contains("年经验") || content.contains("years experience")) experienceIndicators++;
        if (content.contains("高级") || content.contains("senior")) experienceIndicators++;
        if (content.contains("架构") || content.contains("architect")) experienceIndicators++;
        if (content.contains("负责") || content.contains("lead")) experienceIndicators++;
        if (content.contains("主导") || content.contains("managed")) experienceIndicators++;

        return Math.min(1.0, experienceIndicators * 0.2 + 0.3);
    }

    public WeightConfig getCurrentConfig() {
        return WeightConfig.builder()
                .enabled(enabled)
                .dimensions(new LinkedHashMap<>(dimensions))
                .build();
    }

    public WeightConfig updateWeights(Map<String, Double> newDimensions) {
        if (newDimensions != null) {
            dimensions.putAll(newDimensions);
            WeightConfig config = getCurrentConfig();
            config.normalize();
            dimensions.putAll(config.getDimensions());
        }
        return getCurrentConfig();
    }

    public void saveCustomConfig(String name, WeightConfig config) {
        config.normalize();
        customConfigs.put(name, config);
        log.info("Saved custom weight config: {}", name);
    }

    public WeightConfig getCustomConfig(String name) {
        return customConfigs.get(name);
    }

    public Map<String, WeightConfig> getAllCustomConfigs() {
        return new HashMap<>(customConfigs);
    }

    public void deleteCustomConfig(String name) {
        customConfigs.remove(name);
        log.info("Deleted custom weight config: {}", name);
    }

    private double getDimensionWeight(String dimension) {
        return dimensions.getOrDefault(dimension, 0.25);
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Map<String, Double> getDimensions() { return dimensions; }
    public void setDimensions(Map<String, Double> dimensions) { this.dimensions = dimensions; }
}
