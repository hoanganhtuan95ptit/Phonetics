package com.simple.phonetics

import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.simple.phonetics.ui.main.MainActivity
import org.hamcrest.Matchers.allOf
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI Test cho tính năng chọn ngôn ngữ IPA.
 */
@RunWith(AndroidJUnit4::class)
class LanguageSelectionTest : BaseUiTest() {

    /**
     * Test case: Chọn ngôn ngữ IPA khi lần đầu vào app.
     */
    @Test
    fun testInitialLanguageSelection() {
        ActivityScenario.launch(MainActivity::class.java)

        // 1. Đợi RecyclerView hiển thị và load xong
        waitForDisplayed(R.id.recycler_view)
        waitForLanguageLoaded()

        // 2. Chọn ngôn ngữ đầu tiên
        onView(withId(R.id.recycler_view))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        // 3. Đợi nút xác nhận clickable và nhấn nút
        waitForClickable(R.id.frame_confirm)
        onView(withId(R.id.frame_confirm)).perform(click())

        // 4. Đợi vào màn hình Home
        waitForDisplayed(R.id.et_text, timeout = 60000)
    }

    /**
     * Test case: Chọn lại ngôn ngữ IPA ở màn hình Home.
     */
    @Test
    fun testReSelectLanguageFromHome() {
        ActivityScenario.launch(MainActivity::class.java)

        // --- Bước chuẩn bị: Vào Home ---
        navigateToHome()

        // --- 1. Thực hiện chọn lại ngôn ngữ ---
        val homeLanguageIconMatcher = allOf(withId(R.id.iv_language), isDescendantOfA(withId(R.id.toolbar)))
        waitForView(homeLanguageIconMatcher, timeout = 60000)
        onView(homeLanguageIconMatcher).perform(click())

        // 2. Đợi màn hình chọn ngôn ngữ hiện lại
        waitForDisplayed(R.id.recycler_view)
        waitForLanguageLoaded()

        // 3. Chọn ngôn ngữ thứ 2 (Vietnamese)
        onView(withId(R.id.recycler_view))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(1, click()))

        // 4. Đợi nút xác nhận clickable và nhấn Xác nhận
        waitForClickable(R.id.frame_confirm)
        onView(withId(R.id.frame_confirm)).perform(click())

        // 5. Đợi quay lại Home
        waitForDisplayed(R.id.et_text, timeout = 60000)
    }
}
