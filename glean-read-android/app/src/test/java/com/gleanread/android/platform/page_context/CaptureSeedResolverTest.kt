package com.gleanread.android.platform.page_context

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CaptureSeedResolverTest {
    private lateinit var pageContextStore: PageContextStore
    private lateinit var resolver: CaptureSeedResolver

    @Before
    fun setUp() {
        pageContextStore = PageContextStore(ApplicationProvider.getApplicationContext())
        pageContextStore.clear()
        resolver = CaptureSeedResolver(pageContextStore)
    }

    @After
    fun tearDown() {
        pageContextStore.clear()
    }

    @Test
    fun `resolver uses explicit send extras before cached context`() {
        pageContextStore.save(
            PageContextSnapshot(
                sourcePackage = PageContextSupport.ChromePackage,
                sourceTitle = "缓存标题",
                sourceUrl = "https://cached.example.com",
                capturedAt = 10L,
                captureSource = PageContextSupport.AccessibilityCaptureSource,
                confidence = 0.9f,
            ),
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "显式正文")
            putExtra(Intent.EXTRA_SUBJECT, "显式标题")
        }

        val seed = resolver.resolve(
            intent = intent,
            referrer = Uri.parse("android-app://${PageContextSupport.ChromePackage}"),
            now = 11L,
        )

        assertEquals("显式正文", seed.content)
        assertEquals("显式标题", seed.sourceTitle)
        assertEquals("https://cached.example.com", seed.url)
        assertFalse(seed.usedCachedTitle)
        assertTrue(seed.usedCachedUrl)
    }

    @Test
    fun `resolver backfills missing title and url from recent snapshot`() {
        pageContextStore.save(
            PageContextSnapshot(
                sourcePackage = PageContextSupport.WeChatPackage,
                sourceTitle = "公众号文章标题",
                sourceUrl = "https://mp.weixin.qq.com/s/test",
                capturedAt = 100L,
                captureSource = PageContextSupport.AccessibilityCaptureSource,
                confidence = 0.95f,
            ),
        )

        val intent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
            putExtra(Intent.EXTRA_PROCESS_TEXT, "分享出来的选中文本")
        }

        val seed = resolver.resolve(
            intent = intent,
            referrer = Uri.parse("android-app://${PageContextSupport.WeChatPackage}"),
            now = 150L,
        )

        assertEquals("分享出来的选中文本", seed.content)
        assertEquals("公众号文章标题", seed.sourceTitle)
        assertEquals("https://mp.weixin.qq.com/s/test", seed.url)
        assertTrue(seed.usedCachedTitle)
        assertTrue(seed.usedCachedUrl)
    }

    @Test
    fun `resolver ignores expired snapshot`() {
        pageContextStore.save(
            PageContextSnapshot(
                sourcePackage = PageContextSupport.ChromePackage,
                sourceTitle = "过期标题",
                sourceUrl = "https://expired.example.com",
                capturedAt = 0L,
                captureSource = PageContextSupport.AccessibilityCaptureSource,
                confidence = 0.8f,
            ),
        )

        val intent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
            putExtra(Intent.EXTRA_PROCESS_TEXT, "只剩正文")
        }

        val seed = resolver.resolve(
            intent = intent,
            referrer = Uri.parse("android-app://${PageContextSupport.ChromePackage}"),
            now = PageContextSupport.CacheTtlMillis + 1L,
        )

        assertEquals("只剩正文", seed.content)
        assertEquals("", seed.sourceTitle)
        assertEquals("", seed.url)
        assertFalse(seed.usedCachedTitle)
        assertFalse(seed.usedCachedUrl)
    }

    @Test
    fun `resolver uses referrer extras when activity referrer is missing`() {
        pageContextStore.save(
            PageContextSnapshot(
                sourcePackage = PageContextSupport.ChromePackage,
                sourceTitle = "缓存标题",
                sourceUrl = "https://cached.example.com/page",
                capturedAt = 200L,
                captureSource = PageContextSupport.AccessibilityCaptureSource,
                confidence = 0.9f,
            ),
        )

        val intent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
            putExtra(Intent.EXTRA_PROCESS_TEXT, "选中的文本")
            putExtra(
                Intent.EXTRA_REFERRER_NAME,
                "android-app://${PageContextSupport.ChromePackage}",
            )
        }

        val seed = resolver.resolve(
            intent = intent,
            referrer = null,
            now = 220L,
        )

        assertEquals(PageContextSupport.ChromePackage, seed.sourcePackage)
        assertEquals("缓存标题", seed.sourceTitle)
        assertEquals("https://cached.example.com/page", seed.url)
        assertTrue(seed.usedCachedTitle)
        assertTrue(seed.usedCachedUrl)
    }

    @Test
    fun `resolver ignores generic share subject placeholders`() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "分享出来的正文")
            putExtra(Intent.EXTRA_SUBJECT, "Including link:")
        }

        val seed = resolver.resolve(
            intent = intent,
            referrer = null,
            now = 1L,
        )

        assertEquals("分享出来的正文", seed.content)
        assertEquals("", seed.sourceTitle)
    }

    @Test
    fun `resolver ignores browser sharing text placeholder title`() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "分享出来的正文")
            putExtra(Intent.EXTRA_SUBJECT, "Sharing text")
        }

        val seed = resolver.resolve(
            intent = intent,
            referrer = null,
            now = 1L,
        )

        assertEquals("分享出来的正文", seed.content)
        assertEquals("", seed.sourceTitle)
    }
}
