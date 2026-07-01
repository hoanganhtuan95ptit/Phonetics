package com.simple.phonetics

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anyOf
import org.hamcrest.Matchers.containsString
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI Test cho màn hình Home.
 */
@RunWith(AndroidJUnit4::class)
class FLowTest : BaseUiTest() {

    fun awaitHomeReady(){
        // --- Bước chuẩn bị: Vào Home ---
        navigateToHome()

        // --- 1. Kiểm tra Toolbar & Tiêu đề ---
        // Sử dụng Matcher cụ thể hơn để tránh trùng lặp ID (ví dụ với dialog hoặc màn hình trước)
        val homeTitleMatcher = allOf(withId(R.id.tv_title), isDescendantOfA(withId(R.id.toolbar)))
        waitForView(homeTitleMatcher)
        onView(homeTitleMatcher).check(matches(withText(containsString("phonetics"))))
        waitForDisplayed(R.id.iv_language)

        // --- 2. Kiểm tra ô nhập liệu & Các icon chức năng ---
        waitForDisplayed(R.id.et_text)
        waitForDisplayed(R.id.iv_microphone)
        waitForDisplayed(R.id.iv_gallery)
        waitForDisplayed(R.id.iv_camera)

        // --- 3. Kiểm tra thanh Filter (Chips) ---
        waitForDisplayed(R.id.rec_filter)
        // Kiểm tra xem có chip ngôn ngữ đang hiển thị (ví dụ: English hoặc Tiếng Việt)
        waitForView(
            allOf(
                isDescendantOfA(withId(R.id.rec_filter)),
                anyOf(withText(containsString("English")), withText(containsString("Tiếng")))
            )
        )

        // --- 4. Kiểm tra Danh sách chính (RecyclerView) & Các thành phần con ---
        // Phải kiểm tra RecyclerView hiển thị trước khi tương tác với các item bên trong
        onView(withId(R.id.recycler_view)).check(matches(isDisplayed()))

        // 4.1. Kiểm tra Campaign (Banner xanh)
        waitForText("Join the Community")
        onView(withId(R.id.recycler_view))
            .perform(RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(hasDescendant(withText(containsString("Join the Community")))))
        onView(withText(containsString("Join the Community"))).check(matches(isDisplayed()))

        // 4.2. Kiểm tra Section "Game Ipa"
        waitForText("Game Ipa")
        onView(withId(R.id.recycler_view))
            .perform(RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(hasDescendant(withText(containsString("Game Ipa")))))
        onView(withText(containsString("Game Ipa"))).check(matches(isDisplayed()))
        onView(withText("Play game")).check(matches(isDisplayed()))

        // 4.3. Kiểm tra Section "Ipa:"
        waitForText("Ipa:")
        onView(withId(R.id.recycler_view))
            .perform(RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(hasDescendant(withText(containsString("Ipa:")))))
        onView(withText(containsString("Ipa:"))).check(matches(isDisplayed()))

        // 4.4. Kiểm tra nút "View all" ở cuối danh sách Ipa
        onView(withText("View all")).check(matches(isDisplayed()))
    }

    @Test
    fun verifyHomeLayoutAndConfigurationSelection() {
        awaitHomeReady()

        // --- 5. Kiểm tra chuyển cấu hình trong rec_filter ---

        // 5.1. Thay đổi Phonetic
        // Click vào chip đầu tiên trong thanh filter để mở bottom sheet Config
        onView(withId(R.id.rec_filter))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
        waitForDisplayed(R.id.v_anchor)

        // Chọn "English - UK"
        onView(withText("English - UK")).perform(click())
        device.pressBack()
        // Kiểm tra rec_filter cập nhật đúng Phonetic vừa chọn
        onView(allOf(isDescendantOfA(withId(R.id.rec_filter)), withText("English - UK"))).check(matches(isDisplayed()))

        // Mở lại Config để chọn "English - US"
        onView(withId(R.id.rec_filter))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
        waitForDisplayed(R.id.v_anchor)
        onView(withText("English - US")).perform(click())
        device.pressBack()
        onView(allOf(isDescendantOfA(withId(R.id.rec_filter)), withText("English - US"))).check(matches(isDisplayed()))


        // 5.2. Thay đổi Voice
        // Mở Config (có thể click vào chip Voice trong rec_filter, thường là chip cuối cùng)
        // Để chắc chắn, ta cứ mở từ chip đầu tiên
        onView(withId(R.id.rec_filter))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
        waitForDisplayed(R.id.v_anchor)

        // Cuộn xuống để thấy section Voice và chọn "Voice 2"
        onView(allOf(withId(R.id.recycler_view), hasSibling(withId(R.id.v_anchor))))
            .perform(RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(hasDescendant(withText("Voice 2"))))
        onView(withText("Voice 2")).perform(click())
        device.pressBack()

        // Kiểm tra rec_filter cập nhật Voice vừa chọn
        // Vì rec_filter nằm ngang nên cần scroll để tìm thấy chip Voice 2 vừa chọn
        onView(withId(R.id.rec_filter))
            .perform(RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(hasDescendant(withText(containsString("Voice 2")))))
        onView(allOf(isDescendantOfA(withId(R.id.rec_filter)), withText(containsString("Voice 2")))).check(matches(isDisplayed()))
    }
}
