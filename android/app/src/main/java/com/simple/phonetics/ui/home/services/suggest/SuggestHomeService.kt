package com.simple.phonetics.ui.home.services.suggest

import android.graphics.Rect
import android.view.View
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simple.autobind.annotation.AutoBind
import com.simple.core.utils.extentions.asObjectOrNull
import com.simple.coreapp.EventName
import com.simple.coreapp.ui.adapters.texts.ClickTextViewItem
import com.simple.coreapp.utils.ext.listenerOnChangeHeightStatusAndHeightNavigation
import com.simple.coreapp.utils.ext.setVisible
import com.simple.coreapp.utils.extentions.observeLaunch
import com.simple.crashlytics.logCrashlytics
import com.simple.event.listenerEvent
import com.simple.phonetic.entities.Phonetic
import com.simple.phonetics.Id
import com.simple.phonetics.ui.home.HomeFragment
import com.simple.phonetics.ui.home.services.HomeService
import com.simple.phonetics.ui.view.SelectionEdittext
import com.simple.phonetics.utils.exts.listenerLayoutChangeAsync
import com.simple.phonetics.utils.exts.submitListAwaitV2
import com.unknown.coroutines.launchCollect
import com.unknown.theme.utils.exts.colorBackground
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import org.koin.androidx.viewmodel.ext.android.viewModel


@AutoBind(HomeFragment::class)
class SuggestHomeService : HomeService {

    override fun setup(homeFragment: HomeFragment) {

        val binding = homeFragment.binding ?: return

        val suggestHomeViewModel: SuggestHomeViewModel by homeFragment.viewModel()


        binding.recSuggest.layoutManager = LinearLayoutManager(homeFragment.requireContext(), RecyclerView.HORIZONTAL, false)

        suggestHomeViewModel.theme.observe(homeFragment.viewLifecycleOwner) {

            binding.recSuggest.setBackgroundColor(it.colorBackground)
        }

        suggestHomeViewModel.viewItemList.observeLaunch(homeFragment.viewLifecycleOwner) {

            binding.recSuggest.submitListAwaitV2(it)
            binding.recSuggest.setVisible(it.isNotEmpty())
        }


        listenerEvent(homeFragment.viewLifecycleOwner.lifecycle, eventName = EventName.TEXT_VIEW_ITEM_CLICKED) {

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


        KeyboardVisibilityEvent.setEventListener(homeFragment.requireActivity(), homeFragment.viewLifecycleOwner) { isOpen ->

            suggestHomeViewModel.setKeyboardShow(isOpen)
        }

        binding.root.listenerChangeHeightKeyboardAsync().distinctUntilChanged().launchCollect(homeFragment.viewLifecycleOwner) {

            binding.recSuggest.animate().translationY(-it.toFloat()).setDuration(350).start()
        }

        binding.etText.listenerSelectionChangeAsync().distinctUntilChanged().launchCollect(homeFragment.viewLifecycleOwner) {

            suggestHomeViewModel.setText(it)
        }
    }

    private fun View.listenerChangeHeightKeyboardAsync() = channelFlow {

        var mHeightKeyboard = 0
        var mHeightStatusBar = 0

        val onChange: () -> Unit = {

            trySend(mHeightKeyboard - mHeightStatusBar)
        }

        listenerOnChangeHeightStatusAndHeightNavigation().launchCollect(this) {

            mHeightStatusBar = it.first

            onChange.invoke()
        }

        listenerLayoutChangeAsync().map {

            val r = Rect()
            getWindowVisibleDisplayFrame(r)

            val screenHeight: Int = height
            val visibleHeight = r.height()

            screenHeight - visibleHeight
        }.launchCollect(this) {

            mHeightKeyboard = it

            onChange.invoke()
        }

        awaitClose {
        }
    }.asLiveData().asFlow()

    private fun SelectionEdittext.listenerSelectionChangeAsync() = channelFlow {

        onSelectionChangedListener = object : SelectionEdittext.OnSelectionChangedListener {

            override fun onSelectionChanged(selStart: Int, selEnd: Int) {

                runCatching {

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