package code.yousef.dari.shared.data.repository.mappers

import code.yousef.dari.shared.domain.models.*
import kotlinx.datetime.Instant
import kotlinx.datetime.Clock

/**
 * Mapping functions for Category domain and database models
 * Extracted to reduce duplication and improve maintainability
 */

/**
 * Converts database Category entity to domain Category model
 */
fun code.yousef.dari.shared.database.Category.toDomainModel(): Category {
    return Category(
        categoryId = categoryId,
        name = name,
        description = description,
        categoryType = CategoryType.valueOf(categoryType),
        parentCategoryId = parentCategoryId,
        iconName = iconName,
        colorHex = colorHex,
        keywords = keywords?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
        merchantPatterns = merchantPatterns?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
        isActive = isActive == 1L,
        isSystemCategory = isSystemCategory == 1L,
        sortOrder = sortOrder?.toInt() ?: 0,
        level = level?.toInt() ?: 0,
        createdAt = Instant.fromEpochSeconds(createdAt),
        updatedAt = Instant.fromEpochSeconds(updatedAt),
        metadata = deserializeMetadata(metadata ?: "{}")
    )
}

/**
 * Converts database CategorizationRule entity to domain CategorizationRule model
 */
fun code.yousef.dari.shared.database.CategorizationRule.toDomainModel(): CategorizationRule {
    return CategorizationRule(
        ruleId = ruleId,
        categoryId = categoryId,
        name = name,
        description = description,
        ruleType = RuleType.valueOf(ruleType),
        conditions = deserializeConditions(conditions),
        priority = priority.toInt(),
        isActive = isActive == 1L,
        createdAt = Instant.fromEpochSeconds(createdAt),
        updatedAt = Instant.fromEpochSeconds(updatedAt)
    )
}

/**
 * Serializes metadata map to JSON string for database storage
 */
fun serializeMetadata(metadata: Map<String, String>): String {
    return if (metadata.isEmpty()) {
        "{}"
    } else {
        metadata.entries.joinToString(
            prefix = "{",
            postfix = "}",
            separator = ","
        ) { "\"${it.key}\":\"${it.value}\"" }
    }
}

/**
 * Deserializes JSON string to metadata map
 */
fun deserializeMetadata(json: String): Map<String, String> {
    if (json == "{}") return emptyMap()
    
    return try {
        json.removeSurrounding("{", "}")
            .split(",")
            .associate { pair ->
                val (key, value) = pair.split(":")
                key.removeSurrounding("\"") to value.removeSurrounding("\"")
            }
    } catch (e: Exception) {
        emptyMap()
    }
}

/**
 * Serializes rule conditions list to string for database storage
 */
fun serializeConditions(conditions: List<RuleCondition>): String {
    return conditions.joinToString("|") { condition ->
        "${condition.type.name}:${condition.operator.name}:${condition.value}"
    }
}

/**
 * Deserializes string to rule conditions list
 */
fun deserializeConditions(serialized: String): List<RuleCondition> {
    if (serialized.isEmpty()) return emptyList()
    
    return serialized.split("|").mapNotNull { condition ->
        try {
            val parts = condition.split(":")
            if (parts.size >= 3) {
                RuleCondition(
                    type = RuleConditionType.valueOf(parts[0]),
                    operator = RuleOperator.valueOf(parts[1]),
                    value = parts.drop(2).joinToString(":") // Rejoin in case value contains ":"
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }
}