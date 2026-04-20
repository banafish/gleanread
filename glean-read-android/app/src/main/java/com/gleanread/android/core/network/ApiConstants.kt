package com.gleanread.android.core.network

import com.gleanread.android.BuildConfig

/**
 * 集中管理应用内所有的 API 端点
 */
object ApiConstants {
    /**
     * 基础 API URL，由 BuildConfig 注入
     */
    val BASE_URL = BuildConfig.BASE_URL

    /**
     * 极速摘录保存端点
     */
    val CAPTURE_ENDPOINT = "$BASE_URL/api/v1/glean/capture"
}

