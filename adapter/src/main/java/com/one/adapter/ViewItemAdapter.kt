package com.one.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.one.coreapp.utils.extentions.findGenericClassBySuperClass
import java.lang.reflect.Method

abstract class ViewItemAdapter<out VI : ViewItemCloneable, out VB : ViewBinding>(
    private val onItemClick: (View, VI) -> Unit = { _, _ -> }
) {


    var adapter: BaseAsyncAdapter<*, *>? = null


    open fun getViewItemClass(): Class<ViewItemCloneable> {
        return findGenericClassBySuperClass(ViewItemCloneable::class.java)!!.java
    }


    open fun createViewItem(parent: ViewGroup, viewType: Int): VB {

        return findBinding(parent)
    }


    open fun onViewAttachedToWindow(holder: BaseBindingViewHolder<ViewBinding>, adapter: BaseAsyncAdapter<*, *>) {
        this.adapter = adapter
    }

    open fun onViewDetachedFromWindow(holder: BaseBindingViewHolder<ViewBinding>) {
        this.adapter = null
    }


    fun bindView(binding: @UnsafeVariance VB, viewType: Int, position: Int, item: @UnsafeVariance VI, payloads: MutableList<Any>) {

        bind(binding, viewType, position, item, payloads)
    }

    open fun bind(binding: @UnsafeVariance VB, viewType: Int, position: Int, item: @UnsafeVariance VI, payloads: MutableList<Any>) {
    }


    fun bindView(binding: @UnsafeVariance VB, viewType: Int, position: Int, item: @UnsafeVariance VI) {

        binding.root.setOnClickListener { view ->
            getViewItem(position)?.let { onItemClick.invoke(view, it) }
        }

        bind(binding, viewType, position, item)
    }

    open fun bind(binding: @UnsafeVariance VB, viewType: Int, position: Int, item: @UnsafeVariance VI) {
    }


    protected fun getViewItem(position: Int) = adapter?.currentList?.getOrNull(position) as? VI
}

class MultiAdapter(

    vararg adapter: ViewItemAdapter<ViewItemCloneable, ViewBinding>,

    private val onLoadMore: (() -> Unit)? = null,

    private val onViewHolderAttachedToWindow: ((BaseBindingViewHolder<*>) -> Unit)? = null,
    private val onViewHolderDetachedFromWindow: ((BaseBindingViewHolder<*>) -> Unit)? = null,
) : BaseAsyncAdapter<ViewItemCloneable, ViewBinding>() {

    val list: List<ViewItemAdapter<ViewItemCloneable, ViewBinding>> by lazy {

        val adapters = arrayListOf<ViewItemAdapter<ViewItemCloneable, ViewBinding>>()

        adapters.add(LoadingViewAdapter() as ViewItemAdapter<ViewItemCloneable, ViewBinding>)
        adapters.add(LoadMoreViewAdapter(onLoadMore) as ViewItemAdapter<ViewItemCloneable, ViewBinding>)

        adapters.addAll(adapter)

        adapters
    }

    private val typeAndAdapter: Map<Int, ViewItemAdapter<ViewItemCloneable, ViewBinding>> by lazy {

        val map = HashMap<Int, ViewItemAdapter<ViewItemCloneable, ViewBinding>>()

        list.forEachIndexed { index, viewItemAdapter ->
            map[index] = viewItemAdapter
        }

        map
    }

    private val viewItemClassAndType: Map<Class<*>, Int> by lazy {

        val map = HashMap<Class<*>, Int>()

        list.forEachIndexed { index, viewItemAdapter ->
            map[viewItemAdapter.getViewItemClass()] = index
        }

        map
    }

    override fun getItemViewType(position: Int): Int {

        return viewItemClassAndType[getItem(position).javaClass] ?: super.getItemViewType(position)
    }

    override fun createView(parent: ViewGroup, viewType: Int): ViewBinding {

        return typeAndAdapter[viewType]!!.createViewItem(parent, viewType)
    }


    override fun onViewAttachedToWindow(holder: BaseBindingViewHolder<ViewBinding>) {

        typeAndAdapter[holder.viewType]?.onViewAttachedToWindow(holder, this)

        onViewHolderAttachedToWindow?.invoke(holder)
    }

    override fun onViewDetachedFromWindow(holder: BaseBindingViewHolder<ViewBinding>) {

        typeAndAdapter[holder.viewType]?.onViewDetachedFromWindow(holder)

        onViewHolderDetachedFromWindow?.invoke(holder)
    }


    override fun bind(binding: ViewBinding, viewType: Int, position: Int, item: ViewItemCloneable, payloads: MutableList<Any>) {

        typeAndAdapter[viewType]?.bindView(binding, viewType, position, item, payloads)
    }

    override fun bind(binding: ViewBinding, viewType: Int, position: Int, item: ViewItemCloneable) {

        typeAndAdapter[viewType]?.bindView(binding, viewType, position, item)
    }
}


private fun <VB : ViewBinding> Any.findBinding(parent: ViewGroup): VB {

    return findBinding(LayoutInflater.from(parent.context), parent) as VB
}


private fun <VB : ViewBinding> Any.findBinding(inflater: LayoutInflater, container: ViewGroup? = null, attackToParent: Boolean = false): VB {

    val method: Method = findGenericClassBySuperClass(ViewBinding::class.java)!!.java.getDeclaredMethod("inflate", LayoutInflater::class.java, ViewGroup::class.java, Boolean::class.java)
    method.isAccessible = true

    return method.invoke(null, inflater, container, attackToParent) as VB
}


