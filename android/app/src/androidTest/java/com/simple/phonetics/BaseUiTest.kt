package com.simple.phonetics

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.hamcrest.TypeSafeMatcher
import org.junit.Before

/**
 * Base class cho các UI Test, chứa các hàm helper dùng chung.
 */
open class BaseUiTest {

    protected lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiDevice.getInstance(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation())
    }

    protected fun handlePermissionDialog(): Boolean {
        val allowButton = device.findObject(By.text("Allow")) ?: device.findObject(By.text("ALLOW"))
        return if (allowButton != null) {
            allowButton.click()
            Thread.sleep(1000)
            true
        } else {
            false
        }
    }

    protected fun waitForDisplayed(viewId: Int, timeout: Long = 20000) {
        waitForView(withId(viewId), timeout)
    }

    protected fun waitForView(matcher: Matcher<View>, timeout: Long = 20000) {
        val startTime = System.currentTimeMillis()
        val endTime = startTime + timeout
        while (System.currentTimeMillis() < endTime) {
            try {
                onView(matcher).check(matches(isDisplayed()))
                return
            } catch (e: Throwable) {
                val handledPermission = handlePermissionDialog()
                val handledAppDialog = dismissAppDialogs(waitTimeout = 1000)
                if (!handledPermission && !handledAppDialog) {
                    Thread.sleep(500)
                }
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
                handlePermissionDialog()
                dismissAppDialogs(waitTimeout = 1000)
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
                handlePermissionDialog()
                dismissAppDialogs(waitTimeout = 1000)
                Thread.sleep(200)
            }
        }
        onView(withId(viewId)).check(matches(isClickable()))
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

    protected fun navigateToHome() {
        waitForDisplayed(R.id.recycler_view)
        waitForLanguageLoaded()

        onView(withId(R.id.recycler_view)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click())
        )
        waitForClickable(R.id.frame_confirm)
        onView(withId(R.id.frame_confirm)).perform(click())

        waitForView(allOf(withId(R.id.tv_title), isDescendantOfA(withId(R.id.toolbar))), timeout = 60000)
        dismissAppDialogs(waitTimeout = 3000)
        waitForDisplayed(R.id.et_text)
    }

    protected fun dismissAppDialogs(waitTimeout: Long = 5000): Boolean {
        val dialogButtons = listOf("Later", "Late")
        var handled = false
        for (text in dialogButtons) {
            val button = device.wait(Until.findObject(By.textContains(text)), waitTimeout)
            if (button != null) {
                button.click()
                device.wait(Until.gone(By.textContains(text)), 2000)
                handled = true
            }
        }
        return handled
    }
}
