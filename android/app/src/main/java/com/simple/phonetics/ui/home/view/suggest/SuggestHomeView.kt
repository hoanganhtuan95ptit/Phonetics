package com.simple.phonetics.ui.home.view.suggest

import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.auto.service.AutoService
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.coreapp.ui.adapters.texts.ClickTextViewItem
import com.simple.coreapp.utils.ext.launchCollect
import com.simple.coreapp.utils.ext.listenerOnChangeHeightStatusAndHeightNavigation
import com.simple.coreapp.utils.ext.setVisible
import com.simple.coreapp.utils.extentions.observeLaunch
import com.simple.crashlytics.logCrashlytics
import com.simple.event.listenerEvent
import com.simple.phonetics.Id
import com.simple.phonetics.entities.Phonetic
import com.simple.phonetics.ui.home.HomeFragment
import com.simple.phonetics.ui.home.view.HomeView
import com.simple.phonetics.ui.view.SelectionEdittext
import com.simple.phonetics.utils.exts.submitListAwaitV2
import com.unknown.theme.utils.exts.colorBackground
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import org.koin.androidx.viewmodel.ext.android.viewModel


@AutoService(HomeView::class)
class SuggestHomeView : HomeView {

    override fun setup(fragment: HomeFragment) {

        val binding = fragment.binding ?: return


        val suggestHomeViewModel: SuggestHomeViewModel by fragment.viewModel()


        binding.recSuggest.layoutManager = LinearLayoutManager(fragment.requireContext(), RecyclerView.HORIZONTAL, false)

        suggestHomeViewModel.theme.observe(fragment.viewLifecycleOwner) {

            binding.recSuggest.setBackgroundColor(it.colorBackground)
        }

        suggestHomeViewModel.viewItemList.observeLaunch(fragment.viewLifecycleOwner) {

            binding.recSuggest.submitListAwaitV2(it)
            binding.recSuggest.setVisible(it.isNotEmpty())
        }

        listenerEvent(fragment.viewLifecycleOwner.lifecycle, eventName = com.simple.coreapp.EventName.TEXT_VIEW_ITEM_CLICKED) {

            val (view, viewItem) = it.asObjectOrNull<Pair<View, ClickTextViewItem>>() ?: return@listenerEvent

            if (!viewItem.id.startsWith(Id.SUGGEST)) {

                return@listenerEvent
            }

            runCatching {

                var end: Int = binding.etText.selectionEnd
                var start: Int = binding.etText.selectionStart

                if (end < 0) end = 0
                if (start < 0) start = 0

                val spaceIndex = binding.etText.text.toString().lastIndexOf(' ', start - 1)

                val wordStart = if (spaceIndex == -1) {
                    0
                } else {
                    spaceIndex + 1
                }

                binding.etText.text?.replace(wordStart, Math.max(start, end), viewItem.data.asObjectOrNull<Phonetic>()?.text)
            }.getOrElse {

                logCrashlytics("SuggestHomeView", it)
            }
        }


        KeyboardVisibilityEvent.setEventListener(fragment.requireActivity(), fragment.viewLifecycleOwner) { isOpen ->

            suggestHomeViewModel.setKeyboardShow(isOpen)
        }

        binding.root.listenerOnChangeHeightKeyboard().distinctUntilChanged().launchCollect(fragment.viewLifecycleOwner) {

            binding.recSuggest.animate().translationY(-it.toFloat()).setDuration(350).start()
        }

        binding.etText.listenerOnSelectionChanged().distinctUntilChanged().launchCollect(fragment.viewLifecycleOwner) {

            suggestHomeViewModel.setText(it)
        }
    }

    private fun View.listenerOnChangeHeightKeyboard() = channelFlow {

        var mHeightKeyboard = 0
        var mHeightStatusBar = 0

        val onChange: () -> Unit = {

            trySend(mHeightKeyboard - mHeightStatusBar)
        }

        listenerOnChangeHeightStatusAndHeightNavigation().launchCollect(this) {

            mHeightStatusBar = it.first

            onChange.invoke()
        }

        val onGlobalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {

            val r = Rect()
            getWindowVisibleDisplayFrame(r)

            val screenHeight: Int = height
            val visibleHeight = r.height()

            mHeightKeyboard = screenHeight - visibleHeight

            onChange.invoke()
        }


        viewTreeObserver.addOnGlobalLayoutListener(onGlobalLayoutListener)

        awaitClose {

            viewTreeObserver.removeOnGlobalLayoutListener(onGlobalLayoutListener)
        }
    }

    private fun SelectionEdittext.listenerOnSelectionChanged() = channelFlow<String> {

        onSelectionChangedListener = object : SelectionEdittext.OnSelectionChangedListener {

            override fun onSelectionChanged(selStart: Int, selEnd: Int) {

                kotlin.runCatching {

                    val end: Int = selEnd
                    var start: Int = selStart

                    val text = getText().toString()

                    if (start > text.length) start = text.length

                    val spaceIndex = text.lastIndexOf(' ', start - 1)

                    val wordStart = if (spaceIndex == -1) {
                        0
                    } else {
                        spaceIndex + 1
                    }

                    val selectedWord = text.substring(wordStart, start)

                    trySend(selectedWord)
                }.getOrElse {

                    logCrashlytics("SuggestHomeView", it)
                }
            }
        }

        awaitClose {

            onSelectionChangedListener = null
        }
    }
}