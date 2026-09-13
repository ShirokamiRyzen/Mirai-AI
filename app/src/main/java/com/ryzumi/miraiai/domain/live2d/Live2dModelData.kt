package com.ryzumi.miraiai.domain.live2d

data class Live2dModelFile(
    val name: String,
    val relativePath: String,
    val isDefault: Boolean = false
)

data class Live2dExpressionInfo(
    val id: String,
    val name: String,
    val isPreset: Boolean = false
)

data class Live2dPartInfo(
    val id: String,
    val name: String,
    val opacity: Float = 1f
)

data class Live2dParamInfo(
    val id: String,
    val name: String,
    val min: Float,
    val max: Float,
    val defaultValue: Float,
    val currentValue: Float
)

data class Live2dMotionInfo(
    val group: String,
    val index: Int,
    val name: String
)

data class Live2dModelCapabilities(
    val models: List<Live2dModelFile> = emptyList(),
    val expressions: List<Live2dExpressionInfo> = emptyList(),
    val parts: List<Live2dPartInfo> = emptyList(),
    val parameters: List<Live2dParamInfo> = emptyList(),
    val motions: List<Live2dMotionInfo> = emptyList()
)
