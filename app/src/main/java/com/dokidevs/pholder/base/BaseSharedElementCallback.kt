package com.dokidevs.pholder.base

import android.view.View
import androidx.core.app.SharedElementCallback
import androidx.core.view.ViewCompat

/*--- BaseSharedElementCallback ---*/
// Copied from https://github.com/andremion/Louvre/blob/development/louvre/src/main/java/com/andremion/louvre/util/transition/MediaSharedElementCallback.java
// This class enables remapping of view after we have information.
class BaseSharedElementCallback : SharedElementCallback() {

    /* parameters */
    private var sharedElements = HashSet<View>()

    // setSharedElements
    fun setSharedElements(sharedElements: List<View>) {
        this.sharedElements.clear()
        this.sharedElements.addAll(sharedElements)
    }

    // onMapSharedElements
    override fun onMapSharedElements(names: MutableList<String>, sharedElements: MutableMap<String, View>) {
        if (this.sharedElements.isNotEmpty()) {
            removeObsoleteElements(names, sharedElements, mapObsoleteElements(names))
            this.sharedElements.forEach { sharedElementView ->
                val transitionName = ViewCompat.getTransitionName(sharedElementView)
                if (transitionName != null) {
                    names.add(transitionName)
                    sharedElements[transitionName] = sharedElementView
                }
            }
        }
    }

    // mapObsoleteElements
    private fun mapObsoleteElements(names: List<String>): List<String> {
        val elementsToRemove = ArrayList<String>(names.size)
        for (name in names) {
            if (!name.startsWith("android")) {
                elementsToRemove.add(name)
            }
        }
        return elementsToRemove
    }

    // removeObsoleteElements
    private fun removeObsoleteElements(
        names: MutableList<String>,
        sharedElements: MutableMap<String, View>,
        elementsToRemove: List<String>
    ) {
        if (elementsToRemove.isNotEmpty()) {
            names.removeAll(elementsToRemove)
            for (elementToRemove in elementsToRemove) {
                sharedElements.remove(elementToRemove)
            }
        }
    }

}