package com.dokidevs.pholder.base

import android.os.Bundle
import android.os.Parcelable
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.viewpager.widget.PagerAdapter

/*--- BaseFragmentStatePagerAdapter ---*/
// There is a bug in FragmentStatePagerAdapter where after items are initiated, wrong fragment is recalled
// when positions are shifted. E.g. A(0), B(1), C(2) are initiated. A(0) is deleted, leaving items as B(0), C(1).
// When swiping right, position 2 is initiated, but fragment C is recalled as original C(2) is not removed
// from arrayList in memory. This override adds a check to call getItem if wrong fragment is presented.
// See detail in: http://speakman.net.nz/blog/2014/02/20/a-bug-in-and-a-fix-for-the-way-fragmentstatepageradapter-handles-fragment-restoration/
// Solution in: https://github.com/adamsp/FragmentStatePagerIssueExample
abstract class BaseFragmentStatePagerAdapter(private val fragmentManager: FragmentManager) : PagerAdapter() {

    /* parameters */
    protected val registeredFragments = SparseArray<BaseFragment>()
    protected var primaryItem: BaseFragment? = null
    private val savedStates = mutableListOf<Fragment.SavedState?>()
    private val savedFragments = mutableListOf<BaseFragment?>()
    private val savedFragmentTags = mutableListOf<String?>()
    private var fragmentTransaction: FragmentTransaction? = null

    // startUpdate
    override fun startUpdate(container: ViewGroup) {
        if (container.id == View.NO_ID) {
            throw IllegalStateException(
                "ViewPager with adapter " +
                        this +
                        " requires a view id"
            )
        }
    }

    // instantiateItem
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val newFragmentTag = getFragmentTag(position)
        if (savedFragments.size > position) {
            val savedFragment = savedFragments[position]
            if (savedFragment != null && savedFragment.tag == newFragmentTag) {
                putFragment(position, savedFragment)
                return savedFragment
            }
        }
        if (fragmentTransaction == null) {
            fragmentTransaction = fragmentManager.beginTransaction()
        }
        val newFragment = getItem(position)
        if (savedStates.size > position) {
            val savedFragmentTag = savedFragmentTags[position]
            if (savedFragmentTag == newFragmentTag) {
                val savedState = savedStates[position]
                if (savedState != null) {
                    newFragment.setInitialSavedState(savedState)
                }
            }
        }
        while (savedFragments.size <= position) {
            savedFragments.add(null)
            savedFragmentTags.add(null)
        }
        newFragment.setMenuVisibility(false)
        newFragment.userVisibleHint = false
        savedFragments[position] = newFragment
        putFragment(position, newFragment)
        fragmentTransaction!!.add(container.id, newFragment, newFragmentTag)
        return newFragment
    }

    // getFragmentTag
    abstract fun getFragmentTag(position: Int): String?

    // getItem
    abstract fun getItem(position: Int): BaseFragment

    // getItemPosition
    override fun getItemPosition(obj: Any): Int {
        // This method is only called by ViewPager.dataSetChanged() in order to repopulate fragments
        val fragment = obj as BaseFragment
        val newPosition = getItemNewPosition(fragment)
        when (newPosition) {
            POSITION_UNCHANGED -> {
                // Do nothing
            }
            POSITION_NONE -> {
                removeFragment(fragment)
            }
            else -> {
                // Replace fragment
                removeFragment(fragment)
                putFragment(newPosition, fragment)
            }
        }
        return newPosition
    }

    // getItemNewPosition
    abstract fun getItemNewPosition(fragment: BaseFragment): Int

    // putFragment
    private fun putFragment(position: Int, fragment: BaseFragment) {
        registeredFragments.put(position, fragment)
    }

    // removeFragment
    private fun removeFragment(position: Int) {
        registeredFragments.remove(position)
    }

    // removeFragment
    private fun removeFragment(fragment: BaseFragment) {
        val index = registeredFragments.indexOfValue(fragment)
        if (index >= 0) {
            registeredFragments.removeAt(index)
        }
    }

    // getFragments
    fun getFragments(): SparseArray<BaseFragment> {
        return registeredFragments
    }

    // getFragment
    fun getFragment(position: Int): BaseFragment? {
        return registeredFragments[position]
    }

    // destroyItem
    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
        val fragment = obj as BaseFragment
        if (fragmentTransaction == null) {
            fragmentTransaction = fragmentManager.beginTransaction()
        }
        while (savedStates.size <= position) {
            savedStates.add(null)
            savedFragmentTags.add(null)
        }
        savedStates[position] = if (fragment.isAdded) {
            fragmentManager.saveFragmentInstanceState(fragment)
        } else {
            null
        }
        savedFragmentTags[position] = fragment.tag
        savedFragments[position] = null
        fragmentTransaction!!.remove(fragment)
        // Remove fragment only if position and fragment match
        val registeredFragment = registeredFragments[position]
        if (fragment == registeredFragment) {
            removeFragment(position)
        }
    }

    // setPrimaryItem
    override fun setPrimaryItem(container: ViewGroup, position: Int, obj: Any) {
        val fragment = obj as BaseFragment?
        if (fragment !== primaryItem) {
            if (primaryItem != null) {
                primaryItem!!.setMenuVisibility(false)
                primaryItem!!.userVisibleHint = false
            }
            if (fragment != null) {
                fragment.setMenuVisibility(true)
                fragment.userVisibleHint = true
            }
            primaryItem = fragment
        }
    }

    // finishUpdate
    override fun finishUpdate(container: ViewGroup) {
        if (fragmentTransaction != null) {
            fragmentTransaction!!.commitNowAllowingStateLoss()
            fragmentTransaction = null
        }
    }

    // isViewFromObject
    override fun isViewFromObject(view: View, obj: Any): Boolean {
        return (obj as BaseFragment).view === view
    }

    // saveState
    override fun saveState(): Parcelable? {
        var state: Bundle? = null
        if (savedStates.size > 0) {
            state = Bundle()
            state.putParcelableArray("states", savedStates.toTypedArray())
            state.putStringArray("tags", savedFragmentTags.toTypedArray())
        }
        for (i in savedFragments.indices) {
            val savedFragment = savedFragments[i]
            if (savedFragment != null && savedFragment.isAdded) {
                if (state == null) {
                    state = Bundle()
                }
                val key = "f$i"
                fragmentManager.putFragment(state, key, savedFragment)
            }
        }
        return state
    }

    // restoreState
    override fun restoreState(state: Parcelable?, loader: ClassLoader?) {
        if (state != null) {
            val bundle = state as Bundle
            bundle.classLoader = loader
            savedFragmentTags.clear()
            val tags = bundle.getStringArray("tags")
            if (tags != null) {
                savedFragmentTags.addAll(tags)
            }
            savedStates.clear()
            val savedStateArray = bundle.getParcelableArray("states")
            if (savedStateArray != null) {
                for (i in savedStateArray.indices) {
                    savedStates.add(savedStateArray[i] as Fragment.SavedState?)
                }
            }
            savedFragments.clear()
            val keys = bundle.keySet()
            for (key in keys) {
                if (key.startsWith("f")) {
                    val index = Integer.parseInt(key.substring(1))
                    val fragment = fragmentManager.getFragment(bundle, key) as BaseFragment?
                    if (fragment != null) {
                        while (savedFragments.size <= index) {
                            savedFragments.add(null)
                        }
                        fragment.setMenuVisibility(false)
                        savedFragments[index] = fragment
                    }
                }
            }
        }
    }

}