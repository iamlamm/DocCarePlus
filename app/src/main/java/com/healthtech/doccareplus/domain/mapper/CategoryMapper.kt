package com.healthtech.doccareplus.domain.mapper

import com.healthtech.doccareplus.data.local.entity.CategoryEntity
import com.healthtech.doccareplus.domain.model.Category

fun CategoryEntity.toCategory(): Category {
    return Category(
        id = id, code = code, name = name, icon = icon, description = description
    )
}

fun Category.toCategoryEntity(): CategoryEntity {
    return CategoryEntity(
        id = id, code = code, name = name, icon = icon, description = description
    )
}