package com.simple.phonetics

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import com.simple.phonetics.ui.main.MainActivity
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.hamcrest.TypeSafeMatcher
import org.junit.Before
import java.util.regex.Pattern

/**
 * Base class cho các UI Test, chứa các hàm helper dùng chung.
 */
open class BaseUiTest {

    protected lateinit var device: UiDevice

    @Before
    fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        device = UiDevice.getInstance(instrumentation)
    }

    protected fun handlePermissionDialog(): Boolean {
        val allowTextPattern = Pattern.compile("(?i)Allow|Cho phép|While using the app|Trong khi dùng ứng dụng|Only this time|Chỉ lần này")
        val permissionResIdPattern = Pattern.compile(".*permission_allow.*|.*permission_positive_button.*|.*android:id/button1.*")

        val button = device.findObject(By.res(permissionResIdPattern))
            ?: device.findObject(By.text(allowTextPattern))

        return if (button != null) {
            button.click()
            Thread.sleep(1000)
            true
        } else {
            false
        }
    }

    protected fun waitForDisplayed(viewId: Int, timeout: Long = 1000) {
        waitForView(withId(viewId), timeout)
    }

    protected fun waitForView(matcher: Matcher<View>, timeout: Long = 1000) {
        val startTime = System.currentTimeMillis()
        val endTime = startTime + timeout
        while (System.currentTimeMillis() < endTime) {
            try {
                onView(matcher).check(matches(isDisplayed()))
                return
            } catch (e: Throwable) {
                Thread.sleep(500)
            }
        }
        onView(matcher).check(matches(isDisplayed()))
    }

    protected fun waitForText(text: String, timeout: Long = 20000) {
        waitForView(withText(containsString(text)), timeout)
    }

    protected fun waitForLanguageLoaded(timeout: Long = 20000) {
        val startTime = System.currentTimeMillis()
        val endTime = startTime + timeout
        while (System.currentTimeMillis() < endTime) {
            try {
                onView(atPosition(R.id.recycler_view, 0))
                    .check(matches(hasDescendant(allOf(withId(R.id.tv_name), isAssignableFrom(TextView::class.java)))))
                return
            } catch (e: Throwable) {
                Thread.sleep(500)
            }
        }
        onView(atPosition(R.id.recycler_view, 0))
            .check(matches(hasDescendant(allOf(withId(R.id.tv_name), isAssignableFrom(TextView::class.java)))))
    }

    protected fun waitForClickable(viewId: Int, timeout: Long = 10000) {
        val startTime = System.currentTimeMillis()
        val endTime = startTime + timeout
        while (System.currentTimeMillis() < endTime) {
            try {
                onView(withId(viewId)).check(matches(isClickable()))
                return
            } catch (e: Throwable) {
                Thread.sleep(200)
            }
        }
        onView(withId(viewId)).check(matches(isClickable()))
    }

    protected fun waitForGone(viewId: Int, timeout: Long = 10 * 1000) {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeout) {
            try {
                onView(withId(viewId)).check(doesNotExist())
                return // View đã không còn trong hierarchy
            } catch (e: AssertionError) {
                // View vẫn còn tồn tại, thử lại
                Thread.sleep(500)
            }
        }
        throw AssertionError("View with id $viewId vẫn còn tồn tại sau ${timeout}ms")
    }

    protected fun atPosition(recyclerViewId: Int, position: Int): Matcher<View> {
        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("has item at position $position in recycler view with id: $recyclerViewId")
            }

            override fun matchesSafely(view: View): Boolean {
                val parent = view.parent as? ViewGroup ?: return false
                if (parent !is RecyclerView || parent.id != recyclerViewId) return false
                val viewHolder = parent.findViewHolderForAdapterPosition(position)
                return viewHolder != null && viewHolder.itemView == view
            }
        }
    }

    /**
     * Đưa app về màn Home.
     * - Nếu đang ở Home rồi (toolbar + ô nhập et_text đã hiển thị) → return ngay, không làm gì.
     * - Nếu đang ở màn chọn ngôn ngữ → chọn ngôn ngữ đầu tiên rồi confirm.
     */
    protected fun navigateToHome() = logFunction("navigateToHome"){
        ActivityScenario.launch(MainActivity::class.java)

        if (isAtHome()) {
            // App đã sẵn sàng ở Home, quét lại dialog lần nữa cho chắc
            waitDialogSuggestShowAndDismiss()
            return@logFunction
        }

        waitForDisplayed(R.id.recycler_view)
        waitForLanguageLoaded()

        onView(withId(R.id.recycler_view)).perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        waitForClickable(R.id.frame_confirm)
        onView(withId(R.id.frame_confirm)).perform(click())
        waitForGone(R.id.btn_confirm, timeout = 3 * 60 * 1000)

        waitDialogSuggestShowAndDismiss()
        waitDialogPermissionShowAndDismiss()

        waitForView(allOf(withId(R.id.tv_title), isDescendantOfA(withId(R.id.toolbar))), timeout = 60000)
        waitForDisplayed(R.id.et_text)
    }

    /**
     * Trả về true nếu UI hiện tại đang là màn Home.
     * Dấu hiệu: có tv_title trong toolbar VÀ có ô nhập et_text.
     */
    private fun isAtHome(): Boolean {
        return try {
            onView(allOf(withId(R.id.tv_title), isDescendantOfA(withId(R.id.toolbar))))
                .check(matches(isDisplayed()))
            onView(withId(R.id.et_text)).check(matches(isDisplayed()))
            true
        } catch (_: Throwable) {
            false
        }
    }

    protected fun waitDialogSuggestShowAndDismiss() = logFunction("waitDialogSuggestShowAndDismiss")  {
        Thread.sleep(1000)
        waitForView(By.textContains("Later")).also {
            it.click()
        }

        Thread.sleep(1000)
        waitForView(By.textContains("Late")).also {
            it.click()
        }
    }

    protected fun waitDialogPermissionShowAndDismiss() = logFunction("waitDialogPermissionShowAndDismiss")  {
        Thread.sleep(1000)
        waitForView(By.res("com.android.permissioncontroller", "permission_allow_button")).also {
            it.click()
            device.waitForIdle(1000)
        }
    }

    private fun waitForView(selector: BySelector, timeout: Long = 10 * 1000): UiObject2 = logFunction("waitForView") {
        val start = System.currentTimeMillis()
        var view: UiObject2? = null

        while (System.currentTimeMillis() - start < timeout) {
            view = device.findObject(selector)
            if (view != null) break
            Thread.sleep(200)
        }

        view ?: error("View not found: $selector")
    }

    private fun <T> logFunction(name: String, action: () -> T): T = try {
        Log.d("tuanha", "$name: start")
        action.invoke()
    } finally {
        Log.d("tuanha", "$name: end")
    }
}
